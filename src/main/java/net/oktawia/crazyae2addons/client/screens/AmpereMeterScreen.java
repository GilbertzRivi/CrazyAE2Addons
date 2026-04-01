package net.oktawia.crazyae2addons.client.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ToggleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.AmpereMeterMenu;

import java.util.List;

public class AmpereMeterScreen<C extends AmpereMeterMenu> extends AEBaseScreen<C> {

    public ToggleButton direction;

    public AETextField minFe;
    public AETextField maxFe;

    public AmpereMeterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        direction = new ToggleButton(Icon.ARROW_RIGHT, Icon.ARROW_LEFT, this::toggleDirection);
        direction.setTooltipOn(List.of(Component.translatable(LangDefs.AMPERE_METER_DIRECTION_LEFT_TO_RIGHT.getTranslationKey())));
        direction.setTooltipOff(List.of(Component.translatable(LangDefs.AMPERE_METER_DIRECTION_RIGHT_TO_LEFT.getTranslationKey())));
        this.widgets.add("direction", direction);

        minFe = new AETextField(style, this.font, 0, 0, 64, 12);
        minFe.setBordered(false);
        minFe.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        minFe.setPlaceholder(Component.translatable(LangDefs.MIN.getTranslationKey()));
        var unitTooltipKey = IsModLoaded.isGTCEuLoaded() ? LangDefs.FE_PER_TICK_OR_AMPERES : LangDefs.FE_PER_TICK;
        minFe.setTooltipMessage(List.of(Component.translatable(unitTooltipKey.getTranslationKey())));
        minFe.setMaxLength(10);
        minFe.setResponder(this::onMinChanged);
        this.widgets.add("min_fe", minFe);

        maxFe = new AETextField(style, this.font, 0, 0, 64, 12);
        maxFe.setBordered(false);
        maxFe.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        maxFe.setPlaceholder(Component.translatable(LangDefs.MAX.getTranslationKey()));
        maxFe.setTooltipMessage(List.of(Component.translatable(unitTooltipKey.getTranslationKey())));
        maxFe.setMaxLength(10);
        maxFe.setResponder(this::onMaxChanged);
        this.widgets.add("max_fe", maxFe);
    }

    @Override
    protected void init() {
        super.init();

        if (minFe != null) {
            minFe.setValue(String.valueOf(getMenu().minFePerTick));
        }

        if (maxFe != null) {
            maxFe.setValue(String.valueOf(getMenu().maxFePerTick));
        }
    }

    private void onMinChanged(String value) {
        if (minFe == null || !minFe.isFocused()) return;

        int parsed = 0;
        if (!value.isEmpty()) {
            try {
                parsed = Integer.parseInt(value);
            } catch (Exception ignored) {
                return;
            }
        }

        if (parsed != getMenu().minFePerTick) {
            getMenu().changeMin(parsed);
        }
    }

    private void onMaxChanged(String value) {
        if (maxFe == null || !maxFe.isFocused()) return;

        int parsed = 0;
        if (!value.isEmpty()) {
            try {
                parsed = Integer.parseInt(value);
            } catch (Exception ignored) {
                return;
            }
        }

        if (parsed != getMenu().maxFePerTick) {
            getMenu().changeMax(parsed);
        }
    }

    private void toggleDirection(boolean dir) {
        this.direction.setState(!dir);
        this.getMenu().changeDirection(dir);
    }

    @Override
    protected void updateBeforeRender(){
        super.updateBeforeRender();

        direction.setState(getMenu().direction);
        setTextContent("energy", Component.literal(String.format("Transferring: %s %s", getMenu().transfer, getMenu().unit)));

        if (minFe != null && !minFe.isFocused()) {
            String v = String.valueOf(getMenu().minFePerTick);
            if (!v.equals(minFe.getValue())) {
                minFe.setValue(v);
            }
        }

        if (maxFe != null && !maxFe.isFocused()) {
            String v = String.valueOf(getMenu().maxFePerTick);
            if (!v.equals(maxFe.getValue())) {
                maxFe.setValue(v);
            }
        }
    }
}
