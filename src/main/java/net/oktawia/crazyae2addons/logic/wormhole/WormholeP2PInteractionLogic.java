package net.oktawia.crazyae2addons.logic.wormhole;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.parts.IPartItem;
import appeng.me.service.P2PService;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.mixins.accessors.P2PTunnelPartAccessor;
import net.oktawia.crazyae2addons.parts.p2p.WormholeP2PTunnelPart;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class WormholeP2PInteractionLogic {

    private final WormholeP2PTunnelPart part;

    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (part.isClientSide()) {
            return true;
        }
        if (hand == InteractionHand.OFF_HAND) {
            return false;
        }
        if (!CrazyConfig.COMMON.WORMHOLE_ENABLED.get()) {
            return false;
        }

        ItemStack stack = player.getItemInHand(hand);

        if (!stack.isEmpty() && stack.getItem() instanceof IMemoryCard memoryCard) {
            return handleMemoryCard(player, stack, memoryCard);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        ServerTarget target = resolveRemoteTarget();
        if (target == null) {
            return false;
        }

        long chunkKey = new ChunkPos(target.pos()).toLong();
        if (!target.level().getChunkSource().isPositionTicking(chunkKey)) {
            return false;
        }

        if (stack.is(Items.ENDER_PEARL) && CrazyConfig.COMMON.WORMHOLE_TELEPORTATION_ENABLED.get()) {
            if (!serverPlayer.isCreative()) {
                stack.shrink(1);
            }

            serverPlayer.teleportTo(
                    target.level(),
                    target.pos().getX() + 0.5D,
                    target.pos().getY() + 0.1D,
                    target.pos().getZ() + 0.5D,
                    target.hitFace().getOpposite().toYRot(),
                    serverPlayer.getXRot()
            );
            return true;
        }

        if (!CrazyConfig.COMMON.WORMHOLE_REMOTE_INTERACTIONS_ENABLED.get()) {
            return false;
        }

        BlockState state = target.level().getBlockState(target.pos());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target.pos()), target.hitFace(), target.pos(), false);

        WormholeAnchor.set(serverPlayer, target.pos(), target.level());
        var containerBefore = serverPlayer.containerMenu;

        var result = state.use(target.level(), serverPlayer, hand, hit);

        if (serverPlayer.containerMenu == containerBefore) {
            WormholeAnchor.clear(serverPlayer);
        }

        return result.consumesAction();
    }

    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        if (input.contains("myFreq")) {
            short freq = input.getShort("myFreq");
            var grid = part.getMainNode().getGrid();

            if (grid != null) {
                ((P2PTunnelPartAccessor) part).setOutput(true);
                P2PService.get(grid).updateFreq(part, freq);
                part.getConnectionManager().sendBlockUpdateToOppositeSide();
            }
        }
    }

    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        if (mode != SettingsFrom.MEMORY_CARD) {
            return;
        }

        if (!output.getAllKeys().isEmpty()) {
            var iterator = output.getAllKeys().iterator();
            while (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }

        output.putString("myType", IPartItem.getId(part.getPartItem()).toString());
        output.putBoolean("wormhole", true);

        if (part.getFrequency() != 0) {
            output.putShort("myFreq", part.getFrequency());

            var colors = Platform.p2p().toColors(part.getFrequency());
            int[] colorCode = new int[]{
                    colors[0].ordinal(), colors[0].ordinal(),
                    colors[1].ordinal(), colors[1].ordinal(),
                    colors[2].ordinal(), colors[2].ordinal(),
                    colors[3].ordinal(), colors[3].ordinal()
            };
            output.putIntArray(IMemoryCard.NBT_COLOR_CODE, colorCode);
        }
    }

    private boolean handleMemoryCard(Player player, ItemStack stack, IMemoryCard memoryCard) {
        CompoundTag configData = memoryCard.getData(stack);

        if (configData.contains("p2pType") || configData.contains("p2pFreq") || !configData.contains("wormhole")) {
            memoryCard.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
            return false;
        }

        part.importSettings(SettingsFrom.MEMORY_CARD, configData, player);
        memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
        return true;
    }

    private @Nullable ServerTarget resolveRemoteTarget() {
        if (part.getLevel() == null) {
            return null;
        }

        if (part.isOutput()) {
            var input = part.getInput();
            if (input == null || input.getHost() == null) {
                return null;
            }

            var remoteHost = input.getHost().getBlockEntity();
            if (!(remoteHost.getLevel() instanceof ServerLevel targetWorld)) {
                return null;
            }

            BlockPos targetPos = remoteHost.getBlockPos().relative(input.getSide());
            Direction hitFace = input.getSide().getOpposite();
            return new ServerTarget(targetWorld, targetPos, hitFace);
        }

        var outputs = part.getOutputs();
        if (outputs.isEmpty()) {
            return null;
        }

        var output = outputs.iterator().next();
        if (output.getHost() == null) {
            return null;
        }

        var remoteHost = output.getHost().getBlockEntity();
        if (!(remoteHost.getLevel() instanceof ServerLevel targetWorld)) {
            return null;
        }

        BlockPos targetPos = remoteHost.getBlockPos().relative(output.getSide());
        Direction hitFace = output.getSide().getOpposite();
        return new ServerTarget(targetWorld, targetPos, hitFace);
    }

    private record ServerTarget(ServerLevel level, BlockPos pos, Direction hitFace) {
    }
}