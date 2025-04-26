package net.oktawia.crazyae2addons.parts;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKeyType;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.p2p.CapabilityP2PTunnelPart;
import appeng.parts.p2p.P2PModels;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.ChunkyFluidP2PTunnelMenu;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChunkyFluidP2PTunnelPart extends CapabilityP2PTunnelPart<ChunkyFluidP2PTunnelPart, IFluidHandler> implements MenuProvider {

    private static final P2PModels MODELS = new P2PModels(AppEng.makeId("part/p2p/p2p_tunnel_fluids"));
    private static final IFluidHandler NULL_FLUID_HANDLER = new NullFluidHandler();
    private int ContainerIndex;
    public int unitSize = 1000;

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public ChunkyFluidP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, ForgeCapabilities.FLUID_HANDLER);
        inputHandler = new InputFluidHandler();
        outputHandler = new OutputFluidHandler();
        emptyHandler = NULL_FLUID_HANDLER;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ChunkyFluidP2PTunnelMenu(containerId, playerInventory, this);
    }

    @Override
    public boolean onPartActivate(Player p, InteractionHand hand, Vec3 pos) {
        var is = p.getItemInHand(hand);
        if (!p.getCommandSenderWorld().isClientSide() && is.isEmpty()) {
            MenuOpener.open(CrazyMenuRegistrar.CHUNKY_FLUID_P2P_TUNNEL_MENU.get(), p, MenuLocators.forPart(this));
            return true;
        } else {
            return super.onPartActivate(p, hand, pos);
        }
    }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName();
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
            final int outputTunnels = ChunkyFluidP2PTunnelPart.this.getOutputs().size();
            final int amount = resource.getAmount();
            final int unit = ChunkyFluidP2PTunnelPart.this.unitSize;
            if (outputTunnels == 0 || amount < unit) return 0;

            int availableUnits = amount / unit;
            final int unitsPerOutput = availableUnits / outputTunnels;
            int overflowUnits = unitsPerOutput == 0 ? availableUnits : availableUnits % unitsPerOutput;

            List<ChunkyFluidP2PTunnelPart> outputs = Utils.rotate(ChunkyFluidP2PTunnelPart.this.getOutputs(), ContainerIndex);

            for (ChunkyFluidP2PTunnelPart target : outputs) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final IFluidHandler output = capabilityGuard.get();
                    final int toSendUnits = unitsPerOutput + overflowUnits;
                    if (toSendUnits <= 0) break;
                    FluidStack fillStack = resource.copy();
                    fillStack.setAmount(toSendUnits * unit);
                    final int received = output.fill(fillStack, action);
                    int transferredUnits = received / unit;
                    overflowUnits = toSendUnits - transferredUnits;
                    total += received;
                }
            }

            if (action == FluidAction.EXECUTE) {
                deductTransportCost(total, AEKeyType.fluids());
                ContainerIndex++;
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