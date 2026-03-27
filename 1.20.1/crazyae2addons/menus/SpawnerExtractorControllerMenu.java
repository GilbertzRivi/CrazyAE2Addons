package net.oktawia.crazyae2addons.menus;

import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.EntropyCradleControllerBE;
import net.oktawia.crazyae2addons.entities.SpawnerExtractorControllerBE;


public class SpawnerExtractorControllerMenu extends UpgradeableMenu<SpawnerExtractorControllerBE> {

    private final SpawnerExtractorControllerBE host;
    public String PREVIEW = "actionPrev";
    @GuiSync(893)
    public boolean preview;

    public SpawnerExtractorControllerMenu(int id, Inventory ip, SpawnerExtractorControllerBE host) {
        super(CrazyMenuRegistrar.SPAWNER_EXTRACTOR_CONTROLLER_MENU.get(), id, ip, host);
        this.host = host;
        this.preview = host.preview;
        this.registerClientAction(PREVIEW, Boolean.class, this::changePreview);
    }

    public void changePreview(Boolean preview) {
        host.preview = preview;
        this.preview = preview;
        if (isClientSide()){
            sendClientAction(PREVIEW, preview);
        }
    }
}
