package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.display.DisplayGrid;
import net.oktawia.crazyae2addons.logic.display.DisplayImageEntry;
import net.oktawia.crazyae2addons.network.packets.SyncDisplayImagePreviewPacket;
import net.oktawia.crazyae2addons.parts.DisplayPart;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DisplayMenu extends AEBaseMenu {

    private static final Gson GSON = new Gson();
    private static final Type IMAGE_LIST_TYPE = new TypeToken<List<DisplayImageEntry>>() {}.getType();

    public static final String ACTION_SYNC = "syncDisplayValue";
    public static final String ACTION_MODE = "changeMode";
    public static final String ACTION_MARGIN = "changeMargin";
    public static final String ACTION_CENTER = "changeCenter";
    public static final String ACTION_OPEN_INSERT = "openInsert";
    public static final String ACTION_OPEN_IMAGES = "openImages";
    public static final String ACTION_REQUEST_IMAGES = "requestImages";

    @GuiSync(145)
    public String displayValue = "";

    @GuiSync(29)
    public boolean mode;

    @GuiSync(31)
    public boolean margin;

    @GuiSync(32)
    public boolean centerText;

    @GuiSync(33)
    public String pendingInsert = "";

    @GuiSync(34)
    public int pendingInsertCursor = -1;

    @GuiSync(35)
    public int previewGridWidth = 1;

    @GuiSync(36)
    public int previewGridHeight = 1;

    @GuiSync(37)
    public String previewTokensJson = "{}";

    @GuiSync(38)
    public String previewImagesJson = "[]";

    public final DisplayPart host;

    private transient String lastParsedPreviewImagesJson = null;
    private transient List<DisplayImageEntry> parsedPreviewImagesCache = List.of();

    public DisplayMenu(int id, Inventory inv, DisplayPart host) {
        super(CrazyMenuRegistrar.DISPLAY_MENU.get(), id, inv, host);
        this.host = host;

        this.displayValue = host.textValue;
        this.mode = host.mode;
        this.margin = host.margin;
        this.centerText = host.center;

        String pending = host.pendingInsert;
        if (pending != null) {
            this.pendingInsert = pending;
            this.pendingInsertCursor = host.pendingInsertCursor;
            host.pendingInsert = null;
            host.pendingInsertCursor = -1;
        }

        syncPreviewData();

        registerClientAction(ACTION_SYNC, String.class, this::syncValue);
        registerClientAction(ACTION_MODE, Boolean.class, this::changeMode);
        registerClientAction(ACTION_MARGIN, Boolean.class, this::changeMargin);
        registerClientAction(ACTION_CENTER, Boolean.class, this::changeCenter);
        registerClientAction(ACTION_OPEN_INSERT, Integer.class, this::openInsert);
        registerClientAction(ACTION_OPEN_IMAGES, this::openImages);
        registerClientAction(ACTION_REQUEST_IMAGES, this::requestImages);

        createPlayerInventorySlots(inv);
    }

    private void syncPreviewData() {
        var dims = DisplayGrid.computePreviewGridSize(host);
        this.previewGridWidth = Math.max(1, dims.getFirst());
        this.previewGridHeight = Math.max(1, dims.getSecond());

        try {
            this.previewTokensJson = GSON.toJson(host.resolvedTokens != null ? host.resolvedTokens : Map.of());
        } catch (Exception ignored) {
            this.previewTokensJson = "{}";
        }

        try {
            this.previewImagesJson = GSON.toJson(
                    host.getDisplayImages() != null ? host.getDisplayImages() : Collections.emptyList()
            );
        } catch (Exception ignored) {
            this.previewImagesJson = "[]";
        }

        this.lastParsedPreviewImagesJson = null;
    }

    public List<DisplayImageEntry> getPreviewImages() {
        if (lastParsedPreviewImagesJson == null || !lastParsedPreviewImagesJson.equals(previewImagesJson)) {
            try {
                List<DisplayImageEntry> parsed = GSON.fromJson(previewImagesJson, IMAGE_LIST_TYPE);
                parsedPreviewImagesCache = parsed != null ? parsed : List.of();
            } catch (Exception ignored) {
                parsedPreviewImagesCache = List.of();
            }

            lastParsedPreviewImagesJson = previewImagesJson;
        }

        return parsedPreviewImagesCache;
    }

    public void requestImages() {
        if (isClientSide()) {
            sendClientAction(ACTION_REQUEST_IMAGES);
            return;
        }

        if (!(getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        Map<String, byte[]> imageData = host.getDisplayImageData();
        if (imageData == null || imageData.isEmpty()) {
            return;
        }

        for (Map.Entry<String, byte[]> entry : imageData.entrySet()) {
            PacketDistributor.sendToPlayer(
                    player,
                    new SyncDisplayImagePreviewPacket(
                            entry.getKey(),
                            entry.getValue() == null ? new byte[0] : entry.getValue()
                    )
            );
        }
    }

    public void syncValue(String value) {
        this.displayValue = value;
        host.textValue = value;
        host.getHost().markForSave();
        host.getHost().markForUpdate();
        syncPreviewData();

        if (isClientSide()) {
            sendClientAction(ACTION_SYNC, value);
        }
    }

    public void changeMode(boolean v) {
        this.mode = v;
        host.mode = v;
        host.getHost().markForUpdate();
        syncPreviewData();

        if (isClientSide()) {
            DisplayGrid.invalidateClientCache();
            sendClientAction(ACTION_MODE, v);
        }
    }

    public void changeMargin(boolean v) {
        this.margin = v;
        host.margin = v;
        host.getHost().markForUpdate();
        syncPreviewData();

        if (isClientSide()) {
            sendClientAction(ACTION_MARGIN, v);
        }
    }

    public void changeCenter(boolean v) {
        this.centerText = v;
        host.center = v;
        host.getHost().markForUpdate();
        syncPreviewData();

        if (isClientSide()) {
            sendClientAction(ACTION_CENTER, v);
        }
    }

    public void openInsert(int cursorPos) {
        host.pendingInsertCursor = cursorPos;

        if (!isClientSide()) {
            appeng.menu.MenuOpener.open(
                    CrazyMenuRegistrar.DISPLAY_TOKEN_SUBMENU.get(),
                    getPlayer(),
                    appeng.menu.locator.MenuLocators.forPart(host)
            );
        }

        if (isClientSide()) {
            sendClientAction(ACTION_OPEN_INSERT, cursorPos);
        }
    }

    public void openImages() {
        if (!isClientSide()) {
            appeng.menu.MenuOpener.open(
                    CrazyMenuRegistrar.DISPLAY_IMAGES_SUBMENU.get(),
                    getPlayer(),
                    appeng.menu.locator.MenuLocators.forPart(host)
            );
        }

        if (isClientSide()) {
            sendClientAction(ACTION_OPEN_IMAGES);
        }
    }
}