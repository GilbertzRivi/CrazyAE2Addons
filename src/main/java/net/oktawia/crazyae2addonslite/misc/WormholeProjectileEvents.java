package net.oktawia.crazyae2addonslite.misc;

import appeng.blockentity.networking.CableBusBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addonslite.CrazyAddons;
import net.oktawia.crazyae2addonslite.parts.WormholeP2PTunnelPart;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WormholeProjectileEvents {
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        LogUtils.getLogger().info("[WormholeProjectileEvents] {}", event.getEntity());
        Projectile proj = event.getProjectile();
        HitResult hit = event.getRayTraceResult();
        if (!(hit instanceof BlockHitResult bhr)) return;
        if (!(proj.level().getBlockEntity(bhr.getBlockPos()) instanceof CableBusBlockEntity cbbe)) return;
        if (!(cbbe.getPart(bhr.getDirection()) instanceof WormholeP2PTunnelPart)) return;
        event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        proj.setDeltaMovement(proj.getDeltaMovement());
    }
}
