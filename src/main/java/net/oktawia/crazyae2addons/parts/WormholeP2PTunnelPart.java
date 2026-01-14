package net.oktawia.crazyae2addons.parts;

import java.util.*;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.parts.IPartCollisionHelper;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.me.service.P2PService;
import appeng.parts.p2p.P2PModels;
import appeng.parts.p2p.P2PTunnelPart;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.hooks.ticking.TickHandler;
import appeng.items.parts.PartModels;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.misc.CombinedEnergyStorage;
import net.oktawia.crazyae2addons.misc.CombinedFluidHandlerItem;
import net.oktawia.crazyae2addons.misc.FluidHandlerConcatenate;
import net.oktawia.crazyae2addons.misc.WormholeAnchor;
import net.oktawia.crazyae2addons.mixins.P2PTunnelPartAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.my_util.DQuaternion;

public class WormholeP2PTunnelPart extends P2PTunnelPart<WormholeP2PTunnelPart> implements IGridTickable, ICapabilityProvider {

    private static final P2PModels MODELS = new P2PModels(CrazyAddons.makeId("part/wormhole_p2p_tunnel"));
    private static final Set<BlockPos> wormholeUpdateBlacklist = new HashSet<>();
    private int redstonePower = 0;
    private boolean redstoneRecursive = false;
    public static final String PD_WORMHOLE = "crazyae2addonslite_wormhole";
    public static final String PD_HOST_POS = "hostPos";
    public static final String PD_HOST_DIM = "hostDim";
    public static final String PD_HOST_SIDE = "hostSide";
    private static final Map<ServerLevel, Set<WormholeP2PTunnelPart>> LOADED_PARTS = new WeakHashMap<>();
    private int portalMaintenanceCooldown = 0;
    private boolean intangibleCollision = false;

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    private ConnectionUpdate pendingUpdate = ConnectionUpdate.NONE;
    private final Map<WormholeP2PTunnelPart, IGridConnection> connections = new IdentityHashMap<>();

    protected final IManagedGridNode outerNode = CrazyConfig.COMMON.P2PWormholeNesting.get() ?
            GridHelper
                    .createManagedNode(this, NodeListener.INSTANCE)
                    .setTagName("outer")
                    .setInWorldNode(true)
                    .setFlags(GridFlags.DENSE_CAPACITY)
            :
            GridHelper
                    .createManagedNode(this, NodeListener.INSTANCE)
                    .setTagName("outer")
                    .setInWorldNode(true)
                    .setFlags(GridFlags.DENSE_CAPACITY, GridFlags.CANNOT_CARRY_COMPRESSED);

