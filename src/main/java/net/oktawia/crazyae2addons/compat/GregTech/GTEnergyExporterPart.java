package net.oktawia.crazyae2addons.compat.GregTech;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.parts.IPartItem;
import appeng.core.definitions.AEItems;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.common.data.GTItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.parts.EnergyExporterPart;

public class GTEnergyExporterPart extends EnergyExporterPart {
    public GTEnergyExporterPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        this.greg = !this.inv.getStackInSlot(0).isEmpty();
        if (this.getMenu() != null){
            this.getMenu().greg = this.greg;
        }
        if (!greg) return;
        Item tier = this.inv.getStackInSlot(0).getItem();
        if (tier == GTItems.BATTERY_LV_LITHIUM.asItem()){
            voltage = (int) Math.pow(2, 5);
        } else if (tier == GTItems.BATTERY_MV_LITHIUM.asItem()){
            voltage = (int) Math.pow(2, 7);
        } else if (tier == GTItems.BATTERY_HV_LITHIUM.asItem()){
            voltage = (int) Math.pow(2, 9);
        } else if (tier == GTItems.BATTERY_EV_VANADIUM.asItem()){
            voltage = (int) Math.pow(2, 11);
        } else if (tier == GTItems.BATTERY_IV_VANADIUM.asItem()){
            voltage = (int) Math.pow(2, 13);
        } else if (tier == GTItems.BATTERY_LuV_VANADIUM.asItem()){
            voltage = (int) Math.pow(2, 15);
        } else if (tier == GTItems.BATTERY_ZPM_NAQUADRIA.asItem()){
            voltage = (int) Math.pow(2, 17);
        } else if (tier == GTItems.BATTERY_UV_NAQUADRIA.asItem()){
            voltage = (int) Math.pow(2, 19);
        } else {
            this.greg = false;
            if (this.getMenu() != null){
                this.getMenu().greg = this.greg;
            }
        }
        if (this.getMenu() != null){
            this.getMenu().voltage = this.voltage;
        }
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!initialized) {
            upgradesChanged();
            onChangeInventory(this.inv, 0);
            initialized = true;
        }

        BlockEntity neighbor = getLevel().getBlockEntity(getBlockEntity().getBlockPos().relative(getSide()));
        transfered = "0";

        if (neighbor != null) {
            if (getGridNode() == null) return TickRateModulation.IDLE;
            var energyService = getGridNode().getGrid().getEnergyService();
            double stored = energyService.getStoredPower();
            double maxStored = energyService.getMaxStoredPower();

            double minAllowed = stored < 1_000_000_000 ? maxStored * 0.33 : 1_000_000_000;

            if (this.greg && CrazyConfig.COMMON.GregEnergyExporter.get()) {
                neighbor.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, getSide().getOpposite()).ifPresent(gtStorage -> {
                    long euCanInsert = gtStorage.getEnergyCanBeInserted();

                    double maxFE = voltage * maxAmps * FeCompat.ratio(false);
                    double feToTransfer = Math.min(maxFE, euCanInsert);
                    double aeRequired = feToTransfer / 2.0;

                    double maxExtractable = Math.max(0, stored - minAllowed);
                    aeRequired = Math.min(aeRequired, maxExtractable);

                    if (aeRequired > 0) {
                        double extractedSim = energyService.extractAEPower(aeRequired, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                        if (extractedSim >= aeRequired) {
                            double extracted = energyService.extractAEPower(aeRequired, Actionable.MODULATE, PowerMultiplier.CONFIG);

                            gtStorage.acceptEnergyFromNetwork(getSide().getOpposite(), voltage, maxAmps);
                            transfered = Utils.shortenNumber(extracted * 2);
                        }
                    }
                });
            } else {
                neighbor.getCapability(ForgeCapabilities.ENERGY, getSide().getOpposite()).ifPresent(feStorage -> {
                    if (!feStorage.canReceive()) return;

                    int speedLevel = getInstalledUpgrades(AEItems.SPEED_CARD);
                    int maxTransfer = (int) Math.pow(64, speedLevel);
                    int energyMissing = feStorage.getMaxEnergyStored() - feStorage.getEnergyStored();

                    int feToTransfer = Math.min(maxTransfer, energyMissing);
                    double aeRequired = feToTransfer / 2.0;

                    double maxExtractable = Math.max(0, stored - minAllowed);
                    aeRequired = Math.min(aeRequired, maxExtractable);

                    if (aeRequired > 0) {
                        double extractedSim = energyService.extractAEPower(aeRequired, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                        if (extractedSim >= aeRequired) {
                            double extracted = energyService.extractAEPower(aeRequired, Actionable.MODULATE, PowerMultiplier.CONFIG);
                            int accepted = feStorage.receiveEnergy(feToTransfer, false);

                            transfered = Utils.shortenNumber(extracted * 2 * accepted / feToTransfer);
                        }
                    }
                });
            }
        }

        if (this.getMenu() != null) {
            this.getMenu().transfered = transfered;
        }

        return TickRateModulation.IDLE;
    }
}
