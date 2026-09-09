package net.oktawia.crazyae2addons.menus.part;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.guisync.GuiSync;

import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.display.DisplayGrid;
import net.oktawia.crazyae2addons.logic.display.DisplayImageEntry;
import net.oktawia.crazyae2addons.logic.display.DisplayImageStore;
import net.oktawia.crazyae2addons.logic.display.DisplayImageUploadStatus;
import net.oktawia.crazyae2addons.parts.Display;

public class DisplayImagesSubMenu extends AEBaseMenu implements ISubMenu {

    private static final Gson GSON = new Gson();
    private static final Type IMAGE_LIST_TYPE = new TypeToken<List<DisplayImageEntry>>() {
    }.getType();

    public static final String ACTION_SELECT = "selectImage";
    public static final String ACTION_REMOVE = "removeImage";
    public static final String ACTION_UPDATE = "updateImage";
    public static final String ACTION_REORDER = "reorderImage";

    @GuiSync(1)
    public String imagesJson = "[]";

    @GuiSync(2)
    public String selectedImageId = "";

    @Nullable
    private final Display host;

    @GuiSync(20)
    public int previewGridWidth = 1;

    @GuiSync(21)
    public int previewGridHeight = 1;

    @GuiSync(30)
    public int maxImageBytes = 0;

    @GuiSync(31)
    public int maxImageDimension = 0;

    @GuiSync(32)
    public int storageUsedBytes = 0;

    @GuiSync(33)
    public int storageBudgetBytes = 0;

    private transient String lastParsedImagesJson = null;
    private transient List<DisplayImageEntry> parsedImagesCache = List.of();

    public DisplayImagesSubMenu(int id, Inventory inv, @Nullable Display host) {
        super(CrazyMenuRegistrar.DISPLAY_IMAGES_SUBMENU.get(), id, inv, host);
        this.host = host;

        if (!isClientSide()) {
            syncFromHost();
        }

        registerClientAction(ACTION_SELECT, String.class, this::selectImage);
        registerClientAction(ACTION_REMOVE, String.class, this::removeImage);
        registerClientAction(ACTION_UPDATE, String.class, this::updateImageFromPayload);
        registerClientAction(ACTION_REORDER, String.class, this::reorderImageFromPayload);
    }

    @Override
    public Display getHost() {
        return host;
    }

    public List<DisplayImageEntry> getImages() {
        if (!isClientSide() && host != null) {
            return host.getDisplayImages();
        }

        if (lastParsedImagesJson == null || !lastParsedImagesJson.equals(imagesJson)) {
            try {
                List<DisplayImageEntry> parsed = GSON.fromJson(imagesJson, IMAGE_LIST_TYPE);
                parsedImagesCache = parsed != null ? parsed : List.of();
            } catch (Exception e) {
                CrazyAddons.LOGGER.debug("failed to parse display images JSON", e);
                parsedImagesCache = List.of();
            }
            lastParsedImagesJson = imagesJson;
        }

        return parsedImagesCache;
    }

    @Nullable
    public DisplayImageEntry getSelectedImage() {
        List<DisplayImageEntry> images = getImages();
        if (images.isEmpty()) {
            return null;
        }

        if (selectedImageId == null || selectedImageId.isBlank()) {
            return images.get(0);
        }

        for (DisplayImageEntry entry : images) {
            if (entry.id().equals(selectedImageId)) {
                return entry;
            }
        }

        return images.get(0);
    }

    public void selectImage(String id) {
        if (isClientSide()) {
            this.selectedImageId = id == null ? "" : id;
            sendClientAction(ACTION_SELECT, id);
            return;
        }

        if (host == null) {
            return;
        }

        host.selectDisplayImage(id);
        syncFromHost();
        broadcastChanges();
    }

    public void removeImage(String id) {
        if (isClientSide()) {
            var images = new ArrayList<>(getImages());
            images.removeIf(entry -> entry.id().equals(id));
            applyClientMirror(images);

            if (id != null && id.equals(this.selectedImageId)) {
                this.selectedImageId = images.isEmpty() ? "" : images.get(0).id();
            }

            sendClientAction(ACTION_REMOVE, id);
            return;
        }

        if (host == null) {
            return;
        }

        host.removeDisplayImage(id);
        syncFromHost();
        broadcastChanges();
    }

