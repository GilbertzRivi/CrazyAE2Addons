package net.oktawia.crazyae2addons.menus.part;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.IRedstoneTerminalHost;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.RedstoneWindowPacket;
import net.oktawia.crazyae2addons.parts.RedstoneTerminal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class RedstoneTerminalMenu extends AEBaseMenu {

    public record EmitterInfo(BlockPos pos, String name, boolean active) {}

    private static final String ACTION_TOGGLE = "redstone_terminal.toggle";
    private static final String ACTION_SEARCH = "redstone_terminal.search";
    private static final String ACTION_SCROLL = "redstone_terminal.scroll";

    public static final int VISIBLE_ROWS = 7;

    @GuiSync(625)
    public int totalCount = 0;

    public List<EmitterInfo> clientWindow = List.of();
    public int clientWindowOffset = 0;
    public int clientWindowRevision = 0;

    protected IRedstoneTerminalHost redstoneHost;

    private String currentSearch = "";
    private int currentOffset = 0;
    private int windowRevision = 0;

    private List<EmitterInfo> fullList = List.of();

    public RedstoneTerminalMenu(int id, Inventory ip, RedstoneTerminal host) {
        this(CrazyMenuRegistrar.REDSTONE_TERMINAL_MENU.get(), id, ip, host);
    }

    protected RedstoneTerminalMenu(MenuType<?> type, int id, Inventory ip, IRedstoneTerminalHost host) {
        super(type, id, ip, host);
        this.redstoneHost = host;

        registerClientAction(ACTION_TOGGLE, String.class, this::toggle);
        registerClientAction(ACTION_SEARCH, String.class, this::search);
        registerClientAction(ACTION_SCROLL, Integer.class, this::onScroll);

        if (host instanceof IUpgradeableObject upgradeableObject) {
            setupUpgrades(upgradeableObject.getUpgrades());
        }

        createPlayerInventorySlots(ip);

        if (!isClientSide()) {
            refreshList();
        }
    }

    public void search(String search) {
        if (isClientSide()) {
            sendClientAction(ACTION_SEARCH, search);
            return;
        }

        this.currentSearch = search == null ? "" : search;
        this.currentOffset = 0;
        refreshList();
    }

    public void toggle(String name) {
        if (isClientSide()) {
            sendClientAction(ACTION_TOGGLE, name);
            return;
        }

        redstoneHost.toggle(name);
        refreshList();
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

    public void applyClientWindow(RedstoneWindowPacket packet) {
        this.totalCount = packet.totalCount();
        this.clientWindow = packet.window();
        this.clientWindowOffset = packet.windowOffset();
        this.clientWindowRevision = packet.revision();
    }

    protected void refreshList() {
        if (isClientSide()) {
            return;
        }

        List<EmitterInfo> raw = currentSearch.isBlank()
                ? redstoneHost.getEmitters()
                : redstoneHost.getEmitters(currentSearch);

        this.fullList = deduplicateByName(raw);
        this.totalCount = this.fullList.size();

        clampOffset();
        sendWindow();
    }

    private List<EmitterInfo> deduplicateByName(List<EmitterInfo> raw) {
        LinkedHashMap<String, EmitterInfo> map = new LinkedHashMap<>();
        for (EmitterInfo info : raw) {
            map.putIfAbsent(info.name(), info);
        }
        return new ArrayList<>(map.values());
    }

    private void clampOffset() {
        if (fullList.isEmpty()) {
            currentOffset = 0;
            return;
        }

        currentOffset = Math.min(currentOffset, Math.max(0, fullList.size() - VISIBLE_ROWS));
    }

    private void sendWindow() {
        if (!(getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        int from = currentOffset;
        int to = Math.min(from + VISIBLE_ROWS, fullList.size());

        List<EmitterInfo> window = from < fullList.size()
                ? new ArrayList<>(fullList.subList(from, to))
                : List.of();

        int revision = ++windowRevision;

        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new RedstoneWindowPacket(fullList.size(), from, revision, window)
        );
    }
}