package net.oktawia.crazyae2addons.client.misc;

import appeng.client.Point;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IResizableWidget;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.LangDefs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ClonerMaterialListWidget extends AbstractWidget implements IResizableWidget {

    public record MaterialEntry(ItemStack stack, long available, long required, boolean craftable) {
        public boolean complete() {
            return available >= required;
        }

        public long clampedAvailable() {
            return Math.min(available, required);
        }

        public long missing() {
            return Math.max(0L, required - available);
        }

        public boolean canRequestCraft() {
            return craftable && missing() > 0;
        }

        public String sortName() {
            return stack.getHoverName().getString();
        }
    }

    @FunctionalInterface
    public interface CraftRequestHandler {
        void requestCraft(MaterialEntry entry);
    }

    private static final int ROW_HEIGHT = 18;
    private static final int PADDING = 2;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int BUTTON_SIZE = 16;
    private static final int ICON_SIZE = 16;

    private static final Comparator<MaterialEntry> ENTRY_SORTER =
            Comparator.<MaterialEntry>comparingInt(entry -> entry.complete() ? 0 : 1)
                    .thenComparing(Comparator.comparingLong(MaterialEntry::clampedAvailable).reversed())
                    .thenComparing(Comparator.comparingLong(MaterialEntry::required).reversed())
                    .thenComparing(MaterialEntry::sortName, String.CASE_INSENSITIVE_ORDER);

    private List<MaterialEntry> entries = List.of();
    private final List<IconButton> craftButtons = new ArrayList<>();
    private int scrollOffset = 0;
    @Setter
    private boolean craftButtonsEnabled = false;
    private CraftRequestHandler craftRequestHandler = entry -> {};

    public ClonerMaterialListWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public void setCraftRequestHandler(CraftRequestHandler craftRequestHandler) {
        this.craftRequestHandler = craftRequestHandler == null ? entry -> {} : craftRequestHandler;
        rebuildButtons();
    }

    public void setEntries(List<MaterialEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            this.entries = List.of();
            this.scrollOffset = 0;
            rebuildButtons();
            return;
        }

        ArrayList<MaterialEntry> sorted = new ArrayList<>(entries);
        sorted.sort(ENTRY_SORTER);

        this.entries = List.copyOf(sorted);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, getMaxScroll());
        rebuildButtons();
    }

    public MaterialEntry getHoveredEntry(double mouseX, double mouseY) {
        if (!this.visible || !this.active || !this.isMouseOver(mouseX, mouseY) || entries.isEmpty()) {
            return null;
        }

        if (isHoveringCraftButton(mouseX, mouseY)) {
            return null;
        }

        int contentLeft = getX() + PADDING;
        int contentTop = getY() + PADDING;
        int contentRight = getX() + width - PADDING - SCROLLBAR_WIDTH - 1;
        int contentBottom = getY() + height - PADDING;

        if (mouseX < contentLeft || mouseX >= contentRight || mouseY < contentTop || mouseY >= contentBottom) {
            return null;
        }

        int localY = (int) mouseY - contentTop + scrollOffset;
        int index = localY / ROW_HEIGHT;

        if (index < 0 || index >= entries.size()) {
            return null;
        }

        return entries.get(index);
    }

    public boolean isHoveringCraftButton(double mouseX, double mouseY) {
        for (IconButton craftButton : this.craftButtons) {
            if (craftButton.visible && craftButton.active && craftButton.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void move(Point pos) {
        this.setX(pos.getX());
        this.setY(pos.getY());
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, getMaxScroll());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.visible || !this.active || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int step = ROW_HEIGHT;
        this.scrollOffset = Mth.clamp(
                this.scrollOffset - (int) Math.signum(delta) * step,
                0,
                getMaxScroll()
        );
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) {
            return false;
        }

        for (IconButton craftButton : this.craftButtons) {
            if (craftButton.visible && craftButton.active && craftButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (IconButton craftButton : this.craftButtons) {
            if (craftButton.visible && craftButton.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (IconButton craftButton : this.craftButtons) {
            if (craftButton.visible) {
                craftButton.mouseMoved(mouseX, mouseY);
            }
        }

        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }

        int left = getX();
        int top = getY();
        int right = left + width;
        int bottom = top + height;

        guiGraphics.fill(left, top, right, bottom, 0xAF111111);
        guiGraphics.fill(left, top, right, top + 1, 0xFF666666);
        guiGraphics.fill(left, bottom - 1, right, bottom, 0xFF666666);
        guiGraphics.fill(left, top, left + 1, bottom, 0xFF666666);
        guiGraphics.fill(right - 1, top, right, bottom, 0xFF666666);

        hideAllButtons();

        if (entries.isEmpty()) {
            return;
        }

        int contentLeft = left + PADDING;
        int contentTop = top + PADDING;
        int contentRight = right - PADDING - SCROLLBAR_WIDTH - 1;
        int contentBottom = bottom - PADDING;

        guiGraphics.enableScissor(contentLeft, contentTop, contentRight, contentBottom);

        int startIndex = scrollOffset / ROW_HEIGHT;
        int startOffsetY = -(scrollOffset % ROW_HEIGHT);
        int visibleRows = (height / ROW_HEIGHT) + 2;
        MaterialEntry hoveredEntry = getHoveredEntry(mouseX, mouseY);

        for (int i = 0; i < visibleRows; i++) {
            int index = startIndex + i;
            if (index >= entries.size()) {
                break;
            }

            int rowTop = contentTop + startOffsetY + i * ROW_HEIGHT;
            int rowBottom = rowTop + ROW_HEIGHT - 1;

            if (rowBottom < contentTop || rowTop > contentBottom) {
                continue;
            }

            MaterialEntry entry = entries.get(index);
            boolean hovered = hoveredEntry == entry;

            int rowColor = hovered
                    ? 0x5055FFFF
                    : ((index & 1) == 0 ? 0x35222222 : 0x352E2E2E);

            guiGraphics.fill(contentLeft, rowTop, contentRight, rowBottom, rowColor);

            ItemStack displayStack = entry.stack().copy();
            displayStack.setCount(1);

            int iconX = contentLeft + 1;
            int iconY = rowTop + 1;

            guiGraphics.renderItem(displayStack, iconX, iconY);

            int buttonX = contentRight - BUTTON_SIZE - 1;
            int textX = iconX + ICON_SIZE + 3;
            int textY = rowTop + 5;
            int textColor = entry.complete() ? 0xFF55FF55 : 0xFFFF5555;
            String counterText = entry.clampedAvailable() + "/" + entry.required();

            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    counterText,
                    textX,
                    textY,
                    textColor,
                    false
            );

            IconButton craftButton = this.craftButtons.get(index);
            boolean showCraftButton = this.craftButtonsEnabled && entry.canRequestCraft();

            craftButton.visible = showCraftButton;
            craftButton.active = showCraftButton;
            craftButton.setPosition(buttonX, rowTop + 1);

            if (showCraftButton) {
                craftButton.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        guiGraphics.disableScissor();

        renderScrollbar(guiGraphics, left, top, right, bottom);
    }

    private void rebuildButtons() {
        this.craftButtons.clear();

        for (MaterialEntry entry : this.entries) {
            IconButton button = new IconButton(Icon.CRAFT_HAMMER, pressed -> {
                if (this.craftButtonsEnabled && entry.canRequestCraft()) {
                    this.craftRequestHandler.requestCraft(entry);
                }
            });

            button.setTooltip(Tooltip.create(
                    Component.translatable(LangDefs.CRAFT_REQUEST_MISSING.getTranslationKey())
            ));
            button.visible = false;
            button.active = false;

            this.craftButtons.add(button);
        }
    }

    private void hideAllButtons() {
        for (IconButton craftButton : this.craftButtons) {
            craftButton.visible = false;
            craftButton.active = false;
        }
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        int trackLeft = right - SCROLLBAR_WIDTH - 1;
        int trackRight = right - 1;
        int trackTop = top + 1;
        int trackBottom = bottom - 1;

        guiGraphics.fill(trackLeft, trackTop, trackRight, trackBottom, 0x60202020);

        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) {
            guiGraphics.fill(trackLeft, trackTop, trackRight, trackBottom, 0x80505050);
            return;
        }

        int trackHeight = trackBottom - trackTop;
        int contentHeight = entries.size() * ROW_HEIGHT;
        int thumbHeight = Math.max(10, (int) ((trackHeight * (double) height) / contentHeight));
        int thumbTravel = trackHeight - thumbHeight;
        int thumbTop = trackTop + (int) ((scrollOffset / (double) maxScroll) * thumbTravel);

        guiGraphics.fill(trackLeft, thumbTop, trackRight, thumbTop + thumbHeight, 0xFF777777);
    }

    private int getMaxScroll() {
        return Math.max(0, entries.size() * ROW_HEIGHT - (height - PADDING * 2));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}