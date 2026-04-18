package net.oktawia.crazyae2addons.client.screens.part;

import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.IMovableSlot;
import net.oktawia.crazyae2addons.menus.part.EmitterTerminalMenu;
import net.oktawia.crazyae2addons.items.wireless.WirelessEmitterTerminalMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import static net.oktawia.crazyae2addons.menus.part.EmitterTerminalMenu.GSON;

public class EmitterTerminalScreen<C extends EmitterTerminalMenu> extends AEBaseScreen<C> {

    private static final int VISIBLE_ROWS = 6;

    private static final int SLOT_X = 10;
    private static final int SLOT_Y0 = 34;
    private static final int ROW_H = 18;
    private static final int VALUE_W = 40;

    private final Scrollbar scrollbar = new Scrollbar();
    private final AETextField[] valueFields = new AETextField[VISIBLE_ROWS];
    private final String[] boundUuid = new String[VISIBLE_ROWS];
    private final boolean[] suppressChange = new boolean[VISIBLE_ROWS];

    private int lastOffset = -1;
    private int lastEmitterCount = -1;
    private String search = "";

    public EmitterTerminalScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
        if (getMenu() instanceof WirelessEmitterTerminalMenu) {
            this.widgets.add("upgrades", new UpgradesPanel(
                    menu.getSlots(SlotSemantics.UPGRADE),
                    this::getCompatibleUpgrades));
        }
    }

    private List<Component> getCompatibleUpgrades() {
        var list = new ArrayList<Component>();
        list.add(GuiText.CompatibleUpgrades.text());
        list.addAll(Upgrades.getTooltipLinesForMachine(CrazyItemRegistrar.WIRELESS_EMITTER_TERMINAL.get()));
        return list;
    }

    private void setupGui() {
        int normalTextColor = style.getColor(PaletteColor.TEXTFIELD_TEXT).toARGB();

        var searchField = new AETextField(this.style, Minecraft.getInstance().font, 0, 0, 0, 0);
        searchField.setBordered(false);
        searchField.setMaxLength(99);
        searchField.setPlaceholder(Component.translatable("gui.crazyae2addons.redstone_terminal_search"));
        searchField.setResponder(newVal -> {
            this.search = newVal;
            this.getMenu().search(newVal);

            this.scrollbar.setCurrentScroll(0);
            repositionConfigSlots(0, getEmitters().size());
            refreshRows(0, true);

            lastOffset = 0;
            lastEmitterCount = -1;
        });
        this.widgets.add("search", searchField);

        this.scrollbar.setRange(0, 0, 1);
        this.widgets.add("scrollbar", this.scrollbar);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int r = row;

            var tf = new AETextField(
                    this.style,
                    Minecraft.getInstance().font,
                    0, 0,
                    VALUE_W,
                    Minecraft.getInstance().font.lineHeight
            );
            tf.setBordered(false);
            tf.setMaxLength(19);
            tf.setTextColor(normalTextColor);
            tf.setResponder(text -> onValueChanged(r));

            this.valueFields[row] = tf;
            this.widgets.add("value_" + row, tf);
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        List<EmitterTerminalMenu.StorageEmitterInfo> emitters = getEmitters();
        int total = emitters.size();
        int maxStart = Math.max(0, total - VISIBLE_ROWS);

        this.scrollbar.setRange(0, maxStart, 1);

        int offset = Math.min(this.scrollbar.getCurrentScroll(), maxStart);
        this.scrollbar.setCurrentScroll(offset);

        boolean offsetChanged = offset != lastOffset;
        boolean countChanged = total != lastEmitterCount;

        if (offsetChanged || countChanged) {
            repositionConfigSlots(offset, total);
        }

        refreshRows(offset, offsetChanged || countChanged);

        lastOffset = offset;
        lastEmitterCount = total;
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);

        if (getMenu().outOfRange) {
            guiGraphics.fill(8, 32, 168, 145, 0x3F000000);

            Component text = Component.translatable(LangDefs.OUT_OF_RANGE.getTranslationKey());
            int textWidth = Minecraft.getInstance().font.width(text);
            int x = (168 - textWidth) / 2;
            int y = 84;
            guiGraphics.drawString(Minecraft.getInstance().font, text, x, y, 0xFFFF0000, true);
        }
    }

    private void repositionConfigSlots(int offset, int emitterCount) {
        List<Slot> slots = getMenu().getSlots(SlotSemantics.CONFIG);

        for (int i = 0; i < slots.size(); i++) {
            int row = i - offset;
            boolean inView = row >= 0 && row < VISIBLE_ROWS;
            boolean backedByEmitter = i < emitterCount;

            Slot s = slots.get(i);
            if (!(s instanceof AppEngSlot slot)) {
                continue;
            }

            boolean visible = inView && (backedByEmitter || row < VISIBLE_ROWS);

            if (slot instanceof IMovableSlot movable) {
                if (visible && row < VISIBLE_ROWS) {
                    movable.setX(SLOT_X);
                    movable.setY(SLOT_Y0 + row * ROW_H);
                    slot.setSlotEnabled(true);
                    slot.setActive(true);
                } else {
                    movable.setX(-10000);
                    movable.setY(-10000);
                    slot.setSlotEnabled(false);
                    slot.setActive(false);
                }
            } else {
                slot.setSlotEnabled(visible && row < VISIBLE_ROWS);
                slot.setActive(visible && row < VISIBLE_ROWS);
            }
        }
    }

    private void refreshRows(int offset, boolean hard) {
        List<EmitterTerminalMenu.StorageEmitterInfo> emitters = getEmitters();

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int absoluteIndex = offset + row;

            if (absoluteIndex >= emitters.size()) {
                boundUuid[row] = null;

                if (hard && !valueFields[row].isFocused()) {
                    setTextContent("label_" + row, Component.empty());

                    suppressChange[row] = true;
                    try {
                        valueFields[row].setValue("");
                    } finally {
                        suppressChange[row] = false;
                    }
                }

                valueFields[row].setVisible(true);
                valueFields[row].active = false;
                continue;
            }

            var emitter = emitters.get(absoluteIndex);
            String oldUuid = boundUuid[row];
            boundUuid[row] = emitter.uuid();

            String serverValue = emitter.value() == null ? "" : Long.toString(emitter.value());
            boolean uuidChanged = !Objects.equals(oldUuid, emitter.uuid());
            boolean fieldOutOfSync = !Objects.equals(valueFields[row].getValue(), serverValue);

            if (hard || uuidChanged || (fieldOutOfSync && !valueFields[row].isFocused())) {
                setTextContent("label_" + row, getDisplayName(emitter));

                if (!valueFields[row].isFocused()) {
                    suppressChange[row] = true;
                    try {
                        valueFields[row].setValue(serverValue);
                    } finally {
                        suppressChange[row] = false;
                    }
                }
            }

            valueFields[row].setVisible(true);
            valueFields[row].active = true;
        }
    }

    private void onValueChanged(int row) {
        if (suppressChange[row]) {
            return;
        }

        String uuid = boundUuid[row];
        if (uuid == null || uuid.isBlank()) {
            return;
        }

        String text = valueFields[row].getValue();

        if (text.isBlank()) {
            getMenu().setValue(uuid + "|");
            return;
        }

        try {
            long value = Long.parseLong(text.trim());
            if (value < 0) {
                return;
            }

            getMenu().setValue(uuid + "|" + value);
        } catch (NumberFormatException e) {
            CrazyAddons.LOGGER.debug("invalid numeric value in emitter terminal screen", e);
        }
    }

    private Component getDisplayName(EmitterTerminalMenu.StorageEmitterInfo emitter) {
        if (emitter == null || emitter.name() == null) {
            return Component.empty();
        }

        String text = emitter.name().getString();

        text = text.trim();
        if (text.isBlank()) {
            return Component.empty();
        }

        if (text.length() > 13) {
            text = text.substring(0, 13) + "...";
        }

        return Component.literal(text);
    }

    private @NotNull List<EmitterTerminalMenu.StorageEmitterInfo> getEmittersRaw() {
        if (getMenu().emitters == null || getMenu().emitters.isBlank()) {
            return List.of();
        }

        List<EmitterTerminalMenu.StorageEmitterInfo> list = GSON.fromJson(
                getMenu().emitters,
                new TypeToken<List<EmitterTerminalMenu.StorageEmitterInfo>>() {}.getType()
        );

        return list != null ? list : List.of();
    }

    private @NotNull List<EmitterTerminalMenu.StorageEmitterInfo> getEmitters() {
        var raw = getEmittersRaw();

        LinkedHashMap<String, EmitterTerminalMenu.StorageEmitterInfo> map = new LinkedHashMap<>();
        for (var e : raw) {
            if (e == null || e.uuid() == null || e.uuid().isBlank()) {
                continue;
            }
            map.putIfAbsent(e.uuid(), e);
        }

        return new ArrayList<>(map.values());
    }
}
