package net.oktawia.crazyae2addons.menus;

import appeng.api.config.Settings;
import appeng.api.util.IConfigManager;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.parts.TagLevelEmitterPart;

public class TagLevelEmitterMenu extends UpgradeableMenu<TagLevelEmitterPart> {

    private static final String ACTION_SET_EXPRESSION = "setExpression";
    private static final String ACTION_SET_THRESHOLD  = "setThreshold";

    @GuiSync(2)
    public String expression = "";

    @GuiSync(3)
    public long threshold = 0;

    public TagLevelEmitterMenu(int id, Inventory ip, TagLevelEmitterPart host) {
        super(CrazyMenuRegistrar.TAG_LEVEL_EMITTER_MENU.get(), id, ip, host);

        this.expression = host.getExpression();
        this.threshold  = host.getReportingValue();

        registerClientAction(ACTION_SET_EXPRESSION, String.class, this::setExpression);
        registerClientAction(ACTION_SET_THRESHOLD,  Long.class,   this::setThreshold);
    }

    public void setExpression(String expr) {
        this.expression = expr;
        if (isClientSide()) {
            sendClientAction(ACTION_SET_EXPRESSION, expr);
        } else {
            getHost().setExpression(expr);
            getHost().getHost().markForSave();
        }
    }

    public void setThreshold(long value) {
        if (value < 0) value = 0;
        this.threshold = value;
        if (isClientSide()) {
            sendClientAction(ACTION_SET_THRESHOLD, value);
        } else {
            getHost().setReportingValue(value);
            getHost().getHost().markForSave();
        }
    }

    @Override
    protected void setupConfig() {
        // no config slots
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        this.setRedStoneMode(cm.getSetting(Settings.REDSTONE_EMITTER));
    }
}
