package net.oktawia.crazyae2addons.menus;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.GenericStack;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.RestrictedInputSlot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.wct.WCTMenuHost;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.WirelessEmitterTerminalItemLogicHost;
import net.oktawia.crazyae2addons.misc.StorageEmitterInfoAdapter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WirelessEmitterTerminalMenu extends UpgradeableMenu<WirelessEmitterTerminalItemLogicHost> {

    private static final String ACTION_SEARCH = "search";
    private static final String ACTION_SET_VALUE = "setValue";
    private static final int MAX_CONFIG_SLOTS = 1024;

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(EmitterTerminalMenu.StorageEmitterInfo.class, new StorageEmitterInfoAdapter())
            .create();

    public static final MenuType<WirelessEmitterTerminalMenu> TYPE =
            MenuTypeBuilder.create(WirelessEmitterTerminalMenu::new, WirelessEmitterTerminalItemLogicHost.class)
                    .build("wireless_emitter_terminal");

    @GuiSync(625)
    public String emitters = "[]";

    private final WirelessEmitterTerminalItemLogicHost host;
    private String currentSearch = "";

    public WirelessEmitterTerminalMenu(int id, Inventory ip, WirelessEmitterTerminalItemLogicHost host) {
        super(CrazyMenuRegistrar.WIRELESS_EMITTER_TERMINAL_MENU.get(), id, ip, host);
        this.host = host;

        registerClientAction(ACTION_SEARCH, String.class, this::search);
        registerClientAction(ACTION_SET_VALUE, String.class, this::setValue);

        if (!isClientSide()) {
            refreshEmitters();
        }

        this.addSlot(
                new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                        this.host.getSubInventory(WCTMenuHost.INV_SINGULARITY), 0),
                AE2wtlibSlotSemantics.SINGULARITY
        );
    }

    @Override
    protected void setupConfig() {
        var inv = new EmitterConfigInventory();
        for (int i = 0; i < MAX_CONFIG_SLOTS; i++) {
            this.addSlot(new FakeSlot(inv, i), SlotSemantics.CONFIG);
        }
    }

    @Override
    public void onSlotChange(Slot s) {
        super.onSlotChange(s);
        if (!isClientSide()) {
            refreshEmitters();
        }
    }

    public void search(String search) {
        if (isClientSide()) {
            sendClientAction(ACTION_SEARCH, search);
            return;
        }
        this.currentSearch = search == null ? "" : search;
        refreshEmitters();
    }

    public void setValue(String payload) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_VALUE, payload);
            return;
        }
        if (payload == null) return;

        String[] parts = payload.split("\\|", 2);
        if (parts.length != 2 || parts[0].isBlank()) return;

        long value = 0L;
        if (!parts[1].isBlank()) {
            try {
                value = Long.parseLong(parts[1].trim());
            } catch (NumberFormatException ignored) {
                return;
            }
        }

        host.setEmitterValue(parts[0], value);
        refreshEmitters();
    }

    private void refreshEmitters() {
        if (isClientSide()) return;

        List<EmitterTerminalMenu.StorageEmitterInfo> list;
        if (currentSearch == null || currentSearch.isBlank()) {
            list = host.getEmitters();
        } else {
            list = host.getEmitters(currentSearch);
        }
        this.emitters = GSON.toJson(list);
    }

    private List<EmitterTerminalMenu.StorageEmitterInfo> getSyncedEmitters() {
        if (this.emitters == null || this.emitters.isBlank()) return List.of();
        List<EmitterTerminalMenu.StorageEmitterInfo> list = GSON.fromJson(
                this.emitters,
                new TypeToken<List<EmitterTerminalMenu.StorageEmitterInfo>>() {}.getType()
        );
        return list != null ? list : List.of();
    }

    private List<EmitterTerminalMenu.StorageEmitterInfo> getCurrentEmitterList() {
        if (isClientSide()) return getSyncedEmitters();
        if (currentSearch == null || currentSearch.isBlank()) return host.getEmitters();
        return host.getEmitters(currentSearch);
    }

    @Nullable
    private EmitterTerminalMenu.StorageEmitterInfo getEmitterForConfigSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_CONFIG_SLOTS) return null;
        List<EmitterTerminalMenu.StorageEmitterInfo> list = getCurrentEmitterList();
        if (slotIndex >= list.size()) return null;
        return list.get(slotIndex);
    }

    public boolean isWUT() {
        return this.host.getItemStack().getItem() instanceof ItemWUT;
    }

    private final class EmitterConfigInventory implements InternalInventory {

        @Override
        public int size() {
            return MAX_CONFIG_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            var emitter = getEmitterForConfigSlot(slotIndex);
            if (emitter == null || emitter.config() == null) return ItemStack.EMPTY;
            return GenericStack.wrapInItemStack(emitter.config());
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            if (isClientSide()) return;

            var emitter = getEmitterForConfigSlot(slotIndex);
            if (emitter == null) return;

            GenericStack generic = stack.isEmpty() ? null : GenericStack.fromItemStack(stack);
            host.setEmitterConfig(emitter.uuid(), generic);
            refreshEmitters();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.isEmpty() || GenericStack.fromItemStack(stack) != null;
        }

        @Override
        public void sendChangeNotification(int slot) {
            if (!isClientSide()) refreshEmitters();
        }
    }
}
