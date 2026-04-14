package net.oktawia.crazyae2addons.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;
import net.oktawia.crazyae2addons.entities.PenroseControllerBE;
import org.jetbrains.annotations.Nullable;

public class PenroseSphereFormedEvent extends Event {
    private final ServerLevel level;
    private final BlockPos pos;
    private final PenroseControllerBE controller;
    private final ServerPlayer player;

    public PenroseSphereFormedEvent(ServerLevel level, BlockPos pos, PenroseControllerBE controller, @Nullable ServerPlayer player) {
        this.level = level;
        this.pos = pos.immutable();
        this.controller = controller;
        this.player = player;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public PenroseControllerBE getController() {
        return controller;
    }

    @Nullable
    public ServerPlayer getPlayer() {
        return player;
    }
}