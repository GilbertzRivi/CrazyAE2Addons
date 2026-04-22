package net.oktawia.crazyae2addons.compat.gtceu;

import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.pipenet.PipeCoverContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.oktawia.crazyae2addons.util.TemplateUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class GTCEuPasteCompat {

    private static final String GT_CABLE_ID = "gtceu:cable";
    private static final String GT_ITEM_PIPE_ID = "gtceu:item_pipe";
    private static final String GT_FLUID_PIPE_ID = "gtceu:fluid_pipe";
    private static final String GTCEU_ID_PREFIX = "gtceu:";

    private static final List<PendingInit> PENDING = new ArrayList<>();
    private static boolean registered = false;

    private GTCEuPasteCompat() {
    }

    public static void schedulePostPlacementInit(ServerLevel level, BlockPos origin, CompoundTag templateTag) {
        ensureRegistered();
        PENDING.add(new PendingInit(level, origin.immutable(), templateTag.copy(), 1));
    }

    private static void ensureRegistered() {
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(GTCEuPasteCompat.class);
            registered = true;
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Iterator<PendingInit> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            PendingInit pending = iterator.next();

            if (pending.level != level) {
                continue;
            }

            if (pending.delayTicks > 0) {
                pending.delayTicks--;
                continue;
            }

            runPostPlacementInit(level, pending.origin, pending.templateTag);
            iterator.remove();
        }
    }

    private static void runPostPlacementInit(ServerLevel level, BlockPos origin, CompoundTag templateTag) {
        for (TemplateUtil.BlockInfo info : TemplateUtil.parseRawBlocksFromTag(templateTag)) {
            CompoundTag blockEntityTag = info.blockEntityTag();
            if (!isGregBlockEntityTag(blockEntityTag)) {
                continue;
            }

            BlockPos worldPos = origin.offset(info.pos());
            BlockEntity blockEntity = level.getBlockEntity(worldPos);
            if (blockEntity == null) {
                continue;
            }

            if (blockEntity instanceof PipeBlockEntity<?, ?> pipe) {
                initSinglePipe(level, worldPos, pipe, blockEntityTag);
            } else {
                syncGenericGregBlockEntity(level, worldPos, blockEntity, blockEntityTag);
            }
        }
    }

    private static void initSinglePipe(
            ServerLevel level,
            BlockPos pos,
            PipeBlockEntity<?, ?> pipe,
            CompoundTag originalTag
    ) {
        CompoundTag tag = originalTag.copy();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());

        pipe.load(tag);
        pipe.clearRemoved();

        PipeCoverContainer coverContainer = pipe.getCoverContainer();
        coverContainer.onLoad();

        for (Direction side : Direction.values()) {
            CoverBehavior cover = coverContainer.getCoverAtSide(side);
            if (cover == null) {
                continue;
            }

            coverContainer.setCoverAtSide(cover, side);
            cover.onLoad();
            cover.getSyncStorage().markAllDirty();
        }

        pipe.getSyncStorage().markAllDirty();
        coverContainer.getSyncStorage().markAllDirty();

        coverContainer.scheduleNeighborShapeUpdate();
        coverContainer.notifyBlockUpdate();
        coverContainer.scheduleRenderUpdate();
        coverContainer.markDirty();

        pipe.notifyBlockUpdate();
        pipe.scheduleRenderUpdate();
        pipe.onChanged();
        pipe.setChanged();

        BlockState state = level.getBlockState(pos);

        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        level.getChunkSource().blockChanged(pos);

        for (Direction side : Direction.values()) {
            BlockPos neighborPos = pos.relative(side);
            BlockState neighborState = level.getBlockState(neighborPos);
            level.sendBlockUpdated(neighborPos, neighborState, neighborState, Block.UPDATE_ALL);
        }

        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(pipe);
        for (ServerPlayer player : level.players()) {
            player.connection.send(packet);
        }
    }

    private static void syncGenericGregBlockEntity(
            ServerLevel level,
            BlockPos pos,
            BlockEntity blockEntity,
            CompoundTag originalTag
    ) {
        CompoundTag tag = originalTag.copy();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());

        try {
            blockEntity.load(tag);
        } catch (Throwable ignored) {
        }

        try {
            blockEntity.clearRemoved();
        } catch (Throwable ignored) {
        }

        try {
            blockEntity.onLoad();
        } catch (Throwable ignored) {
        }

        try {
            blockEntity.setChanged();
        } catch (Throwable ignored) {
        }

        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        level.getChunkSource().blockChanged(pos);

        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(blockEntity);
        for (ServerPlayer player : level.players()) {
            player.connection.send(packet);
        }
    }

    private static boolean isGregBlockEntityTag(CompoundTag tag) {
        if (tag == null) {
            return false;
        }

        String id = tag.getString("id");
        if (!id.isBlank() && id.startsWith(GTCEU_ID_PREFIX)) {
            return true;
        }

        return GT_CABLE_ID.equals(id)
                || GT_ITEM_PIPE_ID.equals(id)
                || GT_FLUID_PIPE_ID.equals(id)
                || (tag.contains("connections", Tag.TAG_INT)
                && tag.contains("blockedConnections", Tag.TAG_INT)
                && tag.contains("frameMaterial", Tag.TAG_STRING));
    }

    private static final class PendingInit {
        private final ServerLevel level;
        private final BlockPos origin;
        private final CompoundTag templateTag;
        private int delayTicks;

        private PendingInit(ServerLevel level, BlockPos origin, CompoundTag templateTag, int delayTicks) {
            this.level = level;
            this.origin = origin;
            this.templateTag = templateTag;
            this.delayTicks = delayTicks;
        }
    }
}