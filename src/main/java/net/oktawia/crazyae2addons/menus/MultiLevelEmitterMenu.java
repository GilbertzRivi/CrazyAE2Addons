package net.oktawia.crazyae2addons.menus;

import net.oktawia.crazyae2addons.parts.MultiStorageLevelEmitterPart;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import appeng.api.config.Settings;
import appeng.api.stacks.AEKey;
import appeng.api.util.IConfigManager;
import appeng.core.definitions.AEItems;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.FakeSlot;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;

public class MultiLevelEmitterMenu extends UpgradeableMenu<MultiStorageLevelEmitterPart> {

    private static final int FILTER_SLOTS = 16;

    private static final String ACTION_SET_LOGIC_AND = "setLogicAnd";
    private static final String ACTION_SET_COMPARE_SLOT = "setCompareSlot";
    private static final String ACTION_SET_THRESHOLD = "setThresholdPacked";
    private static final String ACTION_SET_CRAFT_SLOT = "setCraftSlot";

    private static final long VALUE_MASK_60 = (1L << 60) - 1L;
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

    public MultiLevelEmitterMenu(int id, Inventory ip, MultiStorageLevelEmitterPart host) {
        super(CrazyMenuRegistrar.MULTI_LEVEL_EMITTER_MENU.get(), id, ip, host);

        this.logicAnd = host.isLogicAnd();
        this.compareMask = buildMaskFromHost(host);
        for (int i = 0; i < FILTER_SLOTS; i++) {
            setThresholdField(i, host.getThreshold(i));
        }
        this.craftMask = 0;
        for (int i = 0; i < 16; i++) {
            if (host.isCraftEmitWhenCrafting(i)) this.craftMask |= (1 << i);
        }

        registerClientAction(ACTION_SET_LOGIC_AND, Boolean.class, this::setLogicAnd);
        registerClientAction(ACTION_SET_COMPARE_SLOT, Integer.class, this::setCompareSlotPacked);
        registerClientAction(ACTION_SET_THRESHOLD, Long.class, this::setThresholdPacked);
        registerClientAction(ACTION_SET_CRAFT_SLOT, Integer.class, this::setCraftSlotPacked);

    }

