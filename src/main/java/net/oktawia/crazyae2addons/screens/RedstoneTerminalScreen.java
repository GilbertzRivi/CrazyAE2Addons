package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.RedstoneTerminalMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.oktawia.crazyae2addons.menus.RedstoneTerminalMenu.GSON;

public class RedstoneTerminalScreen<C extends RedstoneTerminalMenu> extends UpgradeableScreen<C> {

    private static final int VISIBLE_ROWS = 7;

    private final List<Button> toggleButtons = new ArrayList<>(VISIBLE_ROWS);
    private final List<IconButton> stateIcons = new ArrayList<>(VISIBLE_ROWS);

    private boolean initialized = false;
    private String search = "";

    private Scrollbar scrollbar;
    private int lastOffset = -1;
    private int lastEmitterCount = -1;

    public RedstoneTerminalScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!initialized) {
            ensureScrollbarRange();
            refreshEmitters(true);
            initialized = true;
            return;
        }

        int currentOffset = getOffset();
        int count = getEmitters().size();
        if (currentOffset != lastOffset || count != lastEmitterCount) {
            ensureScrollbarRange();
            refreshEmitters(true);
        } else {
            refreshEmitters(false);
        }
    }

    private void setupGui() {
        var search = new AETextField(this.style, Minecraft.getInstance().font, 0, 0, 0, 0);
        search.setBordered(false);
        search.setMaxLength(99);
        search.setPlaceholder(Component.translatable("gui.crazyae2addons.redstone_terminal_search"));
        search.setResponder(newVal -> {
            this.search = newVal;
            this.getMenu().search(newVal);
            if (this.scrollbar != null) {
                this.scrollbar.setRange(0, Math.max(0, getEmitters().size() - VISIBLE_ROWS), 1);
                this.scrollbar.setCurrentScroll(0);
            }
            refreshEmitters(true);
        });
        this.widgets.add("search", search);

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            final int rowIndex = i;

            IconButton state = new IconButton(Icon.REDSTONE_LOW, x -> {});
            state.active = false;

            Button toggle = Button.builder(Component.empty(), btn -> {
                int index = getOffset() + rowIndex;
                List<RedstoneTerminalMenu.EmitterInfo> emitters = getEmitters();
                if (index < emitters.size()) {
                    RedstoneTerminalMenu.EmitterInfo emitter = emitters.get(index);
                    getMenu().toggle(emitter.name());
                    getMenu().search(this.search);
                    refreshEmitters(true);
                }
            }).build();

            this.widgets.add("toggle_" + rowIndex, toggle);
            this.widgets.add("pulse_" + rowIndex, state);

            toggleButtons.add(toggle);
            stateIcons.add(state);
        }

        this.scrollbar = new Scrollbar();
        this.widgets.add("scrollbar", this.scrollbar);
    }

    private int getOffset() {
        return scrollbar != null ? scrollbar.getCurrentScroll() : 0;
    }

    private void ensureScrollbarRange() {
        if (this.scrollbar == null) return;
        int total = getEmitters().size();
        int maxStart = Math.max(0, total - VISIBLE_ROWS);
        int cur = Math.min(this.scrollbar.getCurrentScroll(), maxStart);
        this.scrollbar.setRange(0, maxStart, 1);
        this.scrollbar.setCurrentScroll(cur);
    }

    private void refreshEmitters(boolean hard) {
        List<RedstoneTerminalMenu.EmitterInfo> emitters = getEmitters();
        int offset = getOffset();

        lastOffset = offset;
        lastEmitterCount = emitters.size();

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = offset + i;
            Button toggle = toggleButtons.get(i);
            IconButton state = stateIcons.get(i);

            if (index < emitters.size()) {
                RedstoneTerminalMenu.EmitterInfo emitter = emitters.get(index);

                if (hard) {
                    this.setTextContent("label_" + i, Component.literal(emitter.name()));
                    toggle.setMessage(Component.empty());
                    toggle.visible = true;
                    state.visible = true;
                }

                state.icon = emitter.active() ? Icon.REDSTONE_HIGH : Icon.REDSTONE_LOW;

            } else {
                if (hard) {
                    toggle.setMessage(Component.empty());
                    toggle.visible = false;
                    state.visible = false;
                    this.setTextContent("label_" + i, Component.empty());
                }
            }
        }
    }

    private @NotNull List<RedstoneTerminalMenu.EmitterInfo> getEmittersRaw() {
        return GSON.fromJson(
                getMenu().emitters,
                new TypeToken<List<RedstoneTerminalMenu.EmitterInfo>>() {}.getType()
        );
    }

    private @NotNull List<RedstoneTerminalMenu.EmitterInfo> getEmitters() {
        var raw = getEmittersRaw();
        java.util.LinkedHashMap<String, RedstoneTerminalMenu.EmitterInfo> map = new java.util.LinkedHashMap<>();
        for (var e : raw) {
            map.putIfAbsent(e.name(), e);
        }
        return new java.util.ArrayList<>(map.values());
    }
}