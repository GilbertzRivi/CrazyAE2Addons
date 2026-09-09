package net.oktawia.crazyae2addons.parts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.storage.ISubMenuHost;
import appeng.api.util.AECableType;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.parts.PartModel;
import appeng.util.SettingsFrom;

import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.display.DisplayGrid;
import net.oktawia.crazyae2addons.logic.display.DisplayImageEntry;
import net.oktawia.crazyae2addons.logic.display.DisplayImageStore;
import net.oktawia.crazyae2addons.logic.display.DisplayImageUploadStatus;
import net.oktawia.crazyae2addons.logic.display.DisplayTokenResolver;
import net.oktawia.crazyae2addons.logic.display.SampleRing;
import net.oktawia.crazyae2addons.menus.part.DisplayMenu;

public class Display extends AEBasePart implements MenuProvider, ISubMenuHost, IGridTickable {

    public enum LocalDir {
        UP, DOWN, LEFT, RIGHT
    }

    private static final ResourceLocation MODEL_CHASSIS_OFF = AppEng.makeId("part/transition_plane_off");
    private static final ResourceLocation MODEL_CHASSIS_ON = AppEng.makeId("part/transition_plane_on");
    private static final ResourceLocation MODEL_CHASSIS_HAS_CHANNEL = AppEng
            .makeId("part/transition_plane_has_channel");

    private static final ResourceLocation FRONT_MODEL_OFF = CrazyAddons.makeId("part/display_mon_off");
    private static final ResourceLocation FRONT_MODEL_ON = CrazyAddons.makeId("part/display_mon_on");

    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(MODEL_CHASSIS_OFF, FRONT_MODEL_OFF);

