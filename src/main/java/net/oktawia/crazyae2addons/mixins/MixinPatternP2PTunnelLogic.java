package net.oktawia.crazyae2addons.mixins;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.parts.misc.InterfacePart;
import appeng.parts.storagebus.StorageBusPart;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import stone.mae2.parts.p2p.PatternP2PTunnelLogic;

import java.util.List;

@Mixin(value = PatternP2PTunnelLogic.class, remap = false)
public abstract class MixinPatternP2PTunnelLogic {

    @Unique
    private IPatternDetails pattern;
    @Unique
    private Direction direction;
    @Unique
    private PatternP2PTunnelLogic.Target capturedOutput;

    @Redirect(
            method = "pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;Lnet/minecraft/core/Direction;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lstone/mae2/parts/p2p/PatternP2PTunnelLogic;targetAcceptsAll(Lappeng/helpers/patternprovider/PatternProviderTarget;[Lappeng/api/stacks/KeyCounter;)Z"
            )
    )
    private boolean afterAcceptsAll(PatternProviderTarget target, KeyCounter[] inputHolder) {
        boolean ok = PatternP2PTunnelLogic.targetAcceptsAll(target, inputHolder);
        if (ok && pattern != null) {
            CompoundTag tag = pattern.getDefinition().getTag();
            int c = (tag != null && tag.contains("circuit")) ? tag.getInt("circuit") : -1;
            if (c != -1) {
                traverseGridIfInterface(c, capturedOutput.pos(), capturedOutput.level());
                setCirc(c, capturedOutput.pos(), capturedOutput.level());
            }
        }
        return ok;
    }

    @Inject(
            method = "pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;Lnet/minecraft/core/Direction;)Z",
            at = @At("HEAD")
    )
    private void beforePushPattern(IPatternDetails pattern, KeyCounter[] ingredients, Direction ejectionDirection, CallbackInfoReturnable<Boolean> cir) {
        this.pattern = pattern;
        this.direction = ejectionDirection;
    }

    @Redirect(
            method =
                    "pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;Lnet/minecraft/core/Direction;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;get(I)Ljava/lang/Object;"
            )
    )
    private Object captureOutput(List<?> list, int index) {
        Object obj = list.get(index);
        if (obj instanceof PatternP2PTunnelLogic.Target t) {
            this.capturedOutput = t;
        } else {
            this.capturedOutput = null;
        }
        return obj;
    }

    @Unique
    private void traverseGridIfInterface(int circuit, BlockPos pos, Level level) {
        BlockEntity be = level.getBlockEntity(pos);

        IGridNode node = null;
        if (this.direction == null) return;

        if (be instanceof CableBusBlockEntity cbbe) {
            var part = cbbe.getPart(this.direction);
            if (part instanceof InterfacePart ip) {
                node = ip.getGridNode();
            }
        } else if (be instanceof InterfaceBlockEntity ibe) {
            node = ibe.getGridNode();
        }

        if (node == null) return;

        node.getGrid()
                .getMachines(StorageBusPart.class)
                .forEach(bus -> {
                    if (bus.isUpgradedWith(CrazyItemRegistrar.CIRCUIT_UPGRADE_CARD_ITEM.get())) {
                        BlockEntity busBe = bus.getBlockEntity();
                        if (busBe == null) return;

                        Level busLevel = busBe.getLevel();
                        BlockPos targetPos = busBe.getBlockPos().relative(bus.getSide());
                        setCirc(circuit, targetPos, busLevel);
                    }
                });
    }

    @Unique
    private static void setCirc(int circ, BlockPos pos, Level lvl){
        if (!CrazyConfig.COMMON.enableCPP.get()) return;
        try {
            var machine = SimpleTieredMachine.getMachine(lvl, pos);
            NotifiableItemStackHandler inv;
            if (machine instanceof SimpleTieredMachine STM){
                inv = STM.getCircuitInventory();
            } else if (machine instanceof ItemBusPartMachine IBPM) {
                inv = IBPM.getCircuitInventory();
            } else if (machine instanceof FluidHatchPartMachine FHPM) {
                inv = FHPM.getCircuitInventory();
            } else {
                return;
            }
            if (circ != 0){
                var machineStack = GTItems.PROGRAMMED_CIRCUIT.asStack();
                IntCircuitBehaviour.setCircuitConfiguration(machineStack, circ);
                inv.setStackInSlot(0, machineStack);
            } else {
                inv.setStackInSlot(0, ItemStack.EMPTY);
            }
        } catch (Exception e){
            LogUtils.getLogger().info(e.toString());
        }
    }
}
