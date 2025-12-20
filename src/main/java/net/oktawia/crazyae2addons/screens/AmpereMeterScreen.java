package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ToggleButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.AmpereMeterMenu;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmpereMeterScreen<C extends AmpereMeterMenu> extends AEBaseScreen<C> {

    public ToggleButton direction;

    public AmpereMeterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        direction = new ToggleButton(Icon.ARROW_RIGHT, Icon.ARROW_LEFT, this::toggleDirection);
        direction.setTooltipOn(List.of(Component.translatable("gui.crazyae2addons.ampere_meter_1")));
        direction.setTooltipOff(List.of(Component.translatable("gui.crazyae2addons.ampere_meter_2")));
        this.widgets.add("direction", direction);
    }

    private void toggleDirection(boolean dir) {
        this.direction.setState(!dir);
        this.getMenu().changeDirection(dir);
    }

    @Override
    protected void updateBeforeRender(){
        super.updateBeforeRender();
        direction.setState(getMenu().direction);
        setTextContent("energy", Component.literal(String.format("Transferring: %s %s", getMenu().transfer, getMenu().unit)));
    }
}