// MobKeySelectorMenu.java
package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.MobKeySelectorItem;
import net.oktawia.crazyae2addons.logic.MobKeySelectorHost;

public class MobKeySelectorMenu extends AEBaseMenu {
    @GuiSync(100) public String selectedKey;

    public final String CHOOSE = "chooseMobKey";
    public final MobKeySelectorHost host;

    public MobKeySelectorMenu(int id, Inventory ip, ItemMenuHost host) {
        super(CrazyMenuRegistrar.MOB_KEY_SELECTOR_MENU.get(), id, ip, host);
        this.host = (MobKeySelectorHost) host;
        this.host.setMenu(this);

        this.selectedKey = MobKeySelectorItem.getSelectedKeyId(this.host.getItemStack());

        registerClientAction(CHOOSE, String.class, this::choose);
        createPlayerInventorySlots(ip);
    }

    public void choose(String keyId) {
        this.selectedKey = keyId;
        ItemStack stack = host.getItemStack();
        MobKeySelectorItem.setSelectedKeyId(stack, keyId);
        if (isClientSide()) {
            sendClientAction(CHOOSE, keyId);
        }
        broadcastChanges();
    }
}
