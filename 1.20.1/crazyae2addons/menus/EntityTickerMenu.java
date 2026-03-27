package net.oktawia.crazyae2addons.menus;

import appeng.core.definitions.AEItems;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.OptionalFakeSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.parts.EntityTickerPart;
import net.oktawia.crazyae2addons.screens.EntityTickerScreen;

public class EntityTickerMenu extends UpgradeableMenu<EntityTickerPart> {
    @GuiSync(72)
    public int upgradeNum;

    private String ACTION_SEND_UPGRADE_NUM = "actionSendUpgradeNum";

    public EntityTickerMenu(
            int id, Inventory ip, EntityTickerPart host) {
        super(CrazyMenuRegistrar.ENTITY_TICKER_MENU.get(), id, ip, host);
        getHost().menu = this;
        registerClientAction(ACTION_SEND_UPGRADE_NUM, Integer.class, this::sendUpgradeNum);
        upgradeNum = getHost().getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD);
    }

    public void sendUpgradeNum(int num){
        this.upgradeNum = num;
        if (isClientSide()){
            if (Minecraft.getInstance().screen instanceof EntityTickerScreen screen) {
                screen.refreshGui();
            }
            sendClientAction(ACTION_SEND_UPGRADE_NUM, num);
        }
        else{
            broadcastChanges();
        }
    }

    @Override
    public void broadcastChanges(){
        for (Object o : this.slots) {
            if (o instanceof OptionalFakeSlot fs) {
                if (!fs.isSlotEnabled() && !fs.getDisplayStack().isEmpty()) {
                    fs.clearStack();
                }
            }
        }
        this.standardDetectAndSendChanges();
    }
}