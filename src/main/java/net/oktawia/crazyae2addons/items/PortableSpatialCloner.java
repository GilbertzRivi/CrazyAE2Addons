package net.oktawia.crazyae2addons.items;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.structuretool.AbstractStructureCaptureToolItem;
import net.oktawia.crazyae2addons.logic.structuretool.PortableSpatialClonerHost;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolUtil;
import org.jetbrains.annotations.Nullable;

public class PortableSpatialCloner extends AbstractStructureCaptureToolItem {

    public PortableSpatialCloner(Item.Properties properties) {
        super(DEFAULT_BASE_POWER, DEFAULT_UPGRADE_SLOTS + 1, properties);
    }

    public static ItemStack findHeld(@Nullable Player player) {
        return StructureToolUtil.findHeld(player, PortableSpatialCloner.class);
    }

    public static ItemStack findActive(@Nullable Player player) {
        return StructureToolUtil.findActive(player, PortableSpatialCloner.class);
    }

    @Override
    protected MenuType<?> getToolMenuType() {
        return CrazyMenuRegistrar.PORTABLE_SPATIAL_CLONER_MENU.get();
    }

    @Override
    public ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new PortableSpatialClonerHost(player, inventorySlot, stack);
    }

    @Override
    protected boolean removeCapturedBlocks() {
        return false;
    }

    @Override
    protected Component getCaptureSuccessMessage() {
        return Component.translatable(LangDefs.STRUCTURE_COPIED_AND_SAVED.getTranslationKey());
    }

    @Override
    protected Component getStoredStructureActionNotImplementedMessage() {
        return Component.translatable(LangDefs.COPY_PASTE_NOT_IMPLEMENTED_YET.getTranslationKey());
    }

    @Override
    protected void onUseWithStoredStructure(ServerLevel level, Player player, ItemStack stack) {
        showHud(player, getStoredStructureActionNotImplementedMessage());
    }

    @Override
    protected void onUseOnWithStoredStructure(ServerLevel level, Player player, ItemStack stack, net.minecraft.core.BlockPos clickedFacePos) {
        showHud(player, getStoredStructureActionNotImplementedMessage());
    }
}