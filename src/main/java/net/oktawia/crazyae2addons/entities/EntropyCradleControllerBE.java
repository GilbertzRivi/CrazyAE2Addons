package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.blocks.EntropyCradleCapacitor;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyRecipes;
import net.oktawia.crazyae2addons.menus.EntropyCradleControllerMenu;
import net.oktawia.crazyae2addons.renderer.preview.PreviewInfo;
import net.oktawia.crazyae2addons.misc.EntropyCradleValidator;
import net.oktawia.crazyae2addons.renderer.preview.Previewable;
import net.oktawia.crazyae2addons.recipes.CradleContext;
import net.oktawia.crazyae2addons.recipes.CradleRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntropyCradleControllerBE extends AENetworkInvBlockEntity implements Previewable, IGridTickable, MenuProvider {

    public EntropyCradleValidator validator;
    public int MAX_ENERGY = CrazyConfig.COMMON.CradleCapacity.get();
    public IEnergyStorage storedEnergy;

    private PreviewInfo previewInfo = null;

    @Override
    @OnlyIn(Dist.CLIENT)
    public PreviewInfo getPreviewInfo() {
        return previewInfo;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setPreviewInfo(PreviewInfo info) {
        this.previewInfo = info;
    }

    public boolean preview = false;

    public static final Set<EntropyCradleControllerBE> CLIENT_INSTANCES = new java.util.HashSet<>();

    public EntropyCradleControllerBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.ENTROPY_CRADLE_CONTROLLER_BE.get(), pos, blockState);
        validator = new EntropyCradleValidator();
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.ENTROPY_CRADLE_CONTROLLER.get().asItem())
                );
        this.storedEnergy = new EnergyStorage(MAX_ENERGY, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            CLIENT_INSTANCES.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_INSTANCES.remove(this);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("storedeng")){
            this.storedEnergy.receiveEnergy(data.getInt("storedeng"), false);
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putInt("storedeng", storedEnergy.getEnergyStored());
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Entropy Cradle");
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 5, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!validator.matchesStructure(getLevel(), getBlockPos(), getBlockState(), this))
            return TickRateModulation.IDLE;

        int currentFE = this.storedEnergy.getEnergyStored();

        double fillRatio = currentFE / (double) MAX_ENERGY;

        int maxLevels = 6;
        int litLevels = (int) Math.round(fillRatio * maxLevels);

        for (int level = 0; level < maxLevels; level++) {
            boolean shouldBeLit = level < litLevels;
            validator.markCaps(getLevel(), getBlockPos(), getBlockState(), EntropyCradleCapacitor.POWER, shouldBeLit, level, this.storedEnergy.getEnergyStored() == MAX_ENERGY);
        }

        if (currentFE >= MAX_ENERGY) {
            validator.markCaps(getLevel(), getBlockPos(), getBlockState(), EntropyCradleCapacitor.POWER, true, 0, this.storedEnergy.getEnergyStored() == MAX_ENERGY);
            return TickRateModulation.IDLE;
        }

        int remainingFE = MAX_ENERGY - currentFE;
        int maxAEToExtract = remainingFE / 2;
        if (maxAEToExtract > CrazyConfig.COMMON.CradleChargingSpeed.get()){
            maxAEToExtract = CrazyConfig.COMMON.CradleChargingSpeed.get();
        }

        var extractedAE = getGridNode().getGrid().getEnergyService().extractAEPower(
                maxAEToExtract, Actionable.MODULATE, PowerMultiplier.CONFIG);

        int toInsertFE = (int) (extractedAE * 2);
        this.storedEnergy.receiveEnergy(toInsertFE, false);
        this.setChanged();

        return TickRateModulation.IDLE;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return InternalInventory.empty();
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {}

    public void onRedstonePulse() {
        var extracted = this.storedEnergy.extractEnergy(CrazyConfig.COMMON.CradleCost.get(), false);
        for (int level = 0; level < 6; level++) {
            validator.markCaps(getLevel(), getBlockPos(), getBlockState(), EntropyCradleCapacitor.POWER, false, level, this.storedEnergy.getEnergyStored() == MAX_ENERGY);
        }
        if (extracted < CrazyConfig.COMMON.CradleCost.get()) return;

        var facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos origin;
        if (facing == Direction.NORTH || facing == Direction.WEST) {
            origin = this.getBlockPos().relative(facing.getOpposite().getAxis(), 5).above(3);
        } else {
            origin = this.getBlockPos().relative(facing.getOpposite().getAxis(), -5).above(3);
        }

        var ctx = new CradleContext(getLevel(), origin, facing);

        var opt = getLevel().getRecipeManager()
                .getRecipeFor(CrazyRecipes.CRADLE_TYPE.get(), ctx, getLevel());

        if (opt.isEmpty()) return;

        CradleRecipe r = opt.get();

        fillStructureWithAir(getLevel(), origin, facing);

        getLevel().setBlock(origin, r.resultBlock().defaultBlockState(), 3);

        if (!level.isClientSide()) {
            ((ServerLevel) level).sendParticles(ParticleTypes.EXPLOSION,
                    origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5,
                    30, 0.5, 0.5, 0.5, 0.01);
            level.playSound(null, origin, SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }


    public static void fillStructureWithAir(Level level, BlockPos center, Direction controllerFacing) {
        final int OFFSET = 2;

        for (int y = 0; y < 5; y++) {
            for (int z = 0; z < 5; z++) {
                for (int x = 0; x < 5; x++) {
                    BlockPos relative = rotateOffset(x - OFFSET, y - OFFSET, z - OFFSET, controllerFacing);
                    BlockPos target = center.offset(relative);

                    if (x == 0 || x == 4 || y == 0 || y == 4 || z == 0 || z == 4) {
                        ((ServerLevel) level).sendParticles(ParticleTypes.FIREWORK,
                                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                                5, 0.5, 0.5, 0.5, 0.01);
                    }

                    level.setBlockAndUpdate(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static BlockPos rotateOffset(int x, int y, int z, Direction facing) {
        return switch (facing) {
            case SOUTH -> new BlockPos(-x, y, -z);
            case EAST -> new BlockPos(z, y, -x);
            case WEST -> new BlockPos(-z, y, x);
            default -> new BlockPos(x, y, z);
        };
    }


    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new EntropyCradleControllerMenu(i, inventory, this);
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.ENTROPY_CRADLE_CONTROLLER_MENU.get(), player, locator);
    }
}
