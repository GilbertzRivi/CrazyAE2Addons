package net.oktawia.crazyae2addons.menus;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.FakeSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.network.EmitterWindowPacket;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.parts.EmitterTerminalPart;

import java.util.ArrayList;
import java.util.List;

public class EmitterTerminalMenu extends UpgradeableMenu<EmitterTerminalPart> {

    public record StorageEmitterInfo(String uuid, Component name, GenericStack config, Long value) {}

    private static final String ACTION_SEARCH = "search";
    private static final String ACTION_SET_VALUE = "setValue";
    private static final String ACTION_SCROLL = "scroll";

    public static final int VISIBLE_ROWS = 6;

    @GuiSync(624)
    public int totalEmitterCount = 0;

    // client-side window data
    public List<StorageEmitterInfo> clientWindow = List.of();
    public int clientWindowOffset = 0;
    public int clientWindowRevision = 0;

    private final EmitterTerminalPart host;
    private String currentSearch = "";
    private int currentOffset = 0;
    private int windowRevision = 0;

    private List<StorageEmitterInfo> fullList = List.of();
    private List<StorageEmitterInfo> serverWindow = List.of();

    private WindowedEmitterInventory windowedInventory;

    public EmitterTerminalMenu(int id, Inventory ip, EmitterTerminalPart host) {
        super(CrazyMenuRegistrar.EMITTER_TERMINAL_MENU.get(), id, ip, host);
        this.host = host;

        registerClientAction(ACTION_SEARCH, String.class, this::search);
        registerClientAction(ACTION_SET_VALUE, String.class, this::setValue);
        registerClientAction(ACTION_SCROLL, Integer.class, this::onScroll);

        if (!isClientSide()) {
            refreshEmitters();
        }
    }

    @Override
    protected void setupConfig() {
        windowedInventory = new WindowedEmitterInventory();
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            this.addSlot(new FakeSlot(windowedInventory, i), SlotSemantics.CONFIG);
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
        this.currentOffset = 0;
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

    public void onScroll(int offset) {
        if (isClientSide()) {
            sendClientAction(ACTION_SCROLL, offset);
            return;
        }

        this.currentOffset = Math.max(0, offset);
        clampOffset();
        sendWindow();
    }

    public void applyClientWindow(EmitterWindowPacket pkt) {
        this.totalEmitterCount = pkt.totalCount();
        this.clientWindow = pkt.window();
        this.clientWindowOffset = pkt.windowOffset();
        this.clientWindowRevision = pkt.revision();

        if (this.windowedInventory != null) {
            this.windowedInventory.setClientCache(pkt.window());
        }
    }

    private void refreshEmitters() {
        if (isClientSide()) {
            return;
        }

        fullList = currentSearch.isBlank() ? host.getEmitters() : host.getEmitters(currentSearch);
        totalEmitterCount = fullList.size();
        clampOffset();
        sendWindow();
    }

    private void clampOffset() {
        if (fullList.isEmpty()) {
            currentOffset = 0;
        } else {
            currentOffset = Math.min(currentOffset, Math.max(0, fullList.size() - VISIBLE_ROWS));
        }
    }

    private void sendWindow() {
        if (!(getPlayer() instanceof ServerPlayer sp)) {
            return;
        }

        int from = currentOffset;
        int to = Math.min(from + VISIBLE_ROWS, fullList.size());

        serverWindow = from < fullList.size()
                ? new ArrayList<>(fullList.subList(from, to))
                : List.of();

        int revision = ++windowRevision;

        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> sp),
                new EmitterWindowPacket(fullList.size(), from, revision, serverWindow)
        );
    }

    private final class WindowedEmitterInventory implements InternalInventory {

        private final ItemStack[] clientCache = new ItemStack[VISIBLE_ROWS];

        WindowedEmitterInventory() {
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                clientCache[i] = ItemStack.EMPTY;
            }
        }

        void setClientCache(List<StorageEmitterInfo> window) {
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                if (i < window.size() && window.get(i).config() != null) {
                    clientCache[i] = GenericStack.wrapInItemStack(window.get(i).config());
                } else {
                    clientCache[i] = ItemStack.EMPTY;
                }
            }
        }

        @Override
        public int size() {
            return VISIBLE_ROWS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= VISIBLE_ROWS) {
                return ItemStack.EMPTY;
            }

            if (isClientSide()) {
                return clientCache[slot];
            }

            if (slot >= serverWindow.size()) {
                return ItemStack.EMPTY;
            }

            var emitter = serverWindow.get(slot);
            if (emitter == null || emitter.config() == null) {
                return ItemStack.EMPTY;
            }

            return GenericStack.wrapInItemStack(emitter.config());
        }

        @Override
        public void setItemDirect(int slot, ItemStack stack) {
            if (isClientSide()) {
                if (slot >= 0 && slot < VISIBLE_ROWS) {
                    clientCache[slot] = stack;
                }
                return;
            }

            if (slot < 0 || slot >= VISIBLE_ROWS) {
                return;
            }

            if (slot >= serverWindow.size()) {
                return;
            }

            var emitter = serverWindow.get(slot);
            if (emitter == null || emitter.uuid() == null || emitter.uuid().isBlank()) {
                return;
            }

            GenericStack generic = stack.isEmpty() ? null : convertStack(stack);
            host.setEmitterConfig(emitter.uuid(), generic);

            getHost().getHost().markForSave();
            getHost().getHost().markForUpdate();
            refreshEmitters();
        }

        private GenericStack convertStack(ItemStack stack) {
            if (stack.isEmpty()) {
                return null;
            }

            GenericStack unwrapped = GenericStack.fromItemStack(stack);
            if (unwrapped != null) {
                return unwrapped;
            }

            return new GenericStack(AEItemKey.of(stack), stack.getCount());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.isEmpty() || convertStack(stack) != null;
        }

        @Override
        public void sendChangeNotification(int slot) {
            if (!isClientSide()) {
                refreshEmitters();
            }
        }
    }
}