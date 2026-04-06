package net.oktawia.crazyae2addons.items.wireless;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.RestrictedInputSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.menus.EmitterTerminalMenu;

import java.util.List;

public class WirelessEmitterTerminalMenu extends EmitterTerminalMenu {

    public static final ResourceLocation ID = CrazyAddons.makeId("wireless_emitter_terminal");
    public static final MenuType<WirelessEmitterTerminalMenu> TYPE =
            MenuTypeBuilder.create(WirelessEmitterTerminalMenu::new, EmitterTerminalMenuHost.class)
                    .buildUnregistered(ID);

    private final EmitterTerminalMenuHost host;
    private int outOfRangeCheckTimer = 0;

    public WirelessEmitterTerminalMenu(int id, Inventory ip, EmitterTerminalMenuHost host) {
        super(TYPE, id, ip, host);
        this.host = host;

        IUpgradeInventory upgrades = host.getUpgrades();
        for (int i = 0; i < upgrades.size(); i++) {
            this.addSlot(new RestrictedInputSlot(
                    RestrictedInputSlot.PlacableItemType.UPGRADES,
                    upgrades, i), SlotSemantics.UPGRADE);
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (isClientSide()) {
            return;
        }

        outOfRangeCheckTimer++;
        if (outOfRangeCheckTimer >= 20) {
            outOfRangeCheckTimer = 0;
            outOfRange = host.getActionableNode() == null || host.getActionableNode().getGrid() == null;
            if (!outOfRange) {
                refreshEmitters();
            } else {
                emitters = "[]";
            }
        }
    }

    private void refreshEmitters() {
        if (isClientSide()) {
            return;
        }

        this.outOfRange = host.getActionableNode() == null || host.getActionableNode().getGrid() == null;

        if (this.outOfRange) {
            this.emitters = "[]";
            return;
        }

        List<StorageEmitterInfo> list;

        if (this.currentSearch == null || currentSearch.isBlank()) {
            list = host.getEmitters();
        } else {
            list = host.getEmitters(currentSearch);
        }

        this.emitters = GSON.toJson(list);
    }
}
