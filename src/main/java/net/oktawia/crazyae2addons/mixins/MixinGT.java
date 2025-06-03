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
import net.oktawia.crazyae2addons.interfaces.IPatternProviderTargetCacheExt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(targets = "appeng.helpers.patternprovider.PatternProviderTargetCache")
public abstract class MixinGT implements IPatternProviderTargetCacheExt {

    @Unique private BlockPos pos = null;
    @Unique private Level lvl = null;
    @Unique private IPatternDetails details = null;
    @Shadow
    @Final
    private Direction direction;

    @Inject(
            method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lappeng/api/networking/security/IActionSource;)V",
            at = @At("RETURN")
    )
    private void atCtorTail(ServerLevel l, BlockPos pos, Direction direction, IActionSource src, CallbackInfo ci){
        this.pos = pos;
        this.lvl = l;
    }

    @Unique
    public PatternProviderTarget find(IPatternDetails patternDetails) {
        this.details = patternDetails;
        return ((PatternProviderTargetCacheAccessor) this).callFind();
    }

    @ModifyReturnValue(
            method = "wrapMeStorage(Lappeng/api/storage/MEStorage;)Lappeng/helpers/patternprovider/PatternProviderTarget;",
            at = @At("RETURN"),
            remap = false
    )
    private PatternProviderTarget injectWrapMeStorage(PatternProviderTarget original, MEStorage storage) {
        var self = (PatternProviderTargetCacheAccessor) this;
        IActionSource src = self.getSrc();

        return new PatternProviderTarget() {
            private final BlockPos pos1 = MixinGT.this.pos;
            private final Level lvl1 = MixinGT.this.lvl;
            private final IPatternDetails details1 = MixinGT.this.details;
            @Override
            public long insert(AEKey what, long amount, Actionable type) {
                if (details1 != null) {
                    CompoundTag tag = details1.getDefinition().getTag();
                    int c = (tag != null && tag.contains("circuit")) ? tag.getInt("circuit") : -1;
                    // If the circuit setting is not valid, do nothing.
                    if (c != -1 && c != getCirc(pos1, lvl1)) {
                        // Effectively output is full.
                        return 0;
                    }
                }
                return storage.insert(what, amount, type, src);
            }

            @Override
            public boolean containsPatternInput(Set<AEKey> patternInputs) {
                var stacks = storage.getAvailableStacks();
                if (details1 != null) {
                    CompoundTag tag = details1.getDefinition().getTag();
                    int c = (tag != null && tag.contains("circuit")) ? tag.getInt("circuit") : -1;
                    // If the circuit setting is invalid, try change circuit.
                    if (c != -1 && c != getCirc(pos1, lvl1)) {
                        // Additonial item with other circuit setting.
                        if (stacks.size() > 1) return true;
                        // Check if item is in inventory without circuit.
                        for (var stack : stacks) {
                            if (stack.getKey().getId() != GenericStack.fromItemStack(GTItems.PROGRAMMED_CIRCUIT.asStack()).what().getId()) {
                                return true;
                            }
                        }
                        // Change circuit number in advance.
                        traverseGridIfInterface(c, pos1, lvl1);
                        setCirc(c, pos1, lvl1);
                        return false;
                    }
                    // If the circuit setting is valid, freely check ingredient.
                }
                for (var stack : stacks) {
                    if (patternInputs.contains(stack.getKey().dropSecondary())) {
                        return true;
                    }
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

    @Unique
    private static int getCirc(BlockPos pos, Level lvl) {
        if (!CrazyConfig.COMMON.enableCPP.get()) return 0;
        try {
            var machine = SimpleTieredMachine.getMachine(lvl, pos);
            NotifiableItemStackHandler inv;
            if (machine instanceof SimpleTieredMachine STM) {
                inv = STM.getCircuitInventory();
            } else if (machine instanceof ItemBusPartMachine IBPM) {
                inv = IBPM.getCircuitInventory();
            } else if (machine instanceof FluidHatchPartMachine FHPM) {
                inv = FHPM.getCircuitInventory();
            } else {
                return 0;
            }
            return IntCircuitBehaviour.getCircuitConfiguration(inv.getStackInSlot(0));
        } catch (Exception e) {
            LogUtils.getLogger().info(e.toString());
            return 0;
        }
    }
}
