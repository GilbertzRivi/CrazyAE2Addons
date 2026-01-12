package net.oktawia.crazyae2addonslite.parts;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKeyType;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.me.service.P2PService;
import appeng.parts.p2p.CapabilityP2PTunnelPart;
import appeng.parts.p2p.P2PModels;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.oktawia.crazyae2addonslite.Utils;
import net.oktawia.crazyae2addonslite.mixins.P2PTunnelPartAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RRFluidP2PTunnelPart extends CapabilityP2PTunnelPart<RRFluidP2PTunnelPart, IFluidHandler> {

    private static final P2PModels MODELS = new P2PModels(AppEng.makeId("part/p2p/p2p_tunnel_fluids"));
    private static final IFluidHandler NULL_FLUID_HANDLER = new NullFluidHandler();

    private int ContainerIndex;

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public RRFluidP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, ForgeCapabilities.FLUID_HANDLER);
        inputHandler = new InputFluidHandler();
        outputHandler = new OutputFluidHandler();
        emptyHandler = NULL_FLUID_HANDLER;
        ContainerIndex = 0;
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

            if (configData.contains("p2pType") || configData.contains("p2pFreq") || !configData.contains("RRFluid")) {
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

            ((P2PTunnelPartAccessor) this).setOutput(true);
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
                while (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            }

            output.putString("myType", IPartItem.getId(getPartItem()).toString());
            output.putBoolean("RRFluid", true);
            output.putShort("myFreq", getFrequency());

            var colors = Platform.p2p().toColors(getFrequency());
            var colorCode = new int[]{
                    colors[0].ordinal(), colors[0].ordinal(),
                    colors[1].ordinal(), colors[1].ordinal(),
                    colors[2].ordinal(), colors[2].ordinal(),
                    colors[3].ordinal(), colors[3].ordinal(),
            };
            output.putIntArray(IMemoryCard.NBT_COLOR_CODE, colorCode);
        }
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    private class InputFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int total = 0;

            final int outputTunnels = RRFluidP2PTunnelPart.this.getOutputs().size();
            final int amount = resource.getAmount();

            if (outputTunnels == 0 || amount == 0) {
                return 0;
            }

            final int amountPerOutput = amount / outputTunnels;
            int overflow = amountPerOutput == 0 ? amount : amount % amountPerOutput;

            List<RRFluidP2PTunnelPart> outputs = Utils.rotate(RRFluidP2PTunnelPart.this.getOutputs(), ContainerIndex);

            for (RRFluidP2PTunnelPart target : outputs) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final IFluidHandler output = capabilityGuard.get();
                    final int toSend = amountPerOutput + overflow;

                    if (toSend <= 0) {
                        break;
                    }

                    final FluidStack fillWith = resource.copy();
                    fillWith.setAmount(toSend);

                    final int received = output.fill(fillWith, action);

                    overflow = toSend - received;
                    total += received;
                }
            }

            if (action == FluidAction.EXECUTE) {
                deductTransportCost(total, AEKeyType.fluids());

                ContainerIndex += 1;
                if (ContainerIndex >= outputTunnels) {
                    ContainerIndex = 0;
                }
            }

            return total;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private class OutputFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getTanks();
            }
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getFluidInTank(tank);
            }
        }

        @Override
        public int getTankCapacity(int tank) {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getTankCapacity(tank);
            }
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().isFluidValid(tank, stack);
            }
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            try (CapabilityGuard input = getInputCapability()) {
                FluidStack result = input.get().drain(resource, action);

                if (action.execute()) {
                    deductTransportCost(result.getAmount(), AEKeyType.fluids());
                }

                return result;
            }
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            try (CapabilityGuard input = getInputCapability()) {
                FluidStack result = input.get().drain(maxDrain, action);

                if (action.execute()) {
                    deductTransportCost(result.getAmount(), AEKeyType.fluids());
                }

                return result;
            }
        }
    }

    private static class NullFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 0;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
