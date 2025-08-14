package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.SlotSemantics;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.EjectorMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.network.NetworkHandler;

public class EjectorScreen<C extends EjectorMenu> extends UpgradeableScreen<C> {
    public EjectorScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        var btn = new IconButton(Icon.ENTER, b -> getMenu().applyPatternToConfig());
        this.widgets.add("load", btn);
    }

    @Override
    public void updateBeforeRender() {
        super.updateBeforeRender();
        setTextContent("missing", Component.literal(getMenu().cantCraft));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.minecraft.options.keyPickItem.matchesMouse(button)) {
            var slot = this.getSlotUnderMouse();
            if (getMenu().canModifyAmountForSlot(slot)) {
                var gs = appeng.api.stacks.GenericStack.fromItemStack(slot.getItem());
                if (gs != null) {
                    this.setSlotsHidden(SlotSemantics.CONFIG, true);
                    this.setSlotsHidden(SlotSemantics.PLAYER_HOTBAR, true);
                    this.setSlotsHidden(SlotSemantics.PLAYER_INVENTORY, true);
                    this.minecraft.setScreen(new SetConfigAmountScreen<>(
                            this,
                            gs,
                            newStack -> {
                                if (newStack == null) {
                                    NetworkHandler.INSTANCE.sendToServer(
                                            new net.oktawia.crazyae2addons.network.SetConfigAmountPacket(slot.index, 0L));
                                } else {
                                    NetworkHandler.INSTANCE.sendToServer(
                                            new net.oktawia.crazyae2addons.network.SetConfigAmountPacket(slot.index, newStack.amount()));
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
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    protected void renderTooltip(net.minecraft.client.gui.GuiGraphics gg, int x, int y) {
        if (this.menu.getCarried().isEmpty() && this.menu.canModifyAmountForSlot(this.hoveredSlot)) {
            var lines = new java.util.ArrayList<>(getTooltipFromContainerItem(this.hoveredSlot.getItem()));
            lines.add(Component.literal("Middle mouse button to set amount").withStyle(net.minecraft.ChatFormatting.GRAY));
            drawTooltip(gg, x, y, lines);
            return;
        }
        super.renderTooltip(gg, x, y);
    }
}
