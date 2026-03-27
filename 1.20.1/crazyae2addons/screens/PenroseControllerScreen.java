package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.PenroseControllerMenu;
import net.oktawia.crazyae2addons.misc.BlackHoleWidget;
import net.oktawia.crazyae2addons.misc.IconButton;

public class PenroseControllerScreen<C extends PenroseControllerMenu> extends AEBaseScreen<C> {

    private final BlackHoleWidget bhWidget;

    public PenroseControllerScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.bhWidget = new BlackHoleWidget(menu);
        this.bhWidget.setMassLimits(BlackHoleWidget.INITIAL_BH_MASS, BlackHoleWidget.DEFAULT_MAX_BH_MASS);
        this.widgets.add("bh", this.bhWidget);

        var startBtn = new IconButton(Icon.ENTER, btn -> getMenu().startBlackHole());
        startBtn.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.penrose_start_bh")));
        this.widgets.add("start", startBtn);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);

        var lines = bhWidget.getTooltipLines(mouseX, mouseY);
        if (lines != null) {
            gg.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    @Override
    public void updateBeforeRender() {
        super.updateBeforeRender();

        var activeText = getMenu().blackHoleActive
                ? Component.translatable("gui.crazyae2addons.penrose_bh_on")
                : Component.translatable("gui.crazyae2addons.penrose_bh_off");

        String gen = Utils.shortenNumber(getMenu().feGeneratedGrossPerTick);
        String con = Utils.shortenNumber(getMenu().feConsumedPerTick);

        setTextContent("generation", Component.translatable("gui.crazyae2addons.penrose_bh_status", activeText).append(Component.literal(" | Gen: " + gen + " FE/t | Use: " + con + " FE/t")));
    }
}
