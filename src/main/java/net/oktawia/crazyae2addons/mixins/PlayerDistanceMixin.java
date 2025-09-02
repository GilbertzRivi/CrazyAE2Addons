package net.oktawia.crazyae2addons.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.oktawia.crazyae2addons.misc.WormholeAnchor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PlayerDistanceMixin {

    @Inject(method = "distanceToSqr(DDD)D", at = @At("HEAD"), cancellable = true)
    private void anchorDistance(double x, double y, double z, CallbackInfoReturnable<Double> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        BlockPos anchor = WormholeAnchor.get(player);
        if (anchor == null) return;

        double dx = (anchor.getX() + 0.5D) - x;
        double dy = (anchor.getY() + 0.5D) - y;
        double dz = (anchor.getZ() + 0.5D) - z;
        cir.setReturnValue(dx * dx + dy * dy + dz * dz);
    }

    @Inject(method = "distanceToSqr(Lnet/minecraft/world/entity/Entity;)D", at = @At("HEAD"), cancellable = true)
    private void anchorDistanceEntity(Entity other, CallbackInfoReturnable<Double> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        BlockPos anchor = WormholeAnchor.get(player);
        if (anchor == null) return;

        double dx = (anchor.getX() + 0.5D) - other.getX();
        double dy = (anchor.getY() + 0.5D) - other.getY();
        double dz = (anchor.getZ() + 0.5D) - other.getZ();
        cir.setReturnValue(dx * dx + dy * dy + dz * dz);
    }

    @Inject(method = "blockPosition()Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void anchorBlockPos(CallbackInfoReturnable<BlockPos> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        BlockPos anchor = WormholeAnchor.get(player);
        if (anchor != null) {
            cir.setReturnValue(anchor);
        }
    }
}
