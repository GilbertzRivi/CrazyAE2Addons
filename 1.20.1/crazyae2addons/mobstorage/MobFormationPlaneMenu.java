package net.oktawia.crazyae2addons.mobstorage;

import appeng.core.definitions.AEItems;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.OptionalFakeSlot;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.misc.AppEngMobFilteredFakeSlot;
import net.oktawia.crazyae2addons.misc.AppEngOptionalMobFilteredFakeSlot;

public class MobFormationPlaneMenu extends UpgradeableMenu<MobFormationPlane> {

    public MobFormationPlaneMenu(int id, Inventory ip,
                              MobFormationPlane host) {
        super(CrazyMenuRegistrar.MOB_FORMATION_PLANE_MENU.get(), id, ip, host);
    }

    public void createConfig(int rows, int cols, int optionalRows){
        var inv = getHost().getConfig().createMenuWrapper();

        for (int y = 0; y < rows + optionalRows; y++) {
            for (int x = 0; x < cols; x++) {
                int invIdx = y * cols + x;
                if (y < rows) {
                    this.addSlot(new AppEngMobFilteredFakeSlot(inv, invIdx), SlotSemantics.CONFIG);
                } else {
                    this.addSlot(new AppEngOptionalMobFilteredFakeSlot(inv, this, invIdx, y - rows), SlotSemantics.CONFIG);
                }
            }
        }

    }

    @Override
    protected void setupConfig() {
        createConfig(2, 9, 5);
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        final int upgrades = getUpgrades().getInstalledUpgrades(AEItems.CAPACITY_CARD);
        return upgrades > idx;
    }
}
