package net.oktawia.crazyae2addonslite.menus;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.api.config.YesNo;
import appeng.api.stacks.AEItemKey;
import appeng.api.util.IConfigManager;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.FakeSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addonslite.parts.NBTStorageBusPart;
import org.jetbrains.annotations.Nullable;

public class NBTStorageBusMenu extends UpgradeableMenu<NBTStorageBusPart> {
    public static final String SEND_DATA = "SendData";

    @GuiSync(92)
    public boolean newData;
    @GuiSync(31)
    public String data;
    @GuiSync(3)
    public AccessRestriction rwMode = AccessRestriction.READ_WRITE;
    @GuiSync(4)
    public StorageFilter storageFilter = StorageFilter.EXTRACTABLE_ONLY;
    @GuiSync(7)
    public YesNo filterOnExtract = YesNo.YES;
    @GuiSync(8)
    @Nullable
    public Component connectedTo;
    public NBTStorageBusPart host;

    public NBTStorageBusMenu(int id, Inventory playerInventory, NBTStorageBusPart host) {
        super(CrazyMenuRegistrar.NBT_STORAGE_BUS_MENU.get(), id, playerInventory, host);
        registerClientAction(SEND_DATA, String.class, this::updateData);
        this.addSlot(new FakeSlot(host.inv.createMenuWrapper(), 0), SlotSemantics.CONFIG);
        this.host = host;
        this.data = host.config;
        this.newData = false;
        this.host.setMenu(this);
    }

    public void updateData(String data){
        this.data = data;
        this.host.config = data;
        this.host.getHost().markForSave();
        this.host.onSettingChanged(null, null);
        if (isClientSide()){
            sendClientAction(SEND_DATA, data);
        }
    }

    public void loadNBT(){
        var key = host.inv.getKey(0);
        if (key instanceof AEItemKey ik){
            if (ik.getTag() != null){
                this.data = ik.getTag().toString();
                this.newData = true;
            }
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        this.connectedTo = getHost().getConnectedToDescription();
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        this.setFuzzyMode(cm.getSetting(Settings.FUZZY_MODE));
        this.setReadWriteMode(cm.getSetting(Settings.ACCESS));
        this.setStorageFilter(cm.getSetting(Settings.STORAGE_FILTER));
        this.setFilterOnExtract(cm.getSetting(Settings.FILTER_ON_EXTRACT));
    }

    public AccessRestriction getReadWriteMode() {
        return this.rwMode;
    }

    private void setReadWriteMode(AccessRestriction rwMode) {
        this.rwMode = rwMode;
    }

    public StorageFilter getStorageFilter() {
        return this.storageFilter;
    }

    private void setStorageFilter(StorageFilter storageFilter) {
        this.storageFilter = storageFilter;
    }

    public YesNo getFilterOnExtract() {
        return this.filterOnExtract;
    }

    public void setFilterOnExtract(YesNo filterOnExtract) {
        this.filterOnExtract = filterOnExtract;
    }

    @Override
    public MenuType<?> getType() {
        return CrazyMenuRegistrar.NBT_STORAGE_BUS_MENU.get();
    }
}
