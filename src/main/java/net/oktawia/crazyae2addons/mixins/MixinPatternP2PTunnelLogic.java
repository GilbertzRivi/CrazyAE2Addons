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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import stone.mae2.parts.p2p.PatternP2PTunnelLogic;

@Mixin(value = PatternP2PTunnelLogic.class, remap = false)
public abstract class MixinPatternP2PTunnelLogic {

    @Shadow
    public abstract void refreshOutputs();

    @Unique
    private IPatternDetails crazyae$pattern;

    @Unique
    private Object crazyae$lastCacheObj;

    @Inject(
            method = "pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;Lnet/minecraft/core/Direction;)Z",
            at = @At("HEAD")
    )
    private void crazyae$head(IPatternDetails pattern, KeyCounter[] ingredients, Direction ejectionDirection,
                              CallbackInfoReturnable<Boolean> cir) {

        this.refreshOutputs();

        this.crazyae$pattern = pattern;
        this.crazyae$lastCacheObj = null;
    }

    @Redirect(
            method = "pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;Lnet/minecraft/core/Direction;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/helpers/patternprovider/PatternProviderTargetCache;find()Lappeng/helpers/patternprovider/PatternProviderTarget;"
            )
    )
    private PatternProviderTarget crazyae$captureCacheAndFind(@Coerce Object cacheObj) {
        this.crazyae$lastCacheObj = cacheObj;
        return ((PatternProviderTargetCacheAccessor) cacheObj).callFind();
    }

    @Redirect(
            method = "pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;Lnet/minecraft/core/Direction;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lstone/mae2/parts/p2p/PatternP2PTunnelLogic;targetAcceptsAll(Lappeng/helpers/patternprovider/PatternProviderTarget;[Lappeng/api/stacks/KeyCounter;)Z"
            )
    )
    private boolean crazyae$afterAcceptsAll(PatternProviderTarget target, KeyCounter[] inputHolder) {
        boolean ok = PatternP2PTunnelLogic.targetAcceptsAll(target, inputHolder);
        if (!ok || this.crazyae$pattern == null || this.crazyae$lastCacheObj == null) {
            return ok;
        }

        CompoundTag tag = this.crazyae$pattern.getDefinition().getTag();
        int circuit = (tag != null && tag.contains("circuit")) ? tag.getInt("circuit") : -1;
        if (circuit == -1) return ok;

        var cacheAcc = (PatternProviderTargetCacheAccessor) this.crazyae$lastCacheObj;

        Direction side = cacheAcc.getDirection();
        var bac = cacheAcc.getCache();
        if (side == null || bac == null) return ok;

        var bacAcc = (BlockApiCacheAccessor) (Object) bac;
        Level level = bacAcc.getLevel();
        BlockPos pos = bacAcc.getFromPos();

        traverseGridIfInterface(circuit, pos, level, side);
        setCirc(circuit, pos, level);

        return ok;
    }

    @Unique
    private void traverseGridIfInterface(int circuit, BlockPos pos, Level level, Direction side) {

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || side == null) return;

        IGridNode node = null;

        if (be instanceof CableBusBlockEntity cbbe) {
            var part = cbbe.getPart(side);
            if (part instanceof InterfacePart ip) {
                node = ip.getGridNode();
            }
        } else if (be instanceof InterfaceBlockEntity ibe) {
            node = ibe.getGridNode();
        }

        if (node == null || node.getGrid() == null) return;

        node.getGrid()
                .getMachines(StorageBusPart.class)
                .forEach(bus -> {
                    if (!bus.isUpgradedWith(CrazyItemRegistrar.CIRCUIT_UPGRADE_CARD_ITEM.get())) return;

                    BlockEntity busBe = bus.getBlockEntity();
                    if (busBe == null) return;

                    Level busLevel = busBe.getLevel();

                    if (busLevel == null) throw new NullPointerException("lvl is null can not get block entity");

                    BlockPos targetPos = busBe.getBlockPos().relative(bus.getSide());
                    setCirc(circuit, targetPos, busLevel);
                });
    }

    @Unique
    private static void setCirc(int circ, BlockPos pos, Level lvl) {
        if (!CrazyConfig.COMMON.enableCPP.get()) return;

        try {
            var machine = SimpleTieredMachine.getMachine(lvl, pos);
            NotifiableItemStackHandler inv;

            if (machine instanceof SimpleTieredMachine stm) {
                inv = stm.getCircuitInventory();
            } else if (machine instanceof ItemBusPartMachine ibpm) {
                inv = ibpm.getCircuitInventory();
            } else if (machine instanceof FluidHatchPartMachine fhpm) {
                inv = fhpm.getCircuitInventory();
            } else {
                return;
            }

            if (circ != 0) {
                var machineStack = GTItems.PROGRAMMED_CIRCUIT.asStack();
                IntCircuitBehaviour.setCircuitConfiguration(machineStack, circ);
                inv.setStackInSlot(0, machineStack);
            } else {
                inv.setStackInSlot(0, ItemStack.EMPTY);
            }
        } catch (Exception e) {
            CrazyAddons.LOGGER.warn("Failed to set Circuit ", e);
        }
    }
}
