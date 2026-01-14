package net.oktawia.crazyae2addonslite.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.ForgeMod;
import qouteall.imm_ptl.core.portal.Portal;
import net.oktawia.crazyae2addonslite.CrazyAddons;

import java.util.List;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WormholeCrossPortalClickEvents {

    private static final String PD_WORMHOLE = "crazyae2addonslite_wormhole";

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel localLevel)) return;
        if (!(e.getLevel() instanceof ServerLevel remoteLevel)) return;
        if (!isLookingAtOurPortal(sp, localLevel)) return;
        wrapWithAnchor(sp, remoteLevel, e.getPos());
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel localLevel)) return;
        if (!(e.getTarget().level() instanceof ServerLevel remoteLevel)) return;
        if (!isLookingAtOurPortal(sp, localLevel)) return;
        wrapWithAnchor(sp, remoteLevel, e.getTarget().blockPosition());
    }

    private static void wrapWithAnchor(ServerPlayer sp, ServerLevel targetWorld, BlockPos targetPos) {
        WormholeAnchor.set(sp, targetPos, targetWorld);
        var containerBefore = sp.containerMenu;
        sp.serverLevel().getServer().execute(() -> {
            sp.serverLevel().getServer().execute(() -> {
                if (sp.containerMenu == containerBefore) {
                    WormholeAnchor.clear(sp);
                }
            });
        });
    }

    private static boolean isLookingAtOurPortal(ServerPlayer sp, ServerLevel localLevel) {
        double reach = sp.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        Vec3 end = eye.add(look.scale(reach));
        HitResult blockHit = localLevel.clip(new ClipContext(
            eye, end,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            sp
        ));
        double blockDist = blockHit.getType() == HitResult.Type.MISS ? reach + 1e-6 : blockHit.getLocation().distanceTo(eye);

        var searchBox = sp.getBoundingBox().expandTowards(look.scale(reach)).inflate(2.0);
        List<Portal> portals = localLevel.getEntitiesOfClass(Portal.class, searchBox);
        if (portals.isEmpty()) return false;

        PortalHit best = null;

        for (Portal p : portals) {
            if (!p.getPersistentData().getBoolean(PD_WORMHOLE)) continue;
            PortalHit hit = rayHitPortal(eye, look, reach, p);
            if (hit == null) continue;
            if (hit.t > blockDist - 1e-4) continue;
            if (best == null || hit.t < best.t) {
                best = hit;
            }
        }
        return best != null;
    }

    private record PortalHit(double t) {}

    private static PortalHit rayHitPortal(Vec3 eye, Vec3 look, double reach, Portal p) {
        Vec3 axisW = p.axisW;
        Vec3 axisH = p.axisH;
        Vec3 normal = axisW.cross(axisH);
        double nLen = normal.length();
        if (nLen < 1e-8) return null;
        normal = normal.scale(1.0 / nLen);
        double denom = normal.dot(look);
        if (Math.abs(denom) < 1e-6) return null;
        Vec3 center = p.position();
        double t = normal.dot(center.subtract(eye)) / denom;
        if (t < 0 || t > reach) return null;
        Vec3 hitPoint = eye.add(look.scale(t));
        Vec3 rel = hitPoint.subtract(center);
        Vec3 wN = axisW.normalize();
        Vec3 hN = axisH.normalize();
        double u = rel.dot(wN);
        double v = rel.dot(hN);
        double halfW = p.width / 2.0;
        double halfH = p.height / 2.0;
        if (Math.abs(u) <= halfW + 1e-4 && Math.abs(v) <= halfH + 1e-4) {
            return new PortalHit(t);
        }
        return null;
    }
}
