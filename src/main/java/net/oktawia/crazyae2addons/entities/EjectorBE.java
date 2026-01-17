package net.oktawia.crazyae2addons.entities;

import appeng.api.config.*;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.*;
import appeng.api.storage.AEKeyFilter;
import appeng.api.storage.StorageHelper;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.me.helpers.MachineSource;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.oktawia.crazyae2addons.blocks.EjectorBlock;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.IHackedProvider;
import net.oktawia.crazyae2addons.logic.HackedPatternProviderLogic;
import net.oktawia.crazyae2addons.menus.EjectorMenu;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Future;

public class EjectorBE extends AENetworkBlockEntity implements MenuProvider, IGridTickable, PatternProviderLogicHost, ICraftingRequester, IHackedProvider {

    public ConfigInventory config = ConfigInventory.configStacks(
            AEKeyFilter.none(),
            36,
            null,
            true
    );
    public ConfigInventory storage = ConfigInventory.configStacks(
            AEKeyFilter.none(),
            36,
            null,
            true
    );
    public AppEngInternalInventory pattern = new AppEngInternalInventory(1);
    public Boolean doesWait = false;
    public Future<ICraftingPlan> toCraftPlan;
    public ICraftingLink craftingLink;
    public GenericStack cantCraft = null;
    public EjectorMenu menu = null;
    public int multiplier = 1;
    public PatternProviderLogic logic;
    public GenericStack target;

