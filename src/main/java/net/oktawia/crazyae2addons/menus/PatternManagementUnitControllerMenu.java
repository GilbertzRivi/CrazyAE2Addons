package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.RestrictedInputSlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.PatternManagementUnitControllerBE;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.UpdatePatternsPacket;

import java.util.ArrayList;
import java.util.List;

public class PatternManagementUnitControllerMenu extends AEBaseMenu {

    private static final String SYNC = "patternSync";
    private static final int COLS = 9;
    private static final int VISIBLE_ROWS = 4;

    public static final int ROWS = CrazyConfig.COMMON.PatternUnitCapacity.get();

    public String PREVIEW = "actionPrev";

    @GuiSync(893) public boolean preview;
    @GuiSync(894) public Integer slotNum;

    private final PatternManagementUnitControllerBE host;
    private final Player player;

    public PatternManagementUnitControllerMenu(int id, Inventory ip, PatternManagementUnitControllerBE host) {
        super(CrazyMenuRegistrar.PATTERN_MANAGEMENT_UNIT_CONTROLLER_MENU.get(), id, ip, host);
        this.host = host;
        this.player = ip.player;

        for (int i = 0; i < host.inv.size(); i++) {
            this.addSlot(
                    new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, host.inv, i),
                    SlotSemantics.ENCODED_PATTERN
            );
        }

        this.createPlayerInventorySlots(ip);

        this.preview = host.preview;
        this.slotNum = host.inv.size();

        this.registerClientAction(PREVIEW, Boolean.class, this::changePreview);
        this.registerClientAction(SYNC, Integer.class, this::handleRequestUpdate);
    }

    public void changePreview(Boolean preview) {
        host.preview = preview;
        this.preview = preview;
        if (isClientSide()){
            sendClientAction(PREVIEW, preview);
        }
    }

    private void handleRequestUpdate(int startRow) {
        if (isClientSide()) {
            return;
        }

        int startIndex = Math.max(0, Math.min(slotNum - 1, startRow * COLS));
        int count = Math.min(VISIBLE_ROWS * COLS, Math.max(0, slotNum - startIndex));

        var inventory = this.host.inv;
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
