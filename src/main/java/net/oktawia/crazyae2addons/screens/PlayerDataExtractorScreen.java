package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.PlayerDataExtractorMenu;

import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

public class PlayerDataExtractorScreen<C extends PlayerDataExtractorMenu> extends AEBaseScreen<C> implements CrazyScreen {

    private static final String NAME = "player_data_extractor";

    static {
        CrazyScreen.i18n(NAME, "variables_name", "Variables Name");
        CrazyScreen.i18n(NAME, "delay", "Delay");
        CrazyScreen.i18n(NAME, "read_interval", "Read interval");
        CrazyScreen.i18n(NAME, "closest_player", "Closest player");
        CrazyScreen.i18n(NAME, "bind_player", "Bind player");
        CrazyScreen.i18n(NAME, "bound_to", "Bound to: %s");
        CrazyScreen.i18n(NAME, "selected", "Selected: %s");
        CrazyScreen.i18n(NAME, "fetch", "Fetch");
        CrazyScreen.i18n(NAME, "save", "Save");
        CrazyScreen.i18n(NAME, "page_down", "<");
        CrazyScreen.i18n(NAME, "page_up", ">");
    }

    public boolean initialized = false;
    public boolean initialized2 = false;
    public AbstractWidget btn0;
    public AbstractWidget btn1;
    public AbstractWidget btn2;
    public AbstractWidget btn3;
    public AETextField input;
    public AETextField delay;
    public Button playerButton;

    public PlayerDataExtractorScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        if (!this.initialized) {
            setupGui();
            this.initialized = true;
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (!initialized2) {
            this.input.setValue(this.getMenu().valueName);
            this.delay.setValue(String.valueOf(this.getMenu().delay));
            var player = Minecraft.getInstance().level.getPlayerByUUID(UUID.fromString(getMenu().boundPlayer));
            if (player != null) {
                this.playerButton.setMessage(l10n(NAME, "bound_to", player.getName().getString()));
            }
            renderPage(getMenu().page * 4, (getMenu().page + 1) * 4);
            initialized2 = true;
        }
        Component selected;
        if (!getMenu().available.isEmpty()) {
            selected = l10n(NAME, "selected", Arrays.stream(getMenu().available.split("\\|")).toList().get(getMenu().selected));
        } else {
            selected = l10n(NAME, "selected", "");
        }
        setTextContent("selectedValue", selected);
        if (getMenu().updateGui) {
            updateGui();
            getMenu().updateGui = false;
        }
    }