    public WormholeP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem);
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL, CrazyConfig.COMMON.P2PWormholeNesting.get() ? GridFlags.DENSE_CAPACITY : GridFlags.COMPRESSED_CHANNEL)
                .addService(IGridTickable.class, this);
    }

    private void readRedstoneInput() {
        var targetPos = getBlockEntity().getBlockPos().relative(getSide());
        var state = getLevel().getBlockState(targetPos);
        var block = state.getBlock();

        Direction inputSide = block instanceof RedStoneWireBlock ? Direction.UP : getSide();
        int newPower = block.getSignal(state, getLevel(), targetPos, inputSide);
        sendRedstoneToOutput(newPower);
    }

    private void sendRedstoneToOutput(int power) {
        int reducedPower = Math.max(0, power - 1);

        for (var output : getOutputs()) {
            output.receiveRedstoneInput(reducedPower);
        }
    }

    private void receiveRedstoneInput(int power) {
        if (redstoneRecursive) return;
        redstoneRecursive = true;

        if (isOutput() && getMainNode().isActive()) {
            if (this.redstonePower != power) {
                this.redstonePower = power;
                notifyRedstoneUpdate();
            }
        }

        redstoneRecursive = false;
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    @Override
    public int isProvidingStrongPower() {
        return isOutput() ? redstonePower : 0;
    }

    @Override
    public int isProvidingWeakPower() {
        return isOutput() ? redstonePower : 0;
    }

    private void notifyRedstoneUpdate() {
        var world = getLevel();
        var pos = getBlockEntity().getBlockPos();
        if (world != null) {
            Platform.notifyBlocksOfNeighbors(world, pos);
            Platform.notifyBlocksOfNeighbors(world, pos.relative(getSide()));
        }
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (isClientSide()) return true;
        if (hand == InteractionHand.OFF_HAND) return false;

        var is = player.getItemInHand(hand);
        if (!is.isEmpty() && is.getItem() instanceof IMemoryCard mc) {
            var configData = mc.getData(is);
            if (configData.contains("p2pType") || configData.contains("p2pFreq") || !configData.contains("wormhole")) {
                mc.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
                return false;
            } else {
                this.importSettings(SettingsFrom.MEMORY_CARD, configData, player);
                mc.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                return true;
            }
        }

        var level = getLevel();
        if (!(player instanceof ServerPlayer sp) || level == null) return false;

        BlockPos targetPos;
        Direction hitFace;
        ServerLevel targetWorld = null;

        if (isOutput()) {
            var input = getInput();
            if (input != null && input.getHost() != null) {
                var remoteHost = input.getHost().getBlockEntity();
                targetPos = remoteHost.getBlockPos().relative(input.getSide());
                hitFace = input.getSide().getOpposite();
                targetWorld = (ServerLevel) remoteHost.getLevel();
            } else {
                targetPos = null;
                hitFace = null;
            }
        } else {
            var outs = getOutputs();
            if (!outs.isEmpty()) {
                var out = outs.iterator().next();
                var remoteHost = out.getHost().getBlockEntity();
                targetPos = remoteHost.getBlockPos().relative(out.getSide());
                hitFace = out.getSide().getOpposite();
                targetWorld = (ServerLevel) remoteHost.getLevel();
            } else {
                targetPos = null;
                hitFace = null;
            }
        }

        if (targetPos == null || targetWorld == null) return false;

        long chunkKey = new ChunkPos(targetPos).toLong();
        if (!targetWorld.getChunkSource().isPositionTicking(chunkKey)) return false;

        if (is.is(Items.ENDER_PEARL) && CrazyConfig.COMMON.P2PWormholeTeleportation.get() && !CrazyConfig.COMMON.ImmersiveP2PWormhole.get()){
            if (!sp.isCreative()) is.shrink(1);
            sp.teleportTo(targetWorld, targetPos.getX() + 0.5D, targetPos.getY() + 0.1D, targetPos.getZ() + 0.5D, hitFace.getOpposite().toYRot(), sp.getXRot());
            return true;
        }

        var state = targetWorld.getBlockState(targetPos);
        var hit = new BlockHitResult(Vec3.atCenterOf(targetPos), hitFace, targetPos, false);

        WormholeAnchor.set(sp, targetPos, targetWorld);
        var containerBefore = sp.containerMenu;

        var result = state.use(targetWorld, sp, hand, hit);

        if (sp.containerMenu == containerBefore) {
            WormholeAnchor.clear(sp);
        }
        return result.consumesAction();
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        if (input.contains("myFreq")) {
            var freq = input.getShort("myFreq");
            var grid = getMainNode().getGrid();

            if (grid != null) {
                ((P2PTunnelPartAccessor) this).setOutput(true);
                P2PService.get(grid).updateFreq(this, freq);
                sendBlockUpdateToOppositeSide();
            }
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        if (mode == SettingsFrom.MEMORY_CARD) {
            if (!output.getAllKeys().isEmpty()) {
                var iterator = output.getAllKeys().iterator();
                while (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            }
            output.putString("myType", IPartItem.getId(getPartItem()).toString());
            output.putBoolean("wormhole", true);

            if (getFrequency() != 0) {
                output.putShort("myFreq", getFrequency());

                var colors = Platform.p2p().toColors(getFrequency());
                var colorCode = new int[]{
                        colors[0].ordinal(), colors[0].ordinal(),
                        colors[1].ordinal(), colors[1].ordinal(),
                        colors[2].ordinal(), colors[2].ordinal(),
                        colors[3].ordinal(), colors[3].ordinal(),
                };
                output.putIntArray(IMemoryCard.NBT_COLOR_CODE, colorCode);
            }
        }
    }

    @Override
    protected float getPowerDrainPerTick() {
        return 2.0f;
    }

    @Override
    public void readFromNBT(CompoundTag extra) {
        super.readFromNBT(extra);
        this.outerNode.loadFromNBT(extra);
    }

    @Override
    public void writeToNBT(CompoundTag extra) {
        super.writeToNBT(extra);
        this.outerNode.saveToNBT(extra);
    }

    @Override
    public void onTunnelNetworkChange() {
        super.onTunnelNetworkChange();
        if (!this.isOutput() || !connections.isEmpty()) {
            getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }
        sendBlockUpdateToOppositeSide();

        if (!isClientSide() && getLevel() instanceof ServerLevel sl) {
            TickHandler.instance().addCallable(sl, this::updateOrCreatePortalEntity);
        }
    }


    private void sendBlockUpdateToOppositeSide() {
        var world = getLevel();
        if (world == null || world.isClientSide) return;

        if (isOutput()) {
            var input = getInput();
            if (input != null && input.getHost() != null) {
                var be = input.getHost().getBlockEntity();
                var pos = be.getBlockPos().relative(input.getSide());
                sendNeighborUpdatesAt(be.getLevel(), pos, input.getSide());
            }
        } else {
            for (var out : getOutputs()) {
                if (out.getHost() != null) {
                    var be = out.getHost().getBlockEntity();
                    var pos = be.getBlockPos().relative(out.getSide());
                    sendNeighborUpdatesAt(be.getLevel(), pos, out.getSide());
                }
            }
        }
    }

    private void sendNeighborUpdatesAt(Level level, BlockPos pos, Direction facing) {
        if (level == null || level.isClientSide) return;
        if (wormholeUpdateBlacklist.contains(pos)) return;

        wormholeUpdateBlacklist.add(pos);
        TickHandler.instance().addCallable(level, wormholeUpdateBlacklist::clear);

        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
        level.updateNeighborsAt(pos, state.getBlock());

        var neighbor = pos.relative(facing.getOpposite());
        BlockState neighborState = level.getBlockState(neighbor);
        level.updateNeighborsAt(neighbor, neighborState.getBlock());
    }

    @Override
    public void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChanged(level, pos, neighbor);
        sendBlockUpdateToOppositeSide();
        if (!isOutput()) {
            readRedstoneInput();
        }
    }

    @Override
    public AECableType getExternalCableConnectionType() {
        return AECableType.DENSE_SMART;
    }

    @Override
    public void removeFromWorld() {
        if (!isClientSide()) {
            removePortalEntity();
            if (getLevel() instanceof ServerLevel sl) {
                var set = LOADED_PARTS.get(sl);
                if (set != null) {
                    set.remove(this);
                    if (set.isEmpty()) LOADED_PARTS.remove(sl);
                }
            }
        }
        super.removeFromWorld();
        this.outerNode.destroy();
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.outerNode.create(getLevel(), getBlockEntity().getBlockPos());

        if (!isClientSide() && getLevel() instanceof ServerLevel sl) {
            LOADED_PARTS.computeIfAbsent(sl, k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(this);
            TickHandler.instance().addCallable(sl, this::updateOrCreatePortalEntity);
            TickHandler.instance().addCallable(sl, this::updateOrCreatePortalEntity);
        }
    }

    @Override
    public void setPartHostInfo(Direction side, IPartHost host, BlockEntity blockEntity) {
        super.setPartHostInfo(side, host, blockEntity);
        this.outerNode.setExposedOnSides(EnumSet.of(side));
    }

    @Override
    public IGridNode getExternalFacingNode() {
        return this.outerNode.getNode();
    }

    @Override
    public void onPlacement(Player player) {
        super.onPlacement(player);
        this.outerNode.setOwningPlayer(player);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, true, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!node.isOnline()) {
            pendingUpdate = ConnectionUpdate.DISCONNECT;
        } else {
            pendingUpdate = ConnectionUpdate.CONNECT;
        }

        TickHandler.instance().addCallable(getLevel(), this::updateConnections);
        return TickRateModulation.SLEEP;
    }

    private void updateConnections() {
        var operation = pendingUpdate;
        pendingUpdate = ConnectionUpdate.NONE;

        var mainGrid = getMainNode().getGrid();

        if (isOutput()) {
            operation = ConnectionUpdate.DISCONNECT;
        } else if (mainGrid == null) {
            operation = ConnectionUpdate.DISCONNECT;
        }

        if (operation == ConnectionUpdate.DISCONNECT) {
            for (var cw : connections.values()) {
                cw.destroy();
            }
            connections.clear();
        } else if (operation == ConnectionUpdate.CONNECT) {
            var outputs = getOutputs();

            Iterator<Map.Entry<WormholeP2PTunnelPart, IGridConnection>> it = connections.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<WormholeP2PTunnelPart, IGridConnection> entry = it.next();
                WormholeP2PTunnelPart output = entry.getKey();
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

                var connection = GridHelper.createConnection(getExternalFacingNode(),
                        output.getExternalFacingNode());
                connections.put(output, connection);
            }
        }
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!isActive()) return LazyOptional.empty();
        var world = getLevel();
        if (world == null) return LazyOptional.empty();

        if (isOutput()) {
            var input = getInput();
            if (input == null) return LazyOptional.empty();

            var remoteHost = input.getHost().getBlockEntity();
            var targetPos = remoteHost.getBlockPos().relative(input.getSide());
            var targetBE = remoteHost.getLevel().getBlockEntity(targetPos);
            if (targetBE == null) return LazyOptional.empty();

            return targetBE.getCapability(cap, input.getSide().getOpposite());
        }

        var outputs = getOutputs();
        if (outputs.isEmpty()) return LazyOptional.empty();

        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            List<IItemHandlerModifiable> handlers = new ArrayList<>();
            for (var output : outputs) {
                var be = world.getBlockEntity(output.getHost().getBlockEntity().getBlockPos().relative(output.getSide()));
                if (be != null) {
                    var opt = be.getCapability(ForgeCapabilities.ITEM_HANDLER, output.getSide().getOpposite());
                    opt.ifPresent(handler -> {
                        if (handler instanceof IItemHandlerModifiable modifiable) {
                            handlers.add(modifiable);
                        }
                    });
                }
            }
            if (!handlers.isEmpty()) {
                return LazyOptional.of(() -> (T) new CombinedInvWrapper(handlers.toArray(new IItemHandlerModifiable[0])));
            }
        }

        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            List<IFluidHandler> handlers = new ArrayList<>();
            for (var output : outputs) {
                var be = world.getBlockEntity(output.getHost().getBlockEntity().getBlockPos().relative(output.getSide()));
                if (be != null) {
                    var opt = be.getCapability(ForgeCapabilities.FLUID_HANDLER, output.getSide().getOpposite());
                    opt.ifPresent(handlers::add);
                }
            }
            if (!handlers.isEmpty()) {
                return LazyOptional.of(() -> (T) new FluidHandlerConcatenate(handlers));
            }
        }

        if (cap == ForgeCapabilities.FLUID_HANDLER_ITEM) {
            List<IFluidHandlerItem> handlers = new ArrayList<>();
            for (var output : outputs) {
                var be = world.getBlockEntity(output.getHost().getBlockEntity().getBlockPos().relative(output.getSide()));
                if (be != null) {
                    var opt = be.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM, output.getSide().getOpposite());
                    opt.ifPresent(handlers::add);
                }
            }
            if (!handlers.isEmpty()) {
                return LazyOptional.of(() -> (T) new CombinedFluidHandlerItem(handlers));
            }
        }

        if (cap == ForgeCapabilities.ENERGY) {
            List<IEnergyStorage> storages = new ArrayList<>();
            for (var output : outputs) {
                var be = world.getBlockEntity(output.getHost().getBlockEntity().getBlockPos().relative(output.getSide()));
                if (be != null) {
                    var opt = be.getCapability(ForgeCapabilities.ENERGY, output.getSide().getOpposite());
                    opt.ifPresent(storages::add);
                }
            }
            if (!storages.isEmpty()) {
                return LazyOptional.of(() -> (T) new CombinedEnergyStorage(storages));
            }
        }

        for (var output : outputs) {
            var be = world.getBlockEntity(output.getHost().getBlockEntity().getBlockPos().relative(output.getSide()));
            if (be != null) {
                var result = be.getCapability(cap, output.getSide().getOpposite());
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        return LazyOptional.empty();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capabilityClass) {
        return getCapability(capabilityClass, getSide());
    }

    private enum ConnectionUpdate {
        NONE,
        DISCONNECT,
        CONNECT
    }

    private record RemoteView(ServerLevel level, BlockPos frontBlock, Direction facingOut) {}

    private static Vec3 dirVec(Direction d) {
        var n = d.getNormal();
        return new Vec3(n.getX(), n.getY(), n.getZ());
    }

    private static Vec3 safeNormalize(Vec3 v) {
        double len = v.length();
        return len < 1e-8 ? new Vec3(0, 0, 0) : v.scale(1.0 / len);
    }

    private static Vec3[] makePortalAxesFromNormal(Vec3 normal) {
        normal = safeNormalize(normal);

        Vec3 axisH = projectUpOnPlane(normal);
        Vec3 axisW = safeNormalize(axisH.cross(normal));

        return new Vec3[]{axisW, axisH};
    }

    private static Vec3 rotateVec(DQuaternion q, Vec3 v) {
        Quaternionf qf = q.toMcQuaternion();
        Vector3f tmp = new Vector3f((float) v.x, (float) v.y, (float) v.z);
        tmp.rotate(qf);
        return new Vec3(tmp.x, tmp.y, tmp.z);
    }

    private static DQuaternion rotationFromTo(Vec3 from, Vec3 to) {
        from = safeNormalize(from);
        to = safeNormalize(to);

        double dot = from.dot(to);
        dot = Math.max(-1.0, Math.min(1.0, dot));

        if (dot > 0.999999) {
            return DQuaternion.identity;
        }

        if (dot < -0.999999) {
            Vec3 axis = Math.abs(from.y) < 0.9 ? from.cross(new Vec3(0, 1, 0)) : from.cross(new Vec3(1, 0, 0));
            axis = safeNormalize(axis);
            return DQuaternion.rotationByDegrees(axis, 180.0);
        }

        Vec3 axis = safeNormalize(from.cross(to));
        double angleRad = Math.acos(dot);
        double angleDeg = angleRad * (180.0 / Math.PI);

        return DQuaternion.rotationByDegrees(axis, angleDeg);
    }

    private static Vec3 projectUpOnPlane(Vec3 forward) {
        forward = safeNormalize(forward);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 proj = up.subtract(forward.scale(up.dot(forward)));
        if (proj.lengthSqr() < 1e-8) {
            Vec3 alt = new Vec3(0, 0, 1);
            proj = alt.subtract(forward.scale(alt.dot(forward)));
        }
        return safeNormalize(proj);
    }

    private static double signedAngleAroundAxis(Vec3 from, Vec3 to, Vec3 axis) {
        from = safeNormalize(from);
        to = safeNormalize(to);
        axis = safeNormalize(axis);

        Vec3 cross = from.cross(to);
        double sin = axis.dot(cross);
        double cos = from.dot(to);
        return Math.atan2(sin, cos);
    }

    private static DQuaternion computeViewRotation(Vec3 localNormal, Vec3 remoteNormal) {
        Vec3 srcForward = safeNormalize(localNormal.scale(-1));
        Vec3 dstForward = safeNormalize(remoteNormal);

        Vec3 srcUp = projectUpOnPlane(srcForward);
        Vec3 dstUp = projectUpOnPlane(dstForward);

        DQuaternion qForward = rotationFromTo(srcForward, dstForward);
        Vec3 srcUpRot = rotateVec(qForward, srcUp);
        double rollRad = signedAngleAroundAxis(srcUpRot, dstUp, dstForward);

        DQuaternion qRoll = DQuaternion.rotationByDegrees(dstForward, rollRad * (180.0 / Math.PI));
        return qRoll.hamiltonProduct(qForward);
    }

    private void updateOrCreatePortalEntity() {
        if (!CrazyConfig.COMMON.ImmersiveP2PWormhole.get()) {
            if (intangibleCollision) {
                intangibleCollision = false;
                var lvl = getLevel();
                if (lvl != null && !lvl.isClientSide()) {
                    var p = getBlockEntity().getBlockPos();
                    var st = lvl.getBlockState(p);
                    lvl.sendBlockUpdated(p, st, st, 3);
                }
            }
            removePortalEntity();
            return;
        }

        if (!(getLevel() instanceof ServerLevel sl)) return;

        BlockPos hostPos = getBlockEntity().getBlockPos();
        Direction localFacing = getSide();

        var rr = resolveRemoteViewEx();

        boolean newIntangible = (rr.status() == RemoteResolveStatus.OK);
        if (newIntangible != intangibleCollision) {
            intangibleCollision = newIntangible;

            var lvl = getLevel();
            if (lvl != null && !lvl.isClientSide()) {
                var p = getBlockEntity().getBlockPos();
                var st = lvl.getBlockState(p);
                lvl.sendBlockUpdated(p, st, st, 3);
            }
        }

        if (rr.status() == RemoteResolveStatus.DISCONNECTED) {
            removePortalEntity();
            return;
        }

        if (rr.status() == RemoteResolveStatus.REMOTE_UNLOADED) {
            adoptOrCleanupLocalPortal(sl, hostPos, localFacing, false);
            return;
        }

        var remote = rr.view();
        if (remote == null) {
            removePortalEntity();
            return;
        }

        Vec3 portalCenter = computeExpectedPortalCenter(hostPos, localFacing);

        Vec3 remoteNormal = dirVec(remote.facingOut());
        Vec3 destCenter = Vec3.atCenterOf(remote.frontBlock()).add(remoteNormal.scale(-0.525 + 0.005));

        Vec3 localNormal = dirVec(localFacing);
        Vec3[] localAxes = makePortalAxesFromNormal(localNormal);

        Portal portal = adoptOrCleanupLocalPortal(sl, hostPos, localFacing, false);

        boolean shouldSpawnPortal = false;
        if (portal == null || portal.isRemoved()) {
            shouldSpawnPortal = true;
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("immersive_portals", "portal"));
            if (entityType == null) return;
            portal = new Portal(entityType, sl);
        }

        portal.setPos(portalCenter.x, portalCenter.y, portalCenter.z);
        portal.axisW = localAxes[0];
        portal.axisH = localAxes[1];
        portal.width = 0.55;
        portal.height = 0.55;

        portal.dimensionTo = remote.level().dimension();
        portal.destination = destCenter;

        DQuaternion rot = computeViewRotation(localNormal, remoteNormal);
        portal.setRotation(rot);
        portal.teleportable = true;
        tagPortal(sl, hostPos, localFacing, portal);

        if (shouldSpawnPortal) sl.addFreshEntity(portal);

        portal.reloadAndSyncToClientNextTick();
    }

    private Vec3 computeExpectedPortalCenter(BlockPos hostPos, Direction localFacing) {
        Vec3 localNormal = dirVec(localFacing);
        return Vec3.atCenterOf(hostPos).add(localNormal.scale(0.475 + 0.005));
    }

    private void tagPortal(ServerLevel level, BlockPos hostPos, Direction side, Portal portal) {
        var pd = portal.getPersistentData();
        pd.putBoolean(PD_WORMHOLE, true);
        pd.putLong(PD_HOST_POS, hostPos.asLong());
        pd.putString(PD_HOST_DIM, level.dimension().location().toString());
        pd.putByte(PD_HOST_SIDE, (byte) side.ordinal());
    }

    private boolean isTaggedAsOurs(ServerLevel level, BlockPos hostPos, Direction side, Portal portal) {
        var pd = portal.getPersistentData();
        if (!pd.getBoolean(PD_WORMHOLE)) return false;
        if (pd.getLong(PD_HOST_POS) != hostPos.asLong()) return false;
        if (pd.getByte(PD_HOST_SIDE) != (byte) side.ordinal()) return false;
        return Objects.equals(pd.getString(PD_HOST_DIM), level.dimension().location().toString());
    }

    private List<Portal> findCandidatePortals(ServerLevel level, Vec3 expectedCenter, BlockPos hostPos, Direction side) {
        var box = new AABB(expectedCenter, expectedCenter).inflate(1.0);
        var all = level.getEntitiesOfClass(Portal.class, box);

        List<Portal> result = new ArrayList<>();
        for (Portal p : all) {
            if (isTaggedAsOurs(level, hostPos, side, p)) {
                result.add(p);
                continue;
            }
            if (!p.getPersistentData().contains(PD_WORMHOLE) && p.position().distanceToSqr(expectedCenter) < 0.03) {
                result.add(p);
            }
        }
        return result;
    }

    @Nullable
    private Portal adoptOrCleanupLocalPortal(ServerLevel level, BlockPos hostPos, Direction side, boolean discardAll) {
        Vec3 expectedCenter = computeExpectedPortalCenter(hostPos, side);
        var candidates = findCandidatePortals(level, expectedCenter, hostPos, side);
        if (candidates.isEmpty()) {
            return null;
        }

        Portal best = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (Portal p : candidates) {
            double d = p.position().distanceToSqr(expectedCenter);
            boolean tagged = isTaggedAsOurs(level, hostPos, side, p);
            if (best == null) {
                best = p;
                bestDist = d;
            } else {
                boolean bestTagged = isTaggedAsOurs(level, hostPos, side, best);
                if (tagged && !bestTagged) {
                    best = p;
                    bestDist = d;
                } else if (tagged == bestTagged && d < bestDist) {
                    best = p;
                    bestDist = d;
                }
            }
        }

        for (Portal p : candidates) {
            if (p == best) continue;
            p.discard();
        }

        if (discardAll) {
            best.discard();
            return null;
        }

        tagPortal(level, hostPos, side, best);
        return best;
    }

    private void removePortalEntity() {
        if (!(getLevel() instanceof ServerLevel sl)) return;
        var hostPos = getBlockEntity().getBlockPos();
        var side = getSide();

        adoptOrCleanupLocalPortal(sl, hostPos, side, true);
    }

    private enum RemoteResolveStatus { OK, DISCONNECTED, REMOTE_UNLOADED }
    private record RemoteResolve(RemoteResolveStatus status, @Nullable RemoteView view) {}

    @NotNull
    private RemoteResolve resolveRemoteViewEx() {
        var lvl = getLevel();
        if (!(lvl instanceof ServerLevel)) return new RemoteResolve(RemoteResolveStatus.DISCONNECTED, null);
        if (!isActive()) return new RemoteResolve(RemoteResolveStatus.DISCONNECTED, null);

        if (isOutput()) {
            var input = getInput();
            if (input == null) return new RemoteResolve(RemoteResolveStatus.DISCONNECTED, null);
            if (input.getHost() == null) return new RemoteResolve(RemoteResolveStatus.REMOTE_UNLOADED, null);

            var remoteHost = input.getHost().getBlockEntity();
            if (remoteHost == null || !(remoteHost.getLevel() instanceof ServerLevel sl)) {
                return new RemoteResolve(RemoteResolveStatus.REMOTE_UNLOADED, null);
            }

            Direction remoteSide = input.getSide();
            BlockPos front = remoteHost.getBlockPos().relative(remoteSide);
            return new RemoteResolve(RemoteResolveStatus.OK, new RemoteView(sl, front, remoteSide));
        } else {
            var outs = getOutputs();
            if (outs.isEmpty()) return new RemoteResolve(RemoteResolveStatus.DISCONNECTED, null);

            WormholeP2PTunnelPart chosen = null;
            for (var out : outs) {
                if (out.getHost() == null) continue;
                var remoteHost = out.getHost().getBlockEntity();
                if (remoteHost != null && remoteHost.getLevel() instanceof ServerLevel) {
                    chosen = out;
                    break;
                }
            }

            if (chosen == null) return new RemoteResolve(RemoteResolveStatus.REMOTE_UNLOADED, null);

            var remoteHost = chosen.getHost().getBlockEntity();
            ServerLevel sl = (ServerLevel) remoteHost.getLevel();

            Direction remoteSide = chosen.getSide();
            BlockPos front = remoteHost.getBlockPos().relative(remoteSide);
            return new RemoteResolve(RemoteResolveStatus.OK, new RemoteView(sl, front, remoteSide));
        }
    }

    @Mod.EventBusSubscriber(modid = CrazyAddons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class WormholePortalEvents {

        @SubscribeEvent
        public static void onLevelTick(TickEvent.LevelTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            if (!(e.level instanceof ServerLevel sl)) return;

            var set = LOADED_PARTS.get(sl);
            if (set == null || set.isEmpty()) return;

            for (var part : List.copyOf(set)) {
                if (part.getLevel() != sl) continue;

                if (part.portalMaintenanceCooldown-- > 0) continue;
                part.portalMaintenanceCooldown = 20;

                part.updateOrCreatePortalEntity();
            }
        }

        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent e) {
            if (e.getLevel().isClientSide()) return;
            if (!(e.getEntity() instanceof Portal p)) return;

            var pd = p.getPersistentData();
            if (!pd.getBoolean(PD_WORMHOLE)) return;

            if (!(e.getLevel() instanceof ServerLevel sl)) return;

            BlockPos hostPos = BlockPos.of(pd.getLong(PD_HOST_POS));
            int sideOrd = pd.getByte(PD_HOST_SIDE);
            if (sideOrd < 0 || sideOrd >= Direction.values().length) {
                p.discard();
                return;
            }
            Direction side = Direction.values()[sideOrd];
            if (!hasWormholePartAt(sl, hostPos, side)) {
                p.discard();
            }
        }

        private static boolean hasWormholePartAt(ServerLevel level, BlockPos pos, Direction side) {
            var be = level.getBlockEntity(pos);
            if (be == null) return false;

            if (be instanceof CableBusBlockEntity host) {
                var part = host.getPart(side);
                return part instanceof WormholeP2PTunnelPart;
            }
            return false;
        }
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        if (bch.isBBCollision() && intangibleCollision) {
            return;
        }
        bch.addBox(5, 5, 11.5, 11, 11, 12.5);
        bch.addBox(3, 3, 12.5, 13, 13, 13.5);
        bch.addBox(2, 2, 13.5, 14, 14, 15.5);
    }
}