package net.oktawia.crazyae2addons.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.oktawia.crazyae2addons.entities.ResearchPedestalBottomBE;
import net.oktawia.crazyae2addons.interfaces.ICableMachine;

import java.util.*;

public class ResearchCableBlock extends Block {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;

    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);

    private static final VoxelShape SHAPE_NORTH = Block.box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape SHAPE_SOUTH = Block.box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape SHAPE_WEST  = Block.box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape SHAPE_EAST  = Block.box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape SHAPE_UP    = Block.box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape SHAPE_DOWN  = Block.box(6, 0, 6, 10, 6, 10);

    private static final int NETWORK_HARD_LIMIT = 4096;

    public ResearchCableBlock() {
        super(BlockBehaviour.Properties
                .of()
                .strength(0.5F)
                .noOcclusion()
        );
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(WEST,  false)
                .setValue(EAST,  false)
                .setValue(UP,    false)
                .setValue(DOWN,  false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, WEST, EAST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = this.defaultBlockState();

        for (Direction dir : Direction.values()) {
            BooleanProperty prop = getProperty(dir);
            if (prop == null) continue;

            BlockPos neighborPos = pos.relative(dir);
            boolean connected = canConnect(level, neighborPos, dir);
            state = state.setValue(prop, connected);
        }

        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        BooleanProperty prop = getProperty(direction);
        if (prop == null) return state;

        boolean connected = canConnect(level, neighborPos, direction);
        return state.setValue(prop, connected);
    }

    private static BooleanProperty getProperty(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST  -> WEST;
            case EAST  -> EAST;
            case UP    -> UP;
            case DOWN  -> DOWN;
        };
    }

    private boolean canConnect(LevelAccessor level,
                               BlockPos neighborPos,
                               Direction dirFromCableToNeighbor) {
        BlockState neighborState = level.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();

        if (neighborBlock instanceof ResearchCableBlock) {
            return true;
        }

        if (level.getBlockEntity(neighborPos) instanceof ICableMachine) {
            Direction fromMachineToCable = dirFromCableToNeighbor.getOpposite();
            return canMachineConnectOnSide(level, neighborPos, fromMachineToCable);
        }

        return false;
    }

    private static boolean canMachineConnectOnSide(LevelAccessor level,
                                                   BlockPos machinePos,
                                                   Direction fromMachineToCable) {
        var be = level.getBlockEntity(machinePos);
        if (!(be instanceof ICableMachine)) {
            return false;
        }

        if (be instanceof ResearchPedestalBottomBE pedestal) {
            Direction facing =
                    pedestal.getBlockState().getValue(ResearchPedestalBottomBlock.FACING);
            return facing == fromMachineToCable;
        }

        return true;
    }

    private static boolean isCable(LevelAccessor level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof ResearchCableBlock;
    }

    private static boolean isCableConnectedToMachine(LevelAccessor level,
                                                     BlockPos cablePos,
                                                     BlockPos machinePos,
                                                     Direction dirFromMachineToCable) {
        BlockState cableState = level.getBlockState(cablePos);
        if (!(cableState.getBlock() instanceof ResearchCableBlock)) {
            return false;
        }

        Direction fromCableToMachine = dirFromMachineToCable.getOpposite();
        BooleanProperty prop = getProperty(fromCableToMachine);
        if (prop == null || !cableState.getValue(prop)) {
            return false;
        }

        return canMachineConnectOnSide(level, machinePos, dirFromMachineToCable);
    }

    private static Set<BlockPos> collectMachinesInNetwork(Level level,
                                                          BlockPos startMachinePos) {
        Set<BlockPos> visitedCables = new HashSet<>();
        Set<BlockPos> visitedMachines = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        visitedMachines.add(startMachinePos);

        for (Direction dir : Direction.values()) {
            BlockPos adj = startMachinePos.relative(dir);
            if (!isCable(level, adj)) continue;
            if (!isCableConnectedToMachine(level, adj, startMachinePos, dir)) continue;

            if (visitedCables.add(adj)) {
                queue.add(adj);
            }
        }

        while (!queue.isEmpty()
                && visitedCables.size() + visitedMachines.size() < NETWORK_HARD_LIMIT) {

            BlockPos cablePos = queue.removeFirst();
            BlockState cableState = level.getBlockState(cablePos);
            if (!(cableState.getBlock() instanceof ResearchCableBlock)) {
                continue;
            }

            for (Direction dir : Direction.values()) {
                BooleanProperty prop = getProperty(dir);
                if (prop == null || !cableState.getValue(prop)) {
                    continue;
                }

                BlockPos adj = cablePos.relative(dir);

                if (isCable(level, adj)) {
                    if (visitedCables.add(adj)) {
                        queue.add(adj);
                    }
                    continue;
                }

                if (level.getBlockEntity(adj) instanceof ICableMachine) {
                    Direction fromMachineToCable = dir.getOpposite();
                    if (!canMachineConnectOnSide(level, adj, fromMachineToCable)) {
                        continue;
                    }

                    if (visitedMachines.add(adj)) {
                        for (Direction d2 : Direction.values()) {
                            BlockPos adjCable = adj.relative(d2);
                            if (!isCable(level, adjCable)) continue;
                            if (!isCableConnectedToMachine(level, adjCable, adj, d2)) continue;

                            if (visitedCables.add(adjCable)) {
                                queue.add(adjCable);
                            }
                        }
                    }
                }
            }
        }

        return visitedMachines;
    }

    public static List<BlockPos> findConnectedMachines(Level level, BlockPos startMachinePos) {
        if (level.isClientSide) {
            return Collections.emptyList();
        }

        Set<BlockPos> machines = collectMachinesInNetwork(level, startMachinePos);
        machines.remove(startMachinePos);
        return new ArrayList<>(machines);
    }

    @Override
    public VoxelShape getShape(BlockState state,
                               net.minecraft.world.level.BlockGetter level,
                               BlockPos pos,
                               CollisionContext ctx) {
        return buildShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state,
                                        net.minecraft.world.level.BlockGetter level,
                                        BlockPos pos,
                                        CollisionContext ctx) {
        return buildShape(state);
    }

    private VoxelShape buildShape(BlockState state) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, SHAPE_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SHAPE_SOUTH);
        if (state.getValue(WEST))  shape = Shapes.or(shape, SHAPE_WEST);
        if (state.getValue(EAST))  shape = Shapes.or(shape, SHAPE_EAST);
        if (state.getValue(UP))    shape = Shapes.or(shape, SHAPE_UP);
        if (state.getValue(DOWN))  shape = Shapes.or(shape, SHAPE_DOWN);
        return shape;
    }
}
