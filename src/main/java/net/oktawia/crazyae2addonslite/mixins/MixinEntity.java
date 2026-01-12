package net.oktawia.crazyae2addonslite.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addonslite.misc.WormholeAnchor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    @Mutable
    private Vec3 position;

    @Inject(
            method = "position()Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void anchorPosition(CallbackInfoReturnable<Vec3> cir) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player player)) return;
        WormholeAnchor.Anchor anchor = WormholeAnchor.get(player);
        if (anchor != null) {
            cir.setReturnValue(anchor.center());
        }
    }

    @Inject(
            method = "blockPosition()Lnet/minecraft/core/BlockPos;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void anchorBlockPos(CallbackInfoReturnable<BlockPos> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        WormholeAnchor.Anchor anchor = WormholeAnchor.get(player);
        if (anchor != null) {
            cir.setReturnValue(anchor.pos());
        }
    }

    @Inject(
            method = "distanceToSqr(DDD)D",
            at = @At("HEAD"),
            cancellable = true
    )
    private void anchorDistance(double x, double y, double z, CallbackInfoReturnable<Double> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        WormholeAnchor.Anchor anchor = WormholeAnchor.get(player);;
        if (anchor == null) return;

        double dx = (anchor.pos().getX() + 0.5D) - x;
        double dy = (anchor.pos().getY() + 0.5D) - y;
        double dz = (anchor.pos().getZ() + 0.5D) - z;
        cir.setReturnValue(dx * dx + dy * dy + dz * dz);
    }

    @Inject(
            method = "distanceToSqr(Lnet/minecraft/world/entity/Entity;)D",
            at = @At("HEAD"),
            cancellable = true
    )
    private void anchorDistanceEntity(Entity other, CallbackInfoReturnable<Double> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        WormholeAnchor.Anchor anchor = WormholeAnchor.get(player);
        if (anchor == null) return;

        double dx = (anchor.pos().getX() + 0.5D) - other.getX();
        double dy = (anchor.pos().getY() + 0.5D) - other.getY();
        double dz = (anchor.pos().getZ() + 0.5D) - other.getZ();
        cir.setReturnValue(dx * dx + dy * dy + dz * dz);
    }

    @Inject(
            method = "distanceTo(Lnet/minecraft/world/entity/Entity;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void anchorDistanceTo(Entity entity, CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        WormholeAnchor.Anchor anchor = WormholeAnchor.get(player);
        if (anchor == null) return;

        float f = (float)(anchor.pos().getX() - entity.getX());
        float f1 = (float)(anchor.pos().getY() - entity.getY());
        float f2 = (float)(anchor.pos().getZ() - entity.getZ());
        cir.setReturnValue(Mth.sqrt(f * f + f1 * f1 + f2 * f2));
    }

    @Inject(
            method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D",
            at = @At("HEAD"),
            cancellable = true
    )
    private void anchorDistanceToSqr(Vec3 vec3, CallbackInfoReturnable<Double> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        WormholeAnchor.Anchor anchor = WormholeAnchor.get(player);
        if (anchor == null) return;

        double d0 = anchor.pos().getX() - vec3.x;
        double d1 = anchor.pos().getY() - vec3.y;
        double d2 = anchor.pos().getZ() - vec3.z;
        cir.setReturnValue(d0 * d0 + d1 * d1 + d2 * d2);
    }
}
