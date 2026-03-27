package net.oktawia.crazyae2addons.parts;

import appeng.api.config.PowerUnits;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.capabilities.Capabilities;
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
import net.minecraftforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.mixins.P2PTunnelPartAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExtractingFEP2PTunnelPart extends CapabilityP2PTunnelPart<ExtractingFEP2PTunnelPart, IEnergyStorage> implements IGridTickable {

    private static final P2PModels MODELS =
            new P2PModels(AppEng.makeId("part/p2p/p2p_tunnel_fe"));

    private static final IEnergyStorage NULL_ENERGY_STORAGE =
            new ExtractingFEP2PTunnelPart.NullEnergyStorage();

    private final int speed = CrazyConfig.COMMON.FEp2pSpeed.get();

    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (getBlockEntity() == null || getBlockEntity().getLevel() == null || getBlockEntity().getLevel().isClientSide) {
            return TickRateModulation.IDLE;
        }

        int totalCanReceive = 0;
        for (ExtractingFEP2PTunnelPart target : getOutputs()) {
            try (CapabilityGuard outGuard = target.getAdjacentCapability()) {
                IEnergyStorage outCap = outGuard.get();
                if (outCap != null && outCap.canReceive()) {
                    totalCanReceive += outCap.receiveEnergy(speed, true);
                }
            }
        }
        if (totalCanReceive <= 0) {
            return TickRateModulation.IDLE;
        }

        int canGive;
        try (CapabilityGuard inGuard = getAdjacentCapability()) {
            IEnergyStorage inCap = inGuard.get();
            if (inCap == null || !inCap.canExtract()) {
                return TickRateModulation.IDLE;
            }
            canGive = inCap.extractEnergy(speed, true);
        }
        if (canGive <= 0) {
            return TickRateModulation.IDLE;
        }

        int toExtract = Math.min(canGive, totalCanReceive);

        int pulled;
        try (CapabilityGuard inGuard = getAdjacentCapability()) {
            pulled = inGuard.get().extractEnergy(toExtract, false);
        }
        if (pulled <= 0) {
            return TickRateModulation.IDLE;
        }

        int remaining = pulled;
        int sentTotal = 0;
        for (ExtractingFEP2PTunnelPart target : getOutputs()) {
            if (remaining <= 0) break;
            try (CapabilityGuard outGuard = target.getAdjacentCapability()) {
                IEnergyStorage outCap = outGuard.get();
                if (outCap == null || !outCap.canReceive()) {
                    continue;
                }
                int canAccept = outCap.receiveEnergy(remaining, true);
                if (canAccept <= 0) {
                    continue;
                }
                int accepted = outCap.receiveEnergy(remaining, false);
                remaining -= accepted;
                sentTotal += accepted;
            }
        }

        if (sentTotal > 0) {
            deductEnergyCost(sentTotal, PowerUnits.FE);
        }

        return TickRateModulation.IDLE;
    }

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
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
            if (configData.contains("p2pType") || configData.contains("p2pFreq") || !configData.contains("extractingFE")) {
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
            output.putBoolean("extractingFE", true);

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

    public ExtractingFEP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, Capabilities.FORGE_ENERGY);
        this.getMainNode().addService(IGridTickable.class, this);
        inputHandler = new InputEnergyStorage();
        outputHandler = new OutputEnergyStorage();
        emptyHandler = NULL_ENERGY_STORAGE;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    private class InputEnergyStorage implements IEnergyStorage {
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int totalAccept = 0;
            for (ExtractingFEP2PTunnelPart target : ExtractingFEP2PTunnelPart.this.getOutputs()) {
                try (CapabilityGuard guard = target.getAdjacentCapability()) {
                    IEnergyStorage out = guard.get();
                    totalAccept += out.receiveEnergy(Integer.MAX_VALUE, true);
                }
            }
            if (totalAccept <= 0 || maxReceive <= 0) {
                return 0;
            }

            int inputAvailable;
            try (CapabilityGuard guard = getAdjacentCapability()) {
                IEnergyStorage in = guard.get();
                inputAvailable = in.extractEnergy(Integer.MAX_VALUE, true);
            }

            int toExtract = Math.min(Math.min(maxReceive, totalAccept), inputAvailable);
            if (toExtract <= 0) {
                return 0;
            }

            int extracted;
            try (CapabilityGuard guard = getAdjacentCapability()) {
                extracted = guard.get().extractEnergy(toExtract, simulate);
            }

            int remaining = extracted;
            int sent = 0;
            for (ExtractingFEP2PTunnelPart target : ExtractingFEP2PTunnelPart.this.getOutputs()) {
                if (remaining <= 0) break;
                try (CapabilityGuard guard = target.getAdjacentCapability()) {
                    IEnergyStorage out = guard.get();
                    int accepted = out.receiveEnergy(remaining, simulate);
                    remaining -= accepted;
                    sent += accepted;
                }
            }

            if (!simulate && sent > 0) {
                deductEnergyCost(sent, PowerUnits.FE);
            }
            return sent;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        @Override
        public int getMaxEnergyStored() {
            int total = 0;
            for (ExtractingFEP2PTunnelPart target : ExtractingFEP2PTunnelPart.this.getOutputs()) {
                try (CapabilityGuard guard = target.getAdjacentCapability()) {
                    total += guard.get().receiveEnergy(Integer.MAX_VALUE, true);
                }
            }
            return total;
        }

        @Override
        public int getEnergyStored() {
            return getMaxEnergyStored();
        }
    }

    private class OutputEnergyStorage implements IEnergyStorage {
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            try (CapabilityGuard input = getInputCapability()) {
                final int total = input.get().extractEnergy(maxExtract, simulate);
                if (!simulate && total > 0) {
                    deductEnergyCost(total, PowerUnits.FE);
                }
                return total;
            }
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public boolean canExtract() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().canExtract();
            }
        }

        @Override
        public boolean canReceive() {
            return false;
        }

        @Override
        public int getMaxEnergyStored() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getMaxEnergyStored();
            }
        }

        @Override
        public int getEnergyStored() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getEnergyStored();
            }
        }
    }

    private static class NullEnergyStorage implements IEnergyStorage {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return 0; }
        @Override public int getMaxEnergyStored() { return 0; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return false; }
    }
}
