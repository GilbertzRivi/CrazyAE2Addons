package net.oktawia.crazyae2addons.mobstorage;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import appeng.api.util.AECableType;
import appeng.items.parts.PartModels;
import appeng.me.helpers.MachineSource;
import appeng.parts.AEBasePart;
import appeng.parts.automation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class MobAnnihilationPlane extends AEBasePart implements IGridTickable {

    private static final PlaneModels MODELS = new PlaneModels("part/annihilation_plane",
            "part/annihilation_plane_on");

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    private final IActionSource actionSource = new MachineSource(this);

    private final PlaneConnectionHelper connectionHelper = new PlaneConnectionHelper(this);

    public MobAnnihilationPlane(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().addService(IGridTickable.class, this);
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        if (bch.isBBCollision()) {
            bch.addBox(0, 0, 14, 16, 16, 15.5);
            return;
        }
        connectionHelper.getBoxes(bch);
    }

    public PlaneConnections getConnections() {
        return connectionHelper.getConnections();
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 1;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 5, false,
                true);
    }

    public static @Nullable Mob getMobAt(Level level, BlockPos pos) {
        AABB box = new AABB(
                pos.getX(),     pos.getY(),     pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
        ).inflate(0.001);

        var center = Vec3.atCenterOf(pos);

        return level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && !m.isRemoved())
                .stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(center)))
                .orElse(null);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        var mob = getMobAt(this.getLevel(), this.getBlockEntity().getBlockPos().relative(getSide()));
        if (mob != null){
            MobKey key = MobKey.of(mob.getType());
            var inserted = insertIntoGrid(key, 1, Actionable.MODULATE);
            if (inserted <= 0){
                return TickRateModulation.IDLE;
            }
            mob.remove(Entity.RemovalReason.DISCARDED);
            var world = this.getLevel().getServer().getLevel(this.getLevel().dimension());
            if (world != null){
                world.sendParticles(
                    ParticleTypes.FIREWORK,
                    mob.getX(), mob.getY() + 1, mob.getZ(),
                    20,
                    0.5,
                    0.5,
                    0.5,
                    0.01
                );
            }
        }
        return TickRateModulation.IDLE;
    }

    private long insertIntoGrid(AEKey what, long amount, Actionable mode) {
        var grid = getMainNode().getGrid();
        if (grid == null) {
            return 0;
        }
        return StorageHelper.poweredInsert(grid.getEnergyService(), grid.getStorageService().getInventory(),
                what, amount, this.actionSource, mode);
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
