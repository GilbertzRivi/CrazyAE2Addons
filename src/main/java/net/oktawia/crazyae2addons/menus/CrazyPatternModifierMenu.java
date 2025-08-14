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
import net.oktawia.crazyae2addons.misc.AppEngFilteredSlot;
import net.oktawia.crazyae2addons.misc.AppEngManyFilteredSlot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class CrazyPatternModifierMenu extends AEBaseMenu implements IModifierMenu {

    public static String CHANGE_IGNORE_NBT = "changeIgnoreNBT";
    public static String CHANGE_CIRCUIT = "changeCircuit";

    @GuiSync(892)
    public String textNBT = "";
    @GuiSync(92)
    public String textCirc = "";

    private CrazyPatternModifierHost host;

    public CrazyPatternModifierMenu(int id, Inventory ip, CrazyPatternModifierHost host) {
        super(CrazyMenuRegistrar.CRAZY_PATTERN_MODIFIER_MENU.get(), id, ip, host);
        this.createPlayerInventorySlots(ip);
        host.setMenu(this);
        this.host = host;
        this.addSlot(new AppEngManyFilteredSlot(host.inv, 0, List.of(AEItems.PROCESSING_PATTERN.stack(), AEItems.CRAFTING_PATTERN.stack())), SlotSemantics.STORAGE);
        registerClientAction(CHANGE_IGNORE_NBT, this::changeNBT);
        registerClientAction(CHANGE_CIRCUIT, Integer.class, this::changeCircuit);
    }

    public void changeNBT(){
        if (this.getSlots(SlotSemantics.STORAGE).get(0).getItem().isEmpty()){
            return;
        }
        if (isClientSide()){
            sendClientAction(CHANGE_IGNORE_NBT);
        } else {
            ItemStack item = this.getSlots(SlotSemantics.STORAGE).get(0).getItem();
            CompoundTag tag = item.getOrCreateTag();
            if (tag.contains("ignorenbt")){
                tag.remove("ignorenbt");
                this.textNBT = "Current: Do not ignore NBT";
            } else {
                tag.putBoolean("ignorenbt", true);
                this.textNBT = "Current: ignore NBT";
            }
            item.setTag(tag);
        }
    }

    public void changeCircuit(int val){
        if (this.getSlots(SlotSemantics.STORAGE).get(0).getItem().isEmpty()){
            return;
        }
        if (isClientSide()){
            sendClientAction(CHANGE_CIRCUIT, val);

        } else {
            ItemStack item = this.getSlots(SlotSemantics.STORAGE).get(0).getItem();
            CompoundTag tag = item.getOrCreateTag();

            if (val == -1) {
                tag.remove("circuit");
                tag.remove("CustomModelData");
                this.textCirc = "No circuit selected";
            } else {
                tag.putInt("circuit", val);
                tag.putInt("CustomModelData", val == 0 ? 33 : val);
                this.textCirc = "Selected circuit " + val;
            }

            item.setTag(tag);
        }
    }

    public void ping() {
        ItemStack item = this.getSlots(SlotSemantics.STORAGE).get(0).getItem();
        CompoundTag tag = item.getOrCreateTag();

        if (tag.contains("circuit")) {
            int val = tag.getInt("circuit");
            this.textCirc = "Selected circuit " + val;

        } else {
            this.textCirc = "No circuit selected";
        }

        if (tag.contains("ignorenbt")) {
            this.textNBT = "Current: ignore NBT";
        } else {
            this.textNBT = "Current: Do not ignore NBT";
        }
    }
}
