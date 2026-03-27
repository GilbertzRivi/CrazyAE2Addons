package net.oktawia.crazyae2addons.mixins;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEKey;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.interfaces.IPatternProviderTargetCacheExt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Set;

@Mixin(targets = "appeng.helpers.patternprovider.PatternProviderTargetCache", remap = false, priority = 900)
public abstract class MixinGT implements IPatternProviderTargetCacheExt {

    @Shadow @Final private Direction direction;
    @Shadow @Final private BlockApiCache<?> cache;

    @Unique private IPatternDetails details = null;

    @Override
    public void setDetails(IPatternDetails details) {
        this.details = details;
    }

    @Unique
    public PatternProviderTarget find(IPatternDetails patternDetails) {
        this.details = patternDetails;

        var original = ((PatternProviderTargetCacheAccessor) (Object) this).callFind();
        if (original == null) return null;

        var bacAcc = (BlockApiCacheAccessor) (Object) this.cache;
        final Level lvl1 = bacAcc.getLevel();
        final BlockPos pos1 = bacAcc.getFromPos();
        final Direction side1 = this.direction;
        final IPatternDetails details1 = this.details;

        return new PatternProviderTarget() {

            private boolean applied = false;

            @Override
            public long insert(AEKey what, long amount, Actionable type) {
                if (!applied && details1 != null) {
                    applied = true;

                    CompoundTag tag = details1.getDefinition().getTag();
                    int c = (tag != null && tag.contains("circuit")) ? tag.getInt("circuit") : -1;
                    if (c != -1) {
                        traverseGridIfInterface(c, pos1, lvl1, side1);
                        setCirc(c, pos1, lvl1);
                    }
                }
                return original.insert(what, amount, type);
            }

            @Override
            public boolean containsPatternInput(Set<AEKey> patternInputs) {
                return original.containsPatternInput(patternInputs);
            }
        };
    }

    @Unique
    private static void traverseGridIfInterface(int circuit, BlockPos pos, Level level, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

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
