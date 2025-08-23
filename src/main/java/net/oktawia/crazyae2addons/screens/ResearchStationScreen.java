package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.ResearchStationMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class ResearchStationScreen<C extends ResearchStationMenu> extends AEBaseScreen<C> {

    private IconButton prevBtn;
    private IconButton devBtn;

    public ResearchStationScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        widgets.add("energyBar", new ProgressBar(this.menu.energyBar, style.getImage("progressEnergy"),
                ProgressBar.Direction.HORIZONTAL, Component.literal("Stored Energy")));

        widgets.add("waterBar", new ProgressBar(this.menu.waterBar, style.getImage("progressWater"),
                ProgressBar.Direction.HORIZONTAL, Component.literal("Stored Fluid")));

        widgets.add("recipeBar", new ProgressBar(this.menu.recipeBar, style.getImage("progressRecipe"),
                ProgressBar.Direction.HORIZONTAL, Component.literal("Recipe Progress")));

        prevBtn = new IconButton(Icon.ENTER, btn -> getMenu().changePreview(!getMenu().preview));
        this.widgets.add("prevbtn", prevBtn);

        if (playerInventory.player != null && playerInventory.player.isCreative()) {
            devBtn = new IconButton(Icon.ENTER, btn -> getMenu().unlockAllClick());
            devBtn.setTooltip(Tooltip.create(Component.literal("Write all research to disk")));
            this.widgets.add("devbtn", devBtn);
        }
    }


    @Override
    public void updateBeforeRender(){
        super.updateBeforeRender();
        prevBtn.setTooltip(Tooltip.create(Component.literal(getMenu().preview ? "Hide preview" : "Show preview")));
        if (this.menu.recipeBar.getCurrentProgress() > 0 && (minecraft.level.getGameTime() / 20) % 2 == 0){
            this.setTextContent("working", Component.literal("Loading..."));
        } else {
            this.setTextContent("working", Component.empty());
        }
    }
}