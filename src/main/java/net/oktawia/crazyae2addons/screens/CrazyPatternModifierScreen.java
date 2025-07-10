package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fml.ModList;
import net.oktawia.crazyae2addons.menus.CrazyPatternModifierMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.minecraft.client.gui.components.Button;

public class CrazyPatternModifierScreen<C extends CrazyPatternModifierMenu> extends AEBaseScreen<C> implements CrazyScreen {
    private static final String NAME = "crazy_pattern_modifier";
    public IconButton nbt;
    public IconButton circConfirm;
    public AETextField circ;
    public final Button[] historyButtons = new Button[5];

    static {
        CrazyScreen.i18n(NAME, "gregtech_missing", "GregTech not detected");
        CrazyScreen.i18n(NAME, "circuit_tooltip", "Circuit %s");
        CrazyScreen.i18n(NAME, "empty_button", "-");
    }

    public CrazyPatternModifierScreen(CrazyPatternModifierMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super((C) menu, playerInventory, title, style);
        setupGui();
        this.widgets.add("circ", circ);
    }

    private void setupGui(){
        this.nbt = new IconButton(Icon.ENTER, this::changeNbt);
        this.circConfirm = new IconButton(Icon.ENTER, this::modifyCirc);

        this.widgets.add("nbt", this.nbt);
        this.widgets.add("confirmcirc", this.circConfirm);

        this.circ = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        circ.setBordered(false);
        circ.setMaxLength(2);

        for (int i = 0; i < 5; i++) {
            int index = i;
            historyButtons[i] = Button.builder(l10n(NAME, "empty_button"), btn -> applyHistoryValue(index))
                .bounds(0, 0, 30, 20)
                .build();
            this.widgets.add("button" + (i + 1), historyButtons[i]);
        }
    }

    private void applyHistoryValue(int index) {
        int[] history = {
                getMenu().history1,
                getMenu().history2,
                getMenu().history3,
                getMenu().history4,
                getMenu().history5
        };
        if (index < 0 || index >= history.length) return;
        int val = history[index];
        if (val == -1) return;
        circ.setValue(String.valueOf(val));
        getMenu().changeCircuit(val, false);
    }

    @Override
    protected void updateBeforeRender(){
        super.updateBeforeRender();
        setTextContent("info1", Component.literal(getMenu().textNBT));
        if (ModList.get().isLoaded("gtceu")){
            setTextContent("info2", Component.literal(getMenu().textCirc));
        } else {
            setTextContent("info2", l10n(NAME, "gregtech_missing"));
        }

        int[] history = {
                getMenu().history1,
                getMenu().history2,
                getMenu().history3,
                getMenu().history4,
                getMenu().history5
        };

        for (int i = 0; i < historyButtons.length; i++) {
            int val = history[i];
            Button btn = historyButtons[i];
            if (val != -1) {
                btn.active = true;
                btn.setMessage(Component.literal(String.valueOf(val)));
                btn.setTooltip(Tooltip.create(l10n(NAME, "circuit_tooltip", val)));
            } else {
                btn.active = false;
                btn.setMessage(l10n(NAME, "empty_button"));
                btn.setTooltip(Tooltip.create(Component.empty()));
            }
        }
    }

    public void changeNbt(Button btn) {
        this.getMenu().changeNBT();
    }

    public void modifyCirc(Button btn){
        if (ModList.get().isLoaded("gtceu")){
            if (circ.getValue().isEmpty()){
                this.getMenu().changeCircuit(-1);
            } else if ((circ.getValue().chars().allMatch(Character::isDigit) && !circ.getValue().isEmpty() && Integer.parseInt(circ.getValue()) <= 32)){
                this.getMenu().changeCircuit(Integer.parseInt(circ.getValue()));
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        if (button == 1 && circ != null && circ.isMouseOver(mouseX, mouseY)) {
            circ.setValue("");
            return true;
        }

        return handled;
    }
}