    public void updateImage(String id, int x, int y, int width, int height) {
        if (isClientSide()) {
            var images = new ArrayList<>(getImages());
            for (int i = 0; i < images.size(); i++) {
                DisplayImageEntry current = images.get(i);
                if (current.id().equals(id)) {
                    images.set(i, current.withBounds(x, y, Math.max(1, width), Math.max(1, height)));
                    break;
                }
            }

            this.selectedImageId = id == null ? "" : id;
            applyClientMirror(images);
            sendClientAction(ACTION_UPDATE, id + "|" + x + "|" + y + "|" + width + "|" + height);
            return;
        }

        if (host == null) {
            return;
        }

        host.updateDisplayImage(id, x, y, width, height);
        syncFromHost();
        broadcastChanges();
    }

    public void reorderImage(String id, int delta) {
        if (isClientSide()) {
            var images = new ArrayList<>(getImages());
            int idx = -1;
            for (int i = 0; i < images.size(); i++) {
                if (images.get(i).id().equals(id)) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                int newIdx = Mth.clamp(idx + delta, 0, images.size() - 1);
                if (newIdx != idx) {
                    Collections.swap(images, idx, newIdx);
                }
            }
            applyClientMirror(images);
            sendClientAction(ACTION_REORDER, id + "|" + delta);
            return;
        }

        if (host == null) {
            return;
        }

        host.reorderDisplayImage(id, delta);
        syncFromHost();
        broadcastChanges();
    }

    public DisplayImageUploadStatus addImage(
            String sourceName,
            byte[] pngBytes,
            int frameCount,
            int columns,
            int frameDurationMs) {
        if (isClientSide() || host == null) {
            return DisplayImageUploadStatus.FAILED;
        }

        DisplayImageUploadStatus status = host.addDisplayImage(
                sourceName,
                pngBytes,
                frameCount,
                columns,
                frameDurationMs);
        syncFromHost();
        broadcastChanges();
        return status;
    }

    private void updateImageFromPayload(String payload) {
        if (isClientSide()) {
            return;
        }

        if (host == null) {
            return;
        }

        String[] parts = payload.split("\\|", -1);
        if (parts.length != 5) {
            return;
        }

        try {
            String id = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int w = Integer.parseInt(parts[3]);
            int h = Integer.parseInt(parts[4]);

            host.updateDisplayImage(id, x, y, w, h);
            syncFromHost();
            broadcastChanges();
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to update display image and send preview", e);
        }
    }

    private void reorderImageFromPayload(String payload) {
        if (isClientSide()) {
            return;
        }

        if (host == null) {
            return;
        }

        String[] parts = payload.split("\\|", 2);
        if (parts.length != 2) {
            return;
        }

        try {
            String id = parts[0];
            int delta = Integer.parseInt(parts[1]);
            host.reorderDisplayImage(id, delta);
            syncFromHost();
            broadcastChanges();
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to reorder display image", e);
        }
    }

    private void syncFromHost() {
        if (host == null) {
            this.imagesJson = "[]";
            this.selectedImageId = "";
            this.lastParsedImagesJson = null;
            this.previewGridWidth = 1;
            this.previewGridHeight = 1;
            return;
        }

        try {
            List<DisplayImageEntry> images = host.getDisplayImages();
            this.imagesJson = GSON.toJson(images != null ? images : Collections.emptyList());
        } catch (Exception e) {
            CrazyAddons.LOGGER.debug("failed to serialize display images to JSON", e);
            this.imagesJson = "[]";
        }

        DisplayImageEntry selected = host.getSelectedDisplayImage();
        this.selectedImageId = selected == null ? "" : selected.id();

        this.lastParsedImagesJson = null;

        var dims = DisplayGrid.computePreviewGridSize(host);
        this.previewGridWidth = Math.max(1, dims.getFirst());
        this.previewGridHeight = Math.max(1, dims.getSecond());

        this.maxImageBytes = DisplayImageStore.maxImageBytes();
        this.maxImageDimension = DisplayImageStore.maxImageDimension();
        this.storageBudgetBytes = (int) DisplayImageStore.budgetBytes();
        this.storageUsedBytes = host.getLevel() instanceof ServerLevel level
                ? (int) DisplayImageStore.get(level).usedBytes()
                : 0;
    }

    private void applyClientMirror(List<DisplayImageEntry> images) {
        this.parsedImagesCache = images != null ? List.copyOf(images) : List.of();

        try {
            this.imagesJson = GSON.toJson(this.parsedImagesCache);
        } catch (Exception e) {
            CrazyAddons.LOGGER.debug("failed to serialize display images cache to JSON", e);
            this.imagesJson = "[]";
            this.parsedImagesCache = List.of();
        }

        this.lastParsedImagesJson = this.imagesJson;
    }
}
