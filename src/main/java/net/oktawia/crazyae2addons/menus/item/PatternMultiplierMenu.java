package net.oktawia.crazyae2addons.menus;

import appeng.core.definitions.AEItems;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.PatternMultiplierItem;
import net.oktawia.crazyae2addons.logic.patternmultiplier.PatternMultiplierHost;
import net.oktawia.crazyae2addons.logic.patternmultiplier.PatternMultiplierLogic;

public class PatternMultiplierMenu extends AEBaseMenu {

    public static final String ACTION_MODIFY_PATTERNS = "modifyPatterns";
    public static final String ACTION_CLEAR_PATTERNS = "clearPatterns";
    public static final String ACTION_SET_LIMIT = "setLimit";

    private final PatternMultiplierHost host;

    @GuiSync(73)
    public double mult;

    @GuiSync(74)
    public int limit;

    public PatternMultiplierMenu(int id, Inventory playerInventory, PatternMultiplierHost host) {
        super(CrazyMenuRegistrar.PATTERN_MULTIPLIER_MENU.get(), id, playerInventory, host);
        this.host = host;

        createPlayerInventorySlots(playerInventory);

        this.mult = PatternMultiplierItem.readMultiplier(host.getItemStack());
        this.limit = PatternMultiplierItem.readLimit(host.getItemStack());

        for (int i = 0; i < 36; i++) {
            addSlot(new AppEngSlot(host.getInventory(), i), SlotSemantics.ENCODED_PATTERN);
        }

        registerClientAction(ACTION_MODIFY_PATTERNS, Double.class, this::modifyPatterns);
        registerClientAction(ACTION_CLEAR_PATTERNS, this::clearPatterns);
        registerClientAction(ACTION_SET_LIMIT, Integer.class, this::setLimit);
    }

    public void modifyPatterns(double multiplier) {
        if (multiplier < 0) {
            return;
        }

        this.mult = multiplier;
        persistConfig();

        if (isClientSide()) {
            sendClientAction(ACTION_MODIFY_PATTERNS, multiplier);
            return;
        }

        if (multiplier == 0) {
            return;
        }

        if (PatternMultiplierLogic.applyToInventory(
                host.getInventory(),
                this.mult,
                this.limit,
                getPlayer().level()
        )) {
            host.saveChanges();
        }
    }

    public void clearPatterns() {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR_PATTERNS);
            return;
        }

        var blankPattern = AEItems.BLANK_PATTERN.stack();
        boolean changed = false;

        for (int i = 0; i < host.getInventory().size(); i++) {
            if (!host.getInventory().getStackInSlot(i).isEmpty()) {
                host.getInventory().setItemDirect(i, blankPattern.copy());
                changed = true;
            }
        }

        if (changed) {
            host.saveChanges();
        }
    }

    public void setLimit(int value) {
        this.limit = Math.max(0, value);
        persistConfig();

        if (isClientSide()) {
            sendClientAction(ACTION_SET_LIMIT, this.limit);
        }
    }

    private void persistConfig() {
        PatternMultiplierItem.writeConfig(host.getItemStack(), this.mult, this.limit);
        host.saveChanges();
    }
}