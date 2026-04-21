package net.oktawia.crazyae2addons.menus.part;

import appeng.api.config.Settings;
import appeng.api.util.IConfigManager;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.parts.TagLevelEmitter;

public class TagLevelEmitterMenu extends UpgradeableMenu<TagLevelEmitter> {

    private static final String ACTION_SET_EXPRESSION = "set_expression";
    private static final String ACTION_SET_THRESHOLD = "set_threshold";

    @GuiSync(2)
    public String expression = "";

    @GuiSync(3)
    public long threshold = 0L;

    public TagLevelEmitterMenu(int id, Inventory ip, TagLevelEmitter host) {
        super(CrazyMenuRegistrar.TAG_LEVEL_EMITTER_MENU.get(), id, ip, host);

        this.expression = host.getExpression();
        this.threshold = host.getReportingValue();

        registerClientAction(ACTION_SET_EXPRESSION, String.class, this::setExpression);
        registerClientAction(ACTION_SET_THRESHOLD, Long.class, this::setThreshold);
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