    private static int buildMaskFromHost(MultiStorageLevelEmitterPart host) {
        int mask = 0;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (host.isCompareGe(i)) mask |= (1 << i);
        }
        return mask;
    }

    public boolean isLogicAndClient() { return logicAnd; }

    public void setLogicAnd(boolean and) {
        this.logicAnd = and;

        if (isClientSide()) {
            sendClientAction(ACTION_SET_LOGIC_AND, and);
        } else {
            getHost().setLogicAnd(and);
            getHost().getHost().markForSave();
        }
    }

    public boolean isCompareGeClient(int slot) {
        return slot >= 0 && slot < FILTER_SLOTS && (compareMask & (1 << slot)) != 0;
    }

    public void setCompareGe(int slot, boolean ge) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;

        if (ge) compareMask |= (1 << slot);
        else compareMask &= ~(1 << slot);

        if (isClientSide()) {
            int packed = (slot << 1) | (ge ? 1 : 0);
            sendClientAction(ACTION_SET_COMPARE_SLOT, packed);
        } else {
            getHost().setCompareGe(slot, ge);
            getHost().getHost().markForSave();
        }
    }

    private void setCompareSlotPacked(int packed) {
        int slot = packed >> 1;
        boolean ge = (packed & 1) != 0;
        if (!isClientSide() && slot >= 0 && slot < FILTER_SLOTS) {
            getHost().setCompareGe(slot, ge);
            getHost().getHost().markForSave();
        }
    }

    public long getThresholdClient(int slot) {
        return getThresholdField(slot);
    }

    public void setThreshold(int slot, long value) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;
        if (value < 0) value = 0;

        setThresholdField(slot, value);

        if (isClientSide()) {
            long packed = packThreshold(slot, value);
            sendClientAction(ACTION_SET_THRESHOLD, packed);
        } else {
            getHost().setThreshold(slot, value);
            getHost().getHost().markForSave();
        }
    }

    private void setThresholdPacked(long packed) {
        int slot = (int) (packed >>> 60);
        long value = packed & VALUE_MASK_60;

        if (!isClientSide()) {
            setThresholdField(slot, value);
            getHost().setThreshold(slot, value);
            getHost().getHost().markForSave();
        }
    }

    private static long packThreshold(int slot, long value) {
        if (value < 0) value = 0;
        value &= VALUE_MASK_60;
        return ((long) slot << 60) | value;
    }

    private long getThresholdField(int slot) {
        return switch (slot) {
            case 0 -> t0; case 1 -> t1; case 2 -> t2; case 3 -> t3;
            case 4 -> t4; case 5 -> t5; case 6 -> t6; case 7 -> t7;
            case 8 -> t8; case 9 -> t9; case 10 -> t10; case 11 -> t11;
            case 12 -> t12; case 13 -> t13; case 14 -> t14; case 15 -> t15;
            default -> 0L;
        };
    }

    private void setThresholdField(int slot, long v) {
        switch (slot) {
            case 0 -> t0 = v; case 1 -> t1 = v; case 2 -> t2 = v; case 3 -> t3 = v;
            case 4 -> t4 = v; case 5 -> t5 = v; case 6 -> t6 = v; case 7 -> t7 = v;
            case 8 -> t8 = v; case 9 -> t9 = v; case 10 -> t10 = v; case 11 -> t11 = v;
            case 12 -> t12 = v; case 13 -> t13 = v; case 14 -> t14 = v; case 15 -> t15 = v;
        }
    }

    @Override
    protected void setupConfig() {
        var inv = getHost().getConfig().createMenuWrapper();
        for (int i = 0; i < FILTER_SLOTS; i++) {
            this.addSlot(new FakeSlot(inv, i), SlotSemantics.CONFIG);
        }
    }

    @Override
    public void onSlotChange(Slot s) {
        super.onSlotChange(s);
        getHost().getHost().markForSave();
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        this.setCraftingMode(cm.getSetting(Settings.CRAFT_VIA_REDSTONE));
        if (cm.hasSetting(Settings.FUZZY_MODE)) {
            this.setFuzzyMode(cm.getSetting(Settings.FUZZY_MODE));
        }
        this.setRedStoneMode(cm.getSetting(Settings.REDSTONE_EMITTER));
    }

    public boolean supportsFuzzySearch() {
        return getHost().getConfigManager().hasSetting(Settings.FUZZY_MODE) && hasUpgrade(AEItems.FUZZY_CARD);
    }

    @Nullable
    public AEKey getConfiguredFilter(int slot) {
        if (slot < 0 || slot >= FILTER_SLOTS) return null;
        return getHost().getConfig().getKey(slot);
    }

    public boolean isCraftEmitWhenCraftingClient(int slot) {
        return slot >= 0 && slot < 16 && (craftMask & (1 << slot)) != 0;
    }

    public void setCraftEmitWhenCrafting(int slot, boolean whenCrafting) {
        if (slot < 0 || slot >= 16) return;

        if (whenCrafting) craftMask |= (1 << slot);
        else craftMask &= ~(1 << slot);

        if (isClientSide()) {
            int packed = (slot << 1) | (whenCrafting ? 1 : 0);
            sendClientAction("setCraftSlot", packed);
        } else {
            getHost().setCraftEmitWhenCrafting(slot, whenCrafting);
            getHost().getHost().markForSave();
        }
    }

    private void setCraftSlotPacked(int packed) {
        int slot = packed >> 1;
        boolean whenCrafting = (packed & 1) != 0;

        if (!isClientSide() && slot >= 0 && slot < 16) {
            if (whenCrafting) craftMask |= (1 << slot);
            else craftMask &= ~(1 << slot);

            getHost().setCraftEmitWhenCrafting(slot, whenCrafting);
            getHost().getHost().markForSave();
        }
    }

}