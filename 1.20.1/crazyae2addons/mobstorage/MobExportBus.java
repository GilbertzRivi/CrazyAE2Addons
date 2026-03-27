package net.oktawia.crazyae2addons.mobstorage;

import appeng.api.config.Actionable;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKey;
import appeng.api.storage.AEKeyFilter;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.automation.ExportBusPart;
import appeng.util.ConfigInventory;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;

public class MobExportBus extends ExportBusPart {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation(AppEng.MOD_ID, "part/export_bus_base");

    public final ConfigInventory config;
    private int nextSlot = 0;

    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE,
            new ResourceLocation(AppEng.MOD_ID, "part/export_bus_off"));

    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE,
            new ResourceLocation(AppEng.MOD_ID, "part/export_bus_on"));

    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE,
            new ResourceLocation(AppEng.MOD_ID, "part/export_bus_has_channel"));

    public MobExportBus(IPartItem<?> partItem) {
        super(partItem);
        var filter = new AEKeyFilter() {
            @Override
            public boolean matches(AEKey what) {
                return (what instanceof MobKey);
            }
        };
        this.config = ConfigInventory.configTypes(filter, 18, () -> this.getHost().markForSave());
    }

    public static boolean canSpawn(Level level, BlockPos pos) {
        BlockState here  = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        return here.isAir() && above.isAir();
    }

    @Override
    protected boolean doBusWork(IGrid grid) {
        boolean didSomething = false;
        var schedulingMode = this.getConfigManager().getSetting(Settings.SCHEDULING_MODE);

        int x;
        for (x = 0; x < this.availableSlots() && !didSomething; x++) {
            final int slotToExport = this.getStartingSlot(schedulingMode, x);
            var what = config.getStack(slotToExport);
            if (what == null){
                this.updateSchedulingMode(schedulingMode, x);
                continue;
            }
            if (what.what() instanceof MobKey mk) {
                var extracted = this.getGridNode().getGrid().getStorageService().getInventory().extract(mk, 1, Actionable.MODULATE, IActionSource.ofMachine(this));
                if (extracted <= 0) {
                    this.updateSchedulingMode(schedulingMode, x);
                    continue;
                }
                var spot = getBlockEntity().getBlockPos().relative(getSide());
                if (!canSpawn(this.getLevel(), spot)){
                    this.updateSchedulingMode(schedulingMode, x);
                    continue;
                }
                var world = getLevel().getServer().getLevel(getLevel().dimension());
                if (world == null) {
                    LogUtils.getLogger().info("World is null");
                    return false;
                }
                var mob = mk.getEntityType().spawn(world, spot, MobSpawnType.COMMAND);
                if (mob == null) {
                    LogUtils.getLogger().info("Mob is null");
                    return false;
                }
                world.sendParticles(
                    ParticleTypes.FIREWORK,
                    mob.getX(), mob.getY() + 1, mob.getZ(),
                    20,
                    0.5,
                    0.5,
                    0.5,
                    0.01
                );
                didSomething = true;
                this.updateSchedulingMode(schedulingMode, x);
            }
        }
        return didSomething;
    }

    @Override
    protected int getStartingSlot(SchedulingMode schedulingMode, int x) {
        if (schedulingMode == SchedulingMode.RANDOM) {
            return getLevel().getRandom().nextInt(this.availableSlots());
        }

        if (schedulingMode == SchedulingMode.ROUNDROBIN) {
            return (this.nextSlot ++) % this.availableSlots();
        }

        return x;
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
    protected MenuType<?> getMenuType() {
        return CrazyMenuRegistrar.MOB_EXPORT_BUS_MENU.get();
    }

    @Override
    protected int getUpgradeSlots() {
        return 0;
    }

    @Override
    public void readFromNBT(CompoundTag extra) {
        super.readFromNBT(extra);
        this.config.readFromChildTag(extra, "config");
    }

    @Override
    public void writeToNBT(CompoundTag extra) {
        super.writeToNBT(extra);
        this.config.writeToChildTag(extra, "config");
    }
}
