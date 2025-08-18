package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.NumberEntryWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.CpuPrioMenu;

public class CpuPrioScreen<C extends CpuPrioMenu> extends AEBaseScreen<C> {

    private NumberEntryWidget priority;
    private AETextField priorityInput;
    private boolean initialized = false;

    @Override
    public void updateBeforeRender(){
        if (!this.initialized){
            priorityInput.setValue(Integer.toString(menu.prio));
            this.initialized = true;
        }
    }

    public CpuPrioScreen(C menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);
        priority = new NumberEntryWidget(this.getStyle(), NumberEntryType.UNITLESS);
        priority.setMinValue(-1_000_000);
        priority.setMaxValue( 1_000_000);
        priority.setLongValue(menu.prio);
        priority.setOnConfirm(this::confirm);
        priorityInput = new AETextField(this.getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        priorityInput.setMaxLength(12);
        priorityInput.setBordered(false);
        priority.setOnChange(()-> {
            priority.getIntValue().ifPresent(val -> priorityInput.setValue(String.valueOf(val)));
        });
        this.widgets.add("priorityInput", priority);
        this.widgets.add("priorityInputInput", priorityInput);
        this.widgets.addButton("save", Component.literal("Save"), btn->confirm());
    }


    private void confirm() {
        try {
            var val = Integer.parseInt(priorityInput.getValue());
            getMenu().setPriority(val);
            onClose();
        } catch (Exception ignored) {}
    }
}
