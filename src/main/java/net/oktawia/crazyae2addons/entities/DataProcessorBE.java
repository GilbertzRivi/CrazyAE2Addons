package net.oktawia.crazyae2addons.entities;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.datavariables.DataFlowRunner;
import net.oktawia.crazyae2addons.datavariables.FlowNodeRegistry;
import net.oktawia.crazyae2addons.datavariables.IFlowNode;
import net.oktawia.crazyae2addons.datavariables.nodes.str.EntrypointNode;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.VariableMachine;
import net.oktawia.crazyae2addons.menus.DataProcessorMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.*;

public class DataProcessorBE extends AENetworkInvBlockEntity implements VariableMachine, MenuProvider {

    public String identifier = randomHexId();
    public AppEngInternalInventory inv = new AppEngInternalInventory(this, 1, 1);
    private List<IFlowNode> nodes;

    public DataProcessorBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.DATA_PROCESSOR_BE.get(), pos, blockState);
        this.getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(2).setVisualRepresentation(
                new ItemStack(CrazyBlockRegistrar.DATA_PROCESSOR_BLOCK.get())
        );
    }

    public static String randomHexId() {
        SecureRandom rand = new SecureRandom();
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) sb.append(Integer.toHexString(rand.nextInt(16)).toUpperCase());
        return sb.toString();
    }

    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("ident")){
            this.identifier = data.getString("ident");
        }
        if (data.contains("inv")){
            this.inv.readFromNBT(data, "inv");
        }
    }

    @Override
    public void onReady(){
        super.onReady();
        this.onChangeInventory(getInternalInventory(), 0);
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.inv;
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putString("ident", this.identifier);
        this.inv.writeToNBT(data, "inv");
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        if (getMainNode().getGrid() == null) return;

        if (inv.getStackInSlot(slot).isEmpty()){
            this.nodes = null;
            getMainNode().getGrid().getMachines(MEDataControllerBE.class).stream().findFirst().ifPresent(db -> db.removeNotification(this.identifier));
            return;
        }
        ItemStack stack = inv.getStackInSlot(slot);
        if (!stack.hasTag()) return;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("flow")) return;

        CompoundTag flow = tag.getCompound("flow");

        this.nodes = FlowNodeRegistry.deserializeNodesFromNBT(flow);
        for (IFlowNode node : this.nodes){
            LogUtils.getLogger().info(node.toString());
        }
        for (IFlowNode node : this.nodes) {
            if (node instanceof EntrypointNode ep) {
                getMainNode().getGrid().getMachines(MEDataControllerBE.class).stream().findFirst().ifPresent(db -> db.registerNotification(
                        this.identifier, ep.getValueName(), this.identifier, this.getClass()
                ));
                break;
            }
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        return new DataProcessorMenu(pContainerId, pPlayerInventory, this);
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.DATA_PROCESSOR_MENU.get(), player, locator);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Data Processor");
    }

    @Override
    public String getId() {
        return this.identifier;
    }

    @Override
    public void notifyVariable(String name, String value, MEDataControllerBE db) {
        if (this.nodes != null){
            var runner = new DataFlowRunner(this.nodes);
            runner.run(value, this.identifier, getMainNode().getGrid());
        }
    }
}
