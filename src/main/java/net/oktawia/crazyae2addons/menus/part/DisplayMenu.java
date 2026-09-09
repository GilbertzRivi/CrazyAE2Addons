package net.oktawia.crazyae2addons.menus.part;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.guisync.GuiSync;
import appeng.menu.locator.MenuLocators;

import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.display.DisplayGrid;
import net.oktawia.crazyae2addons.logic.display.DisplayImageEntry;
import net.oktawia.crazyae2addons.parts.Display;

public class DisplayMenu extends AEBaseMenu {

    private static final Gson GSON = new Gson();
    private static final Type IMAGE_LIST_TYPE = new TypeToken<List<DisplayImageEntry>>() {
    }.getType();

    public static final String ACTION_SYNC = "syncDisplayValue";
    public static final String ACTION_MODE = "changeMode";
    public static final String ACTION_MARGIN = "changeMargin";
    public static final String ACTION_CENTER = "changeCenter";
    public static final String ACTION_FONT_SIZE = "changeFontSize";
    public static final String ACTION_OPEN_INSERT = "openInsert";
    public static final String ACTION_OPEN_IMAGES = "openImages";
    public static final String ACTION_CONNECT_DIR = "setConnectDir";

    @GuiSync(145)
    public String displayValue = "";

    @GuiSync(29)
    public boolean mode;

    @GuiSync(31)
    public boolean margin;

    @GuiSync(32)
    public boolean centerText;

    @GuiSync(24)
    public int fontSize;

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

    @GuiSync(39)
    public boolean connectUp = true;

    @GuiSync(40)
    public boolean connectDown = true;

    @GuiSync(41)
    public boolean connectLeft = true;

    @GuiSync(42)
    public boolean connectRight = true;

    @Getter
    private final Display host;

    private transient String lastParsedPreviewImagesJson = null;
    private transient List<DisplayImageEntry> parsedPreviewImagesCache = List.of();

    public DisplayMenu(int id, Inventory inv, Display host) {
        super(CrazyMenuRegistrar.DISPLAY_MENU.get(), id, inv, host);
        this.host = host;

        this.displayValue = host.getTextValue();
        this.mode = host.isMergeMode();
        this.margin = host.isAddMargin();
        this.centerText = host.getCenterText();
        this.fontSize = host.getFontSize();
        this.connectUp = host.canConnectLocal(Display.LocalDir.UP);
        this.connectDown = host.canConnectLocal(Display.LocalDir.DOWN);
        this.connectLeft = host.canConnectLocal(Display.LocalDir.LEFT);
        this.connectRight = host.canConnectLocal(Display.LocalDir.RIGHT);

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
        registerClientAction(ACTION_FONT_SIZE, Integer.class, this::changeFontSize);
        registerClientAction(ACTION_OPEN_INSERT, Integer.class, this::openInsert);
        registerClientAction(ACTION_OPEN_IMAGES, this::openImages);
        registerClientAction(ACTION_CONNECT_DIR, String.class, this::setConnectDirFromPayload);

        createPlayerInventorySlots(inv);
    }

    private void syncPreviewData() {
        var dims = DisplayGrid.computePreviewGridSize(host);
        this.previewGridWidth = Math.max(1, dims.getFirst());
        this.previewGridHeight = Math.max(1, dims.getSecond());

        try {
            this.previewTokensJson = GSON.toJson(host.resolvedTokens != null ? host.resolvedTokens : Map.of());
        } catch (Exception e) {
            CrazyAddons.LOGGER.debug("failed to serialize display tokens to JSON", e);
            this.previewTokensJson = "{}";
        }

        try {
            this.previewImagesJson = CrazyConfig.COMMON.DISPLAY_IMAGES_ENABLED.get()
                    ? GSON.toJson(host.getDisplayImages() != null ? host.getDisplayImages() : Collections.emptyList())
                    : "[]";
        } catch (Exception e) {
            CrazyAddons.LOGGER.debug("failed to serialize display preview images to JSON", e);
            this.previewImagesJson = "[]";
        }

        this.lastParsedPreviewImagesJson = null;
    }

