package net.oktawia.crazyae2addons.menus;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.PatternProviderMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.CrazyPatternProviderBE;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.UpdatePatternsPacket;

import java.util.ArrayList;
import java.util.List;

public class CrazyPatternProviderMenu extends PatternProviderMenu {

    private static final String SYNC = "patternSync";
    private static final int COLS = 9;
    private static final int VISIBLE_ROWS = 4;

    private final PatternProviderLogicHost host;
    private final Player player;

    @GuiSync(38)
    public Integer slotNum;

    public CrazyPatternProviderMenu(int id, Inventory ip, PatternProviderLogicHost host) {
        super(CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(), id, ip, host);
        this.host = host;
        this.player = ip.player;

        if (host.getBlockEntity() instanceof CrazyPatternProviderBE crazyBE) {
            this.slotNum = crazyBE.getAdded() * COLS + (8 * COLS);
        } else {
            this.slotNum = 8 * COLS;
        }

        registerClientAction(SYNC, Integer.class, this::handleRequestUpdate);
    }

    private void handleRequestUpdate(int startRow) {
        if (isClientSide()) {
            return;
        }
        int startIndex = Math.max(0, Math.min(slotNum - 1, startRow * COLS));
        int count = Math.min(VISIBLE_ROWS * COLS, Math.max(0, slotNum - startIndex));

        var inventory = this.host.getLogic().getPatternInv();
        List<ItemStack> visibleStacks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            visibleStacks.add(inventory.getStackInSlot(startIndex + i));
        }

        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                new UpdatePatternsPacket(startIndex, visibleStacks)
        );
    }

    public void requestUpdate(int startRow) {
        if (isClientSide()) {
            sendClientAction(SYNC, startRow);
        } else {
            handleRequestUpdate(startRow);
        }
    }
}
