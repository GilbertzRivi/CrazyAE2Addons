package net.oktawia.crazyae2addons.menus;

import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.IModifierMenu;
import net.oktawia.crazyae2addons.logic.CrazyPatternModifierHost;
import net.oktawia.crazyae2addons.misc.AppEngManyFilteredSlot;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.UpdatePatternsPacket;

import java.util.ArrayList;
import java.util.List;

public class CrazyPatternModifierMenuPP extends AEBaseMenu implements IModifierMenu {

    public static String CHANGE_IGNORE_NBT = "changeIgnoreNBT";
    public static String CHANGE_CIRCUIT = "changeCircuit";
    public static String SYNC = "patternSync";

    // NEW:
    public static String MULT_X2 = "multX2";
    public static String MULT_DIV2 = "multDiv2";

    @GuiSync(892)
    public boolean ignoreNbt = false;

    @GuiSync(92)
    public int circuit = -1;

    private final CrazyPatternModifierHost host;
    private PatternProviderBlockEntity provider;
    private final Player player;

    public CrazyPatternModifierMenuPP(int id, Inventory ip, CrazyPatternModifierHost host) {
        super(CrazyMenuRegistrar.CRAZY_PATTERN_MODIFIER_MENU_PP.get(), id, ip, host);
        host.setMenu(this);
        this.host = host;
        this.player = ip.player;

        this.addSlot(
                new AppEngManyFilteredSlot(
                        host.inv, 0,
                        List.of(AEItems.PROCESSING_PATTERN.stack(), AEItems.CRAFTING_PATTERN.stack())
                ),
                SlotSemantics.STORAGE
        );

        CompoundTag tag = host.getItemStack().getTag();
        if (tag != null && tag.contains("ppos")) {
            BlockPos pos = BlockPos.of(tag.getLong("ppos"));
            BlockEntity be = ip.player.level().getBlockEntity(pos);
            if (be instanceof PatternProviderBlockEntity pp) {
                this.provider = pp;
                var inv = pp.getLogic().getPatternInv();
                for (int i = 0; i < inv.size(); i++) {
                    this.addSlot(new AppEngSlot(inv, i), SlotSemantics.CONFIG);
                }
            }
        }

        registerClientAction(CHANGE_IGNORE_NBT, this::changeNBT);
        registerClientAction(CHANGE_CIRCUIT, Integer.class, this::changeCircuit);
        registerClientAction(SYNC, this::requestUpdate);
        registerClientAction(MULT_X2, this::multX2);
        registerClientAction(MULT_DIV2, this::multDiv2);

        updateStateFromPattern();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!isClientSide()) {
            updateStateFromPattern();
        }
    }

    private void updateStateFromPattern() {
        var slot = this.getSlots(SlotSemantics.STORAGE).get(0);
        ItemStack item = slot.getItem();

        if (item.isEmpty() || item.getTag() == null) {
            this.ignoreNbt = false;
            this.circuit = -1;
            return;
        }

        CompoundTag tag = item.getTag();
        this.ignoreNbt = tag.contains("ignorenbt");
        this.circuit = tag.contains("circuit") ? tag.getInt("circuit") : -1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot clicked = this.slots.get(index);
        if (clicked == null || !clicked.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = clicked.getItem();
        ItemStack copy = stack.copy();

        List<Slot> cfg = this.getSlots(SlotSemantics.CONFIG);
        List<Slot> stor = this.getSlots(SlotSemantics.STORAGE);

        if (cfg == null || cfg.isEmpty() || stor == null || stor.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int cfgStart = this.slots.indexOf(cfg.get(0));
        int cfgEnd = this.slots.indexOf(cfg.get(cfg.size() - 1)) + 1;

        int storStart = this.slots.indexOf(stor.get(0));
        int storEnd = this.slots.indexOf(stor.get(stor.size() - 1)) + 1;

        boolean moved;
        if (index >= cfgStart && index < cfgEnd) {
            moved = this.moveItemStackTo(stack, storStart, storEnd, false);
        } else if (index >= storStart && index < storEnd) {
            moved = this.moveItemStackTo(stack, cfgStart, cfgEnd, false);
        } else {
            return ItemStack.EMPTY;
        }

        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            clicked.set(ItemStack.EMPTY);
        } else {
            clicked.setChanged();
        }

        return copy;
    }

    public void requestUpdate() {
        if (isClientSide()) {
            sendClientAction(SYNC);
        } else {
            if (this.provider != null) {
                var inventory = provider.getLogic().getPatternInv();
                List<ItemStack> visibleStacks = new ArrayList<>();

                for (int i = 0; i < inventory.size(); i++) {
                    visibleStacks.add(inventory.getStackInSlot(i));
                }

                NetworkHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                        new UpdatePatternsPacket(0, visibleStacks)
                );
            }
        }
    }

    public void changeNBT() {
        if (this.getSlots(SlotSemantics.STORAGE).get(0).getItem().isEmpty()) return;

        if (isClientSide()) {
            sendClientAction(CHANGE_IGNORE_NBT);
            return;
        }

        ItemStack item = this.getSlots(SlotSemantics.STORAGE).get(0).getItem();
        CompoundTag tag = item.getOrCreateTag();

        if (tag.contains("ignorenbt")) {
            tag.remove("ignorenbt");
            this.ignoreNbt = false;
        } else {
            tag.putBoolean("ignorenbt", true);
            this.ignoreNbt = true;
        }

        item.setTag(tag);
    }

    public void changeCircuit(int val) {
        if (this.getSlots(SlotSemantics.STORAGE).get(0).getItem().isEmpty()) return;

        if (isClientSide()) {
            sendClientAction(CHANGE_CIRCUIT, val);
            return;
        }

        ItemStack item = this.getSlots(SlotSemantics.STORAGE).get(0).getItem();
        CompoundTag tag = item.getOrCreateTag();

        if (val == -1) {
            tag.remove("circuit");
            tag.remove("CustomModelData");
            this.circuit = -1;
        } else {
            tag.putInt("circuit", val);
            tag.putInt("CustomModelData", val == 0 ? 33 : val);
            this.circuit = val;
        }

        item.setTag(tag);
    }

    public void multX2() {
        applyMultiplier(2.0);
    }

    public void multDiv2() {
        applyMultiplier(0.5);
    }

    private void applyMultiplier(double multiplier) {
        var slot = this.getSlots(SlotSemantics.STORAGE).get(0);
        if (slot.getItem().isEmpty()) return;

        if (isClientSide()) {
            sendClientAction(multiplier == 2.0 ? MULT_X2 : MULT_DIV2);
            return;
        }

        ItemStack original = slot.getItem();
        ItemStack modified = CrazyPatternMultiplierMenu.modify(original, multiplier, 0, this.getPlayer().level());

        slot.set(modified);
        slot.setChanged();
        updateStateFromPattern();
    }

    public void ping() {
        updateStateFromPattern();
    }
}
