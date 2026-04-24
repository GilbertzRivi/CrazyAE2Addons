package net.oktawia.crazyae2addons.client.screens.item;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.NumberEntryWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.item.CpuPrioMenu;

public class CpuPrioScreen<C extends CpuPrioMenu> extends AEBaseScreen<C> {

    private static final int ICON_X = 23;
    private static final int ICON_Y = 53;

    private final NumberEntryWidget priority;
    private final AETextField priorityInput;
    private boolean initialized = false;

    public CpuPrioScreen(C menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);

        this.priority = new NumberEntryWidget(this.getStyle(), NumberEntryType.UNITLESS);
        this.priority.setMinValue(-1_000_000);
        this.priority.setMaxValue(1_000_000);
        this.priority.setLongValue(menu.prio);
        this.priority.setOnConfirm(this::confirm);

        this.priorityInput = new AETextField(this.getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        this.priorityInput.setMaxLength(12);
        this.priorityInput.setBordered(false);
        this.priorityInput.setPlaceholder(Component.translatable(LangDefs.PRIORITY.getTranslationKey()));

        this.priority.setOnChange(() -> {
            if (!this.initialized) {
                return;
            }

            this.priority.getIntValue().ifPresent(val ->
                    this.priorityInput.setValue(Integer.toString(val)));
        });

        this.widgets.add("priority", this.priority);
        this.widgets.add("priorityInput", this.priorityInput);

        this.widgets.addButton(
                "save",
                Component.translatable(LangDefs.SAVE.getTranslationKey()),
                btn -> confirm()
        );
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!this.initialized) {
            this.priorityInput.setValue(Integer.toString(this.menu.prio));
            this.priority.setLongValue(this.menu.prio);
            this.initialized = true;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        ItemStack icon = this.menu.getTargetIcon();
        if (!icon.isEmpty()) {
            int x = this.leftPos + ICON_X;
            int y = this.topPos + ICON_Y;

            guiGraphics.renderItem(icon, x, y);

            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                guiGraphics.renderTooltip(this.font, icon, mouseX, mouseY);
            }
        }
    }

    private void confirm() {
        try {
            int val = Integer.parseInt(this.priorityInput.getValue().trim());
            getMenu().setPriority(val);
            onClose();
        } catch (Exception ignored) {
        }
    }
}