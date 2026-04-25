package net.oktawia.crazyae2addons.menus.part;

import appeng.api.config.Settings;
import appeng.api.util.IConfigManager;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.IAnalogLevelEmitterMenu;
import net.oktawia.crazyae2addons.parts.TagLevelEmitter;

public class TagLevelEmitterMenu extends UpgradeableMenu<TagLevelEmitter> implements IAnalogLevelEmitterMenu {

    private static final String ACTION_SET_EXPRESSION = "set_expression";
    private static final String ACTION_SET_THRESHOLD = "set_threshold";
    private static final String ACTION_SET_ANALOG_LOGARITHMIC_MODE = "set_analog_logarithmic_mode";

    @GuiSync(12)
    public String expression = "";

    @GuiSync(13)
    public long threshold = 0L;

    @GuiSync(14)
    private boolean analogLogarithmicMode = false;

    @GuiSync(16)
    private boolean hasAnalogCard = false;

    public TagLevelEmitterMenu(int id, Inventory ip, TagLevelEmitter host) {
        super(CrazyMenuRegistrar.TAG_LEVEL_EMITTER_MENU.get(), id, ip, host);

        this.expression = host.getExpression();
        this.threshold = host.getReportingValue();
        this.analogLogarithmicMode = host.crazyAE2Addons$isAnalogLogarithmicMode();
        this.hasAnalogCard = host.crazyAE2Addons$hasAnalogCard();

        registerClientAction(ACTION_SET_EXPRESSION, String.class, this::setExpression);
        registerClientAction(ACTION_SET_THRESHOLD, Long.class, this::setThreshold);
        registerClientAction(ACTION_SET_ANALOG_LOGARITHMIC_MODE, Boolean.class, this::crazyAE2Addons$setAnalogLogarithmicMode);
    }

    @Override
    public void broadcastChanges() {
        this.analogLogarithmicMode = getHost().crazyAE2Addons$isAnalogLogarithmicMode();
        this.hasAnalogCard = getHost().crazyAE2Addons$hasAnalogCard();

        super.broadcastChanges();
    }

    public void setExpression(String expression) {
        this.expression = expression == null ? "" : expression;

        if (isClientSide()) {
            sendClientAction(ACTION_SET_EXPRESSION, this.expression);
            return;
        }

        getHost().setExpression(this.expression);
        markHostForSave();
    }

    public void setThreshold(long value) {
        long normalized = Math.max(0L, value);
        this.threshold = normalized;

        if (isClientSide()) {
            sendClientAction(ACTION_SET_THRESHOLD, normalized);
            return;
        }

        getHost().setReportingValue(normalized);
        markHostForSave();
    }

    @Override
    public boolean crazyAE2Addons$hasAnalogCard() {
        return this.hasAnalogCard;
    }

    @Override
    public boolean crazyAE2Addons$isAnalogLogarithmicMode() {
        return this.analogLogarithmicMode;
    }

    @Override
    public void crazyAE2Addons$setAnalogLogarithmicMode(boolean value) {
        this.analogLogarithmicMode = value;

        if (isClientSide()) {
            sendClientAction(ACTION_SET_ANALOG_LOGARITHMIC_MODE, value);
            return;
        }

        getHost().crazyAE2Addons$setAnalogLogarithmicMode(value);
        markHostForSave();
    }

    @Override
    protected void setupConfig() {
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        this.setRedStoneMode(cm.getSetting(Settings.REDSTONE_EMITTER));
    }

    private void markHostForSave() {
        if (getHost().getHost() != null) {
            getHost().getHost().markForSave();
        }
    }
}