    @PartModels
    public static final PartModel MODELS_ON = new PartModel(MODEL_CHASSIS_ON, FRONT_MODEL_OFF);

    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_CHASSIS_HAS_CHANNEL, FRONT_MODEL_ON);

    public static final List<Display> CLIENT_INSTANCES = new CopyOnWriteArrayList<>();

    public static final int MAX_DISPLAY_IMAGES = 50;

    private final PartState state = new PartState();

    public final HashMap<String, String> resolvedTokens = new HashMap<>();
    public final Map<String, SampleRing> rateHistory = new HashMap<>();
    public transient int gridWarmupRemaining = 2;
    private static final String NBT_MEMORY_DISPLAY = "crazy_display_memory";

    @Nullable
    public volatile String pendingInsert = null;
    public volatile int pendingInsertCursor = -1;

    public Display(IPartItem<?> partItem) {
        super(partItem);
        getMainNode()
                .setIdlePowerUsage(1)
                .addService(IGridTickable.class, this);
    }

    @Override
    public IPartModel getStaticModels() {
        if (isPowered() && isActive()) {
            return MODELS_HAS_CHANNEL;
        }
        if (isPowered()) {
            return MODELS_ON;
        }
        return MODELS_OFF;
    }

    public byte getSpin() {
        return state.spin;
    }

    public String getTextValue() {
        return state.textValue;
    }

    public void setTextValue(@Nullable String textValue) {
        state.textValue = textValue == null ? "" : textValue;
        markDirtyAndSync();
    }

    public boolean isMergeMode() {
        return state.mergeMode;
    }

    public void setMergeMode(boolean mode) {
        boolean oldMode = state.mergeMode;
        state.mergeMode = mode;

        if (oldMode != mode && isClientSide()) {
            DisplayGrid.invalidateClientCache();
        }

        markDirtyAndSync();
    }

    public boolean isAddMargin() {
        return state.addMargin;
    }

    public void setAddMargin(boolean margin) {
        state.addMargin = margin;
        markDirtyAndSync();
    }

    public int getFontSize() {
        return state.fontSize;
    }

    public void setFontSize(int fontSize) {
        state.fontSize = Mth.clamp(fontSize, 0, 999);
        markDirtyAndSync();
    }

    public boolean getCenterText() {
        return state.centerText;
    }

    public void setCenterText(boolean center) {
        state.centerText = center;
        markDirtyAndSync();
    }

    @Override
    public void writeToStream(FriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeNbt(state.save());
    }

    @Override
    public boolean readFromStream(FriendlyByteBuf data) {
        boolean wasRegistered = CLIENT_INSTANCES.contains(this);

        boolean oldMode = state.mergeMode;
        boolean oldMargin = state.addMargin;
        boolean oldCenter = state.centerText;
        int oldFontSize = state.fontSize;
        byte oldSpin = state.spin;
        String oldTextValue = state.textValue;
        String oldSelectedImageId = state.selectedDisplayImageId;
        int oldImageCount = state.displayImages.size();

        boolean oldPowered = isPowered();
        Direction oldSide = getSide();

        boolean changed = super.readFromStream(data);

        CompoundTag tag = data.readNbt();
        if (tag != null) {
            state.load(tag);
            state.normalizeSelectedImage();
        }

        if (!wasRegistered) {
            CLIENT_INSTANCES.add(this);
        }

        boolean topologyChanged = !wasRegistered
                || oldMode != state.mergeMode
                || oldPowered != isPowered()
                || oldSide != getSide()
                || oldSpin != state.spin;

        boolean imageStateChanged = oldImageCount != state.displayImages.size()
                || !oldSelectedImageId.equals(state.selectedDisplayImageId);

        boolean displayStateChanged = oldSpin != state.spin
                || oldMargin != state.addMargin
                || oldCenter != state.centerText
                || oldFontSize != state.fontSize
                || !oldTextValue.equals(state.textValue);

        if (topologyChanged) {
            DisplayGrid.invalidateClientCache();
        }

        return changed || topologyChanged || imageStateChanged || displayStateChanged;
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();

        boolean removed = CLIENT_INSTANCES.remove(this);
        if (removed) {
            DisplayGrid.invalidateClientCache();
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(20, 20, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!CrazyConfig.COMMON.DISPLAY_ENABLED.get()) {
            return TickRateModulation.SLEEP;
        }
        DisplayTokenResolver.recomputeVariablesAndNotify(this);
        return TickRateModulation.IDLE;
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(0, 0, 15.5, 16, 16, 16);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 1;
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!CrazyConfig.COMMON.DISPLAY_ENABLED.get()) {
            return true;
        }
        if (!player.level().isClientSide()) {
            if (player.isShiftKeyDown()) {
                MenuOpener.open(CrazyMenuRegistrar.DISPLAY_MENU.get(), player, MenuLocators.forPart(this));
            } else {
                Display menuTarget = DisplayGrid.resolveMenuOrigin(this);
                MenuOpener.open(CrazyMenuRegistrar.DISPLAY_MENU.get(), player, MenuLocators.forPart(menuTarget));
            }
        }
        return true;
    }

    @Override
    public void onPlacement(Player player) {
        super.onPlacement(player);
        byte rotation = (byte) (Mth.floor(player.getYRot() * 4f / 360f + 2.5f) & 3);
        if (getSide() == Direction.UP || getSide() == Direction.DOWN) {
            state.spin = rotation;
            markDirtyAndSync();
        }
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.put("crazy_state", state.save());
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);

        if (tag.contains("crazy_state", Tag.TAG_COMPOUND)) {
            state.load(tag.getCompound("crazy_state"));
            state.normalizeSelectedImage();
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DisplayMenu(id, inv, this);
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(CrazyMenuRegistrar.DISPLAY_MENU.get(), player, subMenu.getLocator());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return getPartItem().asItem().getDefaultInstance();
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    public List<DisplayImageEntry> getDisplayImages() {
        return state.displayImages;
    }

    @Nullable
    public DisplayImageEntry getSelectedDisplayImage() {
        state.normalizeSelectedImage();

        if (state.selectedDisplayImageId == null || state.selectedDisplayImageId.isEmpty()) {
            return state.displayImages.isEmpty() ? null : state.displayImages.get(0);
        }

        for (DisplayImageEntry entry : state.displayImages) {
            if (entry.id().equals(state.selectedDisplayImageId)) {
                return entry;
            }
        }

        return state.displayImages.isEmpty() ? null : state.displayImages.get(0);
    }

    public void selectDisplayImage(@Nullable String id) {
        state.selectedDisplayImageId = id == null ? "" : id;
        state.normalizeSelectedImage();
        markDirtyAndSync();
    }

    public DisplayImageUploadStatus addDisplayImage(
            String sourceName,
            byte[] pngBytes,
            int frameCount,
            int columns,
            int frameDurationMs) {
        if (pngBytes == null || pngBytes.length == 0 || !(getLevel() instanceof ServerLevel)) {
            return DisplayImageUploadStatus.FAILED;
        }

        if (state.displayImages.size() >= MAX_DISPLAY_IMAGES) {
            return DisplayImageUploadStatus.TOO_MANY_IMAGES;
        }

        String id = DisplayImageStore.get(getLevel()).putImageIfFits(pngBytes);

        if (id == null) {
            return DisplayImageUploadStatus.STORAGE_FULL;
        }

        String normalizedName = (sourceName == null || sourceName.isBlank()) ? "image.png" : sourceName;

        int frames = Mth.clamp(frameCount, 1, CrazyConfig.COMMON.DISPLAY_IMAGE_MAX_FRAMES.get());
        int normalizedColumns = frames > 1 ? Mth.clamp(columns, 1, frames) : 1;
        int duration = frames > 1 ? Mth.clamp(frameDurationMs, 20, 2000) : 0;

        state.displayImages.add(new DisplayImageEntry(
                id,
                normalizedName,
                0,
                0,
                100,
                100,
                frames,
                normalizedColumns,
                duration));
        state.selectedDisplayImageId = id;

        markDirtyAndSync();
        return DisplayImageUploadStatus.OK;
    }

    public void removeDisplayImage(String id) {
        if (id == null || id.isBlank()) {
            return;
        }

        boolean removed = state.displayImages.removeIf(entry -> entry.id().equals(id));
        if (removed && getLevel() instanceof ServerLevel) {
            DisplayImageStore.get(getLevel()).release(id);
        }

        if (removed && id.equals(state.selectedDisplayImageId)) {
            state.selectedDisplayImageId = state.displayImages.isEmpty() ? "" : state.displayImages.get(0).id();
        }

        state.normalizeSelectedImage();
        markDirtyAndSync();
    }

    public void updateDisplayImage(String id, int x, int y, int width, int height) {
        if (id == null || id.isBlank()) {
            return;
        }

        for (int i = 0; i < state.displayImages.size(); i++) {
            DisplayImageEntry current = state.displayImages.get(i);
            if (current.id().equals(id)) {
                state.displayImages.set(i, current.withBounds(
                        Mth.clamp(x, 0, 100),
                        Mth.clamp(y, 0, 100),
                        Mth.clamp(width, 0, 100),
                        Mth.clamp(height, 0, 100)));
                state.selectedDisplayImageId = id;
                markDirtyAndSync();
                return;
            }
        }
    }

    public void reorderDisplayImage(String id, int delta) {
        if (id == null || id.isBlank()) {
            return;
        }
        int idx = -1;
        for (int i = 0; i < state.displayImages.size(); i++) {
            if (state.displayImages.get(i).id().equals(id)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return;
        }
        int newIdx = Mth.clamp(idx + delta, 0, state.displayImages.size() - 1);
        if (newIdx == idx) {
            return;
        }
        Collections.swap(state.displayImages, idx, newIdx);
        markDirtyAndSync();
    }

    public boolean canConnectLocal(LocalDir dir) {
        return switch (dir) {
            case UP -> state.connectUp;
            case DOWN -> state.connectDown;
            case LEFT -> state.connectLeft;
            case RIGHT -> state.connectRight;
        };
    }

    public void setConnectLocal(LocalDir dir, boolean value) {
        switch (dir) {
            case UP -> state.connectUp = value;
            case DOWN -> state.connectDown = value;
            case LEFT -> state.connectLeft = value;
            case RIGHT -> state.connectRight = value;
        }
        markDirtyAndSync();
    }

    private void markDirtyAndSync() {
        if (getHost() != null) {
            getHost().markForSave();
            getHost().markForUpdate();
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        super.exportSettings(mode, output);

        if (mode != SettingsFrom.MEMORY_CARD) {
            return;
        }

        Display master = resolveMaster();
        output.put(NBT_MEMORY_DISPLAY, master.state.saveMemoryCardSettings());
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);

        if (mode != SettingsFrom.MEMORY_CARD) {
            return;
        }

        if (!input.contains(NBT_MEMORY_DISPLAY, Tag.TAG_COMPOUND)) {
            return;
        }

        Display master = resolveMaster();
        CompoundTag memory = input.getCompound(NBT_MEMORY_DISPLAY);
        master.state.loadMemoryCardSettings(memory);
        master.importDisplayImagesFromMemoryCard(memory);
        master.markDirtyAndSync();
    }

    private Display resolveMaster() {
        Display master = DisplayGrid.resolveMenuOrigin(this);
        return master == null ? this : master;
    }

    private void importDisplayImagesFromMemoryCard(CompoundTag memory) {
        if (!(getLevel() instanceof ServerLevel)) {
            return;
        }

        if (!memory.contains(PartState.NBT_DISPLAY_IMAGES, Tag.TAG_LIST)) {
            return;
        }

        DisplayImageStore store = DisplayImageStore.get(getLevel());
        List<DisplayImageEntry> cardEntries = PartState
                .readImageEntries(memory.getList(PartState.NBT_DISPLAY_IMAGES, Tag.TAG_COMPOUND));

        List<DisplayImageEntry> imported = new ArrayList<>();
        for (DisplayImageEntry entry : cardEntries) {
            if (store.acquire(entry.id())) {
                imported.add(entry);
            }
        }

        if (!cardEntries.isEmpty() && imported.isEmpty()) {
            return;
        }

        for (DisplayImageEntry entry : state.displayImages) {
            store.release(entry.id());
        }

        state.displayImages.clear();
        state.displayImages.addAll(imported);

        state.selectedDisplayImageId = "";
        state.normalizeSelectedImage();
    }

    @Getter
    @Setter
    public static final class PartState {

        private static final String NBT_TEXT = "text_value";
        private static final String NBT_SPIN = "spin";
        private static final String NBT_MERGE = "merge_mode";
        private static final String NBT_MARGIN = "add_margin";
        private static final String NBT_CENTER = "center_text";
        private static final String NBT_FONT_SIZE = "font_size";
        private static final String NBT_DISPLAY_IMAGES = "display_images";
        private static final String NBT_SELECTED_DISPLAY_IMAGE = "selected_display_image";
        private static final String NBT_CONNECT_UP = "connect_up";
        private static final String NBT_CONNECT_DOWN = "connect_down";
        private static final String NBT_CONNECT_LEFT = "connect_left";
        private static final String NBT_CONNECT_RIGHT = "connect_right";

        private String textValue = "";
        private byte spin = 0;
        private boolean mergeMode = true;
        private boolean addMargin = false;
        private boolean centerText = false;
        private int fontSize = 0;
        private boolean connectUp = true;
        private boolean connectDown = true;
        private boolean connectLeft = true;
        private boolean connectRight = true;

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private final List<DisplayImageEntry> displayImages = new ArrayList<>();

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private String selectedDisplayImageId = "";

        private static ListTag writeImageEntries(List<DisplayImageEntry> entries) {
            ListTag list = new ListTag();
            for (DisplayImageEntry entry : entries) {
                CompoundTag img = new CompoundTag();
                img.putString("id", entry.id());
                img.putString("source", entry.sourceName());
                img.putInt("x", entry.x());
                img.putInt("y", entry.y());
                img.putInt("width", entry.width());
                img.putInt("height", entry.height());
                img.putInt("frames", entry.frameCount());
                img.putInt("columns", entry.columns());
                img.putInt("frameMs", entry.frameDurationMs());
                list.add(img);
            }
            return list;
        }

        private static List<DisplayImageEntry> readImageEntries(ListTag list) {
            List<DisplayImageEntry> entries = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                CompoundTag img = list.getCompound(i);
                int frames = Math.max(1, img.getInt("frames"));
                entries.add(new DisplayImageEntry(
                        img.getString("id"),
                        img.getString("source"),
                        Mth.clamp(img.getInt("x"), 0, 100),
                        Mth.clamp(img.getInt("y"), 0, 100),
                        Mth.clamp(img.getInt("width"), 0, 100),
                        Mth.clamp(img.getInt("height"), 0, 100),
                        frames,
                        frames > 1 ? Math.max(1, img.getInt("columns")) : 1,
                        frames > 1 ? Math.max(1, img.getInt("frameMs")) : 0));
            }
            return entries;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString(NBT_TEXT, textValue == null ? "" : textValue);
            tag.putByte(NBT_SPIN, spin);
            tag.putBoolean(NBT_MERGE, mergeMode);
            tag.putBoolean(NBT_MARGIN, addMargin);
            tag.putBoolean(NBT_CENTER, centerText);
            tag.putInt(NBT_FONT_SIZE, fontSize);

            tag.put(NBT_DISPLAY_IMAGES, writeImageEntries(displayImages));

            tag.putString(NBT_SELECTED_DISPLAY_IMAGE, selectedDisplayImageId == null ? "" : selectedDisplayImageId);
            tag.putBoolean(NBT_CONNECT_UP, connectUp);
            tag.putBoolean(NBT_CONNECT_DOWN, connectDown);
            tag.putBoolean(NBT_CONNECT_LEFT, connectLeft);
            tag.putBoolean(NBT_CONNECT_RIGHT, connectRight);
            return tag;
        }

        public void load(CompoundTag tag) {
            textValue = tag.getString(NBT_TEXT);
            spin = tag.getByte(NBT_SPIN);
            mergeMode = tag.getBoolean(NBT_MERGE);
            addMargin = tag.getBoolean(NBT_MARGIN);
            centerText = tag.getBoolean(NBT_CENTER);
            fontSize = Mth.clamp(tag.getInt(NBT_FONT_SIZE), 0, 999);

            displayImages.clear();
            if (tag.contains(NBT_DISPLAY_IMAGES, Tag.TAG_LIST)) {
                displayImages.addAll(readImageEntries(tag.getList(NBT_DISPLAY_IMAGES, Tag.TAG_COMPOUND)));
            }

            selectedDisplayImageId = tag.getString(NBT_SELECTED_DISPLAY_IMAGE);
            normalizeSelectedImage();

            connectUp = !tag.contains(NBT_CONNECT_UP) || tag.getBoolean(NBT_CONNECT_UP);
            connectDown = !tag.contains(NBT_CONNECT_DOWN) || tag.getBoolean(NBT_CONNECT_DOWN);
            connectLeft = !tag.contains(NBT_CONNECT_LEFT) || tag.getBoolean(NBT_CONNECT_LEFT);
            connectRight = !tag.contains(NBT_CONNECT_RIGHT) || tag.getBoolean(NBT_CONNECT_RIGHT);
        }

        private void normalizeSelectedImage() {
            if (displayImages.isEmpty()) {
                selectedDisplayImageId = "";
                return;
            }

            if (selectedDisplayImageId == null || selectedDisplayImageId.isEmpty()) {
                selectedDisplayImageId = displayImages.get(0).id();
                return;
            }

            for (DisplayImageEntry entry : displayImages) {
                if (entry.id().equals(selectedDisplayImageId)) {
                    return;
                }
            }

            selectedDisplayImageId = displayImages.get(0).id();
        }

        public CompoundTag saveMemoryCardSettings() {
            CompoundTag tag = new CompoundTag();

            tag.putString(NBT_TEXT, textValue == null ? "" : textValue);
            tag.putBoolean(NBT_MERGE, mergeMode);
            tag.putBoolean(NBT_MARGIN, addMargin);
            tag.putBoolean(NBT_CENTER, centerText);
            tag.putInt(NBT_FONT_SIZE, fontSize);
            tag.put(NBT_DISPLAY_IMAGES, writeImageEntries(displayImages));

            return tag;
        }

        public void loadMemoryCardSettings(CompoundTag tag) {
            if (tag.contains(NBT_TEXT, Tag.TAG_STRING)) {
                textValue = tag.getString(NBT_TEXT);
            }

            if (tag.contains(NBT_MERGE, Tag.TAG_BYTE)) {
                mergeMode = tag.getBoolean(NBT_MERGE);
            }

            if (tag.contains(NBT_MARGIN, Tag.TAG_BYTE)) {
                addMargin = tag.getBoolean(NBT_MARGIN);
            }

            if (tag.contains(NBT_CENTER, Tag.TAG_BYTE)) {
                centerText = tag.getBoolean(NBT_CENTER);
            }

            if (tag.contains(NBT_FONT_SIZE, Tag.TAG_INT)) {
                fontSize = Mth.clamp(tag.getInt(NBT_FONT_SIZE), 0, 999);
            }

            normalizeSelectedImage();
        }
    }
}