    public List<DisplayImageEntry> getPreviewImages() {
        if (lastParsedPreviewImagesJson == null || !lastParsedPreviewImagesJson.equals(previewImagesJson)) {
            try {
                List<DisplayImageEntry> parsed = GSON.fromJson(previewImagesJson, IMAGE_LIST_TYPE);
                parsedPreviewImagesCache = parsed != null ? parsed : List.of();
            } catch (Exception e) {
                CrazyAddons.LOGGER.debug("failed to parse display preview images JSON", e);
                parsedPreviewImagesCache = List.of();
            }

            lastParsedPreviewImagesJson = previewImagesJson;
        }

        return parsedPreviewImagesCache;
    }

    public void syncValue(String value) {
        this.displayValue = value;
        host.setTextValue(value);
        host.getHost().markForSave();
        host.getHost().markForUpdate();
        syncPreviewData();

        if (isClientSide()) {
            sendClientAction(ACTION_SYNC, value);
        }
    }

    public void changeMode(boolean v) {
        this.mode = v;
        host.setMergeMode(v);
        host.getHost().markForUpdate();
        syncPreviewData();

        if (isClientSide()) {
            DisplayGrid.invalidateClientCache();
            sendClientAction(ACTION_MODE, v);
        }
    }

    public void changeMargin(boolean v) {
        this.margin = v;
        host.setAddMargin(v);
        host.getHost().markForUpdate();
        syncPreviewData();

        if (isClientSide()) {
            sendClientAction(ACTION_MARGIN, v);
        }
    }

    public void changeCenter(boolean v) {
        this.centerText = v;
        host.setCenterText(v);
        host.getHost().markForUpdate();
        syncPreviewData();

        if (isClientSide()) {
            sendClientAction(ACTION_CENTER, v);
        }
    }

    public void changeFontSize(int size) {
        this.fontSize = size;
        host.setFontSize(size);
        syncPreviewData();

        if (isClientSide()) {
            sendClientAction(ACTION_FONT_SIZE, size);
        }
    }

    public void openInsert(int cursorPos) {
        if (!CrazyConfig.COMMON.DISPLAY_ICONS_ENABLED.get()
                && !CrazyConfig.COMMON.DISPLAY_STOCK_ENABLED.get()
                && !CrazyConfig.COMMON.DISPLAY_DELTA_ENABLED.get()) {
            return;
        }

        host.pendingInsertCursor = cursorPos;

        if (!isClientSide()) {
            MenuOpener.open(
                    CrazyMenuRegistrar.DISPLAY_TOKEN_SUBMENU.get(),
                    getPlayer(),
                    MenuLocators.forPart(host));
        }

        if (isClientSide()) {
            sendClientAction(ACTION_OPEN_INSERT, cursorPos);
        }
    }

    public void setConnectDir(Display.LocalDir dir, boolean val) {
        applyConnectFlag(dir, val);
        host.setConnectLocal(dir, val);
        host.getHost().markForUpdate();

        if (isClientSide()) {
            DisplayGrid.invalidateClientCache();
            sendClientAction(ACTION_CONNECT_DIR, dir.name() + "|" + val);
        }
    }

    private void applyConnectFlag(Display.LocalDir dir, boolean val) {
        switch (dir) {
            case UP -> connectUp = val;
            case DOWN -> connectDown = val;
            case LEFT -> connectLeft = val;
            case RIGHT -> connectRight = val;
        }
    }

    private void setConnectDirFromPayload(String payload) {
        if (isClientSide())
            return;
        String[] parts = payload.split("\\|", 2);
        if (parts.length != 2)
            return;
        try {
            Display.LocalDir dir = Display.LocalDir.valueOf(parts[0]);
            boolean val = Boolean.parseBoolean(parts[1]);
            setConnectDir(dir, val);
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to set connect dir", e);
        }
    }

    public void openImages() {
        if (!CrazyConfig.COMMON.DISPLAY_IMAGES_ENABLED.get()) {
            return;
        }

        if (!isClientSide()) {
            MenuOpener.open(
                    CrazyMenuRegistrar.DISPLAY_IMAGES_SUBMENU.get(),
                    getPlayer(),
                    MenuLocators.forPart(host));
        }

        if (isClientSide()) {
            sendClientAction(ACTION_OPEN_IMAGES);
        }
    }
}
