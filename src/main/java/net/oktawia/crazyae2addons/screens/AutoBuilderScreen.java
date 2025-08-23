package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.menus.AutoBuilderMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoBuilderScreen<C extends AutoBuilderMenu> extends UpgradeableScreen<C> {

    private final AETextField xlabel;
    private final AETextField ylabel;
    private final AETextField zlabel;
    private final AECheckbox skipMissing;
    private boolean initialized = false;
    private IconButton previewBtn;

    private ItemStack missingIcon = ItemStack.EMPTY;
    private String missingCountText = "";
    private boolean parsedOk = false;
    private String lastMissingString = null;

    private static final int MISSING_ICON_X = 108;
    private static final int MISSING_ICON_Y = 12;

    private static final Pattern AMOUNT_PREFIX =
            Pattern.compile("^\\s*([0-9][0-9_.,\\skKmMbBtT]*)\\s*[x×]\\s+", Pattern.CASE_INSENSITIVE);

    public AutoBuilderScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        skipMissing = new AECheckbox(0,0,0,0, style, Component.literal("Skip"));
        skipMissing.setTooltip(Tooltip.create(Component.literal("Start building even if not all blocks are available")));
        skipMissing.setChangeListener(() -> getMenu().updateMissing(skipMissing.isSelected()));

        var front = new IconButton(Icon.ARROW_UP, btn -> changeForward(1));
        front.setTooltip(Tooltip.create(Component.literal("Add 1 offset to FRONT")));
        var back = new IconButton(Icon.ARROW_DOWN, btn -> changeForward(-1));
        back.setTooltip(Tooltip.create(Component.literal("Add 1 offset to BACK")));

        var right = new IconButton(Icon.ARROW_UP, btn -> changeRight(1));
        right.setTooltip(Tooltip.create(Component.literal("Add 1 offset to RIGHT")));
        var left = new IconButton(Icon.ARROW_DOWN, btn -> changeRight(-1));
        left.setTooltip(Tooltip.create(Component.literal("Add 1 offset to LEFT")));

        var up = new IconButton(Icon.ARROW_UP, btn -> changey(1));
        up.setTooltip(Tooltip.create(Component.literal("Add 1 offset UP")));
        var down = new IconButton(Icon.ARROW_DOWN, btn -> changey(-1));
        down.setTooltip(Tooltip.create(Component.literal("Add 1 offset DOWN")));

        this.xlabel = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.ylabel = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.zlabel = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        xlabel.setBordered(false);
        ylabel.setBordered(false);
        zlabel.setBordered(false);
        this.widgets.add("skipmissing", skipMissing);
        this.widgets.add("e", front);
        this.widgets.add("w", back);
        this.widgets.add("n", right);
        this.widgets.add("s", left);
        this.widgets.add("u", up);
        this.widgets.add("d", down);
        this.widgets.add("xl", xlabel);
        this.widgets.add("yl", ylabel);
        this.widgets.add("zl", zlabel);
        this.previewBtn = new IconButton(Icon.ENTER, btn -> {
            getMenu().togglePreview();
        });

        this.widgets.add("preview", this.previewBtn);

        this.addRenderableOnly((gg, mouseX, mouseY, partialTicks) -> {
            if (parsedOk && !missingIcon.isEmpty()) {
                int x = leftPos + MISSING_ICON_X;
                int y = topPos + MISSING_ICON_Y;
                gg.renderItem(missingIcon, x, y);
                if (!missingCountText.isEmpty()) {
                    gg.renderItemDecorations(font, missingIcon, x, y, missingCountText);
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
    protected void renderTooltip(net.minecraft.client.gui.GuiGraphics gg, int mouseX, int mouseY) {
        if (parsedOk && !missingIcon.isEmpty() && isMouseOverMissingIcon(mouseX, mouseY)) {
            gg.renderTooltip(this.font, missingIcon, mouseX, mouseY);
            return;
        }
        super.renderTooltip(gg, mouseX, mouseY);
    }

    private void changeRight(int i) {
        getMenu().xax += i;
        getMenu().syncOffset();
        this.xlabel.setValue(String.valueOf(getMenu().xax));
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
    public void updateBeforeRender(){
        super.updateBeforeRender();

        String s = getMenu().missingItem;

        if (!Objects.equals(s, lastMissingString)) {
            lastMissingString = s;
            parseMissing(s);
        }

        String e = getMenu().energyNeededText;
        if (e == null) e = "";
        this.setTextContent("energy", Component.literal(e));

        boolean on = getMenu().preview;
        this.previewBtn.setTooltip(Tooltip.create(
                Component.literal(on ? "Hide preview" : "Show preview")
        ));

        if (parsedOk && !missingIcon.isEmpty()) {
            this.setTextContent("missing", Component.empty());
        } else {
            this.setTextContent("missing",
                    !Objects.equals(s, "0 Air") && s != null && !s.isEmpty()
                            ? Component.literal(s)
                            : Component.literal("nothing"));
        }

        if (!this.initialized){
            xlabel.setValue(String.valueOf(getMenu().xax));
            ylabel.setValue(String.valueOf(getMenu().yax));
            zlabel.setValue(String.valueOf(getMenu().zax));
            this.skipMissing.setSelected(getMenu().skipEmpty);
            this.previewBtn.setTooltip(Tooltip.create(
                    Component.literal(getMenu().preview ? "Hide preview" : "Show preview")
            ));
            initialized = true;
        }
    }

    private void parseMissing(String s) {
        parsedOk = false;
        missingIcon = ItemStack.EMPTY;
        missingCountText = "";

        if (s == null || s.isEmpty() || "0 Air".equalsIgnoreCase(s) || "nothing".equalsIgnoreCase(s)) return;

        s = s.replace('\u00A0', ' ').trim();
        String rest = s;

        Matcher mAmt = AMOUNT_PREFIX.matcher(s);
        if (mAmt.find()) {
            missingCountText = mAmt.group(1).trim();
            rest = s.substring(mAmt.end()).trim();
        }

        ResourceLocation rl = tryExtractRL(firstWord(rest));

        if (rl == null) rl = scanForRL(rest);
        if (rl == null) rl = scanForRL(s);
        if (rl == null) return;

        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == Items.AIR) {
            item = BuiltInRegistries.ITEM.get(rl);
        }
        if (item == Items.AIR) {
            var block = ForgeRegistries.BLOCKS.getValue(rl);
            if (block == null || block == Blocks.AIR) {
                block = BuiltInRegistries.BLOCK.get(rl);
            }
            if (block != Blocks.AIR) {
                item = block.asItem();
            }
        }
        if (item == Items.AIR) return;

        missingIcon = new ItemStack(item);
        parsedOk = true;
    }

    private static String firstWord(String txt) {
        if (txt == null) return "";
        int i = 0, n = txt.length();
        while (i < n && Character.isWhitespace(txt.charAt(i))) i++;
        int j = i;
        while (j < n && !Character.isWhitespace(txt.charAt(j))) j++;
        return txt.substring(i, j);
    }

    private static ResourceLocation tryExtractRL(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String t = sanitizeRL(raw);
        if (!t.contains(":")) return null;
        return ResourceLocation.tryParse(t);
    }

    private static ResourceLocation scanForRL(String txt) {
        if (txt == null) return null;
        int idx = txt.indexOf(':');
        if (idx < 0) return null;

        java.util.function.IntPredicate ok = c ->
                Character.isLetterOrDigit(c) ||
                        c == '_' || c == '-' || c == '.' || c == '/' || c == ':';

        int l = idx - 1;
        int r = idx + 1;
        while (l >= 0 && ok.test(txt.charAt(l))) l--;
        while (r < txt.length() && ok.test(txt.charAt(r))) r++;

        String candidate = txt.substring(l + 1, r);
        return tryExtractRL(candidate);
    }

    private static String sanitizeRL(String raw) {
        String t = raw.trim();
        int start = 0, end = t.length();
        while (start < end && !isRlChar(t.charAt(start))) start++;
        while (end > start && !isRlChar(t.charAt(end - 1))) end--;
        t = t.substring(start, end);
        while (!t.isEmpty()) {
            char c = t.charAt(t.length() - 1);
            if (isRlChar(c)) break;
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    private static boolean isRlChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == '/' || c == ':';
    }
}
