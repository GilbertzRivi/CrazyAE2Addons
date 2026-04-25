package net.oktawia.crazyae2addons.parts.p2p;

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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.mixins.accessors.P2PTunnelPartAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RRFluidP2PTunnelPart extends CapabilityP2PTunnelPart<RRFluidP2PTunnelPart, IFluidHandler> {

    private static final P2PModels MODELS = new P2PModels(AppEng.makeId("part/p2p/p2p_tunnel_fluids"));
    private static final IFluidHandler NULL_FLUID_HANDLER = new NullFluidHandler();

    private int containerIndex = 0;

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public RRFluidP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, ForgeCapabilities.FLUID_HANDLER);
        this.inputHandler = new InputFluidHandler();
        this.outputHandler = new OutputFluidHandler();
        this.emptyHandler = NULL_FLUID_HANDLER;
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (isClientSide()) {
            return true;
        }

        if (hand == InteractionHand.OFF_HAND) {
            return false;
        }

        var heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof IMemoryCard memoryCard)) {
            return false;
        }

        var data = memoryCard.getData(heldItem);
        if (isInvalidMemoryCardData(data, "RRFluid")) {
            memoryCard.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
            return false;
        }

        importSettings(SettingsFrom.MEMORY_CARD, data, player);
        memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
        return true;
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        if (!input.contains("myFreq")) {
            return;
        }

        short freq = input.getShort("myFreq");
        ((P2PTunnelPartAccessor) this).setOutput(true);

        var grid = getMainNode().getGrid();
        if (grid != null) {
            P2PService.get(grid).updateFreq(this, freq);
        } else {
            setFrequency(freq);
            onTunnelNetworkChange();
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        if (mode != SettingsFrom.MEMORY_CARD) {
            return;
        }

        clearTag(output);
        output.putString("myType", IPartItem.getId(getPartItem()).toString());
        output.putBoolean("RRFluid", true);
        output.putShort("myFreq", getFrequency());
        output.putIntArray(IMemoryCard.NBT_COLOR_CODE, createColorCode());
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(isPowered(), isActive());
    }

    private boolean isInvalidMemoryCardData(CompoundTag data, String markerKey) {
        return data.contains("p2pType") || data.contains("p2pFreq") || !data.contains(markerKey);
    }

    private void clearTag(CompoundTag tag) {
        for (String key : List.copyOf(tag.getAllKeys())) {
            tag.remove(key);
        }
    }

    private int[] createColorCode() {
        var colors = Platform.p2p().toColors(getFrequency());
        return new int[] {
                colors[0].ordinal(), colors[0].ordinal(),
                colors[1].ordinal(), colors[1].ordinal(),
                colors[2].ordinal(), colors[2].ordinal(),
                colors[3].ordinal(), colors[3].ordinal()
        };
    }

    private List<RRFluidP2PTunnelPart> getRotatedOutputs() {
        var outputs = new ArrayList<>(getOutputs());
        if (outputs.isEmpty()) {
            return outputs;
        }

        int startIndex = Math.floorMod(containerIndex, outputs.size());
        Collections.rotate(outputs, -startIndex);
        return outputs;
    }

    private void advanceContainerIndex(int outputCount) {
        if (outputCount <= 0) {
            containerIndex = 0;
            return;
        }

        containerIndex++;
        if (containerIndex >= outputCount) {
            containerIndex = 0;
        }
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
            if (!CrazyConfig.COMMON.RR_FLUID_P2P_ENABLED.get()) {
                return 0;
            }

            int amount = resource.getAmount();
            if (amount <= 0) {
                return 0;
            }

            var outputs = getRotatedOutputs();
            int outputCount = outputs.size();
            if (outputCount == 0) {
                return 0;
            }

            int basePerOutput = amount / outputCount;
            int remainderToDistribute = amount % outputCount;

            int totalFilled = 0;
            int carry = 0;

            for (int i = 0; i < outputCount; i++) {
                int planned = basePerOutput + (i < remainderToDistribute ? 1 : 0) + carry;
                if (planned <= 0) {
                    continue;
                }

                var target = outputs.get(i);
                try (CapabilityGuard guard = target.getAdjacentCapability()) {
                    IFluidHandler output = guard.get();

                    FluidStack toFill = resource.copy();
                    toFill.setAmount(planned);

                    int filled = output.fill(toFill, action);
                    totalFilled += filled;
                    carry = planned - filled;
                }
            }

            if (action == FluidAction.EXECUTE && totalFilled > 0) {
                deductTransportCost(totalFilled, AEKeyType.fluids());
                advanceContainerIndex(outputCount);
            }

            return totalFilled;
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

                if (action.execute() && !result.isEmpty()) {
                    deductTransportCost(result.getAmount(), AEKeyType.fluids());
                }

                return result;
            }
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            try (CapabilityGuard input = getInputCapability()) {
                FluidStack result = input.get().drain(maxDrain, action);

                if (action.execute() && !result.isEmpty()) {
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