package net.oktawia.crazyae2addons.mobstorage;

import appeng.api.behaviors.PlacementStrategy;
import appeng.api.config.*;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKey;
import appeng.api.storage.*;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigManager;
import appeng.core.definitions.AEItems;
import appeng.helpers.IConfigInvHost;
import appeng.helpers.IPriorityHost;
import appeng.items.parts.PartModels;
import appeng.me.helpers.MachineSource;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.FormationPlaneMenu;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.parts.automation.*;
import appeng.util.ConfigInventory;
import appeng.util.prioritylist.IPartitionList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MobFormationPlane extends UpgradeablePart implements IStorageProvider, IPriorityHost, IConfigInvHost {

    private static final PlaneModels MODELS = new PlaneModels("part/formation_plane",
            "part/formation_plane_on");
    private boolean wasOnline = false;
    private int priority = 0;
    private final PlaneConnectionHelper connectionHelper = new PlaneConnectionHelper(this);
    private final MEStorage inventory = new InWorldStorage();
    private final ConfigInventory config;
    @Nullable
    private PlacementStrategy placementStrategies;
    private IncludeExclude filterMode = IncludeExclude.WHITELIST;
    private IPartitionList filter;

    public MobFormationPlane(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().addService(IStorageProvider.class, this);
        var filter = new AEKeyFilter() {
            @Override
            public boolean matches(AEKey what) {
                return (what instanceof MobKey);
            }
        };
        this.config = ConfigInventory.configTypes(filter, 63, this::updateFilter);
        this.getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.getConfigManager().registerSetting(Settings.PLACE_BLOCK, YesNo.YES);
        this.getConfigManager().registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
    }

    protected final PlacementStrategy getPlacementStrategies() {
        if (placementStrategies == null) {
            var node = getMainNode().getNode();
            if (node == null) {
                return PlacementStrategy.noop();
            }
            var self = this.getHost().getBlockEntity();
            var pos = self.getBlockPos().relative(this.getSide());
            var side = getSide().getOpposite();
            placementStrategies = new MobPlacementStrategies((ServerLevel) self.getLevel(), pos, side);
        }
        return placementStrategies;
    }

    protected final void updateFilter() {
        this.filter = createFilter();
        this.filterMode = isUpgradedWith(AEItems.INVERTER_CARD)
                ? IncludeExclude.BLACKLIST
                : IncludeExclude.WHITELIST;
    }

    @Override
    protected int getUpgradeSlots() {
        return 5;
    }

    @Override
    public void upgradesChanged() {
        this.updateFilter();
    }

    @Override
    public void onSettingChanged(IConfigManager manager, Setting<?> setting) {
        this.getHost().markForSave();
    }

    private void remountStorage() {
        IStorageProvider.requestUpdate(getMainNode());
    }

    @Override
    protected void onMainNodeStateChanged(IGridNodeListener.State reason) {
        var currentOnline = this.getMainNode().isOnline();
        if (this.wasOnline != currentOnline) {
            this.wasOnline = currentOnline;
            this.remountStorage();
            this.getHost().markForUpdate();
        }
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        connectionHelper.getBoxes(bch);
    }

    public PlaneConnections getConnections() {
        return connectionHelper.getConnections();
    }

    @Override
    public void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor) {
        if (pos.relative(this.getSide()).equals(neighbor)) {
            if (!isClientSide()) {
                getPlacementStrategies().clearBlocked();
            }
        } else {
            connectionHelper.updateConnections();
        }
    }

    @Override
    public void onUpdateShape(Direction side) {
        var ourSide = getSide();
        if (ourSide.getAxis() != side.getAxis()) {
            connectionHelper.updateConnections();
        }
    }

    protected long placeInWorld(AEKey what, long amount, Actionable type) {
        return getPlacementStrategies().placeInWorld(what, amount, type, true);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 1;
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        this.priority = data.getInt("priority");
        this.config.readFromChildTag(data, "config");
        remountStorage();
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        data.putInt("priority", this.getPriority());
        this.config.writeToChildTag(data, "config");
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(int newValue) {
        this.priority = newValue;
        this.getHost().markForSave();
        this.remountStorage();
    }

    @Override
    public void mountInventories(IStorageMounts mounts) {
        if (getMainNode().isOnline()) {
            updateFilter();
            mounts.mount(inventory, priority);
        }
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(getMenuType(), player, MenuLocators.forPart(this));
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(getPartItem());
    }

    private void openConfigMenu(Player player) {
        MenuOpener.open(getMenuType(), player, MenuLocators.forPart(this));
    }

    protected MenuType<?> getMenuType() {
        return CrazyMenuRegistrar.MOB_FORMATION_PLANE_MENU.get();
    }

    private IPartitionList createFilter() {
        var builder = IPartitionList.builder();
        var slotsToUse = 18 + this.getInstalledUpgrades(AEItems.CAPACITY_CARD) * 9;
        for (var x = 0; x < this.config.size() && x < slotsToUse; x++) {
            builder.add(this.config.getKey(x));
        }
        return builder.build();
    }

    class InWorldStorage implements MEStorage {
        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (filter != null && !filter.matchesFilter(what, filterMode)) {
                return 0;
            }

            return placeInWorld(what, amount, mode);
        }

        @Override
        public Component getDescription() {
            return getPartItem().asItem().getDescription();
        }
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!isClientSide()) {
            openConfigMenu(player);
        }
        return true;
    }

    @Override
    public ConfigInventory getConfig() {
        return config;
    }

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(PlaneModelData.CONNECTIONS, getConnections())
                .build();
    }
}
