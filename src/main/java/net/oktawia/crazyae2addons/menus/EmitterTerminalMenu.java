package net.oktawia.crazyae2addons.menus;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.GenericStack;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.FakeSlot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.misc.StorageEmitterInfoAdapter;
import net.oktawia.crazyae2addons.parts.EmitterTerminalPart;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EmitterTerminalMenu extends UpgradeableMenu<EmitterTerminalPart> {

    public record StorageEmitterInfo(String uuid, Component name, GenericStack config, Long value) { }

    private static final String ACTION_SEARCH = "search";
    private static final String ACTION_SET_VALUE = "setValue";

    private static final int MAX_CONFIG_SLOTS = 1024;

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(StorageEmitterInfo.class, new StorageEmitterInfoAdapter())
            .create();

    @GuiSync(624)
    public String emitters = "[]";

    private final EmitterTerminalPart host;
    private String currentSearch = "";

    public EmitterTerminalMenu(int id, Inventory ip, EmitterTerminalPart host) {
        super(CrazyMenuRegistrar.EMITTER_TERMINAL_MENU.get(), id, ip, host);
        this.host = host;

        registerClientAction(ACTION_SEARCH, String.class, this::search);
        registerClientAction(ACTION_SET_VALUE, String.class, this::setValue);

        if (!isClientSide()) {
            refreshEmitters();
        }
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
            getHost().getHost().markForSave();
            getHost().getHost().markForUpdate();
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

        if (payload == null) {
            return;
        }

        String[] parts = payload.split("\\|", 2);
        if (parts.length != 2 || parts[0].isBlank()) {
            return;
        }

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
        if (isClientSide()) {
            return;
        }

        List<StorageEmitterInfo> list;

        if (currentSearch == null || currentSearch.isBlank()) {
            list = host.getEmitters();
        } else {
            list = host.getEmitters(currentSearch);
        }

        this.emitters = GSON.toJson(list);
    }

    private List<StorageEmitterInfo> getSyncedEmitters() {
        if (this.emitters == null || this.emitters.isBlank()) {
            return List.of();
        }

        List<StorageEmitterInfo> list = GSON.fromJson(
                this.emitters,
                new TypeToken<List<StorageEmitterInfo>>() {}.getType()
        );

        return list != null ? list : List.of();
    }

    private List<StorageEmitterInfo> getCurrentEmitterList() {
        if (isClientSide()) {
            return getSyncedEmitters();
        }

        if (currentSearch == null || currentSearch.isBlank()) {
            return host.getEmitters();
        } else {
            return host.getEmitters(currentSearch);
        }
    }

    @Nullable
    private StorageEmitterInfo getEmitterForConfigSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_CONFIG_SLOTS) {
            return null;
        }

        List<StorageEmitterInfo> list = getCurrentEmitterList();
        if (slotIndex >= list.size()) {
            return null;
        }

        return list.get(slotIndex);
    }

    private final class EmitterConfigInventory implements InternalInventory {

        @Override
        public int size() {
            return MAX_CONFIG_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            var emitter = getEmitterForConfigSlot(slotIndex);
            if (emitter == null || emitter.config() == null) {
                return ItemStack.EMPTY;
            }

            return GenericStack.wrapInItemStack(emitter.config());
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            if (isClientSide()) {
                return;
            }

            var emitter = getEmitterForConfigSlot(slotIndex);
            if (emitter == null) {
                return;
            }

            GenericStack generic = stack.isEmpty() ? null : GenericStack.fromItemStack(stack);
            host.setEmitterConfig(emitter.uuid(), generic);

            getHost().getHost().markForSave();
            getHost().getHost().markForUpdate();
            refreshEmitters();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.isEmpty() || GenericStack.fromItemStack(stack) != null;
        }

        @Override
        public void sendChangeNotification(int slot) {
            if (!isClientSide()) {
                refreshEmitters();
            }
        }
    }
}