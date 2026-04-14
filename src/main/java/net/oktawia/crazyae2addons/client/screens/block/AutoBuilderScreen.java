package net.oktawia.crazyae2addons.client.screens.block;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.AutoBuilderMenu;
import net.oktawia.crazyae2addons.client.misc.IconButton;

import java.util.regex.Pattern;

public class AutoBuilderScreen<C extends AutoBuilderMenu> extends UpgradeableScreen<C> {

    private final AETextField xlabel;
    private final AETextField ylabel;
    private final AETextField zlabel;
    private final AECheckbox skipMissing;
    private boolean initialized = false;
    private IconButton previewBtn;

    private ItemStack missingIcon = ItemStack.EMPTY;

    private static final int MISSING_ICON_X = 130;
    private static final int MISSING_ICON_Y = 2;

    private static final Pattern AMOUNT_PREFIX =
            Pattern.compile("^\\s*([0-9][0-9_.,\\skKmMbBtT]*)\\s*[x\u00d7]\\s+", Pattern.CASE_INSENSITIVE);

    public AutoBuilderScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        skipMissing = new AECheckbox(0, 0, 0, 0, style, Component.empty());
        skipMissing.setTooltip(Tooltip.create(Component.translatable(LangDefs.SKIP_MISSING_TOOLTIP.getTranslationKey())));
        skipMissing.setChangeListener(() -> getMenu().updateMissing(skipMissing.isSelected()));

        var front = new IconButton(Icon.ARROW_UP, btn -> changeForward(1));
        front.setTooltip(Tooltip.create(Component.translatable(LangDefs.MOVE_FORWARD.getTranslationKey())));
        var back = new IconButton(Icon.ARROW_DOWN, btn -> changeForward(-1));
        back.setTooltip(Tooltip.create(Component.translatable(LangDefs.MOVE_BACKWARD.getTranslationKey())));

        var right = new IconButton(Icon.ARROW_UP, btn -> changeRight(1));
        right.setTooltip(Tooltip.create(Component.translatable(LangDefs.MOVE_RIGHT.getTranslationKey())));
        var left = new IconButton(Icon.ARROW_DOWN, btn -> changeRight(-1));
        left.setTooltip(Tooltip.create(Component.translatable(LangDefs.MOVE_LEFT.getTranslationKey())));

        var up = new IconButton(Icon.ARROW_UP, btn -> changey(1));
        up.setTooltip(Tooltip.create(Component.translatable(LangDefs.MOVE_UP.getTranslationKey())));
        var down = new IconButton(Icon.ARROW_DOWN, btn -> changey(-1));
        down.setTooltip(Tooltip.create(Component.translatable(LangDefs.MOVE_DOWN.getTranslationKey())));

        this.xlabel = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.ylabel = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.zlabel = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        xlabel.setBordered(false);
        ylabel.setBordered(false);
        zlabel.setBordered(false);
        this.widgets.add("skipmissing", skipMissing);
        this.widgets.add("front", front);
        this.widgets.add("back", back);
        this.widgets.add("right", right);
        this.widgets.add("left", left);
        this.widgets.add("up", up);
        this.widgets.add("down", down);
        this.widgets.add("xl", xlabel);
        this.widgets.add("yl", ylabel);
        this.widgets.add("zl", zlabel);
        this.previewBtn = new IconButton(Icon.ENTER, btn -> getMenu().togglePreview());
        this.widgets.add("preview", this.previewBtn);

        this.addRenderableOnly((gg, mouseX, mouseY, partialTicks) -> {
            if (!missingIcon.isEmpty()) {
                int x = leftPos + MISSING_ICON_X;
                int y = topPos + MISSING_ICON_Y;
                gg.renderItem(missingIcon, x, y);
                if (getMenu().getHost().getMissingItemStack() != null) {
                    setTextContent("missing", Component.translatable(LangDefs.MISSING.getTranslationKey()));
                    gg.renderItemDecorations(font, missingIcon, x, y, String.valueOf(getMenu().getHost().getMissingItemAmount()));
                } else {
                    setTextContent("missing", Component.empty());
                }
            }
        });
    }

    private boolean isMouseOverMissingIcon(int mouseX, int mouseY) {
        int x = leftPos + MISSING_ICON_X;
        int y = topPos + MISSING_ICON_Y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    @Override
    protected void renderTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        if (!missingIcon.isEmpty() && isMouseOverMissingIcon(mouseX, mouseY)) {
            gg.renderTooltip(this.font, missingIcon, mouseX, mouseY);
            return;
        }
        super.renderTooltip(gg, mouseX, mouseY);
    }

    private void changeRight(int i) {
        getMenu().xax += i;
        getMenu().syncOffset();
        this.xlabel.setValue(String.valueOf(getMenu().getHost().offset.getX()));
    }

    private void changeForward(int i) {
        getMenu().zax += i;
        getMenu().syncOffset();
        this.zlabel.setValue(String.valueOf(getMenu().zax));
    }

    private void changey(int i) {
        getMenu().yax += i;
        getMenu().syncOffset();
        this.ylabel.setValue(String.valueOf(getMenu().yax));
    }

    @Override
    public void updateBeforeRender() {
        super.updateBeforeRender();

        missingIcon = getMenu().getHost().getMissingItemStack();

        String e = LangDefs.ENERGY_NEEDED.getEnglishText() + String.format("%, .0f AE", getMenu().getHost().requiredEnergyAE).replace('\u00A0', ' ');
        this.setTextContent("energy", Component.literal(e));

        boolean on = getMenu().getHost().isPreviewEnabled();
        this.previewBtn.setTooltip(Tooltip.create(
                Component.translatable(on ? LangDefs.HIDE_PREVIEW.getTranslationKey() : LangDefs.SHOW_PREVIEW.getTranslationKey())
        ));

        xlabel.setValue(String.valueOf(getMenu().xax));
        ylabel.setValue(String.valueOf(getMenu().yax));
        zlabel.setValue(String.valueOf(getMenu().zax));

        if (!this.initialized) {
            this.skipMissing.setSelected(getMenu().getHost().skipEmpty);
            this.skipMissing.active = !getMenu().skipEmptyLocked;
            initialized = true;
        }
    }
}
