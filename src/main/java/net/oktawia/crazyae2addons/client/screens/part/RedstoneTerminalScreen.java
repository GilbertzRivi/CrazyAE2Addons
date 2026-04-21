package net.oktawia.crazyae2addons.client.screens.part;

import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.part.RedstoneTerminalMenu;
import net.oktawia.crazyae2addons.network.packets.RedstoneWindowPacket;

import java.util.ArrayList;
import java.util.List;

public class RedstoneTerminalScreen<C extends RedstoneTerminalMenu> extends AEBaseScreen<C> {

    private static final int VISIBLE_ROWS = RedstoneTerminalMenu.VISIBLE_ROWS;

    private final List<IconButton> toggleButtons = new ArrayList<>(VISIBLE_ROWS);
    private final Scrollbar scrollbar = new Scrollbar();

    private AETextField searchField;

    private int lastSentOffset = -1;
    private int lastEmitterCount = -1;
    private int lastAppliedRevision = -1;
    private boolean needsRefresh = false;

    public RedstoneTerminalScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
    }

    private void setupGui() {
        this.searchField = new AETextField(this.style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.searchField.setBordered(false);
        this.searchField.setMaxLength(99);
        this.searchField.setPlaceholder(Component.translatable(LangDefs.SEARCH.getTranslationKey()));
        this.searchField.setResponder(newValue -> {
            getMenu().search(newValue);
            scrollbar.setCurrentScroll(0);
            lastSentOffset = -1;
            lastAppliedRevision = -1;
        });
        this.widgets.add("search", this.searchField);

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            final int rowIndex = i;

            IconButton toggle = new IconButton(Icon.REDSTONE_LOW, button -> {
                List<RedstoneTerminalMenu.EmitterInfo> window = getMenu().clientWindow;
                if (rowIndex < 0 || rowIndex >= window.size()) {
                    return;
                }

                RedstoneTerminalMenu.EmitterInfo emitter = window.get(rowIndex);
                getMenu().toggle(emitter.name());
            });

            toggle.visible = false;
            toggle.active = false;

            this.toggleButtons.add(toggle);
            this.widgets.add("toggle_" + i, toggle);
        }

        this.scrollbar.setRange(0, 0, 1);
        this.widgets.add("scrollbar", this.scrollbar);
    }

    public void applyWindow(RedstoneWindowPacket packet) {
        int currentOffset = this.scrollbar.getCurrentScroll();
        if (packet.windowOffset() != currentOffset) {
            return;
        }
        if (packet.revision() < lastAppliedRevision) {
            return;
        }

        this.lastAppliedRevision = packet.revision();
        getMenu().applyClientWindow(packet);
        this.needsRefresh = true;
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        int total = getMenu().totalCount;
        int maxStart = Math.max(0, total - VISIBLE_ROWS);

        this.scrollbar.setRange(0, maxStart, 1);

        int offset = Math.min(this.scrollbar.getCurrentScroll(), maxStart);
        this.scrollbar.setCurrentScroll(offset);

        if (total != lastEmitterCount) {
            this.lastEmitterCount = total;
            this.needsRefresh = true;
        }

        if (offset != lastSentOffset) {
            getMenu().onScroll(offset);
            this.lastSentOffset = offset;
        }

        if (needsRefresh) {
            refreshRows(getMenu().clientWindow);
            this.needsRefresh = false;
        }
    }

    private void refreshRows(List<RedstoneTerminalMenu.EmitterInfo> window) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            IconButton toggle = this.toggleButtons.get(row);

            if (row >= window.size()) {
                toggle.visible = false;
                toggle.active = false;
                setTextContent("label_" + row, Component.empty());
                continue;
            }

            RedstoneTerminalMenu.EmitterInfo emitter = window.get(row);

            toggle.visible = true;
            toggle.active = true;
            toggle.setIcon(emitter.active() ? Icon.REDSTONE_HIGH : Icon.REDSTONE_LOW);

            setTextContent("label_" + row, Component.literal(emitter.name()));
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        return this.scrollbar.onMouseWheel(
                new Point((int) Math.round(x - leftPos), (int) Math.round(y - topPos)),
                delta
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && this.searchField != null && this.searchField.isMouseOver(mouseX, mouseY)) {
            this.searchField.setValue("");
            this.searchField.setFocused(true);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}