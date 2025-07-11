package net.oktawia.crazyae2addons.entities;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.VariableMachine;
import net.oktawia.crazyae2addons.menus.MEDataControllerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MEDataControllerBE extends AENetworkInvBlockEntity implements IGridTickable, MenuProvider {

    public AppEngInternalInventory inv = new AppEngInternalInventory(this, 6);
    public final Map<MachineRecord, VariableRecord> variables = new HashMap<>();
    public final Map<VariableRecord, MachineRecord> notifications = new HashMap<>();

    public MEDataControllerBE(BlockPos pos, BlockState state) {
        super(CrazyBlockEntityRegistrar.ME_DATA_CONTROLLER_BE.get(), pos, state);
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(4)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(new ItemStack(CrazyBlockRegistrar.ME_DATA_CONTROLLER_BLOCK.get()));
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);

        this.inv.readFromNBT(tag, "inv");

        this.variables.clear();
        this.notifications.clear();

        if (tag.contains("Variables", Tag.TAG_LIST)) {
            ListTag variableList = tag.getList("Variables", Tag.TAG_COMPOUND);
            for (Tag baseTag : variableList) {
                if (baseTag instanceof CompoundTag entryTag) {
                    CompoundTag machineTag = entryTag.getCompound("machine");
                    CompoundTag variableTag = entryTag.getCompound("variable");

                    MachineRecord machine = null;
                    try {
                        machine = new MachineRecord(
                                machineTag.getString("id"),
                                Class.forName(machineTag.getString("name"))
                        );
                    } catch (Exception e) {
                        LogUtils.getLogger().info(e.toString());
                    }

                    VariableRecord variable = new VariableRecord(
                            variableTag.getString("id"),
                            variableTag.getString("name"),
                            variableTag.getString("value")
                    );
                    if (variable.value != null && !variable.value.isEmpty()){
                        this.variables.put(machine, variable);
                    }
                }
            }
        }

        if (tag.contains("Notifications", Tag.TAG_LIST)) {
            ListTag notifList = tag.getList("Notifications", Tag.TAG_COMPOUND);
            for (Tag baseTag : notifList) {
                if (baseTag instanceof CompoundTag entryTag) {
                    CompoundTag machineTag = entryTag.getCompound("machine");
                    CompoundTag variableTag = entryTag.getCompound("variable");

                    MachineRecord machine = null;
                    try {
                        machine = new MachineRecord(
                                machineTag.getString("id"),
                                Class.forName(machineTag.getString("name"))
                        );
                    } catch (Exception e) {
                        LogUtils.getLogger().info(e.toString());
                    }

                    VariableRecord variable = new VariableRecord(
                            variableTag.getString("id"),
                            variableTag.getString("name"),
                            variableTag.getString("value")
                    );

                    if (machine != null) {
                        this.notifications.put(variable, machine);
                    }
                }
            }
        }
    }


    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        this.inv.writeToNBT(tag, "inv");

        ListTag variableList = new ListTag();
        for (var entry : variables.entrySet()) {
            CompoundTag entryTag = getVariableTag(entry.getKey(), entry.getValue());
            variableList.add(entryTag);
        }
        tag.put("Variables", variableList);

        ListTag notifList = new ListTag();
        for (var entry : notifications.entrySet()) {
            CompoundTag entryTag = getNotifTag(entry);
            variableList.add(entryTag);
        }
        tag.put("Notifications", notifList);
    }

    private static @NotNull CompoundTag getVariableTag(MachineRecord machine, VariableRecord variable) {
        CompoundTag machineTag = new CompoundTag();
        machineTag.putString("id", machine.id);
        machineTag.putString("name", machine.type.getCanonicalName());
        CompoundTag variableTag = new CompoundTag();
        variableTag.putString("id", variable.id);
        variableTag.putString("name", variable.name);
        variableTag.putString("value", variable.value);
        CompoundTag entryTag = new CompoundTag();
        entryTag.put("machine", machineTag);
        entryTag.put("variable", variableTag);
        return entryTag;
    }

    private static @NotNull CompoundTag getNotifTag(Map.Entry<VariableRecord, MachineRecord> entry) {
        return getVariableTag(entry.getValue(), entry.getKey());
    }

    public void addVariable(String machineId, Class<?> machineClass, String variableId, String variableName, String variableValue) {
        if (variables.size() >= getMaxVariables()) return;

        var machineRecord = new MachineRecord(machineId, machineClass);
        var variableRecord = new VariableRecord(variableId, variableName, variableValue);

        VariableRecord previous = variables.get(machineRecord);

        boolean isChanged = previous == null || !Objects.equals(previous.value(), variableValue);

        variables.put(machineRecord, variableRecord);

        if (isChanged) {
            notifySubscribers(variableRecord);
        }

        setChanged();
    }

    private void notifySubscribers(VariableRecord variable) {
        if (getMainNode().getGrid() == null) return;
        List<MachineRecord> toNotify = new ArrayList<>();
        for (var entry : notifications.entrySet()){
            if (Objects.equals(entry.getKey().name, variable.name)){
                toNotify.add(entry.getValue());
            }
        }

        for (MachineRecord target : toNotify) {
            AtomicBoolean notified = new AtomicBoolean(false);
            getMainNode().getGrid().getMachines(target.type).forEach(machine -> {
                if (machine instanceof VariableMachine vm && Objects.equals(vm.getId(), target.id)) {
                    vm.notifyVariable(variable.name, variable.value, this);
                    notified.set(true);
                }
            });
            if (!notified.get()){
                notifications.remove(variable);
            }
        }
    }

    public void registerNotification(String variableId, String variableName, String machineId, Class<?> machineType) {
        notifications.computeIfAbsent(new VariableRecord(variableId, variableName, ""),
                k -> new MachineRecord(machineId, machineType));
        setChanged();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(20, 20, false, true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (getMainNode().getGrid() == null || getLevel() == null) return TickRateModulation.IDLE;

        getMainNode().getGrid().getMachines(MEDataControllerBE.class).forEach(machine -> {
            if (!machine.equals(this)) {
                getLevel().destroyBlock(getBlockPos(), true);
            }
        });

        return TickRateModulation.IDLE;
    }

    public void removeVariable(String variableId, String variableName) {
        variables.entrySet().removeIf(
                entry -> Objects.equals(entry.getValue().id, variableId) &&
                        Objects.equals(entry.getValue().name, variableName));
        setChanged();
    }

    public void removeNotification(String machineId) {
        notifications.entrySet().removeIf(
                entry -> Objects.equals(entry.getValue().id, machineId));
        setChanged();
    }

    public int getMaxVariables() {
        int max = 0;
        for (ItemStack stack : inv) {
            if (stack.is(AEItems.CELL_COMPONENT_1K.asItem())) max += 1;
            else if (stack.is(AEItems.CELL_COMPONENT_4K.asItem())) max += 4;
            else if (stack.is(AEItems.CELL_COMPONENT_16K.asItem())) max += 16;
            else if (stack.is(AEItems.CELL_COMPONENT_64K.asItem())) max += 64;
            else if (stack.is(AEItems.CELL_COMPONENT_256K.asItem())) max += 256;
        }
        return max;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.inv;
    }

    @Override
    public void onChangeInventory(InternalInventory internalInventory, int i) {
        this.variables.clear();
        this.notifications.clear();
        this.setChanged();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new MEDataControllerMenu(i, inventory, this);
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.ME_DATA_CONTROLLER_MENU.get(), player, locator);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("ME Data Controller");
    }

    public record VariableRecord(String id, String name, String value) {}
    public record MachineRecord(String id, Class<?> type) {}
}
