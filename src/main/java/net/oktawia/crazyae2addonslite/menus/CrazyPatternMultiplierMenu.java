package net.oktawia.crazyae2addonslite.menus;

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
import net.oktawia.crazyae2addonslite.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addonslite.logic.CrazyPatternMultiplierHost;
import net.oktawia.crazyae2addonslite.misc.AppEngManyFilteredSlot;

import java.util.List;

public class CrazyPatternMultiplierMenu extends AEBaseMenu {

    public static String ACTION_MODIFY_PATTERNS = "actionModifyPatterns";
    public static String CLEAR = "clearPatterns";
    public static String CIRCUIT = "circuitAction";
    public static String LIMIT = "limitAction";
    public CrazyPatternMultiplierHost host;

    @GuiSync(73)
    public double mult;
    @GuiSync(74)
    public Integer limit;

    public CrazyPatternMultiplierMenu(int id, Inventory ip, CrazyPatternMultiplierHost host) {
        super(CrazyMenuRegistrar.CRAZY_PATTERN_MULTIPLIER_MENU.get(), id, ip, host);
        this.createPlayerInventorySlots(ip);
        this.host = host;
        this.mult = host.getItemStack().getTag() == null ? 0 : host.getItemStack().getTag().getDouble("mult");
        if (host.getItemStack().getOrCreateTag().contains("limit")){
            this.limit = host.getItemStack().getOrCreateTag().getInt("limit");
        } else {
            this.limit = 0;
        }
        host.setMenu(this);
        for (int i = 0; i < 36; i++){
            this.addSlot(new AppEngManyFilteredSlot(host.inv, i, List.of(AEItems.PROCESSING_PATTERN.stack(), AEItems.CRAFTING_PATTERN.stack(), AEItems.BLANK_PATTERN.stack())), SlotSemantics.ENCODED_PATTERN);
        }
        registerClientAction(ACTION_MODIFY_PATTERNS, Double.class, this::modifyPatterns);
        registerClientAction(CLEAR, this::clearPatterns);
        registerClientAction(CIRCUIT, Integer.class, this::setCircuit);
        registerClientAction(LIMIT, Integer.class, this::setLimit);
    }

    public void modifyPatterns(Double multiplier) {
        if (multiplier <= 0) return;
        CompoundTag tag = this.host.getItemStack().getOrCreateTag();
        tag.putDouble("mult", multiplier);
        this.host.getItemStack().setTag(tag);
        if (isClientSide()) {
            sendClientAction(ACTION_MODIFY_PATTERNS, multiplier);
            return;
        }
        for (int i = 0; i < host.inv.size(); i++) {
            ItemStack is = modify(host.inv.getStackInSlot(i), multiplier, this.limit, this.getPlayer().level());
            this.host.inv.setItemDirect(i, is);
        }
    }

    public static ItemStack modify(ItemStack stack, double multiplier, int limit, Level level) {
        if (!(stack.getItem() instanceof EncodedPatternItem pattern))
            return stack;

        var originalTag = stack.getTag();
        var ignoreNbtTag = originalTag != null && originalTag.contains("ignorenbt") ? originalTag.get("ignorenbt").copy() : null;
        var circuitTag = originalTag != null && originalTag.contains("circuit") ? originalTag.get("circuit").copy() : null;
        var customModelDataTag = originalTag != null && originalTag.contains("CustomModelData")
                ? originalTag.get("CustomModelData").copy()
                : null;

        var detail = pattern.decode(stack, level, false);
        if (!(detail instanceof AEProcessingPattern process))
            return stack;

        GenericStack[] input = process.getSparseInputs();
        GenericStack[] output = process.getOutputs();

        if (limit > 0) {
            int totalOutput = 0;
            for (GenericStack out : output) {
                if (out != null)
                    totalOutput += (int) out.amount();
            }

            if (totalOutput > 0) {
                double maxMultiplier = Math.floor((double) limit / totalOutput);
                if (maxMultiplier < multiplier) {
                    multiplier = maxMultiplier;
                }
            }
        }

        if (multiplier < 1.0) {
            double inv = 1.0 / multiplier;
            int divisor = (int) Math.round(inv);

            if (Math.abs(inv - divisor) > 1e-9) {
                return stack;
            }

            for (GenericStack gs : input) {
                if (gs == null) continue;
                if (gs.what() instanceof AEFluidKey) continue;
                long amt = gs.amount();
                if (amt % divisor != 0) {
                    return stack;
                }
            }

            for (GenericStack gs : output) {
                if (gs == null) continue;
                if (gs.what() instanceof AEFluidKey) continue;
                long amt = gs.amount();
                if (amt % divisor != 0) {
                    return stack;
                }
            }
        }

        GenericStack[] newInputs = new GenericStack[input.length];
        GenericStack[] newOutputs = new GenericStack[output.length];

        for (int i = 0; i < input.length; i++) {
            if (input[i] != null) {
                int amt = (int) Math.max(Math.floor(input[i].amount() * multiplier), 1);
                newInputs[i] = new GenericStack(input[i].what(), amt);
            }
        }

        for (int i = 0; i < output.length; i++) {
            if (output[i] != null) {
                int amt = (int) Math.max(Math.floor(output[i].amount() * multiplier), 1);
                newOutputs[i] = new GenericStack(output[i].what(), amt);
            }
        }

        ItemStack modifiedStack = PatternDetailsHelper.encodeProcessingPattern(newInputs, newOutputs);

        if (ignoreNbtTag != null) {
            modifiedStack.getOrCreateTag().put("ignorenbt", ignoreNbtTag);
        }
        if (circuitTag != null) {
            modifiedStack.getOrCreateTag().put("circuit", circuitTag);
        }
        if (customModelDataTag != null) {
            modifiedStack.getOrCreateTag().put("CustomModelData", customModelDataTag);
        }

        return modifiedStack;
    }


    public void clearPatterns(){
        if (isClientSide()){
            sendClientAction(CLEAR);
        } else {
            ItemStack is = AEItems.BLANK_PATTERN.stack();
            for (int i = 0; i < host.inv.size(); i++) {
                if (!host.inv.getStackInSlot(i).isEmpty()){
                    this.host.inv.setItemDirect(i, is.copyWithCount(1));
                }
            }
        }
    }

    public void setCircuit(Integer circuit){
        if (isClientSide()) {
            sendClientAction(CIRCUIT, circuit);
        } else {
            for (int i = 0; i < host.inv.size(); i++) {

                ItemStack item = host.inv.getStackInSlot(i);
                CompoundTag tag = item.getOrCreateTag();

                if (circuit == -1) {
                    tag.remove("circuit");
                    tag.remove("CustomModelData");
                } else {
                    tag.putInt("circuit", circuit);
                    tag.putInt("CustomModelData", circuit == 0 ? 33 : circuit);
                }

                item.setTag(tag);
            }
        }
    }

    public void setLimit(Integer val) {
        this.limit = val;
        CompoundTag tag = this.host.getItemStack().getOrCreateTag();
        tag.putInt("limit", val);
        if (isClientSide()){
            sendClientAction(LIMIT, val);
        }
    }
}
