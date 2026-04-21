package net.oktawia.crazyae2addons.menus.part;

import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.guisync.GuiSync;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.display.DisplayGrid;
import net.oktawia.crazyae2addons.logic.display.DisplayImageEntry;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.SyncDisplayImagePreviewPacket;
import net.oktawia.crazyae2addons.parts.Display;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DisplayImagesSubMenu extends AEBaseMenu implements ISubMenu {

    private static final Gson GSON = new Gson();
    private static final Type IMAGE_LIST_TYPE = new TypeToken<List<DisplayImageEntry>>() {}.getType();

    public static final String ACTION_SELECT = "selectImage";
    public static final String ACTION_REMOVE = "removeImage";
    public static final String ACTION_UPDATE = "updateImage";
    public static final String ACTION_REQUEST_PREVIEW = "requestPreview";

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
        registerClientAction(ACTION_REQUEST_PREVIEW, this::requestPreview);
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
        sendSelectedPreviewToClient();
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
        sendSelectedPreviewToClient();
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
        sendSelectedPreviewToClient();
    }

    public void addImage(String sourceName, byte[] pngBytes, int width, int height) {
        if (isClientSide()) {
            return;
        }

        if (host == null) {
            return;
        }

        host.addDisplayImage(sourceName, pngBytes, width, height);
        syncFromHost();
        broadcastChanges();
        sendSelectedPreviewToClient();
    }

    public void requestPreview() {
        if (isClientSide()) {
            sendClientAction(ACTION_REQUEST_PREVIEW);
            return;
        }

        sendSelectedPreviewToClient();
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
            sendSelectedPreviewToClient();
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to update display image and send preview", e);
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

    private void sendSelectedPreviewToClient() {
        if (host == null) {
            return;
        }

        if (!(getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        DisplayImageEntry selected = host.getSelectedDisplayImage();
        String imageId = selected == null ? "" : selected.id();
        byte[] bytes = selected == null ? new byte[0] : host.getDisplayImageBytes(imageId);
        if (bytes == null) {
            bytes = new byte[0];
        }

        NetworkHandler.sendToPlayer(player, new SyncDisplayImagePreviewPacket(imageId, bytes));
    }
}