package net.oktawia.crazyae2addons.logic.wormhole;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.hooks.ticking.TickHandler;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.parts.p2p.WormholeP2PTunnelPart;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class WormholeP2PConnectionManager {

    private static final Set<BlockPos> UPDATE_BLACKLIST = new HashSet<>();

    private final WormholeP2PTunnelPart part;
    private final Map<WormholeP2PTunnelPart, IGridConnection> connections = new IdentityHashMap<>();

    private ConnectionUpdate pendingUpdate = ConnectionUpdate.NONE;

    public void schedule(boolean online) {
        pendingUpdate = online ? ConnectionUpdate.CONNECT : ConnectionUpdate.DISCONNECT;
    }

    public boolean hasConnections() {
        return !connections.isEmpty();
    }

    public void disconnectAll() {
        for (var connection : connections.values()) {
            connection.destroy();
        }
        connections.clear();
        pendingUpdate = ConnectionUpdate.NONE;
    }

    public void updateConnections() {
        var operation = pendingUpdate;
        pendingUpdate = ConnectionUpdate.NONE;

        var mainGrid = part.getMainNode().getGrid();

        if (part.isOutput()) {
            operation = ConnectionUpdate.DISCONNECT;
        } else if (mainGrid == null) {
            operation = ConnectionUpdate.DISCONNECT;
        }

        if (operation == ConnectionUpdate.DISCONNECT) {
            disconnectAll();
            return;
        }

        var outputs = part.getOutputs();

        Iterator<Map.Entry<WormholeP2PTunnelPart, IGridConnection>> it = connections.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            var output = entry.getKey();
            var connection = entry.getValue();

            if (output.getMainNode().getGrid() != mainGrid
                    || !output.getMainNode().isOnline()
                    || !outputs.contains(output)) {
                connection.destroy();
                it.remove();
            }
        }

        for (var output : outputs) {
            if (!output.getMainNode().isOnline() || connections.containsKey(output)) {
                continue;
            }

            IGridNode a = part.getExternalFacingNode();
            IGridNode b = output.getExternalFacingNode();
            IGridConnection connection = GridHelper.createConnection(a, b);
            connections.put(output, connection);
        }
    }

    public void sendBlockUpdateToOppositeSide() {
        var world = part.getLevel();
        if (world == null || world.isClientSide) {
            return;
        }

        if (part.isOutput()) {
            var input = part.getInput();
            if (input != null && input.getHost() != null) {
                var be = input.getHost().getBlockEntity();
                var pos = be.getBlockPos().relative(input.getSide());
                sendNeighborUpdatesAt(be.getLevel(), pos, input.getSide());
            }
        } else {
            for (var output : part.getOutputs()) {
                if (output.getHost() != null) {
                    var be = output.getHost().getBlockEntity();
                    var pos = be.getBlockPos().relative(output.getSide());
                    sendNeighborUpdatesAt(be.getLevel(), pos, output.getSide());
                }
            }
        }
    }

    private void sendNeighborUpdatesAt(Level level, BlockPos pos, Direction facing) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (UPDATE_BLACKLIST.contains(pos)) {
            return;
        }

        UPDATE_BLACKLIST.add(pos);
        TickHandler.instance().addCallable(level, UPDATE_BLACKLIST::clear);

        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
        level.updateNeighborsAt(pos, state.getBlock());

        BlockPos neighbor = pos.relative(facing.getOpposite());
        BlockState neighborState = level.getBlockState(neighbor);
        level.updateNeighborsAt(neighbor, neighborState.getBlock());
    }

    private enum ConnectionUpdate {
        NONE,
        DISCONNECT,
        CONNECT
    }
}