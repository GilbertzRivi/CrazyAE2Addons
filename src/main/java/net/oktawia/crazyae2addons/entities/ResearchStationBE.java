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
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyFluidRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.StructureGadgetItem;
import net.oktawia.crazyae2addons.menus.ResearchStationMenu;
import net.oktawia.crazyae2addons.misc.ResearchStationValidator;
import net.oktawia.crazyae2addons.recipes.ResearchRecipe;
import net.oktawia.crazyae2addons.recipes.ResearchRecipeType;
import net.oktawia.crazyae2addons.renderer.preview.PreviewInfo;
import net.oktawia.crazyae2addons.renderer.preview.Previewable;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ResearchStationBE extends AENetworkInvBlockEntity implements Previewable, IGridTickable, MenuProvider {

    public ResearchStationValidator validator;
    public static int MAX_ENERGY = 25_000;

    public AppEngInternalInventory inv = new AppEngInternalInventory(this, 19);
    public InternalInventory input = inv.getSubInventory(0, 18);
    public InternalInventory disk  = inv.getSubInventory(18, 19);

    private long lastTankQueryGameTime = -100;
    private int cachedWaterPct = 0;

    private PreviewInfo previewInfo = null;
    public boolean formed = false;

    private int progressTicks = 0;
    @Nullable private ResearchRecipe activeRecipe = null;

    private boolean copyingKeys = false;
    private static final int COPY_DURATION_TICKS = 200;
    private static final int COPY_ENERGY_PER_TICK = 50;
    private static final int COPY_FLUID_PER_TICK  = 25;

    public boolean preview = false;

    public static final Set<ResearchStationBE> CLIENT_INSTANCES = new HashSet<>();

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
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        this.inv.writeToNBT(data, "inv");
    }

    public ResearchStationBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.RESEARCH_STATION_BE.get(), pos, blockState);
        validator = new ResearchStationValidator();
        this.getMainNode()
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(new ItemStack(CrazyBlockRegistrar.RESEARCH_STATION.get().asItem()));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public PreviewInfo getPreviewInfo() { return previewInfo; }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setPreviewInfo(PreviewInfo info) { this.previewInfo = info; }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            CLIENT_INSTANCES.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_INSTANCES.remove(this);
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
        return Component.literal("Research Station");
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 5, false, false);
    }

    private void hardReset() {
        this.progressTicks = 0;
        this.activeRecipe = null;
        this.copyingKeys = false; // reset także trybu kopiowania
        setChanged();
    }

    @Nullable
    private ResearchRecipe findMatchingRecipe() {
        if (level == null) return null;

        SimpleContainer cont = new SimpleContainer(input.size());
        for (int i = 0; i < input.size(); i++) {
            cont.setItem(i, input.getStackInSlot(i).copy());
        }

        boolean hasDisk = !disk.getStackInSlot(0).isEmpty();

        var all = level.getRecipeManager().getAllRecipesFor(ResearchRecipeType.INSTANCE);
        for (ResearchRecipe r : all) {
            if (!r.matches(cont, level)) continue;
            if (r.requiresStabilizer && !validator.hasStabilizer(level, worldPosition)) continue;

            if (r.driveRequired) {
                if (!hasDisk) continue;
                if (diskHasKeyFor(r)) continue;
            }

            return r;
        }
        return null;
    }

    public int getProgressPct() {
        if (activeRecipe == null && !copyingKeys) return 0;
        int dur = activeRecipe != null ? Math.max(1, activeRecipe.duration) : COPY_DURATION_TICKS;
        int pct = (int) Math.round(1000.0 * this.progressTicks / dur);
        return Math.max(0, Math.min(1000, pct));
    }

    private boolean canWork(@Nullable ResearchRecipe rr) {
        if (rr == null) return false;
        if (level == null) return false;
        if (!this.formed) return false;

        if (rr.requiresStabilizer && !validator.hasStabilizer(level, worldPosition)) return false;

        if (rr.driveRequired) {
            ItemStack driveStack = disk.getStackInSlot(0);
            if (driveStack.isEmpty()) return false;
            if (diskHasKeyFor(rr)) return false;
        }

        SimpleContainer cont = new SimpleContainer(input.size());
        for (int i = 0; i < input.size(); i++) {
            cont.setItem(i, input.getStackInSlot(i).copy());
        }

        return rr.matches(cont, level);
    }

    private boolean drainPerTick(ResearchRecipe rr, boolean simulate) {
        int needE = Math.max(0, rr.energyPerTick);
        if (needE > 0) {
            int ext = storedEnergy.extractEnergy(needE, true);
            if (ext < needE) return false;
            if (!simulate) storedEnergy.extractEnergy(needE, false);
        }

        int needW = Math.max(0, rr.fluidPerTick);
        if (needW > 0) {
            IFluidHandler fh = getExternalTank();
            if (fh == null) return false;

            FluidStack req = new FluidStack(CrazyFluidRegistrar.RESEARCH_FLUID_SOURCE.get(), needW);
            FluidStack drainedSim = fh.drain(req, IFluidHandler.FluidAction.SIMULATE);
            if (drainedSim.getAmount() < needW) return false;
            if (!simulate) fh.drain(req, IFluidHandler.FluidAction.EXECUTE);
        }
        return true;
    }

    private boolean drainCopyPerTick(boolean simulate) {
        int needE = COPY_ENERGY_PER_TICK;
        int ext = storedEnergy.extractEnergy(needE, true);
        if (ext < needE) return false;
        if (!simulate) storedEnergy.extractEnergy(needE, false);

        IFluidHandler fh = getExternalTank();
        if (fh == null) return false;

        FluidStack req = new FluidStack(CrazyFluidRegistrar.RESEARCH_FLUID_SOURCE.get(), COPY_FLUID_PER_TICK);
        FluidStack drainedSim = fh.drain(req, IFluidHandler.FluidAction.SIMULATE);
        if (drainedSim.getAmount() < COPY_FLUID_PER_TICK) return false;
        if (!simulate) fh.drain(req, IFluidHandler.FluidAction.EXECUTE);

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

    private void consumeInputsFor(ResearchRecipe rr) {
        if (rr == null) return;

        Map<Item, Integer> left = new HashMap<>();
        for (ResearchRecipe.Consumable c : rr.consumables) {
            left.merge(c.item, Math.max(0, c.count), Integer::sum);
        }

        int gadgetSlot = -1;
        for (int i = 0; i < input.size(); i++) {
            ItemStack st = input.getStackInSlot(i);
            if (!st.isEmpty() && st.getItem() instanceof StructureGadgetItem) {
                gadgetSlot = i;
                break;
            }
        }
        if (!left.isEmpty()) {
            for (int i = 0; i < input.size(); i++) {
                ItemStack st = input.getStackInSlot(i);
                if (st.isEmpty()) continue;
                if (st.getItem() instanceof StructureGadgetItem) continue;

                Item it = st.getItem();
                Integer need = left.get(it);
                if (need == null || need <= 0) continue;

                int take = Math.min(need, st.getCount());
                if (take > 0) {
                    st.shrink(take);
                    input.setItemDirect(i, st.isEmpty() ? ItemStack.EMPTY : st);
                    left.put(it, need - take);
                }
            }
        }
        if (gadgetSlot >= 0) {
            ItemStack gadget = input.getStackInSlot(gadgetSlot);
            var tag = new CompoundTag();
            tag.putInt("energy", gadget.getOrCreateTag().getInt("energy"));
            gadget.setTag(tag);
            input.setItemDirect(gadgetSlot, gadget);
        }

        setChanged();
    }

    // === NOWE: logika wyszukiwania źródłowego dysku w inputach i scalania kluczy ===

    /** Zwraca indeks slotu w inputach, w którym leży dysk zawierający listę kluczy (NBT "keys"). */
    private int findSourceDriveSlot() {
        for (int i = 0; i < input.size(); i++) {
            ItemStack st = input.getStackInSlot(i);
            if (st.isEmpty()) continue;
            CompoundTag tag = st.getTag();
            if (tag != null && tag.contains(NBT_KEYS, Tag.TAG_LIST)) {
                return i;
            }
        }
        return -1;
    }

    /** Zbiera klucze z podanego ItemStacka (jeśli ma listę "keys"). */
    private Set<String> readKeys(ItemStack stack) {
        Set<String> out = new HashSet<>();
        if (stack.isEmpty()) return out;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_KEYS, Tag.TAG_LIST)) return out;
        ListTag list = tag.getList(NBT_KEYS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    /** Sprawdza czy istnieje praca kopiowania: mamy dysk w out, dysk w input, i są jakieś brakujące klucze. */
    private boolean hasCopyJobAvailable() {
        ItemStack outDrive = disk.getStackInSlot(0);
        if (outDrive.isEmpty()) return false;

        int srcSlot = findSourceDriveSlot();
        if (srcSlot < 0) return false;

        Set<String> src = readKeys(input.getStackInSlot(srcSlot));
        if (src.isEmpty()) return false;

        Set<String> dst = readKeys(outDrive);
        // czy są klucze, których nie ma na dysku wyjściowym?
        for (String k : src) {
            if (!dst.contains(k)) return true;
        }
        return false;
    }

    /** Wykonuje faktyczne dopisanie brakujących kluczy z dysku źródłowego w inputach do dysku w slocie dysku. */
    private void performCopyKeys() {
        ItemStack outDrive = disk.getStackInSlot(0);
        if (outDrive.isEmpty()) return;

        int srcSlot = findSourceDriveSlot();
        if (srcSlot < 0) return;

        ItemStack inDrive = input.getStackInSlot(srcSlot);
        if (inDrive.isEmpty()) return;

        Set<String> src = readKeys(inDrive);

        CompoundTag tag = outDrive.getOrCreateTag();
        ListTag list = tag.contains(NBT_KEYS, Tag.TAG_LIST)
                ? tag.getList(NBT_KEYS, Tag.TAG_STRING)
                : new ListTag();

        Set<String> existing = new HashSet<>();
        for (int i = 0; i < list.size(); i++) existing.add(list.getString(i));

        boolean changed = false;
        for (String k : src) {
            if (existing.add(k)) {
                list.add(StringTag.valueOf(k));
                changed = true;
            }
        }

        if (changed) {
            tag.put(NBT_KEYS, list);
            outDrive.setTag(tag);
            disk.setItemDirect(0, outDrive);
            setChanged();
        }
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (level == null || level.isClientSide) {
            return TickRateModulation.IDLE;
        }

        if (!validator.matchesStructure(getLevel(), getBlockPos(), getBlockState())) {
            this.formed = false;
            hardReset();
            return TickRateModulation.IDLE;
        } else {
            this.formed = true;
        }

        // 1) jeżeli nie trwa żaden proces, spróbuj zacząć receptę lub kopiowanie kluczy
        if (activeRecipe == null && !copyingKeys) {
            activeRecipe = findMatchingRecipe();
            progressTicks = 0;

            if (activeRecipe == null) {
                // brak przepisu – sprawdź tryb kopiowania dysk→dysk
                if (hasCopyJobAvailable()) {
                    copyingKeys = true;
                    progressTicks = 0;
                    setChanged();
                } else {
                    setChanged();
                    return TickRateModulation.IDLE;
                }
            }
        }

        // 2) obsługa kopiowania dysk→dysk
        if (copyingKeys) {
            // warunki nadal spełnione?
            if (!formed || !hasCopyJobAvailable()) {
                hardReset();
                return TickRateModulation.IDLE;
            }

            if (!drainCopyPerTick(true)) {
                hardReset();
                return TickRateModulation.IDLE;
            }
            drainCopyPerTick(false);

            progressTicks++;

            if (progressTicks >= COPY_DURATION_TICKS) {
                performCopyKeys(); // tylko dopisuje brakujące
                finishedEffect();
                hardReset();
                return TickRateModulation.IDLE;
            }

            setChanged();
            return TickRateModulation.URGENT;
        }

        // 3) standardowa logika przepisu
        if (!canWork(activeRecipe)) {
            hardReset();
            return TickRateModulation.IDLE;
        }

        if (!drainPerTick(activeRecipe, true)) {
            hardReset();
            return TickRateModulation.IDLE;
        }
        drainPerTick(activeRecipe, false);

        progressTicks++;

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
        this.copyingKeys = false; // przerwij kopiowanie przy zmianie zawartości
        setChanged();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new ResearchStationMenu(i, inventory, this);
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.RESEARCH_STATION_MENU.get(), player, locator);
    }

    private Direction getFacing() {
        var state = getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    @Nullable
    private IFluidHandler getExternalTank() {
        if (level == null) return null;
        BlockPos pos = worldPosition.relative(getFacing().getOpposite(), 4);
        var be = level.getBlockEntity(pos);
        if (be == null) return null;

        Direction sideFromTankTowardController = getFacing();
        var cap = be.getCapability(ForgeCapabilities.FLUID_HANDLER, sideFromTankTowardController);
        if (cap.isPresent()) return cap.orElse(null);

        return be.getCapability(ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
    }

    /** Zwraca rzeczywistą ilość naszego płynu w mB (nie procent). */
    public int getWaterPct() {
        if (level == null) return 0;
        if (!this.formed) return 0;

        long gt = level.getGameTime();
        if (gt - lastTankQueryGameTime < 10) {
            return cachedWaterPct;
        }
        lastTankQueryGameTime = gt;

        IFluidHandler fh = getExternalTank();
        if (fh == null) {
            cachedWaterPct = 0;
            return 0;
        }

        long water = 0;
        int tanks = fh.getTanks();
        for (int i = 0; i < tanks; i++) {
            FluidStack fs = fh.getFluidInTank(i);
            if (fs.getFluid() == CrazyFluidRegistrar.RESEARCH_FLUID_SOURCE.get()) {
                water += fs.getAmount();
            }
        }

        cachedWaterPct = (int) Math.min(Integer.MAX_VALUE, water); // mB
        return cachedWaterPct;
    }

    /** Zwraca rzeczywistą ilość FE w buforze (nie procent). */
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
