package net.oktawia.crazyae2addons.menus;

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

    public static final String START = "actionStartBh";

    @GuiSync(0) public boolean blackHoleActive;
    @GuiSync(1) public long bhMass;
    @GuiSync(2) public double heat;
    @GuiSync(3) public long storedEnergy;
    @GuiSync(4) public long massDeltaPerSec;

    // telemetry dysku
    @GuiSync(5) public long diskMassSingu;
    @GuiSync(6) public int diskFlowSinguPerTick;
    @GuiSync(7) public int accretionSinguPerTick;
    @GuiSync(8) public int orbitDelaySec;

    @GuiSync(9)  public long storedEnergyInDisk;

    // >>> nowe:
    @GuiSync(10) public long feGeneratedGrossPerTick;
    @GuiSync(11) public long feConsumedPerTick;

    public PenroseControllerMenu(int id, Inventory ip, PenroseControllerBE host) {
        super(CrazyMenuRegistrar.PENROSE_CONTROLLER_MENU.get(), id, ip, host);
        this.createPlayerInventorySlots(ip);
        this.host = host;

        this.blackHoleActive = host.isBlackHoleActive();
        this.bhMass = host.getBlackHoleMass();
        this.heat = host.getHeat();
        this.storedEnergy = host.getStoredEnergy();
        this.massDeltaPerSec = host.getLastSecondMassDelta();

        this.diskMassSingu = host.getDiskMassSingu();
        this.diskFlowSinguPerTick = host.getDiskFlowSinguPerTick();
        this.accretionSinguPerTick = host.getLastAccretionSinguPerTick();
        this.orbitDelaySec = host.getOrbitDelaySeconds();

        this.storedEnergyInDisk = host.getStoredEnergyInDisk();
        this.feGeneratedGrossPerTick = host.getLastGeneratedFePerTickGross();
        this.feConsumedPerTick = host.getLastConsumedFePerTick();

        this.addSlot(this.diskSlot = new RestrictedInputSlot(
                        RestrictedInputSlot.PlacableItemType.STORAGE_CELLS, host.diskInv, 0),
                SlotSemantics.STORAGE_CELL);

        this.registerClientAction(START, this::startBlackHole);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!isClientSide()) {
            this.blackHoleActive = host.isBlackHoleActive();
            this.bhMass = host.getBlackHoleMass();
            this.heat = host.getHeat();
            this.storedEnergy = host.getStoredEnergy();
            this.massDeltaPerSec = host.getLastSecondMassDelta();

            this.diskMassSingu = host.getDiskMassSingu();
            this.diskFlowSinguPerTick = host.getDiskFlowSinguPerTick();
            this.accretionSinguPerTick = host.getLastAccretionSinguPerTick();
            this.orbitDelaySec = host.getOrbitDelaySeconds();

            this.storedEnergyInDisk = host.getStoredEnergyInDisk();
            this.feGeneratedGrossPerTick = host.getLastGeneratedFePerTickGross();
            this.feConsumedPerTick = host.getLastConsumedFePerTick();
        }
    }

    public void startBlackHole() {
        host.startBlackHole();
        if (isClientSide()) {
            sendClientAction(START);
        }
    }
}
