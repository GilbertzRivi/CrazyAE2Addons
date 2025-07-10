package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.AEKeyFilter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.me.helpers.MachineSource;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.ConfigInventory;
import com.google.common.collect.ImmutableSet;
import dev.shadowsoffire.apotheosis.spawn.spawner.ApothSpawnerTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.SpawnerExtractorControllerMenu;
import net.oktawia.crazyae2addons.misc.CachedBlockInfo;
import net.oktawia.crazyae2addons.misc.SpawnerExtractorPreviewRenderer;
import net.oktawia.crazyae2addons.misc.SpawnerExtractorValidator;
import net.oktawia.crazyae2addons.mobstorage.MobKey;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public class SpawnerExtractorControllerBE extends AENetworkBlockEntity implements MenuProvider, IUpgradeableObject, IGridTickable {

    public IUpgradeInventory upgrades = UpgradeInventories.forMachine(CrazyBlockRegistrar.SPAWNER_EXTRACTOR_CONTROLLER.get(), 4, this::saveChanges);
    public SpawnerExtractorValidator validator;
    public boolean spawnerDissabled = false;

    public boolean preview = false;

    public List<CachedBlockInfo> ghostCache = null;

    public static final Set<SpawnerExtractorControllerBE> CLIENT_INSTANCES = new java.util.HashSet<>();

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

    public SpawnerExtractorControllerBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.SPAWNER_EXTRACTOR_CONTROLLER_BE.get(), pos, blockState);
        validator = new SpawnerExtractorValidator();
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.SPAWNER_EXTRACTOR_CONTROLLER.get().asItem())
                );
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("upgrades")) {
            this.upgrades.readFromNBT(data, "upgrades");
        }
        if (!isClientSide() && getLevel() != null){
            BlockPos spawnerPos = getBlockPos().offset(getSpawnerPos(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)));
            this.disableSpawner(getLevel(), spawnerPos);
            this.spawnerDissabled = true;
        }
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (int i = 0; i < upgrades.size(); i++) {
            drops.add(upgrades.getStackInSlot(i));
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        this.upgrades.writeToNBT(data, "upgrades");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new SpawnerExtractorControllerMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Spawner Extractor Controller");
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.SPAWNER_EXTRACTOR_CONTROLLER_MENU.get(), player, locator);
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        }
        return super.getSubInventory(id);
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(20, 20, false, false);
    }

    public void disableSpawner(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SpawnerBlockEntity spawner) {
            BaseSpawner logic = spawner.getSpawner();
            ObfuscationReflectionHelper.setPrivateValue(
                    BaseSpawner.class,
                    logic,
                    Integer.MAX_VALUE,
                    "f_45442_"
            );
            spawner.setChanged();
        }
    }

    public void enableSpawner(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SpawnerBlockEntity spawner) {
            BaseSpawner logic = spawner.getSpawner();
            ObfuscationReflectionHelper.setPrivateValue(
                    BaseSpawner.class,
                    logic,
                    200,
                    "f_45442_"
            );
            spawner.setChanged();
        }
    }

    public static EntityType<?> getEntityTypeFromSpawner(SpawnerBlockEntity spawner) {
        CompoundTag fullTag = spawner.getUpdateTag();
        if (fullTag.contains("SpawnData", Tag.TAG_COMPOUND)) {
            CompoundTag spawnData = fullTag.getCompound("SpawnData");
            return EntityType.byString(spawnData.getCompound("entity").getString("id"))
                    .orElse(null);
        }
        return null;
    }

    public static EntityType<?> getEntityTypeFromApothSpawner(ApothSpawnerTile spawner) {
        CompoundTag fullTag = spawner.serializeNBT();
        if (fullTag.contains("SpawnData", Tag.TAG_COMPOUND)) {
            CompoundTag spawnData = fullTag.getCompound("SpawnData");
            return EntityType.byString(spawnData.getCompound("entity").getString("id"))
                    .orElse(null);
        }
        return null;
    }

    public static BlockPos getSpawnerPos(Direction facing) {
        return switch (facing.getOpposite()) {
            case NORTH -> new BlockPos(0, 2, -3);
            case SOUTH -> new BlockPos(0, 2, 3);
            case WEST  -> new BlockPos(-3, 2, 0);
            case EAST  -> new BlockPos(3, 2, 0);
            default -> BlockPos.ZERO;
        };
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!CrazyConfig.COMMON.enablePeacefullSpawner.get() && getLevel().getDifficulty() == Difficulty.PEACEFUL) return TickRateModulation.IDLE;
        BlockPos spawnerPos = getBlockPos().offset(getSpawnerPos(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)));
        var spawner = getLevel().getBlockEntity(spawnerPos);
        if (!validator.matchesStructure(getLevel(), getBlockPos(), getBlockState(), this)){
            if (spawnerDissabled){
                this.enableSpawner(getLevel(), spawnerPos);
                spawnerDissabled = false;
            }
            return TickRateModulation.IDLE;
        }
        if (!spawnerDissabled){
            this.disableSpawner(getLevel(), spawnerPos);
            spawnerDissabled = true;
        }
        EntityType<?> type = null;

        if (IsModLoaded.isApothLoaded() && spawner instanceof ApothSpawnerTile ast){
            type = getEntityTypeFromApothSpawner(ast);
        } else if (spawner instanceof SpawnerBlockEntity sbe){
            type = getEntityTypeFromSpawner(sbe);
        }
        if (type != null){
            MEStorage inv = getGridNode().getGrid().getStorageService().getInventory();
            IEnergyService eng = getGridNode().getGrid().getEnergyService();
            long amt = this.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD) + 1;
            StorageHelper.poweredInsert(eng, inv, MobKey.of(type), amt, IActionSource.ofMachine(this), Actionable.MODULATE);
        }
        return TickRateModulation.IDLE;
    }
}
