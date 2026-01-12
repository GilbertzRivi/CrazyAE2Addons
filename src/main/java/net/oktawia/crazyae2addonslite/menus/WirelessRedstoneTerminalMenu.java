package net.oktawia.crazyae2addonslite.menus;

import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.RestrictedInputSlot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.wct.WCTMenuHost;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addonslite.logic.WirelessRedstoneTerminalItemLogicHost;
import net.oktawia.crazyae2addonslite.misc.BlockPosAdapter;

public class WirelessRedstoneTerminalMenu extends UpgradeableMenu<WirelessRedstoneTerminalItemLogicHost> {
    public String TOGGLE = "syncToggle";
    public String SEARCH = "syncSearch";
    public WirelessRedstoneTerminalItemLogicHost host;
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BlockPos.class, new BlockPosAdapter())
            .create();

    public static final MenuType<WirelessRedstoneTerminalMenu> TYPE =
            MenuTypeBuilder.create(WirelessRedstoneTerminalMenu::new, WirelessRedstoneTerminalItemLogicHost.class)
                    .build("wireless_redstone_terminal");

    @GuiSync(231)
    public String emitters;

    public WirelessRedstoneTerminalMenu(int id, Inventory ip, WirelessRedstoneTerminalItemLogicHost host) {
        super(CrazyMenuRegistrar.WIRELESS_REDSTONE_TERMINAL_MENU.get(), id, ip, host);
        this.host = host;
        if (!isClientSide()){
            this.emitters = GSON.toJson(host.getEmitters());
        }
        registerClientAction(TOGGLE, String.class, this::toggle);
        registerClientAction(SEARCH, String.class, this::search);
        this.addSlot(
                new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.QE_SINGULARITY, this.host.getSubInventory(WCTMenuHost.INV_SINGULARITY), 0),
                AE2wtlibSlotSemantics.SINGULARITY
        );
    }

    public void search(String search) {
        if (isClientSide()){
            sendClientAction(SEARCH, search);
        } else {
            this.emitters = GSON.toJson(host.getEmitters(search));
        }
    }

    public void toggle(String name) {
        if (isClientSide()){
            sendClientAction(TOGGLE, name);
        } else {
            host.toggle(name);
        }
    }

    public boolean isWUT() {
        return this.host.getItemStack().getItem() instanceof ItemWUT;
    }
}
