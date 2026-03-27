package net.oktawia.crazyae2addons.menus;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.CrazyEmitterMultiplierHost;
import net.oktawia.crazyae2addons.logic.CrazyPatternMultiplierHost;
import net.oktawia.crazyae2addons.misc.AppEngFilteredSlot;

public class CrazyEmitterMultiplierMenu extends AEBaseMenu {

    public static String ACTION_SAVE_MULT = "actionSaveMult";
    public static String ACTION_SAVE_VALUE = "actionSaveValue";
    public CrazyEmitterMultiplierHost host;

    @GuiSync(73)
    public double value;
    @GuiSync(74)
    public boolean mult;

    public CrazyEmitterMultiplierMenu(int id, Inventory ip, CrazyEmitterMultiplierHost host) {
        super(CrazyMenuRegistrar.CRAZY_EMITTER_MULTIPLIER_MENU.get(), id, ip, host);
        this.createPlayerInventorySlots(ip);
        this.host = host;
        this.value = host.getItemStack().getTag() == null ? 0 : host.getItemStack().getTag().getDouble("val");
        this.mult = host.getItemStack().getTag() == null ? false : host.getItemStack().getTag().getBoolean("mult");
        host.setMenu(this);
        registerClientAction(ACTION_SAVE_VALUE, Double.class, this::saveValue);
        registerClientAction(ACTION_SAVE_MULT, Boolean.class, this::saveMult);
    }

    public void saveMult(boolean mult) {
        CompoundTag tag = this.host.getItemStack().getOrCreateTag();
        tag.putBoolean("mult", mult);
        this.host.getItemStack().setTag(tag);
        if (isClientSide()) {
            sendClientAction(ACTION_SAVE_MULT, mult);
        }
    }

    public void saveValue(double val) {
        if (val <= 0) return;
        CompoundTag tag = this.host.getItemStack().getOrCreateTag();
        tag.putDouble("val", val);
        this.host.getItemStack().setTag(tag);
        if (isClientSide()) {
            sendClientAction(ACTION_SAVE_VALUE, val);
        }
    }
}
