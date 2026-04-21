package net.oktawia.crazyae2addons.menus.part;

import appeng.api.config.Settings;
import appeng.api.stacks.AEKey;
import appeng.api.util.IConfigManager;
import appeng.core.definitions.AEItems;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.FakeSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.parts.MultiLevelEmitter;
import org.jetbrains.annotations.Nullable;

public class MultiLevelEmitterMenu extends UpgradeableMenu<MultiLevelEmitter> {

    private static final int FILTER_SLOTS = MultiLevelEmitter.FILTER_SLOTS;
    private static final int PACKED_BOOL_SLOT_SHIFT = 1;
    private static final int PACKED_THRESHOLD_SHIFT = 60;
    private static final long PACKED_THRESHOLD_MASK = (1L << PACKED_THRESHOLD_SHIFT) - 1L;

    private static final String ACTION_SET_LOGIC_AND = "set_logic_and";
    private static final String ACTION_SET_COMPARE_SLOT = "set_compare_slot";
    private static final String ACTION_SET_THRESHOLD = "set_threshold";
    private static final String ACTION_SET_CRAFT_SLOT = "set_craft_slot";

    @GuiSync(2)
    public int compareMask;

    @GuiSync(3)
    public boolean logicAnd;

    @GuiSync(4)
    public int craftMask;

    @GuiSync(10) public long t0;
    @GuiSync(11) public long t1;
    @GuiSync(12) public long t2;
    @GuiSync(13) public long t3;
    @GuiSync(14) public long t4;
    @GuiSync(15) public long t5;
    @GuiSync(16) public long t6;
    @GuiSync(17) public long t7;
    @GuiSync(18) public long t8;
    @GuiSync(19) public long t9;
    @GuiSync(20) public long t10;
    @GuiSync(21) public long t11;
    @GuiSync(22) public long t12;
    @GuiSync(23) public long t13;
    @GuiSync(24) public long t14;
    @GuiSync(25) public long t15;

    public MultiLevelEmitterMenu(int id, Inventory ip, MultiLevelEmitter host) {
        super(CrazyMenuRegistrar.MULTI_LEVEL_EMITTER_MENU.get(), id, ip, host);

        loadFromHost(host);

        registerClientAction(ACTION_SET_LOGIC_AND, Boolean.class, this::setLogicAnd);
        registerClientAction(ACTION_SET_COMPARE_SLOT, Integer.class, this::setCompareSlotPacked);
        registerClientAction(ACTION_SET_THRESHOLD, Long.class, this::setThresholdPacked);
        registerClientAction(ACTION_SET_CRAFT_SLOT, Integer.class, this::setCraftSlotPacked);
    }

    private void loadFromHost(MultiLevelEmitter host) {
        logicAnd = host.isLogicAnd();
        compareMask = host.getCompareMask();
        craftMask = host.getCraftMask();

        for (int i = 0; i < FILTER_SLOTS; i++) {
            setThresholdField(i, host.getThreshold(i));
        }
    }

    public boolean isLogicAndClient() {
        return logicAnd;
    }

    public void setLogicAnd(boolean logicAnd) {
        this.logicAnd = logicAnd;

        if (isClientSide()) {
            sendClientAction(ACTION_SET_LOGIC_AND, logicAnd);
            return;
        }

        getHost().setLogicAnd(logicAnd);
        markHostForSave();
    }

    public boolean isCompareGeClient(int slot) {
        return isValidSlot(slot) && isBitSet(compareMask, slot);
    }

    public void setCompareGe(int slot, boolean ge) {
        if (!isValidSlot(slot)) {
            return;
        }

        compareMask = setMaskBit(compareMask, slot, ge);

        if (isClientSide()) {
            sendClientAction(ACTION_SET_COMPARE_SLOT, packBooleanSlot(slot, ge));
            return;
        }

        getHost().setCompareGe(slot, ge);
        markHostForSave();
    }

    private void setCompareSlotPacked(int packed) {
        int slot = unpackSlot(packed);
        boolean ge = unpackFlag(packed);

        if (!isClientSide() && isValidSlot(slot)) {
            compareMask = setMaskBit(compareMask, slot, ge);
            getHost().setCompareGe(slot, ge);
            markHostForSave();
        }
    }

    public long getThresholdClient(int slot) {
        return getThresholdField(slot);
    }

    public void setThreshold(int slot, long value) {
        if (!isValidSlot(slot)) {
            return;
        }

        long clamped = Math.max(0L, value);
        setThresholdField(slot, clamped);

        if (isClientSide()) {
            sendClientAction(ACTION_SET_THRESHOLD, packThreshold(slot, clamped));
            return;
        }

        getHost().setThreshold(slot, clamped);
        markHostForSave();
    }

