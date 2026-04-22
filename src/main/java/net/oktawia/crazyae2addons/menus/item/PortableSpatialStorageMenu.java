package net.oktawia.crazyae2addons.menus.item;

import appeng.menu.AEBaseMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.cutpaste.CutPasteStackState;
import net.oktawia.crazyae2addons.logic.cutpaste.PortableSpatialStorageHost;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.SendLongStringToClientPacket;
import net.oktawia.crazyae2addons.util.TemplateUtil;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PortableSpatialStorageMenu extends AEBaseMenu {

    private static final String ACTION_REQUEST_PREVIEW = "request_preview";
    private static final String ACTION_FLIP_HORIZONTAL = "flip_horizontal";
    private static final String ACTION_FLIP_VERTICAL = "flip_vertical";
    private static final String ACTION_ROTATE_CLOCKWISE = "rotate_clockwise";

    private static final String ACTION_OFFSET_LEFT = "offset_left";
    private static final String ACTION_OFFSET_RIGHT = "offset_right";
    private static final String ACTION_OFFSET_UP = "offset_up";
    private static final String ACTION_OFFSET_DOWN = "offset_down";
    private static final String ACTION_OFFSET_FRONT = "offset_front";
    private static final String ACTION_OFFSET_BACK = "offset_back";

    private static final String PREVIEW_SIDE_MAP_KEY = "crazy_preview_side_map";
    private static final int CHUNK_SIZE = 1_000_000;

    protected final PortableSpatialStorageHost host;

    public PortableSpatialStorageMenu(int id, Inventory playerInventory, PortableSpatialStorageHost host) {
        super(CrazyMenuRegistrar.PORTABLE_SPATIAL_STORAGE_MENU.get(), id, playerInventory, host);
        this.host = host;

        setupUpgrades(host.getUpgrades());

        registerClientAction(ACTION_REQUEST_PREVIEW, this::requestPreview);
        registerClientAction(ACTION_FLIP_HORIZONTAL, this::flipHorizontal);
        registerClientAction(ACTION_FLIP_VERTICAL, this::flipVertical);
        registerClientAction(ACTION_ROTATE_CLOCKWISE, Integer.class, this::rotateClockwise);

        registerClientAction(ACTION_OFFSET_LEFT, this::offsetLeft);
        registerClientAction(ACTION_OFFSET_RIGHT, this::offsetRight);
        registerClientAction(ACTION_OFFSET_UP, this::offsetUp);
        registerClientAction(ACTION_OFFSET_DOWN, this::offsetDown);
        registerClientAction(ACTION_OFFSET_FRONT, this::offsetFront);
        registerClientAction(ACTION_OFFSET_BACK, this::offsetBack);

        createPlayerInventorySlots(playerInventory);

        if (!isClientSide()) {
            requestPreview();
        }
    }

    public void requestPreview() {
        if (isClientSide()) {
            sendClientAction(ACTION_REQUEST_PREVIEW);
            return;
        }

        if (!host.hasStoredStructure()) {
            clearItemPreviewMirror();
            sendPreviewString("");
            return;
        }

        byte[] bytes = host.getStructureBytes();
        if (bytes == null || bytes.length == 0) {
            clearItemPreviewMirror();
            sendPreviewString("");
            return;
        }

        try {
            CompoundTag tag = TemplateUtil.decompressNbt(bytes);
            syncItemPreviewMirror(tag);
            sendPreviewString(TemplateUtil.toBase64(bytes));
        } catch (Exception ignored) {
            clearItemPreviewMirror();
            sendPreviewString("");
        }
    }

    public void flipHorizontal() {
        if (isClientSide()) {
            sendClientAction(ACTION_FLIP_HORIZONTAL);
            return;
        }

        if (!host.hasStoredStructure()) {
            return;
        }

        Direction sourceFacing = CutPasteStackState.getSourceFacing(host.getItemStack());

        applyTransformAndResend(
                tag -> TemplateUtil.applyFlipHToTag(tag, sourceFacing),
                buildHorizontalFlipSideMap(sourceFacing)
        );
    }

    public void flipVertical() {
        if (isClientSide()) {
            sendClientAction(ACTION_FLIP_VERTICAL);
            return;
        }

        if (!host.hasStoredStructure()) {
            return;
        }

        applyTransformAndResend(
                TemplateUtil::applyFlipVToTag,
                buildVerticalFlipSideMap()
        );
    }

    public void rotateClockwise(Integer times) {
        int turns = times == null ? 1 : times;
        int normalized = normalizeQuarterTurns(turns);

        if (isClientSide()) {
            sendClientAction(ACTION_ROTATE_CLOCKWISE, turns);
            return;
        }

        if (!host.hasStoredStructure() || normalized == 0) {
            return;
        }

        applyTransformAndResend(
                tag -> TemplateUtil.applyRotateCWToTag(tag, turns),
                buildRotationSideMap(normalized)
        );
    }

    public void offsetLeft() {
        if (isClientSide()) {
            sendClientAction(ACTION_OFFSET_LEFT);
            return;
        }
        applyOffsetAndResend(-1, 0, 0);
    }

    public void offsetRight() {
        if (isClientSide()) {
            sendClientAction(ACTION_OFFSET_RIGHT);
            return;
        }
        applyOffsetAndResend(1, 0, 0);
    }

    public void offsetUp() {
        if (isClientSide()) {
            sendClientAction(ACTION_OFFSET_UP);
            return;
        }
        applyOffsetAndResend(0, 1, 0);
    }

    public void offsetDown() {
        if (isClientSide()) {
            sendClientAction(ACTION_OFFSET_DOWN);
            return;
        }
        applyOffsetAndResend(0, -1, 0);
    }

    public void offsetFront() {
        if (isClientSide()) {
            sendClientAction(ACTION_OFFSET_FRONT);
            return;
        }
        applyOffsetAndResend(0, 0, -1);
    }

    public void offsetBack() {
        if (isClientSide()) {
            sendClientAction(ACTION_OFFSET_BACK);
            return;
        }
        applyOffsetAndResend(0, 0, 1);
    }

    @FunctionalInterface
    private interface TagTransform {
        CompoundTag apply(CompoundTag tag);
    }

    private void applyTransformAndResend(TagTransform transform, int[] appliedSideMap) {
        byte[] bytes = host.getStructureBytes();
        if (bytes == null || bytes.length == 0) {
            return;
        }

        try {
            CompoundTag tag = TemplateUtil.decompressNbt(bytes);
            CompoundTag transformed = transform.apply(tag);
            host.setStructureBytes(TemplateUtil.compressNbt(transformed));
            updatePreviewSideMap(appliedSideMap);
            syncItemPreviewMirror(transformed);
        } catch (Exception ignored) {
            return;
        }

        requestPreview();
    }

    private void applyOffsetAndResend(int dx, int dy, int dz) {
        byte[] bytes = host.getStructureBytes();
        if (bytes == null || bytes.length == 0) {
            return;
        }

        try {
            CompoundTag tag = TemplateUtil.decompressNbt(bytes);
            CompoundTag transformed = TemplateUtil.applyOffsetToTag(tag, dx, dy, dz);
            host.setStructureBytes(TemplateUtil.compressNbt(transformed));
            syncItemPreviewMirror(transformed);
        } catch (Exception ignored) {
            return;
        }

        requestPreview();
    }

    private void syncItemPreviewMirror(CompoundTag structureTag) {
        CompoundTag stackTag = host.getItemStack().getOrCreateTag();
        TemplateUtil.copyPreviewTransformState(structureTag, stackTag);
    }

    private void clearItemPreviewMirror() {
        CompoundTag stackTag = host.getItemStack().getOrCreateTag();
        TemplateUtil.setTemplateOffset(stackTag, BlockPos.ZERO);
        TemplateUtil.setEnergyOrigin(stackTag, BlockPos.ZERO);
    }

    private int normalizeQuarterTurns(int turns) {
        return ((turns % 4) + 4) % 4;
    }

    private void sendPreviewString(String base64) {
        if (!(getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        NetworkHandler.sendToPlayer(serverPlayer, new SendLongStringToClientPacket("__RESET__"));

        if (base64 != null && !base64.isEmpty()) {
            byte[] bytes = base64.getBytes(StandardCharsets.UTF_8);
            int total = (int) Math.ceil((double) bytes.length / CHUNK_SIZE);

            for (int i = 0; i < total; i++) {
                int start = i * CHUNK_SIZE;
                int end = Math.min(bytes.length, (i + 1) * CHUNK_SIZE);
                byte[] part = Arrays.copyOfRange(bytes, start, end);

                NetworkHandler.sendToPlayer(
                        serverPlayer,
                        new SendLongStringToClientPacket(new String(part, StandardCharsets.UTF_8))
                );
            }
        }

        NetworkHandler.sendToPlayer(serverPlayer, new SendLongStringToClientPacket("__END__"));
    }

    private void updatePreviewSideMap(int[] appliedSideMap) {
        CompoundTag stackTag = host.getItemStack().getOrCreateTag();
        int[] current = readPreviewSideMap(stackTag);
        int[] combined = composeSideMaps(current, appliedSideMap);
        writePreviewSideMap(stackTag, combined);
    }

    private int[] readPreviewSideMap(CompoundTag tag) {
        int[] identity = identitySideMap();

        if (tag == null || !tag.contains(PREVIEW_SIDE_MAP_KEY, net.minecraft.nbt.Tag.TAG_INT_ARRAY)) {
            return identity;
        }

        int[] raw = tag.getIntArray(PREVIEW_SIDE_MAP_KEY);
        if (raw.length != Direction.values().length) {
            return identity;
        }

        for (Direction side : Direction.values()) {
            int mapped = raw[side.ordinal()];
            if (mapped < 0 || mapped >= Direction.values().length) {
                return identity;
            }
        }

        return raw;
    }

    private void writePreviewSideMap(CompoundTag tag, int[] sideMap) {
        tag.putIntArray(PREVIEW_SIDE_MAP_KEY, sideMap);
    }

    private int[] composeSideMaps(int[] current, int[] applied) {
        int[] result = new int[Direction.values().length];

        for (Direction side : Direction.values()) {
            int currentMapped = current[side.ordinal()];
            result[side.ordinal()] = applied[currentMapped];
        }

        return result;
    }

    private int[] identitySideMap() {
        int[] map = new int[Direction.values().length];
        for (Direction side : Direction.values()) {
            map[side.ordinal()] = side.ordinal();
        }
        return map;
    }

    private int[] buildRotationSideMap(int normalizedQuarterTurns) {
        int[] map = identitySideMap();

        for (Direction side : Direction.values()) {
            Direction mapped = side;
            for (int i = 0; i < normalizedQuarterTurns; i++) {
                mapped = rotateClockwiseSide(mapped);
            }
            map[side.ordinal()] = mapped.ordinal();
        }

        return map;
    }

    private Direction rotateClockwiseSide(Direction side) {
        return switch (side) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
        };
    }

    private int[] buildHorizontalFlipSideMap(Direction sourceFacing) {
        int[] map = identitySideMap();

        if (sourceFacing.getAxis() == Direction.Axis.Z) {
            map[Direction.EAST.ordinal()] = Direction.WEST.ordinal();
            map[Direction.WEST.ordinal()] = Direction.EAST.ordinal();
        } else {
            map[Direction.NORTH.ordinal()] = Direction.SOUTH.ordinal();
            map[Direction.SOUTH.ordinal()] = Direction.NORTH.ordinal();
        }

        return map;
    }

    private int[] buildVerticalFlipSideMap() {
        int[] map = identitySideMap();
        map[Direction.UP.ordinal()] = Direction.DOWN.ordinal();
        map[Direction.DOWN.ordinal()] = Direction.UP.ordinal();
        return map;
    }
}