    public EjectorBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.EJECTOR_BE.get(), pos, blockState);
        this.logic = new HackedPatternProviderLogic(getMainNode(), this);
        this.getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.getConfigManager().registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.EJECTOR_BLOCK.get().asItem())
                );
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("config")) {
            this.config.readFromChildTag(data, "config");
        }
        if (data.contains("storage")) {
            this.storage.readFromChildTag(data, "storage");
        }
        if (data.contains("pattern")) {
            this.pattern.readFromNBT(data, "pattern");
        }
        if (data.contains("mult")) {
            this.multiplier = data.getInt("mult");
        }
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        drops.add(pattern.getStackInSlot(0));
        for (int i = 0; i < storage.size(); i++) {
            if (storage.getKey(i) instanceof AEItemKey itemKey) {
                drops.add(itemKey.toStack());
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        this.config.writeToChildTag(data, "config");
        this.storage.writeToChildTag(data, "storage");
        this.pattern.writeToNBT(data, "pattern");
        data.putInt("mult", this.multiplier);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new EjectorMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.crazyae2addons.ejector");
    }

    @Override
    public PatternProviderLogic getLogic() {
        return this.logic;
    }

    @Override
    public EnumSet<Direction> getTargets() {
        BlockState state = this.getBlockState();
        Direction front = state.getValue(BlockStateProperties.FACING);
        return EnumSet.of(front);
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.EJECTOR_MENU.get(), player, locator);
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(CrazyBlockRegistrar.EJECTOR_BLOCK.get());
    }

    public void doWork() {
        if (getGridNode() == null || getGridNode().getGrid() == null || !getMainNode().isActive() || doesWait) return;
        this.cantCraft = GenericStack.fromItemStack(ItemStack.EMPTY);

        var tag = new CompoundTag();
        tag.putString("id", UUID.randomUUID().toString());
        this.target = new GenericStack(AEItemKey.of(CrazyBlockRegistrar.EJECTOR_BLOCK.get(), tag), 1);

        ArrayList<GenericStack> input = new ArrayList<>();
        for (int slot = 0; slot < this.config.size(); slot++) {
            var gs = this.config.getStack(slot);
            if (gs == null) continue;

            long amt = gs.amount();
            if (amt <= 0) continue;

            input.add(new GenericStack(gs.what(), amt * (long) this.multiplier));
        }
        if (input.isEmpty()) return;

        var patternStack = PatternDetailsHelper.encodeProcessingPattern(input.toArray(new GenericStack[0]), List.of(target).toArray(new GenericStack[0]));
        if (tryPushPatternImmediately(patternStack, input)) {
            this.cantCraft = null;
            if (this.menu != null) this.menu.cantCraft = "nothing";
            return;
        }

        this.getLogic().getPatternInv().setItemDirect(0, patternStack);
        this.getLogic().updatePatterns();

        toCraftPlan = getGridNode().getGrid().getCraftingService().beginCraftingCalculation(
                getLevel(),
                () -> new MachineSource(this),
                target.what(),
                target.amount(),
                CalculationStrategy.REPORT_MISSING_ITEMS
        );
    }

    public void setMenu(EjectorMenu ejectorMenu) {
        this.menu = ejectorMenu;
    }

    public ItemStack getMainMenuIcon() {
        return CrazyBlockRegistrar.EJECTOR_BLOCK.get().asItem().getDefaultInstance();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 5, false, false);
    }

    public void cancelCraft(){
        if (this.craftingLink != null){
            this.craftingLink.cancel();
        }
        getLevel().setBlockAndUpdate(getBlockPos(), getBlockState().setValue(EjectorBlock.ISCRAFTING, false));
        this.getLogic().getPatternInv().setItemDirect(0, ItemStack.EMPTY);
        this.getLogic().updatePatterns();
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {

        if (this.toCraftPlan != null && this.toCraftPlan.isDone()){
            try {
                var result = getGridNode().getGrid().getCraftingService().submitJob(
                        this.toCraftPlan.get(), this, null, true, IActionSource.ofMachine(this)
                );
                if (result.successful() && result.link() != null) {
                    this.craftingLink = result.link();
                    getLevel().setBlockAndUpdate(getBlockPos(), getBlockState().setValue(EjectorBlock.ISCRAFTING, true));
                    this.cantCraft = null;
                    if (this.menu != null){
                        this.menu.cantCraft = "nothing";
                    }
                    this.toCraftPlan = null;
                } else if (!result.successful()) {
                    var plan = this.toCraftPlan.get();
                    this.toCraftPlan = null;

                    this.getLogic().getPatternInv().setItemDirect(0, ItemStack.EMPTY);
                    this.getLogic().updatePatterns();

                    Object2LongMap.Entry<AEKey> firstMissing = null;
                    try {
                        KeyCounter missing = plan.missingItems();
                        if (missing != null && !missing.isEmpty()) {
                            firstMissing = missing.iterator().next();
                        }
                    } catch (Throwable ignored) {
                    }

                    if (firstMissing != null) {
                        this.cantCraft = new GenericStack(firstMissing.getKey(), firstMissing.getLongValue());
                        if (this.menu != null) {
                            this.menu.cantCraft = String.format(
                                    "%sx %s",
                                    this.cantCraft.what().formatAmount(this.cantCraft.amount(), AmountFormat.SLOT),
                                    this.cantCraft.what().toString()
                            );
                        }
                    } else {
                        this.cantCraft = plan.finalOutput();
                        if (this.menu != null) {
                            this.menu.cantCraft = String.format(
                                    "%sx %s",
                                    this.cantCraft.what().formatAmount(this.cantCraft.amount(), AmountFormat.SLOT),
                                    this.cantCraft.what().toString()
                            );
                        }
                    }

                    getLevel().setBlockAndUpdate(getBlockPos(),
                            getBlockState().setValue(EjectorBlock.ISCRAFTING, false));
                    this.craftingLink.cancel();
                }
            } catch (Exception ignored) {}
        }
        return TickRateModulation.IDLE;
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return ImmutableSet.of(this.craftingLink);
    }

    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        return 0;
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        getLevel().setBlockAndUpdate(getBlockPos(), getBlockState().setValue(EjectorBlock.ISCRAFTING, false));
        this.getLogic().getPatternInv().setItemDirect(0, ItemStack.EMPTY);
        this.getLogic().updatePatterns();
    }

    private boolean tryPushPatternImmediately(ItemStack patternStack, List<GenericStack> inputs) {
        var level = getLevel();
        var grid = getGrid();
        if (level == null || grid == null) return false;

        var energy = grid.getEnergyService();
        var meInv = grid.getStorageService().getInventory();
        var src = IActionSource.ofMachine(this);

        for (var gs : inputs) {
            long available = StorageHelper.poweredExtraction(
                    energy, meInv, gs.what(), gs.amount(), src, Actionable.SIMULATE
            );
            if (available < gs.amount()) return false;
        }

        var extracted = new ArrayList<GenericStack>(inputs.size());
        var inputHolder = new KeyCounter[inputs.size()];

        for (int i = 0; i < inputs.size(); i++) {
            var gs = inputs.get(i);

            long pulled = StorageHelper.poweredExtraction(
                    energy, meInv, gs.what(), gs.amount(), src, Actionable.MODULATE
            );

            if (pulled < gs.amount()) {
                for (var back : extracted) {
                    StorageHelper.poweredInsert(energy, meInv, back.what(), back.amount(), src, Actionable.MODULATE);
                }
                if (pulled > 0) {
                    StorageHelper.poweredInsert(energy, meInv, gs.what(), pulled, src, Actionable.MODULATE);
                }
                return false;
            }

            extracted.add(gs);

            var kc = new KeyCounter();
            kc.add(gs.what(), gs.amount());
            inputHolder[i] = kc;
        }

        var patternDetails = PatternDetailsHelper.decodePattern(patternStack, level);
        if (patternDetails == null || !patternDetails.supportsPushInputsToExternalInventory()) {
            for (var back : extracted) {
                StorageHelper.poweredInsert(energy, meInv, back.what(), back.amount(), src, Actionable.MODULATE);
            }
            return false;
        }

        pushInputsLikeProvider(inputHolder);
        return true;
    }

    private void pushInputsLikeProvider(KeyCounter[] inputHolder) {
        var level = getLevel();
        var grid = getGrid();
        if (level == null || grid == null) return;

        var direction = getBlockState().getValue(EjectorBlock.FACING);
        var targetEntity = level.getBlockEntity(getBlockPos().relative(direction));

        PatternProviderTarget target = null;
        if (targetEntity != null) {
            target = PatternProviderTarget.get(
                    level,
                    targetEntity.getBlockPos(),
                    targetEntity,
                    direction.getOpposite(),
                    IActionSource.ofMachine(this)
            );
        }

        for (var kc : inputHolder) {
            if (kc == null || kc.isEmpty()) continue;

            for (var entry : kc) {
                var what = entry.getKey();
                long amt = entry.getLongValue();
                if (amt <= 0) continue;

                if (target != null) {
                    long inserted = target.insert(what, amt, Actionable.MODULATE);
                    if (inserted < amt) {
                        StorageHelper.poweredInsert(
                                grid.getEnergyService(),
                                grid.getStorageService().getInventory(),
                                what,
                                amt - inserted,
                                IActionSource.ofMachine(this)
                        );
                    }
                } else {
                    StorageHelper.poweredInsert(
                            grid.getEnergyService(),
                            grid.getStorageService().getInventory(),
                            what,
                            amt,
                            IActionSource.ofMachine(this)
                    );
                }
            }
        }
    }
}
