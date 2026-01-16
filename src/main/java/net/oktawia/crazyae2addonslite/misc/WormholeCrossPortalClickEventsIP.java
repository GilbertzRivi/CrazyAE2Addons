package net.oktawia.crazyae2addonslite.misc;

import appeng.blockentity.networking.CableBusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.oktawia.crazyae2addonslite.parts.WormholeP2PTunnelPart;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.List;

final class WormholeCrossPortalClickEventsIP {

    private WormholeCrossPortalClickEventsIP() {}

    private static final String PD_WORMHOLE  = "crazyae2addonslite_wormhole";
    private static final String PD_HOST_POS  = "hostPos";
    private static final String PD_HOST_SIDE = "hostSide";

    static ServerLevel isLookingAtOurPortal(ServerPlayer sp, ServerLevel localLevel) {
        double reach = sp.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();

        var searchBox = sp.getBoundingBox().expandTowards(look.scale(reach)).inflate(2.0);
        List<Portal> portals = localLevel.getEntitiesOfClass(Portal.class, searchBox);
        if (portals.isEmpty()) return null;

        for (Portal p : portals) {
            if (!p.getPersistentData().getBoolean(PD_WORMHOLE)) continue;

            if (rayHitPortal(eye, look, reach, p, sp)) {
                var server = localLevel.getServer();
                if (server == null) return null;
                return server.getLevel(p.getDestDim());
            }
        }
        return null;
    }

    private static boolean rayHitPortal(Vec3 eye, Vec3 look, double reach, Portal p, ServerPlayer player) {
        var tag = p.getPersistentData();

        BlockPos partPos = BlockPos.of(tag.getLong(PD_HOST_POS));

        int sideIdx = tag.getByte(PD_HOST_SIDE) & 0xFF;
        if (sideIdx < 0 || sideIdx >= Direction.values().length) return false;
        Direction partSide = Direction.values()[sideIdx];

        var be = player.level().getBlockEntity(partPos);
        if (!(be instanceof CableBusBlockEntity cbbe)) return false;

        var part = cbbe.getPart(partSide);
        if (!(part instanceof WormholeP2PTunnelPart)) return false;

        double len = look.length();
        if (len < 1e-8) return false;
        Vec3 dir = look.scale(1.0 / len);

        Vec3 normal = dirVec(partSide);
        Vec3 center = Vec3.atCenterOf(partPos).add(normal.scale(0.5));

        Vec3[] axes = makeAxesFromNormal(normal);
        Vec3 wN = axes[0];
        Vec3 hN = axes[1];

        double denom = normal.dot(dir);
        if (Math.abs(denom) < 1e-6) return false;

        double t = normal.dot(center.subtract(eye)) / denom;

        if (t < -1e-4 || t > reach) return false;
        if (t < 0) t = 0;

        Vec3 hit = eye.add(dir.scale(t));

        Vec3 rel = hit.subtract(center);
        double u = rel.dot(wN);
        double v = rel.dot(hN);

        double halfW = p.width / 2.0;
        double halfH = p.height / 2.0;

        return (Math.abs(u) <= halfW + 1e-4) && (Math.abs(v) <= halfH + 1e-4);
    }

    private static Vec3 dirVec(Direction d) {
        return new Vec3(d.getStepX(), d.getStepY(), d.getStepZ());
    }

    private static Vec3[] makeAxesFromNormal(Vec3 normalUnitOrAxis) {
        Vec3 n = normalUnitOrAxis;
        double nLen = n.length();
        if (nLen < 1e-8) {
            return new Vec3[]{new Vec3(1, 0, 0), new Vec3(0, 1, 0)};
        }
        n = n.scale(1.0 / nLen);

        Vec3 up = (Math.abs(n.y) < 0.9) ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);

        Vec3 w = n.cross(up);
        double wLen = w.length();
        if (wLen < 1e-8) {
            up = new Vec3(0, 0, 1);
            w = n.cross(up);
            wLen = w.length();
            if (wLen < 1e-8) return new Vec3[]{new Vec3(1, 0, 0), new Vec3(0, 1, 0)};
        }
        w = w.scale(1.0 / wLen);

        Vec3 h = w.cross(n);
        double hLen = h.length();
        if (hLen > 1e-8) h = h.scale(1.0 / hLen);

        return new Vec3[]{w, h};
    }
}
