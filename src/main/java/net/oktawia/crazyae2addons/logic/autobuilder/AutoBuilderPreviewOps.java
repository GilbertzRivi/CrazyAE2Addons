package net.oktawia.crazyae2addons.logic.autobuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;

import java.util.ArrayList;
import java.util.List;

public final class AutoBuilderPreviewOps {

    private AutoBuilderPreviewOps() {
    }

    static void syncPreviewNow(AutoBuilderBE be) {
        be.setPreviewDirty(true);
        be.setChanged();
        if (!be.isClientSide()) {
            be.syncManaged();
        }
    }

    public static void togglePreview(AutoBuilderBE be) {
        be.previewEnabled = !be.previewEnabled;

        if (be.previewEnabled) {
            if (!be.isClientSide()) {
                rebuildPreviewFromCode(be);
                syncPreviewNow(be);
            }
        } else {
            be.previewPositions = new BlockPos[0];
            be.previewPalette = new String[0];
            be.previewIndices = new int[0];
            be.previewDirty = true;

            if (be.getLevel() != null && be.getLevel().isClientSide) {
                be.previewInfo = null;
            }

            be.setChanged();

            if (!be.isClientSide()) {
                syncPreviewNow(be);
            }
        }
    }

    public static void rebuildPreviewFromCode(AutoBuilderBE be) {
        be.loadCode();

        List<BlockPos> newPositions = new ArrayList<>();
        List<String> newPalette = new ArrayList<>();
        var indices = new it.unimi.dsi.fastutil.ints.IntArrayList();

        if (be.code == null || be.code.isEmpty()) {
            be.previewPositions = new BlockPos[0];
            be.previewPalette = new String[0];
            be.previewIndices = new int[0];
            be.previewDirty = true;

            if (be.getLevel() != null && be.getLevel().isClientSide) {
                be.previewInfo = null;
            }

            be.setChanged();
            if (!be.isClientSide()) {
                syncPreviewNow(be);
            }
            return;
        }

        BlockPos localCursor = BlockPos.ZERO;

        for (String token : be.code) {
            if (newPositions.size() >= be.PREVIEW_LIMIT) {
                break;
            }
            if (token == null || token.isEmpty()) {
                continue;
            }
            if (token.startsWith("Z|")) {
                continue;
            }

            if (token.equals("H")) {
                localCursor = BlockPos.ZERO;
                continue;
            }

            if (token.length() == 1) {
                char c = token.charAt(0);
                if ("FBLRUD".indexOf(c) >= 0) {
                    localCursor = BuilderCoordMath.stepLocal(localCursor, c);
                }
                continue;
            }

            String blockKey = null;

            if (token.startsWith("P|")) {
                blockKey = token.substring(2);
            } else if (token.startsWith("PEQ|") || token.startsWith("PNE|")) {
                String[] parts = token.substring(4).split("\\|", 2);
                if (parts.length == 2) {
                    blockKey = parts[0];
                }
            }

            if (blockKey == null || blockKey.isEmpty()) {
                continue;
            }

            int paletteIndex = newPalette.indexOf(blockKey);
            if (paletteIndex < 0) {
                newPalette.add(blockKey);
                paletteIndex = newPalette.size() - 1;
            }

            BlockPos localTotal = new BlockPos(
                    be.offset.getX() + localCursor.getX(),
                    be.offset.getY() + localCursor.getY(),
                    be.offset.getZ() + localCursor.getZ()
            );

            newPositions.add(transformRelative(be, localTotal));
            indices.add(paletteIndex);
        }

        be.previewPositions = newPositions.toArray(BlockPos[]::new);
        be.previewPalette = newPalette.toArray(String[]::new);
        be.previewIndices = indices.toIntArray();
        be.previewDirty = true;

        if (be.getLevel() != null && be.getLevel().isClientSide) {
            be.previewInfo = null;
        }

        be.setChanged();

        if (!be.isClientSide()) {
            syncPreviewNow(be);
        }
    }

    public static void setGhostRenderPos(AutoBuilderBE be, BlockPos pos) {
        if (pos == null) {
            return;
        }

        be.ghostRenderPos = pos;
        be.previewDirty = true;

        if (be.getLevel() != null && be.getLevel().isClientSide) {
            be.previewInfo = null;
        }

        be.setChanged();

        if (!be.isClientSide()) {
            be.syncManaged();
        }
    }

    public static void onOffsetChanged(AutoBuilderBE be, BlockPos oldOffset) {
        if (be.isClientSide() && be.previewEnabled && be.previewPositions.length > 0) {
            BlockPos oldWorld = be.getBlockPos().offset(localToWorldOffset(be, oldOffset));
            BlockPos newWorld = be.getBlockPos().offset(localToWorldOffset(be, be.offset));
            int dx = newWorld.getX() - oldWorld.getX();
            int dy = newWorld.getY() - oldWorld.getY();
            int dz = newWorld.getZ() - oldWorld.getZ();

            for (int i = 0; i < be.previewPositions.length; i++) {
                if (be.previewPositions[i] != null) {
                    be.previewPositions[i] = be.previewPositions[i].offset(dx, dy, dz);
                }
            }

            be.previewDirty = true;
            be.previewInfo = null;
        }

        resetGhostToHome(be);

        if (be.previewEnabled && !be.isClientSide()) {
            rebuildPreviewFromCode(be);
        }

        AutoBuilderWorldOps.recalculateRequiredEnergy(be);
        be.setChanged();

        if (!be.isClientSide()) {
            be.syncManaged();
        }
    }

    public static void resetGhostToHome(AutoBuilderBE be) {
        setGhostRenderPos(be, homePos(be));
    }

    static BlockPos homePos(AutoBuilderBE be) {
        return transformRelative(be, be.offset);
    }

    static BlockPos transformRelative(AutoBuilderBE be, BlockPos local) {
        return be.getBlockPos().offset(localToWorldOffset(be, local));
    }

    static Direction getFacing(AutoBuilderBE be) {
        BlockState state = be.getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        }
        return Direction.NORTH;
    }

    static BlockPos localToWorldOffset(AutoBuilderBE be, BlockPos local) {
        return BuilderCoordMath.localToWorldOffset(local, getFacing(be));
    }

    static BlockPos stepRelative(AutoBuilderBE be, BlockPos pos, char cmd) {
        return BuilderCoordMath.stepRelative(pos, cmd, getFacing(be));
    }
}