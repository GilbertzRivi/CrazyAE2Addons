package net.oktawia.crazyae2addons.parts;

import appeng.api.config.*;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.core.definitions.AEItems;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.parts.automation.UpgradeablePart;
import appeng.parts.p2p.P2PModels;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.EnergyExporterMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class EnergyInterfacePart extends AEBasePart {

    private static final P2PModels MODELS = new P2PModels(
            new ResourceLocation(CrazyAddons.MODID, "part/energy_interface"));

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    private final LazyOptional<IEnergyStorage> energyStorage = LazyOptional.of(() -> new IEnergyStorage() {
        @Override public int getEnergyStored() {
            if (getMainNode().getGrid() == null) return 0;
            return (int) getMainNode().getGrid().getEnergyService().getStoredPower();
        }
        @Override public int getMaxEnergyStored() {
            if (getMainNode().getGrid() == null) return 0;
            return (int) getMainNode().getGrid().getEnergyService().getMaxStoredPower();
        }
        @Override public boolean canExtract() {
            return true;
        }
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            var grid = getMainNode().getGrid();
            if (grid == null) {
                return 0;
            }

            var energyService = grid.getEnergyService();
            double storedAE = energyService.getStoredPower();
            double maxAE = energyService.getMaxStoredPower();
            double minAllowed = Math.min(0.3 * maxAE, 0.5e9);

            double available = storedAE - minAllowed;
            if (available <= 0) return 0;

            double maxExtractAE = Math.min(available, maxExtract / 2.0);

            double extracted = energyService.extractAEPower(
                    maxExtractAE,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE,
                    PowerMultiplier.ONE
            );

            return (int) (extracted * 2);
        }
        @Override public boolean canReceive() {
            return true;
        }
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            var grid = getMainNode().getGrid();
            if (grid == null) {
                return 0;
            }
            return maxReceive - ((int) getMainNode().getGrid().getEnergyService().injectPower((double) maxReceive / 2, simulate ? Actionable.SIMULATE : Actionable.MODULATE)) * 2;
        }
    });

    public EnergyInterfacePart(IPartItem<?> partItem) {
        super(partItem);
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(1);
    }

    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(5, 5, 12, 11, 11, 13);
        bch.addBox(3, 3, 13, 13, 13, 14);
        bch.addBox(2, 2, 14, 14, 14, 16);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        if (cap == ForgeCapabilities.ENERGY && CrazyConfig.COMMON.EnergyInterfaceEnabled.get()) {
            return energyStorage.cast();
        }
        return super.getCapability(cap);
    }
}