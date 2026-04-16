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
import appeng.items.parts.PartModels;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.parts.automation.PlaneModels;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.display.DisplayGrid;
import net.oktawia.crazyae2addons.logic.display.DisplayImageEntry;
import net.oktawia.crazyae2addons.logic.display.DisplayTokenResolver;
import net.oktawia.crazyae2addons.logic.display.SampleRing;
import net.oktawia.crazyae2addons.menus.part.DisplayMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class DisplayPart extends AEBasePart implements MenuProvider, ISubMenuHost, IGridTickable {

    private static final PlaneModels MODELS = new PlaneModels(
            "part/display_mon_off",
            "part/display_mon_on"
    );

    public static final List<DisplayPart> CLIENT_INSTANCES = new CopyOnWriteArrayList<>();

    private final PartState state = new PartState();

    public HashMap<String, String> resolvedTokens = new HashMap<>();
    public final Map<String, SampleRing> rateHistory = new HashMap<>();
    public transient int gridWarmupRemaining = 2;

    @Nullable
    public volatile String pendingInsert = null;
    public volatile int pendingInsertCursor = -1;

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public DisplayPart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode()
                .setIdlePowerUsage(1)
                .addService(IGridTickable.class, this);
    }

    public byte getSpin() {
        return state.spin;
    }

    public void setSpin(byte spin) {
        state.spin = spin;
    }

    public String getTextValue() {
        return state.getTextValue();
    }

    public void setTextValue(@Nullable String textValue) {
        state.setTextValue(textValue == null ? "" : textValue);
        markDirtyAndSync();
    }

    public boolean isMergeMode() {
        return state.isMergeMode();
    }

    public void setMergeMode(boolean mode) {
        boolean oldMode = state.isMergeMode();
        state.setMergeMode(mode);

        if (oldMode != mode && isClientSide()) {
            DisplayGrid.invalidateClientCache();
        }

        markDirtyAndSync();
    }

    public boolean isAddMargin() {
        return state.isAddMargin();
    }

    public void setAddMargin(boolean margin) {
        state.setAddMargin(margin);
        markDirtyAndSync();
    }

    public boolean getCenterText() {
        return state.isCenterText();
    }

    public void setCenterText(boolean center) {
        state.setCenterText(center);
        markDirtyAndSync();
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        state.writeToBuff(data);
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean wasRegistered = CLIENT_INSTANCES.contains(this);

        boolean oldMode = state.isMergeMode();
        boolean oldMargin = state.isAddMargin();
        boolean oldCenter = state.isCenterText();
        byte oldSpin = state.getSpin();
        String oldTextValue = state.getTextValue();
        String oldSelectedImageId = state.selectedDisplayImageId;
        int oldImageCount = state.displayImages.size();

        boolean oldPowered = this.isPowered();
        Direction oldSide = this.getSide();

        boolean changed = super.readFromStream(data);
        state.readFromBuff(data);
        state.normalizeSelectedImage();

        if (!wasRegistered) {
            CLIENT_INSTANCES.add(this);
        }

        boolean topologyChanged =
                !wasRegistered
                        || oldMode != state.isMergeMode()
                        || oldPowered != this.isPowered()
                        || oldSide != this.getSide();

        boolean imageStateChanged =
                oldImageCount != state.displayImages.size()
                        || !oldSelectedImageId.equals(state.selectedDisplayImageId);

        boolean displayStateChanged =
                oldSpin != state.getSpin()
                        || oldMargin != state.isAddMargin()
                        || oldCenter != state.isCenterText()
                        || !oldTextValue.equals(state.getTextValue());

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
        return new TickingRequest(20, 20, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
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
    public IPartModel getStaticModels() {
        return MODELS.getModel(isPowered(), isActive());
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!player.level().isClientSide()) {
            DisplayPart menuTarget = DisplayGrid.resolveMenuOrigin(this);
            MenuOpener.open(CrazyMenuRegistrar.DISPLAY_MENU.get(), player, MenuLocators.forPart(menuTarget));
        }
        return true;
    }

    @Override
    public void onPlacement(Player player) {
        super.onPlacement(player);
        byte rotation = (byte) (Mth.floor(player.getYRot() * 4f / 360f + 2.5) & 3);
        if (getSide() == Direction.UP || getSide() == Direction.DOWN) {
            state.setSpin(rotation);
        }
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeToNBT(tag, registries);
        tag.put("crazy_state", state.serializeNBT(registries));
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.readFromNBT(tag, registries);

        if (tag.contains("crazy_state", Tag.TAG_COMPOUND)) {
            state.deserializeNBT(registries, tag.getCompound("crazy_state"));
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
        return new ItemStack(CrazyItemRegistrar.DISPLAY_MONITOR_PART.get());
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
            return state.displayImages.isEmpty() ? null : state.displayImages.getFirst();
        }

        for (DisplayImageEntry entry : state.displayImages) {
            if (entry.id().equals(state.selectedDisplayImageId)) {
                return entry;
            }
        }

        return state.displayImages.isEmpty() ? null : state.displayImages.getFirst();
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
        state.displayImages.add(new DisplayImageEntry(
                id,
                normalizedName,
                0,
                0,
                100,
                100
        ));
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
            state.selectedDisplayImageId = state.displayImages.isEmpty() ? "" : state.displayImages.getFirst().id();
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
                        Math.clamp(x, 0, 100),
                        Math.clamp(y, 0, 100),
                        Math.clamp(width, 0, 100),
                        Math.clamp(height, 0, 100)
                ));
                state.selectedDisplayImageId = id;
                markDirtyAndSync();
                return;
            }
        }
    }

    public byte @Nullable [] getDisplayImageBytes(String id) {
        return state.displayImageData.get(id);
    }

    private void markDirtyAndSync() {
        if (getHost() != null) {
            getHost().markForSave();
            getHost().markForUpdate();
        }
    }

    @Getter
    @Setter
    public static final class PartState implements IPersistedSerializable {
        private static final String NBT_DISPLAY_IMAGES = "display_images";
        private static final String NBT_DISPLAY_IMAGE_DATA = "display_image_data";
        private static final String NBT_SELECTED_DISPLAY_IMAGE = "selected_display_image";

        @Persisted
        private String textValue = "";

        @Persisted
        private byte spin = 0;

        @Persisted
        private boolean mergeMode = true;

        @Persisted
        private boolean addMargin = false;

        @Persisted
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

        @Override
        public Tag serializeAdditionalNBT(HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();

            ListTag imageList = getTags();
            tag.put(NBT_DISPLAY_IMAGES, imageList);

            CompoundTag imageDataTag = new CompoundTag();
            for (Map.Entry<String, byte[]> entry : displayImageData.entrySet()) {
                imageDataTag.putByteArray(entry.getKey(), entry.getValue());
            }
            tag.put(NBT_DISPLAY_IMAGE_DATA, imageDataTag);

            tag.putString(NBT_SELECTED_DISPLAY_IMAGE, selectedDisplayImageId == null ? "" : selectedDisplayImageId);
            return tag;
        }

        private @NotNull ListTag getTags() {
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
            return imageList;
        }

        @Override
        public void deserializeAdditionalNBT(Tag rawTag, HolderLookup.Provider provider) {
            if (!(rawTag instanceof CompoundTag tag)) {
                return;
            }

            displayImages.clear();
            if (tag.contains(NBT_DISPLAY_IMAGES, Tag.TAG_LIST)) {
                ListTag imageList = tag.getList(NBT_DISPLAY_IMAGES, Tag.TAG_COMPOUND);
                for (int i = 0; i < imageList.size(); i++) {
                    CompoundTag img = imageList.getCompound(i);
                    displayImages.add(new DisplayImageEntry(
                            img.getString("id"),
                            img.getString("source"),
                            Math.clamp(img.getInt("x"), 0, 100),
                            Math.clamp(img.getInt("y"), 0, 100),
                            Math.clamp(img.getInt("width"), 0, 100),
                            Math.clamp(img.getInt("height"), 0, 100)
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

            if (selectedDisplayImageId == null || selectedDisplayImageId.isBlank()) {
                selectedDisplayImageId = displayImages.getFirst().id();
                return;
            }

            for (DisplayImageEntry entry : displayImages) {
                if (entry.id().equals(selectedDisplayImageId)) {
                    return;
                }
            }

            selectedDisplayImageId = displayImages.getFirst().id();
        }
    }
}