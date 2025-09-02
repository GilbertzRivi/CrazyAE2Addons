package net.oktawia.crazyae2addons.parts;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartItem;
import appeng.me.service.P2PService;
import appeng.parts.p2p.FluidP2PTunnelPart;
import appeng.parts.p2p.ItemP2PTunnelPart;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.mixins.P2PTunnelPartAccessor;
import org.jetbrains.annotations.Nullable;

public class ExtractingFluidP2PTunnelPart extends FluidP2PTunnelPart implements IGridTickable {

    private final int speed = CrazyConfig.COMMON.Fluidp2pSpeed.get();

    public ExtractingFluidP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().addService(IGridTickable.class, this);
    }
    
    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (isClientSide()) {
            return true;
        }

        if (hand == InteractionHand.OFF_HAND) {
            return false;
        }

        var is = player.getItemInHand(hand);

        if (!is.isEmpty() && is.getItem() instanceof IMemoryCard mc) {
            var configData = mc.getData(is);
            if (configData.contains("p2pType") || configData.contains("p2pFreq") || !configData.contains("extractingFluid")) {
                mc.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
                return false;
            } else {
                this.importSettings(SettingsFrom.MEMORY_CARD, configData, player);
                mc.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                return true;
            }
        }
        return false;
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        if (input.contains("myFreq")) {
            var freq = input.getShort("myFreq");

            ((P2PTunnelPartAccessor)this).setOutput(true);
            var grid = getMainNode().getGrid();
            if (grid != null) {
                P2PService.get(grid).updateFreq(this, freq);
            } else {
                setFrequency(freq);
                onTunnelNetworkChange();
            }
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        if (mode == SettingsFrom.MEMORY_CARD) {
            if (!output.getAllKeys().isEmpty()) {
                var iterator = output.getAllKeys().iterator();
                while (iterator.hasNext()){
                    iterator.next();
                    iterator.remove();
                }
            };
            output.putString("myType", IPartItem.getId(getPartItem()).toString());
            output.putBoolean("extractingFluid", true);

            if (getFrequency() != 0) {
                output.putShort("myFreq", getFrequency());

                var colors = Platform.p2p().toColors(getFrequency());
                var colorCode = new int[] { colors[0].ordinal(), colors[0].ordinal(), colors[1].ordinal(),
                        colors[1].ordinal(), colors[2].ordinal(), colors[2].ordinal(), colors[3].ordinal(),
                        colors[3].ordinal(), };
                output.putIntArray(IMemoryCard.NBT_COLOR_CODE, colorCode);
            }
        }
    }
    
    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!this.isOutput() || !this.isActive() || this.getOutputs().isEmpty()) {
            return TickRateModulation.IDLE;
        }

        if (getBlockEntity() == null || getBlockEntity().getLevel() == null || getBlockEntity().getLevel().isClientSide) {
            return TickRateModulation.IDLE;
        }

        try (CapabilityGuard guard = this.getInputCapability()) {
            IFluidHandler input = guard.get();
            int toDrain = speed;

            for (int tank = 0; tank < input.getTanks() && toDrain > 0; tank++) {
                FluidStack available = input.getFluidInTank(tank);
                if (available.isEmpty()) continue;

                FluidStack toTryDrain = available.copy();
                toTryDrain.setAmount(Math.min(available.getAmount(), toDrain));

                FluidStack drainedSimulated = input.drain(toTryDrain, IFluidHandler.FluidAction.SIMULATE);
                if (drainedSimulated.isEmpty()) continue;

                int filled = inputHandler.fill(drainedSimulated, IFluidHandler.FluidAction.EXECUTE);

                if (filled > 0) {
                    FluidStack drained = input.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    toDrain -= drained.getAmount();
                }
            }

            return TickRateModulation.FASTER;
        } catch (Exception e) {
            return TickRateModulation.IDLE;
        }
    }
}
