package net.oktawia.crazyae2addons.entities;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.menus.ResearchStationMenu;
import net.oktawia.crazyae2addons.recipes.ResearchRecipe;
import net.oktawia.crazyae2addons.recipes.ResearchRecipeType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ResearchStationBE extends AENetworkInvBlockEntity implements IGridTickable, MenuProvider {

    public static int MAX_ENERGY = 25_000;

    public AppEngInternalInventory inv = new AppEngInternalInventory(this, 1);
    public InternalInventory disk = inv.getSubInventory(0, 1);

    private int progressTicks = 0;

    @Nullable
    private ResearchRecipe activeRecipe = null;

    private boolean copyingKeys = false;
    private static final int COPY_DURATION_TICKS = 200;
    private static final int COPY_ENERGY_PER_TICK = 50;

    public boolean preview = false;

    public static final Set<ResearchStationBE> CLIENT_INSTANCES = new HashSet<>();

    private static class PedestalUse {
        final BlockPos topPos;
        final BlockPos bottomPos;
        int used; // ile itemów trzeba skonsumować z tego pedestału

        PedestalUse(BlockPos topPos, BlockPos bottomPos, int used) {
            this.topPos = topPos.immutable();
            this.bottomPos = bottomPos.immutable();
            this.used = used;
        }
    }

    private final List<PedestalUse> activePedestals = new ArrayList<>();

    private final EnergyStorage storedEnergy = new EnergyStorage(MAX_ENERGY, 500, 500, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            setChanged();
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            setChanged();
            return extracted;
        }
    };
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> storedEnergy);

    private static final String NBT_KEYS = "keys";

    @Nullable
    private String getUnlockKey(@Nullable ResearchRecipe rr) {
        if (rr == null || rr.unlock == null || rr.unlock.key == null) return null;
        return rr.unlock.key.toString();
    }

    private boolean diskHasKeyFor(@Nullable ResearchRecipe rr) {
        if (rr == null) return false;
        String expected = getUnlockKey(rr);
        if (expected == null) return false;

        ItemStack driveStack = disk.getStackInSlot(0);
        if (driveStack.isEmpty()) return false;

        var tag = driveStack.getOrCreateTag();
        if (!tag.contains(NBT_KEYS, Tag.TAG_LIST)) return false;

        ListTag list = tag.getList(NBT_KEYS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (expected.equals(list.getString(i))) return true;
        }
        return false;
    }

    private void writeKeyToDisk(@Nullable ResearchRecipe rr) {
        if (rr == null) return;
        String key = getUnlockKey(rr);
        if (key == null) return;

        ItemStack driveStack = disk.getStackInSlot(0);
        if (driveStack.isEmpty()) return;

        var tag = driveStack.getOrCreateTag();
        ListTag list = tag.contains(NBT_KEYS, Tag.TAG_LIST)
                ? tag.getList(NBT_KEYS, Tag.TAG_STRING)
                : new ListTag();

        for (int i = 0; i < list.size(); i++) {
            if (key.equals(list.getString(i))) {
                driveStack.setTag(tag);
                disk.setItemDirect(0, driveStack);
                setChanged();
                return;
            }
        }

        list.add(StringTag.valueOf(key));
        tag.put(NBT_KEYS, list);
        driveStack.setTag(tag);
        disk.setItemDirect(0, driveStack);
        setChanged();
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("inv")) {
            this.inv.readFromNBT(data, "inv");
        }
        if (data.contains("pwr")) {
            this.storedEnergy.deserializeNBT(data.get("pwr"));
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        this.inv.writeToNBT(data, "inv");
        data.put("pwr", this.storedEnergy.serializeNBT());
    }

    public ResearchStationBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.RESEARCH_STATION_BE.get(), pos, blockState);
        this.getMainNode()
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(new ItemStack(CrazyBlockRegistrar.RESEARCH_STATION.get().asItem()));
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
        if (capability == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.crazyae2addons.research_station");
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 5, false, false);
    }

    private void hardReset() {
        this.progressTicks = 0;
        this.activeRecipe = null;
        this.copyingKeys = false;
        this.activePedestals.clear();
        setChanged();
    }

    private List<ItemStack> gatherPedestalStacksForMatching() {
        List<ItemStack> result = new ArrayList<>();
        if (level == null) {
            return result;
        }

        BlockPos base = this.worldPosition;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                pos.set(base.getX() + dx, base.getY() + 1, base.getZ() + dz);
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ResearchPedestalTopBE top) {
                    ItemStack stored = top.getStoredStack();
                    if (!stored.isEmpty()) {
                        result.add(stored.copy());
                    }
                }
            }
        }

        return result;
    }

    /**
     * Przypisuje konkretne pedestały do poszczególnych consumables:
     * - każdy wpis consumable = dokładnie JEDEN pedestal,
     * - c.count = minimalna ilość itemów na tym pedestale,
     * - c.computation = minimalne getConnectedComputation() wymagane dla tego pedestału.
     */
    @Nullable
    private List<PedestalUse> allocatePedestalsFor(ResearchRecipe rr) {
        if (level == null || rr == null) {
            return null;
        }

        BlockPos base = this.worldPosition;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        List<BlockPos> topPositions = new ArrayList<>();
        List<BlockPos> bottomPositions = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        List<Integer> computations = new ArrayList<>();

        // Zczytujemy wszystkie potencjalne pedestały (top + bottom + stack + computation)
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                pos.set(base.getX() + dx, base.getY() + 1, base.getZ() + dz);
                BlockEntity be = level.getBlockEntity(pos);
                if (!(be instanceof ResearchPedestalTopBE top)) {
                    continue;
                }

                ItemStack stored = top.getStoredStack();
                if (stored.isEmpty()) {
                    continue;
                }

                BlockPos bottomPos = pos.below();
                BlockEntity beBottom = level.getBlockEntity(bottomPos);
                if (!(beBottom instanceof ResearchPedestalBottomBE bottom)) {
                    continue;
                }

                int computation = bottom.getConnectedComputation();

                topPositions.add(pos.immutable());
                bottomPositions.add(bottomPos.immutable());
                stacks.add(stored.copy());
                computations.add(computation);
            }
        }

        if (topPositions.isEmpty()) {
            return null;
        }

        int n = topPositions.size();
        boolean[] usedPedestal = new boolean[n];
        List<PedestalUse> result = new ArrayList<>();

        // sortujemy consumables po wymaganej komputacji (najpierw te "cięższe")
        List<ResearchRecipe.Consumable> consumables = new ArrayList<>(rr.consumables);
        consumables.sort(Comparator.comparingInt((ResearchRecipe.Consumable c) -> c.computation).reversed());

        // każdy consumable musi dostać dokładnie jeden pedestal
        for (ResearchRecipe.Consumable c : consumables) {
            if (c.count <= 0) continue;

            boolean assigned = false;

            for (int i = 0; i < n; i++) {
                if (usedPedestal[i]) continue;

                ItemStack st = stacks.get(i);
                if (st.isEmpty()) continue;
                if (st.getItem() != c.item) continue;
                if (st.getCount() < c.count) continue;

                int availableComputation = computations.get(i);
                if (availableComputation < c.computation) continue;

                usedPedestal[i] = true;

                BlockPos topPos = topPositions.get(i);
                BlockPos bottomPos = bottomPositions.get(i);

                // użyjemy z tego pedestału dokładnie c.count itemów
                result.add(new PedestalUse(topPos, bottomPos, c.count));
                assigned = true;
                break;
            }

            if (!assigned) {
                // nie udało się znaleźć pedestału dla jednego z consumables -> całość nie jest valid
                return null;
            }
        }

        if (result.isEmpty()) {
            return null;
        }

        return result;
    }

    private boolean doPedestalsWork() {
        if (level == null) {
            return false;
        }
        if (activePedestals.isEmpty()) {
            return false;
        }

        for (PedestalUse use : activePedestals) {
            BlockEntity beBottom = level.getBlockEntity(use.bottomPos);
            if (!(beBottom instanceof ResearchPedestalBottomBE bottom)) {
                return false;
            }

            if (!bottom.doWork()) {
                return false;
            }
        }

        return true;
    }

    private int getTickComputation() {
        if (level == null || activePedestals.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (PedestalUse use : activePedestals) {
            BlockEntity beBottom = level.getBlockEntity(use.bottomPos);
            if (beBottom instanceof ResearchPedestalBottomBE bottom) {
                int c = bottom.getConnectedComputation();
                if (c > 0) {
                    sum += c;
                }
            }
        }
        return sum;
    }

    @Nullable
    private ResearchRecipe findMatchingRecipe() {
        if (level == null) return null;

        List<ItemStack> pedestalStacks = gatherPedestalStacksForMatching();

        int totalSize = pedestalStacks.size();
        SimpleContainer cont = new SimpleContainer(totalSize);

        for (int i = 0; i < pedestalStacks.size(); i++) {
            cont.setItem(i, pedestalStacks.get(i));
        }

        boolean hasDisk = !disk.getStackInSlot(0).isEmpty();

        var all = level.getRecipeManager().getAllRecipesFor(ResearchRecipeType.INSTANCE);
        for (ResearchRecipe r : all) {
            if (!r.matches(cont, level)) continue;

            if (r.driveRequired) {
                if (!hasDisk) continue;
                if (diskHasKeyFor(r)) continue;
            }

            List<PedestalUse> pedestals = allocatePedestalsFor(r);
            if (pedestals == null || pedestals.isEmpty()) {
                continue;
            }

            this.activePedestals.clear();
            this.activePedestals.addAll(pedestals);

            return r;
        }
        return null;
    }

    public int getProgressPct() {
        if (activeRecipe == null && !copyingKeys) return 0;

        int dur = activeRecipe != null
                ? Math.max(1, activeRecipe.duration)
                : COPY_DURATION_TICKS;

        int pct = (int) Math.round(1000.0 * this.progressTicks / (double) dur);
        return Math.max(0, Math.min(1000, pct));
    }

    private boolean canWork(@Nullable ResearchRecipe rr) {
        if (rr == null) return false;
        if (level == null) return false;

        if (rr.driveRequired) {
            ItemStack driveStack = disk.getStackInSlot(0);
            if (driveStack.isEmpty()) return false;
            if (diskHasKeyFor(rr)) return false;
        }

        List<ItemStack> pedestalStacks = gatherPedestalStacksForMatching();
        int totalSize = pedestalStacks.size();
        SimpleContainer cont = new SimpleContainer(totalSize);

        for (int i = 0; i < pedestalStacks.size(); i++) {
            cont.setItem(i, pedestalStacks.get(i));
        }

        if (!rr.matches(cont, level)) {
            return false;
        }

        List<PedestalUse> pedestals = allocatePedestalsFor(rr);
        if (pedestals == null || pedestals.isEmpty()) {
            return false;
        }
        this.activePedestals.clear();
        this.activePedestals.addAll(pedestals);

        return true;
    }

    private boolean drainPerTick(ResearchRecipe rr, boolean simulate) {
        int needE = Math.max(0, rr.energyPerTick);
        if (needE > 0) {
            int ext = storedEnergy.extractEnergy(needE, true);
            if (ext < needE) return false;
            if (!simulate) storedEnergy.extractEnergy(needE, false);
        }
        return true;
    }

    private void finishedEffect() {
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANT,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                    8, 0.2, 0.2, 0.2, 0.01);
            sl.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.4f, 1.2f);
        }
    }

    /**
     * Konsumuje itemy z pedestałów zgodnie z `PedestalUse.used`
     * (czyli tyle ile podała recepta w `Consumable.count`).
     */
    private void consumeInputsFor(ResearchRecipe rr) {
        if (rr == null || level == null) return;

        List<PedestalUse> uses = !activePedestals.isEmpty() ? activePedestals : allocatePedestalsFor(rr);
        if (uses == null) return;

        for (PedestalUse use : uses) {
            if (use.used <= 0) continue;

            BlockEntity beTop = level.getBlockEntity(use.topPos);
            if (!(beTop instanceof ResearchPedestalTopBE top)) {
                continue;
            }

            int toConsume = use.used;

            ItemStack stack = top.takeStoredStack();
            if (stack.isEmpty()) continue;

            if (stack.getCount() > toConsume) {
                stack.shrink(toConsume);
                top.setStoredStack(stack);
            }
            // jeśli <= toConsume, zjadamy cały stack – pedestal zostaje pusty
        }

        setChanged();
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (level == null || level.isClientSide) {
            return TickRateModulation.IDLE;
        }

        if (activeRecipe == null && !copyingKeys) {
            activeRecipe = findMatchingRecipe();
            progressTicks = 0;

            if (activeRecipe == null) {
                setChanged();
                return TickRateModulation.IDLE;
            }
        }

        if (!canWork(activeRecipe)) {
            hardReset();
            return TickRateModulation.IDLE;
        }

        if (!doPedestalsWork()) {
            hardReset();
            return TickRateModulation.IDLE;
        }

        if (!drainPerTick(activeRecipe, true)) {
            hardReset();
            return TickRateModulation.IDLE;
        }
        drainPerTick(activeRecipe, false);

        int tickComp = getTickComputation();
        if (tickComp <= 0) {
            hardReset();
            return TickRateModulation.IDLE;
        }

        progressTicks += tickComp;

        if (progressTicks >= activeRecipe.duration) {
            final ResearchRecipe done = activeRecipe;
            consumeInputsFor(done);
            writeKeyToDisk(done);
            finishedEffect();
            hardReset();
            setChanged();
            return TickRateModulation.IDLE;
        }

        setChanged();
        return TickRateModulation.URGENT;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.inv;
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        this.activeRecipe = null;
        this.progressTicks = 0;
        this.copyingKeys = false;
        this.activePedestals.clear();
        setChanged();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new ResearchStationMenu(i, inventory, this);
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.RESEARCH_STATION_MENU.get(), player, locator);
    }

    public int getEnergyPct() {
        return storedEnergy.getEnergyStored();
    }

    public void unlockAllToDisk() {
        if (level == null) return;

        ItemStack driveStack = disk.getStackInSlot(0);
        if (driveStack.isEmpty()) return;

        var tag = driveStack.getOrCreateTag();
        ListTag list = tag.contains(NBT_KEYS, Tag.TAG_LIST)
                ? tag.getList(NBT_KEYS, Tag.TAG_STRING)
                : new ListTag();

        java.util.Set<String> existing = new java.util.HashSet<>();
        for (int i = 0; i < list.size(); i++) existing.add(list.getString(i));

        var all = level.getRecipeManager().getAllRecipesFor(ResearchRecipeType.INSTANCE);
        for (ResearchRecipe r : all) {
            String k = getUnlockKey(r);
            if (k == null) continue;
            if (existing.add(k)) {
                list.add(StringTag.valueOf(k));
            }
        }

        tag.put(NBT_KEYS, list);
        driveStack.setTag(tag);
        disk.setItemDirect(0, driveStack);
        setChanged();
    }
}
