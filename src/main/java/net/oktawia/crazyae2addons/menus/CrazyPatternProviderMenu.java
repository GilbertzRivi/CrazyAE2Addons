package net.oktawia.crazyae2addons.menus;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.PatternProviderMenu;
import appeng.menu.slot.RestrictedInputSlot;
import lombok.Getter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.CrazyPatternProviderBE;
import net.oktawia.crazyae2addons.parts.CrazyPatternProviderPart;

public class CrazyPatternProviderMenu extends PatternProviderMenu {

    @Getter
    private final PatternProviderLogicHost host;

    @GuiSync(38)
    public Integer slotNum;

    public CrazyPatternProviderMenu(int id, Inventory ip, PatternProviderLogicHost host) {
        super(CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(), id, ip, host);
        this.host = host;

        this.slotNum = host.getLogic().getPatternInv().size();

        if (!IsModLoaded.APP_FLUX) {
            appeng.api.upgrades.IUpgradeInventory upgradeInv = null;
            if (host.getBlockEntity() instanceof CrazyPatternProviderBE crazyBE) {
                upgradeInv = crazyBE.getUpgrades();
            } else if (host instanceof CrazyPatternProviderPart crazyPart) {
                upgradeInv = crazyPart.getUpgrades();
            }

            if (upgradeInv != null) {
                for (int i = 0; i < upgradeInv.size(); i++) {
                    var slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.UPGRADES, upgradeInv, i);
                    slot.setNotDraggable();
                    this.addSlot(slot, SlotSemantics.UPGRADE);
                }
            }
        }
    }
}