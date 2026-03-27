package net.oktawia.crazyae2addons.parts;

import appeng.api.behaviors.StackExportStrategy;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.storage.IStorageService;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.AppEng;
import appeng.core.settings.TickRates;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.automation.IOBusPart;
import appeng.parts.automation.StackWorldBehaviors;
import appeng.util.ConfigInventory;
import appeng.util.SettingsFrom;
import appeng.util.prioritylist.DefaultPriorityList;
import com.mojang.logging.LogUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.implementations.StackTransferContextImplementation;
import net.oktawia.crazyae2addons.menus.NBTExportBusMenu;
import net.oktawia.crazyae2addons.misc.NBTMatcher;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

public class NBTExportBusPart extends IOBusPart {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation(AppEng.MOD_ID, "part/export_bus_base");

    public final ConfigInventory inv = ConfigInventory.configTypes(1, () -> {});

    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE,
            new ResourceLocation(AppEng.MOD_ID, "part/export_bus_off"));

    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE,
            new ResourceLocation(AppEng.MOD_ID, "part/export_bus_on"));

    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE,
            new ResourceLocation(AppEng.MOD_ID, "part/export_bus_has_channel"));

    @Nullable
    private StackExportStrategy exportStrategy;
    public String data;

    private NBTExportBusMenu menu;

    public NBTExportBusPart(IPartItem<?> partItem) {
        super(TickRates.ExportBus, StackWorldBehaviors.hasExportStrategyFilter(), partItem);
        this.data = "";
    }

    @Override
    protected boolean doBusWork(IGrid grid) {
        var storageService = grid.getStorageService();
        var context = createTransferContext(storageService, grid.getEnergyService());
        var stacks = storageService.getInventory().getAvailableStacks();
        if (stacks.isEmpty()) {
            return false;
        }

        var availableKeys = getItemKeys(stacks);
        var matchingKeys = availableKeys.stream()
                .filter(item -> NBTMatcher.doesItemMatch(item, this.data))
                .collect(Collectors.toCollection(NonNullList::create));

        if (matchingKeys.isEmpty()) {
            return false;
        }

        boolean didWork = false;
        for (AEItemKey key : matchingKeys) {
            int transferFactor = 4;
            long amount = (long) context.getOperationsRemaining() * transferFactor;
            long transferred = getExportStrategy().transfer(context, key, amount);
            if (transferred > 0) {
                context.reduceOperationsRemaining(Math.max(1, transferred / transferFactor));
                didWork = true;
            }
            if (!context.hasOperationsLeft()) {
                break;
            }
        }

        return didWork;
    }

    private StackTransferContextImplementation createTransferContext(IStorageService storageService,
                                                                     IEnergyService energyService) {
        return new StackTransferContextImplementation(
                storageService,
                energyService,
                this.source,
                getOperationsPerTick(),
                DefaultPriorityList.INSTANCE);
    }

    private StackExportStrategy getExportStrategy() {
        if (exportStrategy == null) {
            var self = this.getHost().getBlockEntity();
            var fromPos = self.getBlockPos().relative(this.getSide());
            var fromSide = getSide().getOpposite();
            exportStrategy = StackWorldBehaviors.createExportFacade((ServerLevel) getLevel(), fromPos, fromSide);
        }
        return exportStrategy;
    }

    public NonNullList<AEItemKey> getItemKeys(KeyCounter keyCounter) {
        NonNullList<AEItemKey> keys = NonNullList.create();
        keyCounter.forEach(key -> {
            if (key.getKey() instanceof AEItemKey) {
                keys.add((AEItemKey) key.getKey());
            }
        });
        return keys;
    }

    @Override
    protected MenuType<?> getMenuType() {
        return CrazyMenuRegistrar.NBT_EXPORT_BUS_MENU.get();
    }

    public void setMenu(NBTExportBusMenu menu) {
        this.menu = menu;
    }

    public NBTExportBusMenu getMenu() {
        return this.menu;
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(4, 4, 12, 12, 12, 14);
        bch.addBox(5, 5, 14, 11, 11, 15);
        bch.addBox(6, 6, 15, 10, 10, 16);
        bch.addBox(6, 6, 11, 10, 10, 12);
    }

    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return MODELS_ON;
        } else {
            return MODELS_OFF;
        }
    }

    @Override
    public void readFromNBT(CompoundTag extra) {
        super.readFromNBT(extra);
        if (extra.contains("filter")) {
            this.data = extra.getString("filter");
        } else {
            this.data = "";
        }
    }

    @Override
    public void writeToNBT(CompoundTag extra) {
        super.writeToNBT(extra);
        extra.putString("filter", this.data);
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        if (input.contains("filter")) {
            this.data = input.getString("filter");
        } else {
            this.data = "";
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        super.exportSettings(mode, output);
        if (mode == SettingsFrom.MEMORY_CARD) {
            output.putString("filter", this.data);
        }
    }
}