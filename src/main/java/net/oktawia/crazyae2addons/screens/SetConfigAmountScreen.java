package net.oktawia.crazyae2addons.screens;

import java.util.function.Consumer;

import appeng.client.gui.widgets.TabButton;
import com.google.common.primitives.Longs;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import net.minecraft.network.chat.Component;
import net.oktawia.crazyae2addons.menus.EjectorMenu;

public class SetConfigAmountScreen<C extends EjectorMenu>
        extends AESubScreen<C, EjectorScreen<C>> {

    private final NumberEntryWidget amount;
    private final GenericStack currentStack;
    private final Consumer<GenericStack> setter;

    public SetConfigAmountScreen(EjectorScreen<C> parentScreen,
                                 GenericStack currentStack,
                                 Consumer<GenericStack> setter) {
        super(parentScreen, "/screens/set_processing_pattern_amount.json");

        this.currentStack = currentStack;
        this.setter = setter;

        widgets.addButton("save", GuiText.Set.text(), this::confirm);

        var icon = getMenu().host.getMainMenuIcon();
        var button = new TabButton(icon, icon.getHoverName(), btn -> {
            returnToParent();
        });
        widgets.add("back", button);

        this.amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.of(currentStack.what()));
        this.amount.setLongValue(currentStack.amount());
        this.amount.setMaxValue(getMaxAmount());
        this.amount.setTextFieldStyle(style.getWidget("amountToStockInput"));
        this.amount.setMinValue(0);
        this.amount.setHideValidationIcon(true);
        this.amount.setOnConfirm(this::confirm);

        addClientSideSlot(new ClientDisplaySlot(currentStack), SlotSemantics.MACHINE_OUTPUT);
    }

    @Override
    protected void init() {
        super.init();
        setSlotsHidden(SlotSemantics.TOOLBOX, true);
    }

    private void confirm() {
        this.amount.getLongValue().ifPresent(newAmount -> {
            newAmount = Longs.constrainToRange(newAmount, 0, getMaxAmount());
            if (newAmount <= 0) {
                setter.accept(null);
            } else {
                setter.accept(new GenericStack(currentStack.what(), newAmount));
            }
            returnToParent();
        });
    }

    private long getMaxAmount() {
        return 999_999L * (long) currentStack.what().getAmountPerUnit();
    }
}

