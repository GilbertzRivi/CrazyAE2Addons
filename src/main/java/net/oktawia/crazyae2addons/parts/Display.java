package net.oktawia.crazyae2addons.parts;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.display.DisplayGrid;
import net.oktawia.crazyae2addons.logic.display.DisplayImageEntry;
import net.oktawia.crazyae2addons.logic.display.DisplayTokenResolver;
import net.oktawia.crazyae2addons.logic.display.SampleRing;
import net.oktawia.crazyae2addons.menus.part.DisplayMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class Display extends AEBasePart implements MenuProvider, ISubMenuHost, IGridTickable {

    private static final ResourceLocation MODEL_CHASSIS_OFF = AppEng.makeId("part/transition_plane_off");
    private static final ResourceLocation MODEL_CHASSIS_ON = AppEng.makeId("part/transition_plane_on");
    private static final ResourceLocation MODEL_CHASSIS_HAS_CHANNEL = AppEng.makeId("part/transition_plane_has_channel");

    private static final ResourceLocation FRONT_MODEL_OFF = CrazyAddons.makeId("part/display_mon_off");
    private static final ResourceLocation FRONT_MODEL_ON = CrazyAddons.makeId("part/display_mon_on");

    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(MODEL_CHASSIS_OFF, FRONT_MODEL_OFF);

    @PartModels
    public static final PartModel MODELS_ON = new PartModel(MODEL_CHASSIS_ON, FRONT_MODEL_OFF);

    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_CHASSIS_HAS_CHANNEL, FRONT_MODEL_ON);

    public static final List<Display> CLIENT_INSTANCES = new CopyOnWriteArrayList<>();

    private final PartState state = new PartState();

    public final HashMap<String, String> resolvedTokens = new HashMap<>();
    public final Map<String, SampleRing> rateHistory = new HashMap<>();
    public transient int gridWarmupRemaining = 2;

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

        boolean topologyChanged =
                !wasRegistered
                        || oldMode != state.mergeMode
                        || oldPowered != isPowered()
                        || oldSide != getSide()
                        || oldSpin != state.spin;

        boolean imageStateChanged =
                oldImageCount != state.displayImages.size()
                        || !oldSelectedImageId.equals(state.selectedDisplayImageId);

        boolean displayStateChanged =
                oldSpin != state.spin
                        || oldMargin != state.addMargin
                        || oldCenter != state.centerText
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

    public Map<String, byte[]> getDisplayImageData() {
        return state.displayImageData;
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

    public void addDisplayImage(String sourceName, byte[] pngBytes, int width, int height) {
        if (pngBytes == null || pngBytes.length == 0) {
            return;
        }

        String id = UUID.randomUUID().toString();
        String normalizedName = (sourceName == null || sourceName.isBlank()) ? "image.png" : sourceName;

        state.displayImageData.put(id, pngBytes);
        state.displayImages.add(new DisplayImageEntry(id, normalizedName, 0, 0, 100, 100));
        state.selectedDisplayImageId = id;

        markDirtyAndSync();
    }

    public void removeDisplayImage(String id) {
        if (id == null || id.isBlank()) {
            return;
        }

        boolean removed = state.displayImages.removeIf(entry -> entry.id().equals(id));
        state.displayImageData.remove(id);

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
                        Mth.clamp(height, 0, 100)
                ));
                state.selectedDisplayImageId = id;
                markDirtyAndSync();
                return;
            }
        }
    }

    public byte[] getDisplayImageBytes(String id) {
        return state.displayImageData.get(id);
    }

    private void markDirtyAndSync() {
        getHost().markForSave();
        if (getHost() != null) {
            getHost().markForSave();
            getHost().markForUpdate();
        }
    }

    @Getter
    @Setter
    public static final class PartState {

        private static final String NBT_TEXT = "text_value";
        private static final String NBT_SPIN = "spin";
        private static final String NBT_MERGE = "merge_mode";
        private static final String NBT_MARGIN = "add_margin";
        private static final String NBT_CENTER = "center_text";
        private static final String NBT_DISPLAY_IMAGES = "display_images";
        private static final String NBT_DISPLAY_IMAGE_DATA = "display_image_data";
        private static final String NBT_SELECTED_DISPLAY_IMAGE = "selected_display_image";

        private String textValue = "";
        private byte spin = 0;
        private boolean mergeMode = true;
        private boolean addMargin = false;
        private boolean centerText = false;

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private final List<DisplayImageEntry> displayImages = new ArrayList<>();

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private final Map<String, byte[]> displayImageData = new HashMap<>();

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private String selectedDisplayImageId = "";

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString(NBT_TEXT, textValue == null ? "" : textValue);
            tag.putByte(NBT_SPIN, spin);
            tag.putBoolean(NBT_MERGE, mergeMode);
            tag.putBoolean(NBT_MARGIN, addMargin);
            tag.putBoolean(NBT_CENTER, centerText);

            ListTag imageList = new ListTag();
            for (DisplayImageEntry entry : displayImages) {
                CompoundTag img = new CompoundTag();
                img.putString("id", entry.id());
                img.putString("source", entry.sourceName());
                img.putInt("x", entry.x());
                img.putInt("y", entry.y());
                img.putInt("width", entry.width());
                img.putInt("height", entry.height());
                imageList.add(img);
            }
            tag.put(NBT_DISPLAY_IMAGES, imageList);

            CompoundTag imageDataTag = new CompoundTag();
            for (Map.Entry<String, byte[]> entry : displayImageData.entrySet()) {
                imageDataTag.putByteArray(entry.getKey(), entry.getValue());
            }
            tag.put(NBT_DISPLAY_IMAGE_DATA, imageDataTag);

            tag.putString(NBT_SELECTED_DISPLAY_IMAGE, selectedDisplayImageId == null ? "" : selectedDisplayImageId);
            return tag;
        }

        public void load(CompoundTag tag) {
            textValue = tag.getString(NBT_TEXT);
            spin = tag.getByte(NBT_SPIN);
            mergeMode = tag.getBoolean(NBT_MERGE);
            addMargin = tag.getBoolean(NBT_MARGIN);
            centerText = tag.getBoolean(NBT_CENTER);

            displayImages.clear();
            if (tag.contains(NBT_DISPLAY_IMAGES, Tag.TAG_LIST)) {
                ListTag imageList = tag.getList(NBT_DISPLAY_IMAGES, Tag.TAG_COMPOUND);
                for (int i = 0; i < imageList.size(); i++) {
                    CompoundTag img = imageList.getCompound(i);
                    displayImages.add(new DisplayImageEntry(
                            img.getString("id"),
                            img.getString("source"),
                            Mth.clamp(img.getInt("x"), 0, 100),
                            Mth.clamp(img.getInt("y"), 0, 100),
                            Mth.clamp(img.getInt("width"), 0, 100),
                            Mth.clamp(img.getInt("height"), 0, 100)
                    ));
                }
            }

            displayImageData.clear();
            if (tag.contains(NBT_DISPLAY_IMAGE_DATA, Tag.TAG_COMPOUND)) {
                CompoundTag imageDataTag = tag.getCompound(NBT_DISPLAY_IMAGE_DATA);
                for (String key : imageDataTag.getAllKeys()) {
                    displayImageData.put(key, imageDataTag.getByteArray(key));
                }
            }

            selectedDisplayImageId = tag.getString(NBT_SELECTED_DISPLAY_IMAGE);
            normalizeSelectedImage();
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
    }
}