package net.oktawia.crazyae2addons.mixins;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.parts.misc.InterfacePart;
import appeng.parts.storagebus.StorageBusPart;
import appeng.util.BlockApiCache;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.interfaces.IPatternProviderTargetCacheExt;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import stone.mae2.appeng.helpers.patternprovider.PatternProviderTargetCache;

import java.util.Set;

@Mixin(value = PatternProviderTargetCache.class, priority = 900)
public abstract class MixinMAE23 implements IPatternProviderTargetCacheExt {

    @Unique
    private BlockPos pos = null;
    @Unique private Level lvl = null;
    @Unique private IPatternDetails details = null;
    @Shadow
    @Final
    private Direction direction;
    @Shadow @Nullable
    public abstract PatternProviderTarget find();

    @Shadow @Final private BlockApiCache<MEStorage> cache;

    @Inject(
            method = "Lstone/mae2/appeng/helpers/patternprovider/PatternProviderTargetCache;<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lappeng/api/networking/security/IActionSource;)V",
            at = @At("RETURN")
    )
    private void atCtorTail(ServerLevel l, BlockPos pos, Direction direction, IActionSource src, CallbackInfo ci){
        this.pos = pos;
        this.lvl = l;
    }

    @Unique
    public void setDetails(IPatternDetails details){
        this.details = details;
    }

    @Inject(
            method = "Lstone/mae2/appeng/helpers/patternprovider/PatternProviderTargetCache;find()Lappeng/helpers/patternprovider/PatternProviderTarget;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    public void injectToFind(CallbackInfoReturnable<PatternProviderTarget> cir) {
        var original = cir.getReturnValue();
        if (original != null){
            cir.setReturnValue(new PatternProviderTarget() {
                private final BlockPos pos1 = MixinMAE23.this.pos;
                private final Level lvl1 = MixinMAE23.this.lvl;
                private final IPatternDetails details1 = MixinMAE23.this.details;
                @Override
                public long insert(AEKey what, long amount, Actionable type) {
                    if (details1 != null){
                        CompoundTag tag = details1.getDefinition().getTag();
                        int c = (tag != null && tag.contains("circuit")) ? tag.getInt("circuit") : -1;
                        if (c != -1) {
                            traverseGridIfInterface(c, pos1, lvl1);
                            setCirc(c, pos1, lvl1);
                        }
                    }
                    return original.insert(what, amount, type);
                }

                @Override
                public boolean containsPatternInput(Set<AEKey> patternInputs) {
                    return original.containsPatternInput(patternInputs);
                }
            });
        }
    }


    @Unique
    private void traverseGridIfInterface(int circuit, BlockPos pos, Level level) {
        BlockEntity be = level.getBlockEntity(pos);

        IGridNode node = null;

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