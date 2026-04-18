package net.oktawia.crazyae2addons.client.screens.block.penrose;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.client.misc.BlackHoleWidget;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.block.penrose.PenroseControllerMenu;

public class PenroseControllerScreen<C extends PenroseControllerMenu> extends AEBaseScreen<C> {

    private final BlackHoleWidget bhWidget;

    public PenroseControllerScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.bhWidget = new BlackHoleWidget(menu);
        this.widgets.add("bh", this.bhWidget);

        var startBtn = new IconButton(Icon.ENTER, btn -> getMenu().startBlackHole());
        startBtn.setTooltip(Tooltip.create(Component.translatable(LangDefs.PENROSE_START_BH.getTranslationKey())));
        this.widgets.add("start", startBtn);

        var previewBtn = new IconButton(Icon.ENTER,
                btn -> getMenu().changePreview(!getMenu().getHost().isPreviewEnabled()));
        previewBtn.setTooltip(Tooltip.create(Component.translatable(LangDefs.TOGGLE_PREVIEW.getTranslationKey())));
        this.widgets.add("preview", previewBtn);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);

        var lines = bhWidget.getTooltipLines(mouseX, mouseY);
        if (lines != null) {
            gg.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
}