package net.oktawia.crazyae2addons.mixins;


import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
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
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.entities.CraftingGuardBE;
import net.oktawia.crazyae2addons.interfaces.IPatternProviderTargetCacheExt;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import stone.mae2.appeng.helpers.patternprovider.PatternProviderTargetCache;

import java.util.Set;

@Mixin(value = PatternProviderTargetCache.class, priority = 920)
public abstract class MixinGTMAE2 implements IPatternProviderTargetCacheExt {

    @Shadow public abstract @Nullable PatternProviderTarget find();

    @Shadow @Final private IActionSource src;
    @Shadow @Final private Direction direction;
    @Unique
    private BlockPos pos = null;
    @Unique private Level lvl = null;
    @Unique private IPatternDetails details = null;
    @Unique private CraftingGuardBE guard = null;
    @Unique private boolean exclusiveMode = false;

    @Unique public void setGuard(CraftingGuardBE guard){
        this.guard = guard;
    }

    @Unique public void setExclusiveMode(boolean mode){
        this.exclusiveMode = mode;
    }

    @ModifyReturnValue(
            method = "find()Lappeng/helpers/patternprovider/PatternProviderTarget;",
            at = @At("RETURN"),
            remap = false
    )
    private PatternProviderTarget rerouteToExtendedFind(PatternProviderTarget original) {
        if (this.details != null) {
            return this.find(this.details, original);
        }
        return original;
    }

    @Inject(
            method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lappeng/api/networking/security/IActionSource;)V",
            at = @At("RETURN")
    )
    private void atCtorTail(ServerLevel l, BlockPos pos, Direction direction, IActionSource src, CallbackInfo ci){
        this.pos = pos;
        this.lvl = l;
    }

    @Unique
    public void setDetails(IPatternDetails patternDetails) {
        this.details = patternDetails;
    }

    @Unique
    public PatternProviderTarget find(IPatternDetails details, PatternProviderTarget original) {
        this.details = details;
        if (original == null) return null;

        return new PatternProviderTarget() {
            private final BlockPos pos1           = MixinGTMAE2.this.pos;
            private final Level lvl1              = MixinGTMAE2.this.lvl;
            private final IPatternDetails det1    = MixinGTMAE2.this.details;
            private final CraftingGuardBE guard1  = MixinGTMAE2.this.guard;
            private final boolean exclMode1       = MixinGTMAE2.this.exclusiveMode;

            @Override
            public long insert(AEKey what, long amount, Actionable type) {
                if (det1 != null){
                    CompoundTag tag = det1.getDefinition().getTag();
                    int c = (tag != null && tag.contains("circuit")) ? tag.getInt("circuit") : -1;
                    if (c != -1){
                        traverseGridIfInterface(c, pos1, lvl1);
                        setCirc(c, pos1, lvl1);
                    }
                }
                var result = original.insert(what, amount, type);
                if (this.guard1 != null && result > 0 && this.guard1.getLevel() != null && this.guard1.getLevel().getServer() != null){
                    this.guard1.excluded.put(this.pos1, this.guard1.getLevel().getServer().getTickCount());
                }
                return result;
            }

            @Override
            public boolean containsPatternInput(Set<AEKey> patternInputs) {
                if (original.containsPatternInput(patternInputs)) return true;
                var server = this.lvl1.getServer();
                if (server != null && this.guard1 != null && this.guard1.excluded.get(this.pos1) != null){
                    return this.guard1.excluded.get(this.pos1) == server.getTickCount() && this.exclMode1;
                }
                return false;
            }
        };
    }

    @Unique
    private void traverseGridIfInterface(int circuit, BlockPos pos, Level level) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CableBusBlockEntity cbbe)) return;
        var part = cbbe.getPart(this.direction);
        if (!(part instanceof InterfacePart ip)) return;

        ip.getGridNode().getGrid()
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