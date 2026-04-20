package net.oktawia.crazyae2addons.client.screens.block;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.SlotSemantics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.client.screens.SetConfigAmountScreen;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.entities.EjectorBE;
import net.oktawia.crazyae2addons.menus.block.EjectorMenu;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.SetConfigAmountPacket;

import java.util.ArrayList;

public class EjectorScreen<C extends EjectorMenu> extends AEBaseScreen<C> {

    private static final int MISSING_ICON_X = 80;
    private static final int MISSING_ICON_Y = 22;

    public EjectorScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        var btn = new IconButton(Icon.ENTER, b -> getMenu().applyPatternToConfig());
        btn.setTooltip(Tooltip.create(Component.translatable(LangDefs.EJECTOR_LOAD_PATTERN.getTranslationKey())));
        this.widgets.add("load", btn);

        this.addRenderableOnly((gg, mouseX, mouseY, partialTicks) -> {
            ItemStack missingIcon = getHost().getCantCraftStack();
            if (!missingIcon.isEmpty()) {
                int x = leftPos + MISSING_ICON_X;
                int y = topPos + MISSING_ICON_Y;
                gg.renderItem(missingIcon, x, y);

                String countText = getHost().getCantCraftCountText();
                if (countText != null && !countText.isEmpty()) {
                    gg.renderItemDecorations(font, missingIcon, x, y, countText);
                }
            }
        });
    }

    private EjectorBE getHost() {
        return getMenu().getHost();
    }

    @Override
    public void updateBeforeRender() {
        super.updateBeforeRender();

        if (!getHost().getCantCraftStack().isEmpty()) {
            setTextContent("missing", Component.empty());
            return;
        }

        String missingText = getHost().getCantCraftText();
        if (missingText == null || missingText.isBlank()) {
            setTextContent("missing", Component.translatable(LangDefs.NOTHING.getTranslationKey()));
        } else {
            setTextContent("missing", Component.literal(missingText));
        }
    }

    private boolean isMouseOverMissingIcon(int mouseX, int mouseY) {
        int x = leftPos + MISSING_ICON_X;
        int y = topPos + MISSING_ICON_Y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 2) {
            Slot slot = this.getSlotUnderMouse();
            if (getMenu().canModifyAmountForSlot(slot)) {
                var gs = GenericStack.fromItemStack(slot.getItem());
                if (gs != null) {
                    this.setSlotsHidden(SlotSemantics.CONFIG, true);
                    this.setSlotsHidden(SlotSemantics.PLAYER_HOTBAR, true);
                    this.setSlotsHidden(SlotSemantics.PLAYER_INVENTORY, true);
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new SetConfigAmountScreen<>(
                                this,
                                gs,
                                newStack -> {
                                    if (newStack == null) {
                                        NetworkHandler.sendToServer(new SetConfigAmountPacket(slot.index, 0L));
                                    } else {
                                        NetworkHandler.sendToServer(new SetConfigAmountPacket(slot.index, newStack.amount()));
                                    }

                                    this.setSlotsHidden(SlotSemantics.CONFIG, false);
                                    this.setSlotsHidden(SlotSemantics.PLAYER_HOTBAR, false);
                                    this.setSlotsHidden(SlotSemantics.PLAYER_INVENTORY, false);
                                }
                        ));
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics gg, int x, int y) {
        ItemStack missingIcon = getHost().getCantCraftStack();
        if (!missingIcon.isEmpty() && isMouseOverMissingIcon(x, y)) {
            gg.renderTooltip(this.font, missingIcon, x, y);
            return;
        }

        if (this.menu.getCarried().isEmpty() && this.menu.canModifyAmountForSlot(this.hoveredSlot)) {
            var lines = new ArrayList<>(getTooltipFromContainerItem(this.hoveredSlot.getItem()));
            lines.add(Component.translatable(LangDefs.EJECTOR_MIDDLE_CLICK.getTranslationKey()).withStyle(ChatFormatting.GRAY));
            drawTooltip(gg, x, y, lines);
            return;
        }

        super.renderTooltip(gg, x, y);
    }
}