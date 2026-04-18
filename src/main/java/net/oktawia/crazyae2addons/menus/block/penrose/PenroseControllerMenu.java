package net.oktawia.crazyae2addons.menus.block.penrose;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.RestrictedInputSlot;
import lombok.Getter;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.penrose.PenroseControllerBE;

public class PenroseControllerMenu extends AEBaseMenu {

    @Getter
    private final PenroseControllerBE host;
    public final RestrictedInputSlot diskSlot;

    public static final String START = "actionStartBh";
    public static final String PREVIEW = "actionPrev";

    public PenroseControllerMenu(int id, Inventory inv, PenroseControllerBE host) {
        super(CrazyMenuRegistrar.PENROSE_CONTROLLER_MENU.get(), id, inv, host);
        this.host = host;

        this.createPlayerInventorySlots(inv);

        this.addSlot(
                this.diskSlot = new RestrictedInputSlot(
                        RestrictedInputSlot.PlacableItemType.STORAGE_CELLS,
                        host.getDiskInv(),
                        0
                ),
                SlotSemantics.STORAGE_CELL
        );

        this.registerClientAction(START, this::startBlackHole);
        this.registerClientAction(PREVIEW, Boolean.class, this::changePreview);
    }

    public void startBlackHole() {
        if (isClientSide()) {
            sendClientAction(START);
        } else {
            host.startBlackHole();
        }
    }

    public void changePreview(Boolean preview) {
        if (isClientSide()) {
            sendClientAction(PREVIEW, preview);
        } else {
            host.setPreviewEnabled(preview != null && preview);
        }
    }
}