    public void setupGui() {
        btn0 = Button.builder(Component.literal("0 "), (btn) -> {
            setSelected(Integer.valueOf(Arrays.stream(btn.getMessage().getString().split(" ")).toList().get(0)));
        }).build();
        btn1 = Button.builder(Component.literal("1 "), (btn) -> {
            setSelected(Integer.valueOf(Arrays.stream(btn.getMessage().getString().split(" ")).toList().get(0)));
        }).build();
        btn2 = Button.builder(Component.literal("2 "), (btn) -> {
            setSelected(Integer.valueOf(Arrays.stream(btn.getMessage().getString().split(" ")).toList().get(0)));
        }).build();
        btn3 = Button.builder(Component.literal("3 "), (btn) -> {
            setSelected(Integer.valueOf(Arrays.stream(btn.getMessage().getString().split(" ")).toList().get(0)));
        }).build();
        this.widgets.add("button0", btn0);
        this.widgets.add("button1", btn1);
        this.widgets.add("button2", btn2);
        this.widgets.add("button3", btn3);
        this.widgets.addButton("data", l10n(NAME, "fetch"), (btn) -> {
            getMenu().getData();
            updateGui();
        });
        this.widgets.addButton("down", l10n(NAME, "page_down"), (btn) -> {
            int newPage = getMenu().page - 1;
            if (newPage >= 0 && pageHasData(newPage)) {
                getMenu().page = newPage;
                updateGui();
            }
        });
        this.widgets.addButton("up", l10n(NAME, "page_up"), (btn) -> {
            int newPage = getMenu().page + 1;
            if (pageHasData(newPage)) {
                getMenu().page = newPage;
                updateGui();
            }
        });
        this.widgets.addButton("save", l10n(NAME, "save"), (btn) -> {
            updateVariableName();
        });
        this.input = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.input.setPlaceholder(l10n(NAME, "variables_name"));
        this.input.setValue(getMenu().valueName);
        this.input.setMaxLength(999);
        this.input.setBordered(false);
        this.delay = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.delay.setPlaceholder(l10n(NAME, "delay"));
        this.delay.setTooltip(Tooltip.create(l10n(NAME, "read_interval")));
        this.delay.setValue(String.valueOf(getMenu().delay));
        this.delay.setMaxLength(10);
        this.delay.setBordered(false);
        this.widgets.add("input", this.input);
        this.widgets.add("delay", this.delay);
        this.widgets.addButton("toggleMode", getMenu().playerMode ? l10n(NAME, "closest_player") : l10n(NAME, "bind_player"), (btn) -> {
            getMenu().togglePlayerMode();
            btn.setMessage(getMenu().playerMode ? l10n(NAME, "closest_player") : l10n(NAME, "bind_player"));
        });
        String playerName = "Unknown";
        this.playerButton = new Button.Builder(l10n(NAME, "bound_to", playerName), (btn) -> {
            if (Minecraft.getInstance().player != null) {
                getMenu().bindPlayer(Minecraft.getInstance().player.getStringUUID());
                btn.setMessage(l10n(NAME, "bound_to", Minecraft.getInstance().player.getName().getString()));
            }
        }).build();
        this.widgets.add("bindPlayer", this.playerButton);
    }

    public void renderPage(int start, int end) {
        try {
            setTextContent("selectedValue", l10n(NAME, "selected", Arrays.stream(getMenu().available.split(Pattern.quote("|"))).toList().get(getMenu().selected)));
        } catch (Exception ignored) {}

        String[] parts = getMenu().available.split("\\|");
        for (int i = start; i < end; i++) {
            int localIndex = i - start;
            if (i >= 0 && i < parts.length) {
                String label = i + " " + parts[i];
                getButton(localIndex).setMessage(Component.literal(label));
                getButton(localIndex).visible = true;
            } else {
                getButton(localIndex).visible = false;
            }
        }
    }

    private AbstractWidget getButton(int index) {
        return switch (index) {
            case 0 -> btn0;
            case 1 -> btn1;
            case 2 -> btn2;
            case 3 -> btn3;
            default -> btn0;
        };
    }

    private boolean pageHasData(int page) {
        String[] parts = getMenu().available.split("\\|");
        int start = page * 4;
        for (int i = start; i < start + 4; i++) {
            if (i < parts.length) return true;
        }
        return false;
    }

    public void setSelected(Integer what) {
        if (what == -1) {
            return;
        }
        getMenu().selected = what;
        getMenu().syncValue(what);
        updateGui();
    }

    public void updateGui() {
        setTextContent("selectedValue", l10n(NAME, "selected", Arrays.stream(getMenu().available.split("\\|")).toList().get(getMenu().selected)));
        renderPage(getMenu().page * 4, (getMenu().page + 1) * 4);
    }

    public static boolean isAscii(String input) {
        return input.chars().allMatch(c -> c <= 127);
    }

    public void updateVariableName() {
        String name = this.input.getValue();
        String delay = this.delay.getValue();
        if (isAscii(name) && !name.isEmpty() && delay.chars().allMatch(Character::isDigit)) {
            name = name.toUpperCase();
            this.input.setTextColor(0x00FF00);
            Runnable setColorFunction = () -> this.input.setTextColor(0xFFFFFF);
            Utils.asyncDelay(setColorFunction, 1);
            getMenu().saveName(name);
            getMenu().saveDelay(delay.isEmpty() ? 0 : Integer.parseInt(delay));
        }
    }
}
