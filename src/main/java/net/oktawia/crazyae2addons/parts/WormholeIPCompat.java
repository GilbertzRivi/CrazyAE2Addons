package net.oktawia.crazyae2addons.parts;

import appeng.blockentity.networking.CableBusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.my_util.DQuaternion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WormholeIPCompat {
    private WormholeIPCompat() {}

    private static final String PD_WORMHOLE = "crazyae2addonslite_wormhole";
    public static final String PD_HOST_POS = "hostPos";
    private static final String PD_HOST_DIM = "hostDim";
    public static final String PD_HOST_SIDE = "hostSide";

    private enum RemoteResolveStatus { OK, DISCONNECTED, REMOTE_UNLOADED }
    private record RemoteView(ServerLevel level, BlockPos frontBlock, Direction facingOut) {}
    private record RemoteResolve(RemoteResolveStatus status, @Nullable RemoteView view) {}

    public static boolean updateOrCreatePortalEntity(WormholeP2PTunnelPart part, ServerLevel sl) {
        BlockPos hostPos = part.getBlockEntity().getBlockPos();
        Direction localFacing = part.getSide();

        var rr = resolveRemoteViewEx(part);

        boolean intangible = (rr.status() == RemoteResolveStatus.OK);
        if (rr.status() == RemoteResolveStatus.DISCONNECTED) {
            removePortalEntity(part, sl);
            return false;
        }

        if (rr.status() == RemoteResolveStatus.REMOTE_UNLOADED) {
            adoptOrCleanupLocalPortal(sl, hostPos, localFacing, true);
            return false;
        }

        var remote = rr.view();
        if (remote == null) {
            removePortalEntity(part, sl);
            return false;
        }

        long chunkKey = new ChunkPos(remote.frontBlock()).toLong();
        if (!remote.level().getChunkSource().isPositionTicking(chunkKey)) {
            adoptOrCleanupLocalPortal(sl, hostPos, localFacing, true);
            return false;
        }

        Vec3 portalCenter = computeExpectedPortalCenter(hostPos, localFacing);

        Vec3 remoteNormal = dirVec(remote.facingOut());
        Vec3 destCenter = Vec3.atCenterOf(remote.frontBlock()).add(remoteNormal.scale(-0.525));

        Vec3 localNormal = dirVec(localFacing);
        Vec3[] localAxes = makePortalAxesFromNormal(localNormal);

        Portal portal = adoptOrCleanupLocalPortal(sl, hostPos, localFacing, false);

        boolean shouldSpawnPortal = false;
        if (portal == null || portal.isRemoved()) {
            shouldSpawnPortal = true;
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(
                    new ResourceLocation("immersive_portals", "portal")
            );
            if (entityType == null) return intangible;

            portal = new Portal(entityType, sl);
            portal.setPos(portalCenter.x, portalCenter.y, portalCenter.z);
        }

        portal.setPos(portalCenter.x, portalCenter.y, portalCenter.z);
        portal.axisW = localAxes[0];
        portal.axisH = localAxes[1];
        portal.width = 0.5;
        portal.height = 0.5;

        portal.dimensionTo = remote.level().dimension();
        portal.destination = destCenter;

        DQuaternion rot = computeViewRotation(localNormal, remoteNormal);
        portal.setRotation(rot);
        portal.teleportable = true;

        tagPortal(sl, hostPos, localFacing, portal);

        if (shouldSpawnPortal) sl.addFreshEntity(portal);

        portal.reloadAndSyncToClientNextTick();
        return intangible;
    }

    public static void removePortalEntity(WormholeP2PTunnelPart part, ServerLevel sl) {
        var hostPos = part.getBlockEntity().getBlockPos();
        var side = part.getSide();
        adoptOrCleanupLocalPortal(sl, hostPos, side, true);
    }

    @NotNull
    private static RemoteResolve resolveRemoteViewEx(WormholeP2PTunnelPart part) {
        var lvl = part.getLevel();
        if (!(lvl instanceof ServerLevel)) return new RemoteResolve(RemoteResolveStatus.DISCONNECTED, null);
        if (!part.isActive()) return new RemoteResolve(RemoteResolveStatus.DISCONNECTED, null);

        if (part.isOutput()) {
            var input = part.getInput();
            if (input == null) return new RemoteResolve(RemoteResolveStatus.DISCONNECTED, null);
            if (input.getHost() == null) return new RemoteResolve(RemoteResolveStatus.REMOTE_UNLOADED, null);

            var remoteHost = input.getHost().getBlockEntity();
            if (remoteHost == null || !(remoteHost.getLevel() instanceof ServerLevel sl)) {
                return new RemoteResolve(RemoteResolveStatus.REMOTE_UNLOADED, null);
            }

            Direction remoteSide = input.getSide();
            BlockPos front = remoteHost.getBlockPos().relative(remoteSide);
            return new RemoteResolve(RemoteResolveStatus.OK, new RemoteView(sl, front, remoteSide));
        } else {
            var outs = part.getOutputs();
            if (outs.isEmpty()) return new RemoteResolve(RemoteResolveStatus.DISCONNECTED, null);

            WormholeP2PTunnelPart chosen = null;
            for (var out : outs) {
                if (out.getHost() == null) continue;
                var remoteHost = out.getHost().getBlockEntity();
                if (remoteHost != null && remoteHost.getLevel() instanceof ServerLevel) {
                    chosen = out;
                    break;
                }
            }

            if (chosen == null) return new RemoteResolve(RemoteResolveStatus.REMOTE_UNLOADED, null);

            var remoteHost = chosen.getHost().getBlockEntity();
            ServerLevel sl = (ServerLevel) remoteHost.getLevel();

            Direction remoteSide = chosen.getSide();
            BlockPos front = remoteHost.getBlockPos().relative(remoteSide);
            return new RemoteResolve(RemoteResolveStatus.OK, new RemoteView(sl, front, remoteSide));
        }
    }

    private static Vec3 computeExpectedPortalCenter(BlockPos hostPos, Direction localFacing) {
        Vec3 localNormal = dirVec(localFacing);
        return Vec3.atCenterOf(hostPos).add(localNormal.scale(0.475));
    }

    private static void tagPortal(ServerLevel level, BlockPos hostPos, Direction side, Portal portal) {
        CompoundTag pd = portal.getPersistentData();
        pd.putBoolean(PD_WORMHOLE, true);
        pd.putLong(PD_HOST_POS, hostPos.asLong());
        pd.putString(PD_HOST_DIM, level.dimension().location().toString());
        pd.putByte(PD_HOST_SIDE, (byte) side.ordinal());
    }

    private static boolean isTaggedAsOurs(ServerLevel level, BlockPos hostPos, Direction side, Portal portal) {
        var pd = portal.getPersistentData();
        if (!pd.getBoolean(PD_WORMHOLE)) return false;
        if (pd.getLong(PD_HOST_POS) != hostPos.asLong()) return false;
        if (pd.getByte(PD_HOST_SIDE) != (byte) side.ordinal()) return false;
        return Objects.equals(pd.getString(PD_HOST_DIM), level.dimension().location().toString());
    }

    private static List<Portal> findCandidatePortals(ServerLevel level, Vec3 expectedCenter, BlockPos hostPos, Direction side) {
        var box = new AABB(expectedCenter, expectedCenter).inflate(1.0);
        var all = level.getEntitiesOfClass(Portal.class, box);

        List<Portal> result = new ArrayList<>();
        for (Portal p : all) {
            if (isTaggedAsOurs(level, hostPos, side, p)) {
                result.add(p);
                continue;
            }
            if (!p.getPersistentData().contains(PD_WORMHOLE) && p.position().distanceToSqr(expectedCenter) < 0.03) {
                result.add(p);
            }
        }
        return result;
    }

    @Nullable
    private static Portal adoptOrCleanupLocalPortal(ServerLevel level, BlockPos hostPos, Direction side, boolean discardAll) {
        Vec3 expectedCenter = computeExpectedPortalCenter(hostPos, side);
        var candidates = findCandidatePortals(level, expectedCenter, hostPos, side);
        if (candidates.isEmpty()) return null;

        Portal best = null;
        double bestDist = Double.POSITIVE_INFINITY;

        for (Portal p : candidates) {
            double d = p.position().distanceToSqr(expectedCenter);
            boolean tagged = isTaggedAsOurs(level, hostPos, side, p);

            if (best == null) {
                best = p;
                bestDist = d;
            } else {
                boolean bestTagged = isTaggedAsOurs(level, hostPos, side, best);
                if (tagged && !bestTagged) {
                    best = p;
                    bestDist = d;
                } else if (tagged == bestTagged && d < bestDist) {
                    best = p;
                    bestDist = d;
                }
            }
        }

        for (Portal p : candidates) {
            if (p == best) continue;
            p.discard();
        }

        if (discardAll) {
            best.discard();
            return null;
        }

        tagPortal(level, hostPos, side, best);
        return best;
    }

    private static Vec3 dirVec(Direction d) {
        var n = d.getNormal();
        return new Vec3(n.getX(), n.getY(), n.getZ());
    }

    private static Vec3 safeNormalize(Vec3 v) {
        double len = v.length();
        return len < 1e-8 ? new Vec3(0, 0, 0) : v.scale(1.0 / len);
    }

    private static Vec3[] makePortalAxesFromNormal(Vec3 n) {
        int nx = (int) Math.round(n.x);
        int ny = (int) Math.round(n.y);

        if (ny != 0) {
            Vec3 w = new Vec3(1, 0, 0);
            Vec3 h = new Vec3(0, 0, 1);
            if (w.cross(h).dot(n) < 0) w = w.scale(-1);
            return new Vec3[]{w, h};
        }

        if (nx != 0) {
            Vec3 w = new Vec3(0, 0, 1);
            Vec3 h = new Vec3(0, 1, 0);
            if (w.cross(h).dot(n) < 0) w = w.scale(-1);
            return new Vec3[]{w, h};
        }

        Vec3 w = new Vec3(1, 0, 0);
        Vec3 h = new Vec3(0, 1, 0);
        if (w.cross(h).dot(n) < 0) w = w.scale(-1);
        return new Vec3[]{w, h};
    }

    private static Vec3 rotateVec(DQuaternion q, Vec3 v) {
        Quaternionf qf = q.toMcQuaternion();
        Vector3f tmp = new Vector3f((float) v.x, (float) v.y, (float) v.z);
        tmp.rotate(qf);
        return new Vec3(tmp.x, tmp.y, tmp.z);
    }

    private static DQuaternion rotationFromTo(Vec3 from, Vec3 to) {
        from = safeNormalize(from);
        to = safeNormalize(to);

        double dot = from.dot(to);
        dot = Math.max(-1.0, Math.min(1.0, dot));

        if (dot > 0.999999) {
            return DQuaternion.identity;
        }

        if (dot < -0.999999) {
            Vec3 axis = Math.abs(from.y) < 0.9 ? from.cross(new Vec3(0, 1, 0)) : from.cross(new Vec3(1, 0, 0));
            axis = safeNormalize(axis);
            return DQuaternion.rotationByDegrees(axis, 180.0);
        }

        Vec3 axis = safeNormalize(from.cross(to));
        double angleRad = Math.acos(dot);
        double angleDeg = angleRad * (180.0 / Math.PI);

        return DQuaternion.rotationByDegrees(axis, angleDeg);
    }

    private static Vec3 projectUpOnPlane(Vec3 forward) {
        forward = safeNormalize(forward);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 proj = up.subtract(forward.scale(up.dot(forward)));
        if (proj.lengthSqr() < 1e-8) {
            Vec3 alt = new Vec3(0, 0, 1);
            proj = alt.subtract(forward.scale(alt.dot(forward)));
        }
        return safeNormalize(proj);
    }

    private static double signedAngleAroundAxis(Vec3 from, Vec3 to, Vec3 axis) {
        from = safeNormalize(from);
        to = safeNormalize(to);
        axis = safeNormalize(axis);

        Vec3 cross = from.cross(to);
        double sin = axis.dot(cross);
        double cos = from.dot(to);
        return Math.atan2(sin, cos);
    }

    private static DQuaternion computeViewRotation(Vec3 localNormal, Vec3 remoteNormal) {
        Vec3 srcForward = safeNormalize(localNormal.scale(-1));
        Vec3 dstForward = safeNormalize(remoteNormal);

        Vec3 srcUp = projectUpOnPlane(srcForward);
        Vec3 dstUp = projectUpOnPlane(dstForward);

        DQuaternion qForward = rotationFromTo(srcForward, dstForward);
        Vec3 srcUpRot = rotateVec(qForward, srcUp);
        double rollRad = signedAngleAroundAxis(srcUpRot, dstUp, dstForward);

        DQuaternion qRoll = DQuaternion.rotationByDegrees(dstForward, rollRad * (180.0 / Math.PI));
        return qRoll.hamiltonProduct(qForward);
    }

    public static final class Events {
        @SubscribeEvent
        public void onEntityJoin(EntityJoinLevelEvent e) {
            if (e.getLevel().isClientSide()) return;
            if (!(e.getEntity() instanceof Portal p)) return;

            var pd = p.getPersistentData();
            if (!pd.getBoolean(PD_WORMHOLE)) return;

            if (!(e.getLevel() instanceof ServerLevel sl)) return;

            BlockPos hostPos = BlockPos.of(pd.getLong(PD_HOST_POS));
            int sideOrd = pd.getByte(PD_HOST_SIDE);
            if (sideOrd < 0 || sideOrd >= Direction.values().length) {
                p.discard();
                return;
            }
            Direction side = Direction.values()[sideOrd];
            if (!hasWormholePartAt(sl, hostPos, side)) {
                p.discard();
            }
        }

        private boolean hasWormholePartAt(ServerLevel level, BlockPos pos, Direction side) {
            var be = level.getBlockEntity(pos);
            if (be == null) return false;

            if (be instanceof CableBusBlockEntity host) {
                var part = host.getPart(side);
                return part instanceof WormholeP2PTunnelPart;
            }
            return false;
        }
    }
}