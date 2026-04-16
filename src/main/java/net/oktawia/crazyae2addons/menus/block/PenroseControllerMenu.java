package net.oktawia.crazyae2addons.menus.block;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.RestrictedInputSlot;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.PenroseControllerBE;

public class PenroseControllerMenu extends AEBaseMenu {

    private final PenroseControllerBE host;

    public final RestrictedInputSlot diskSlot;

    public static final String START   = "actionStartBh";
    public static final String PREVIEW = "actionPrev";

    @GuiSync(0)  public boolean blackHoleActive;
    @GuiSync(1)  public long    bhMass;
    @GuiSync(2)  public double  heat;
    @GuiSync(3)  public long    storedEnergy;
    @GuiSync(4)  public long    massDeltaPerSec;
    @GuiSync(5)  public long    diskMassSingu;
    @GuiSync(6)  public int     diskFlowSinguPerTick;
    @GuiSync(7)  public int     accretionSinguPerTick;
    @GuiSync(8)  public int     orbitDelaySec;
    @GuiSync(9)  public long    storedEnergyInDisk;
    @GuiSync(10) public long    feGeneratedGrossPerTick;
    @GuiSync(11) public long    feConsumedPerTick;
    @GuiSync(12) public long    initialBhMass;
    @GuiSync(13) public long    maxBhMass;
    @GuiSync(14) public double  maxHeatGK;

    public PenroseControllerMenu(int id, Inventory inv, PenroseControllerBE host) {
        super(CrazyMenuRegistrar.PENROSE_CONTROLLER_MENU.get(), id, inv, host);
        this.host = host;
        this.createPlayerInventorySlots(inv);

        this.addSlot(this.diskSlot = new RestrictedInputSlot(
                        RestrictedInputSlot.PlacableItemType.STORAGE_CELLS, host.diskInv, 0),
                SlotSemantics.STORAGE_CELL);

        this.registerClientAction(START,   this::startBlackHole);
        this.registerClientAction(PREVIEW, Boolean.class, this::changePreview);

        syncFromHost();
    }

    private void syncFromHost() {
        this.blackHoleActive        = host.isBlackHoleActive();
        this.bhMass                 = host.getBlackHoleMass();
        this.heat                   = host.getHeat();
        this.storedEnergy           = host.getStoredEnergy();
        this.massDeltaPerSec        = host.getLastSecondMassDelta();
        this.diskMassSingu          = host.getDiskMassSingu();
        this.diskFlowSinguPerTick   = host.getDiskFlowSinguPerTick();
        this.accretionSinguPerTick  = host.getLastAccretionSinguPerTick();
        this.orbitDelaySec          = host.getOrbitDelaySeconds();
        this.storedEnergyInDisk     = host.getStoredEnergyInDisk();
        this.feGeneratedGrossPerTick = host.getLastGeneratedFePerTickGross();
        this.feConsumedPerTick      = host.getLastConsumedFePerTick();
        this.initialBhMass          = host.getInitialBhMass();
        this.maxBhMass              = host.getMaxBhMass();
        this.maxHeatGK              = host.getMaxHeatGK();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!isClientSide()) {
            syncFromHost();
        }
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
        }
    }
}
