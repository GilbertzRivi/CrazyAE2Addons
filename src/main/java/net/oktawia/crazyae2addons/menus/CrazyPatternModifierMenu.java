package net.oktawia.crazyae2addons.menus;

import appeng.core.definitions.AEItems;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.IModifierMenu;
import net.oktawia.crazyae2addons.logic.CrazyPatternModifierHost;
import net.oktawia.crazyae2addons.misc.AppEngManyFilteredSlot;

import java.util.List;

public class CrazyPatternModifierMenu extends AEBaseMenu implements IModifierMenu {

    public static String CHANGE_IGNORE_NBT = "changeIgnoreNBT";
    public static String CHANGE_CIRCUIT = "changeCircuit";

    public static String MULT_X2 = "multX2";
    public static String MULT_DIV2 = "multDiv2";

    @GuiSync(892)
    public boolean ignoreNbt = false;

    @GuiSync(92)
    public int circuit = -1;

    private final CrazyPatternModifierHost host;

    public CrazyPatternModifierMenu(int id, Inventory ip, CrazyPatternModifierHost host) {
        super(CrazyMenuRegistrar.CRAZY_PATTERN_MODIFIER_MENU.get(), id, ip, host);
        this.createPlayerInventorySlots(ip);
        this.host = host;
        host.setMenu(this);

        this.addSlot(
                new AppEngManyFilteredSlot(
                        host.inv,
                        0,
                        List.of(AEItems.PROCESSING_PATTERN.stack(), AEItems.CRAFTING_PATTERN.stack())
                ),
                SlotSemantics.STORAGE
        );

        registerClientAction(CHANGE_IGNORE_NBT, this::changeNBT);
        registerClientAction(CHANGE_CIRCUIT, Integer.class, this::changeCircuit);

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
