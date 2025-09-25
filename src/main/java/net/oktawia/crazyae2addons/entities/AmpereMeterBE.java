package net.oktawia.crazyae2addons.entities;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator; // <-- ważne
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;         // NeoForge
import net.neoforged.neoforge.energy.IEnergyStorage;            // NeoForge

import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.menus.AmpereMeterMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AmpereMeterBE extends AEBaseBlockEntity implements MenuProvider {

    public AmpereMeterMenu menu;
    public boolean direction = false;
    public String transfer = "-";
    public String unit = "-";
    public Integer numTransfer = 0;
    public HashMap<Integer, Integer> maxTrans = new HashMap<>();
    private int lastTick = 0;
    private long secondBuffer = 0;

    public AmpereMeterBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.AMPERE_METER_BE.get(), pos, blockState);
    }

    public void setMenu(AmpereMeterMenu menu){
        this.menu = menu;
    }
    public AmpereMeterMenu getMenu(){
        return this.menu;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player pPlayer) {
        return new AmpereMeterMenu(i, inventory, this);
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("dir")) {
            this.direction = tag.getBoolean("dir");
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("dir", this.direction);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Ampere Meter");
    }

    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.AMPERE_METER_MENU.get(), player, locator);
    }

    public final IEnergyStorage feLogicInput = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (AmpereMeterBE.this.getLevel() == null) return 0;

            Direction outputSide = !AmpereMeterBE.this.direction
                    ? Utils.getRightDirection(getBlockState())
                    : Utils.getLeftDirection(getBlockState());

            BlockPos outPos = AmpereMeterBE.this.getBlockPos().relative(outputSide);

            AtomicInteger transferred = new AtomicInteger();
            var lvl = AmpereMeterBE.this.getLevel();

            // NeoForge lookup: level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side)
            IEnergyStorage out = lvl.getCapability(Capabilities.EnergyStorage.BLOCK, outPos, outputSide.getOpposite());
            if (out != null) {
                transferred.set(out.receiveEnergy(maxReceive, simulate));
            }

            if (level != null && level.getServer() != null) {
                int currentTick = level.getServer().getTickCount();

                if (!simulate) secondBuffer += transferred.get();

                if (lastTick != -1 && (currentTick - lastTick) >= 20) {
                    int fePerTick = Math.toIntExact(secondBuffer / (currentTick - lastTick));
                    AmpereMeterBE.this.unit = "FE/t";
                    AmpereMeterBE.this.numTransfer = fePerTick;
                    AmpereMeterBE.this.transfer = Utils.shortenNumber(fePerTick);

                    if (AmpereMeterBE.this.getMenu() != null) {
                        AmpereMeterBE.this.getMenu().unit = AmpereMeterBE.this.unit;
                        AmpereMeterBE.this.getMenu().transfer = AmpereMeterBE.this.transfer;
                    }

                    secondBuffer = 0;
                    lastTick = currentTick;
                } else if (lastTick == -1) {
                    lastTick = currentTick;
                }
            }

            return transferred.get();
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return 0; }
        @Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    public final IEnergyStorage feLogicOutput = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (AmpereMeterBE.this.getLevel() == null) return 0;

            Direction inputSide = AmpereMeterBE.this.direction
                    ? Utils.getRightDirection(getBlockState())
                    : Utils.getLeftDirection(getBlockState());

            BlockPos inPos = AmpereMeterBE.this.getBlockPos().relative(inputSide);

            AtomicInteger transferred = new AtomicInteger();
            var lvl = AmpereMeterBE.this.getLevel();

            IEnergyStorage input = lvl.getCapability(Capabilities.EnergyStorage.BLOCK, inPos, inputSide.getOpposite());
            if (input != null) {
                transferred.set(input.receiveEnergy(maxExtract, simulate));
            }

            if (level != null && level.getServer() != null) {
                int currentTick = level.getServer().getTickCount();

                if (!simulate) secondBuffer += transferred.get();

                if (lastTick != -1 && (currentTick - lastTick) >= 20) {
                    int fePerTick = Math.toIntExact(secondBuffer / (currentTick - lastTick));
                    AmpereMeterBE.this.unit = "FE/t";
                    AmpereMeterBE.this.numTransfer = fePerTick;
                    AmpereMeterBE.this.transfer = Utils.shortenNumber(fePerTick);

                    if (AmpereMeterBE.this.getMenu() != null) {
                        AmpereMeterBE.this.getMenu().unit = AmpereMeterBE.this.unit;
                        AmpereMeterBE.this.getMenu().transfer = AmpereMeterBE.this.transfer;
                    }

                    secondBuffer = 0;
                    lastTick = currentTick;
                } else if (lastTick == -1) {
                    lastTick = currentTick;
                }
            }

            return transferred.get();
        }

        @Override public int getEnergyStored() { return Integer.MAX_VALUE; }
        @Override public int getMaxEnergyStored() { return 0; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };
}
