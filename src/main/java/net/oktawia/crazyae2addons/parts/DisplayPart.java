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
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
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
import net.oktawia.crazyae2addons.defs.components.DisplayPartData;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.display.DisplayGrid;
import net.oktawia.crazyae2addons.logic.display.DisplayImageEntry;
import net.oktawia.crazyae2addons.logic.display.DisplayTokenResolver;
import net.oktawia.crazyae2addons.logic.display.SampleRing;
import net.oktawia.crazyae2addons.menus.DisplayMenu;
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

    private static final String NBT_DISPLAY_DATA = "display_data";
    private static final String NBT_DISPLAY_IMAGES = "display_images";
    private static final String NBT_DISPLAY_IMAGE_DATA = "display_image_data";
    private static final String NBT_SELECTED_DISPLAY_IMAGE = "selected_display_image";

    public byte spin = 0;
    public String textValue = "";
    public boolean mode = true;
    public boolean margin = false;
    public boolean center = false;

    public HashMap<String, String> resolvedTokens = new HashMap<>();
    public final Map<String, SampleRing> rateHistory = new HashMap<>();
    public transient int gridWarmupRemaining = 2;

    @Nullable
    public volatile String pendingInsert = null;
    public volatile int pendingInsertCursor = -1;

    private final List<DisplayImageEntry> displayImages = new ArrayList<>();
    private final Map<String, byte[]> displayImageData = new HashMap<>();
    private String selectedDisplayImageId = "";

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

    @Override
    public void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);

        data.writeUtf(textValue != null ? textValue : "");
        data.writeByte(spin);
        data.writeByte((mode ? 1 : 0) | (margin ? 2 : 0) | (center ? 4 : 0));

        data.writeVarInt(displayImages.size());
        for (DisplayImageEntry entry : displayImages) {
            data.writeUtf(entry.id());
            data.writeUtf(entry.sourceName());
            data.writeVarInt(entry.x());
            data.writeVarInt(entry.y());
            data.writeVarInt(entry.width());
            data.writeVarInt(entry.height());

            byte[] bytes = displayImageData.get(entry.id());
            data.writeByteArray(bytes == null ? new byte[0] : bytes);
        }

        data.writeUtf(selectedDisplayImageId == null ? "" : selectedDisplayImageId);
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean wasRegistered = CLIENT_INSTANCES.contains(this);

        boolean oldMode = this.mode;
        boolean oldMargin = this.margin;
        boolean oldCenter = this.center;
        byte oldSpin = this.spin;
        String oldTextValue = this.textValue;
        String oldSelectedImageId = this.selectedDisplayImageId;
        int oldImageCount = this.displayImages.size();

        boolean oldPowered = this.isPowered();
        Direction oldSide = this.getSide();

        boolean changed = super.readFromStream(data);

        textValue = data.readUtf();
        spin = data.readByte();

        int flags = data.readByte();
        mode = (flags & 1) != 0;
        margin = (flags & 2) != 0;
        center = (flags & 4) != 0;

        displayImages.clear();
        displayImageData.clear();

        int imageCount = data.readVarInt();
        for (int i = 0; i < imageCount; i++) {
            String id = data.readUtf();
            String sourceName = data.readUtf();
            int x = data.readVarInt();
            int y = data.readVarInt();
            int width = data.readVarInt();
            int height = data.readVarInt();
            byte[] bytes = data.readByteArray();

            displayImages.add(new DisplayImageEntry(
                    id,
                    sourceName,
                    Math.clamp(x, 0, 100),
                    Math.clamp(y, 0, 100),
                    Math.clamp(width, 0, 100),
                    Math.clamp(height, 0, 100)
            ));

            if (bytes != null && bytes.length > 0) {
                displayImageData.put(id, bytes);
            }
        }

        selectedDisplayImageId = data.readUtf();
        normalizeSelectedImage();

        if (!wasRegistered) {
            CLIENT_INSTANCES.add(this);
        }

        boolean topologyChanged =
                !wasRegistered
                        || oldMode != this.mode
                        || oldPowered != this.isPowered()
                        || oldSide != this.getSide();

        boolean imageStateChanged =
                oldImageCount != this.displayImages.size()
                        || !oldSelectedImageId.equals(this.selectedDisplayImageId);

        boolean displayStateChanged =
                oldSpin != this.spin
                        || oldMargin != this.margin
                        || oldCenter != this.center
                        || !oldTextValue.equals(this.textValue);

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
            this.spin = rotation;
        }
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeToNBT(tag, registries);

        tag.put(NBT_DISPLAY_DATA, DisplayPartData.CODEC.encodeStart(
                NbtOps.INSTANCE,
                new DisplayPartData(
                        this.textValue,
                        this.spin,
                        this.mode,
                        this.margin,
                        this.center
                )
        ).getOrThrow());

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
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.readFromNBT(tag, registries);

        if (tag.contains(NBT_DISPLAY_DATA, Tag.TAG_COMPOUND)) {
            DisplayPartData data = DisplayPartData.CODEC
                    .parse(NbtOps.INSTANCE, tag.getCompound(NBT_DISPLAY_DATA))
                    .getOrThrow();

            this.textValue = data.textValue();
            this.spin = data.spin();
            this.mode = data.mode();
            this.margin = data.margin();
            this.center = data.center();
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
        return displayImages;
    }

    public Map<String, byte[]> getDisplayImageData() {
        return displayImageData;
    }

    @Nullable
    public DisplayImageEntry getSelectedDisplayImage() {
        normalizeSelectedImage();

        if (selectedDisplayImageId == null || selectedDisplayImageId.isEmpty()) {
            return displayImages.isEmpty() ? null : displayImages.getFirst();
        }

        for (DisplayImageEntry entry : displayImages) {
            if (entry.id().equals(selectedDisplayImageId)) {
                return entry;
            }
        }

        return displayImages.isEmpty() ? null : displayImages.getFirst();
    }

    public void selectDisplayImage(@Nullable String id) {
        this.selectedDisplayImageId = id == null ? "" : id;
        normalizeSelectedImage();
        markDirtyAndSync();
    }

    public void addDisplayImage(String sourceName, byte[] pngBytes, int width, int height) {
        if (pngBytes == null || pngBytes.length == 0) {
            return;
        }

        String id = UUID.randomUUID().toString();
        String normalizedName = (sourceName == null || sourceName.isBlank()) ? "image.png" : sourceName;

        displayImageData.put(id, pngBytes);
        displayImages.add(new DisplayImageEntry(
                id,
                normalizedName,
                0,
                0,
                100,
                100
        ));
        selectedDisplayImageId = id;

        markDirtyAndSync();
    }

    public void removeDisplayImage(String id) {
        if (id == null || id.isBlank()) {
            return;
        }

        boolean removed = displayImages.removeIf(entry -> entry.id().equals(id));
        displayImageData.remove(id);

        if (removed && id.equals(selectedDisplayImageId)) {
            selectedDisplayImageId = displayImages.isEmpty() ? "" : displayImages.getFirst().id();
        }

        normalizeSelectedImage();
        markDirtyAndSync();
    }

    public void updateDisplayImage(String id, int x, int y, int width, int height) {
        if (id == null || id.isBlank()) {
            return;
        }

        for (int i = 0; i < displayImages.size(); i++) {
            DisplayImageEntry current = displayImages.get(i);
            if (current.id().equals(id)) {
                displayImages.set(i, current.withBounds(
                        Math.clamp(x, 0, 100),
                        Math.clamp(y, 0, 100),
                        Math.clamp(width, 0, 100),
                        Math.clamp(height, 0, 100)
                ));
                selectedDisplayImageId = id;
                markDirtyAndSync();
                return;
            }
        }
    }

    @Nullable
    public byte[] getDisplayImageBytes(String id) {
        return displayImageData.get(id);
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

    private void markDirtyAndSync() {
        if (getHost() != null) {
            getHost().markForSave();
            getHost().markForUpdate();
        }
    }
}