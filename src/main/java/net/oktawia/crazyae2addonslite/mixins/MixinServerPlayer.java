package net.oktawia.crazyae2addonslite.mixins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.oktawia.crazyae2addonslite.misc.WormholeAnchor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"
            )
    )
    private boolean wormhole$stillValidScoped(AbstractContainerMenu menu, Player player) {
        if (WormholeAnchor.get(player) != null) {
            return true;
        }
        return menu.stillValid(player);
    }
}