    private void setThresholdPacked(long packed) {
        int slot = (int) (packed >>> PACKED_THRESHOLD_SHIFT);
        long value = packed & PACKED_THRESHOLD_MASK;

        if (!isClientSide() && isValidSlot(slot)) {
            setThresholdField(slot, value);
            getHost().setThreshold(slot, value);
            markHostForSave();
        }
    }

    public boolean isCraftEmitWhenCraftingClient(int slot) {
        return isValidSlot(slot) && isBitSet(craftMask, slot);
    }

    public void setCraftEmitWhenCrafting(int slot, boolean whenCrafting) {
        if (!isValidSlot(slot)) {
            return;
        }

        craftMask = setMaskBit(craftMask, slot, whenCrafting);

        if (isClientSide()) {
            sendClientAction(ACTION_SET_CRAFT_SLOT, packBooleanSlot(slot, whenCrafting));
            return;
        }

        getHost().setCraftEmitWhenCrafting(slot, whenCrafting);
        markHostForSave();
    }

    private void setCraftSlotPacked(int packed) {
        int slot = unpackSlot(packed);
        boolean whenCrafting = unpackFlag(packed);

        if (!isClientSide() && isValidSlot(slot)) {
            craftMask = setMaskBit(craftMask, slot, whenCrafting);
            getHost().setCraftEmitWhenCrafting(slot, whenCrafting);
            markHostForSave();
        }
    }

    @Override
    protected void setupConfig() {
        var wrapper = getHost().getConfig().createMenuWrapper();
        for (int i = 0; i < FILTER_SLOTS; i++) {
            addSlot(new FakeSlot(wrapper, i), SlotSemantics.CONFIG);
        }
    }

    @Override
    public void onSlotChange(Slot slot) {
        super.onSlotChange(slot);
        markHostForSave();
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        setCraftingMode(cm.getSetting(Settings.CRAFT_VIA_REDSTONE));

        if (cm.hasSetting(Settings.FUZZY_MODE)) {
            setFuzzyMode(cm.getSetting(Settings.FUZZY_MODE));
        }

        setRedStoneMode(cm.getSetting(Settings.REDSTONE_EMITTER));
    }

    public boolean supportsFuzzySearch() {
        return getHost().getConfigManager().hasSetting(Settings.FUZZY_MODE)
                && hasUpgrade(AEItems.FUZZY_CARD);
    }

    @Nullable
    public AEKey getConfiguredFilter(int slot) {
        return isValidSlot(slot) ? getHost().getConfig().getKey(slot) : null;
    }

    private long getThresholdField(int slot) {
        return switch (slot) {
            case 0 -> t0;
            case 1 -> t1;
            case 2 -> t2;
            case 3 -> t3;
            case 4 -> t4;
            case 5 -> t5;
            case 6 -> t6;
            case 7 -> t7;
            case 8 -> t8;
            case 9 -> t9;
            case 10 -> t10;
            case 11 -> t11;
            case 12 -> t12;
            case 13 -> t13;
            case 14 -> t14;
            case 15 -> t15;
            default -> 0L;
        };
    }

    private void setThresholdField(int slot, long value) {
        switch (slot) {
            case 0 -> t0 = value;
            case 1 -> t1 = value;
            case 2 -> t2 = value;
            case 3 -> t3 = value;
            case 4 -> t4 = value;
            case 5 -> t5 = value;
            case 6 -> t6 = value;
            case 7 -> t7 = value;
            case 8 -> t8 = value;
            case 9 -> t9 = value;
            case 10 -> t10 = value;
            case 11 -> t11 = value;
            case 12 -> t12 = value;
            case 13 -> t13 = value;
            case 14 -> t14 = value;
            case 15 -> t15 = value;
            default -> {
            }
        }
    }

    private void markHostForSave() {
        if (getHost().getHost() != null) {
            getHost().getHost().markForSave();
        }
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < FILTER_SLOTS;
    }

    private static boolean isBitSet(int mask, int slot) {
        return (mask & (1 << slot)) != 0;
    }

    private static int setMaskBit(int mask, int slot, boolean enabled) {
        return enabled ? (mask | (1 << slot)) : (mask & ~(1 << slot));
    }

    private static int packBooleanSlot(int slot, boolean enabled) {
        return (slot << PACKED_BOOL_SLOT_SHIFT) | (enabled ? 1 : 0);
    }

    private static int unpackSlot(int packed) {
        return packed >> PACKED_BOOL_SLOT_SHIFT;
    }

    private static boolean unpackFlag(int packed) {
        return (packed & 1) != 0;
    }

    private static long packThreshold(int slot, long value) {
        return ((long) slot << PACKED_THRESHOLD_SHIFT) | (Math.max(0L, value) & PACKED_THRESHOLD_MASK);
    }
}