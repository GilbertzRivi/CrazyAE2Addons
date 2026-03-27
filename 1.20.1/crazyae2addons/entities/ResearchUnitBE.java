package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyFluidRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.ICableMachine;
import net.oktawia.crazyae2addons.menus.MobFarmControllerMenu;
import net.oktawia.crazyae2addons.menus.ResearchUnitMenu;
import net.oktawia.crazyae2addons.misc.MobFarmValidator;
import net.oktawia.crazyae2addons.misc.ResearchUnitValidator;
import net.oktawia.crazyae2addons.renderer.preview.PreviewInfo;
import net.oktawia.crazyae2addons.renderer.preview.Previewable;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResearchUnitBE extends AENetworkBlockEntity implements IGridTickable, Previewable, MenuProvider, ICableMachine {

    public ResearchUnitValidator validator;
    public boolean preview = false;
    public boolean formed = false;

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

    public static final Set<ResearchUnitBE> CLIENT_INSTANCES = new HashSet<>();

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

    public ResearchUnitBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.RESEARCH_UNIT_BE.get(), pos, blockState);
        this.getMainNode()
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(new ItemStack(CrazyBlockRegistrar.RESEARCH_UNIT.get().asItem()));
        validator = new ResearchUnitValidator();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (getLevel() == null){
            return TickRateModulation.SAME;
        }
        if (getLevel().getGameTime() % 20 == 0){
            if (!validator.matchesStructure(getLevel(), getBlockPos(), getBlockState(), this)){
                formed = false;
                return TickRateModulation.SAME;
            } else {
                formed = true;
            }
        }
        return TickRateModulation.SAME;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new ResearchUnitMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.crazyae2addons.research_unit");
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.RESEARCH_UNIT_MENU.get(), player, locator);
    }

    public int getComputation() {
        if (!formed || getLevel() == null) return 0;

        int cpu_1k = validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "ae2:1k_crafting_storage");
        int cpu_4k = validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "ae2:4k_crafting_storage");
        int cpu_16k = validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "ae2:16k_crafting_storage");
        int cpu_64k = validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "ae2:64k_crafting_storage");
        int cpu_256k = validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "ae2:256k_crafting_storage");

        long base = (cpu_1k
                + (long) cpu_4k * 4
                + (long) cpu_16k * 16
                + (long) cpu_64k * 64
                + (long) cpu_256k * 256) / 16;

        long extra = 0;
        UnmodifiableConfig cfg = CrazyConfig.COMMON.ResearchUnitExtraQBlocks.get();
        if (cfg != null && cfg.valueMap() != null && !cfg.valueMap().isEmpty()) {
            for (var e : cfg.valueMap().entrySet()) {
                String blockId = e.getKey();
                Object v = e.getValue();
                if (blockId == null || blockId.isBlank() || !(v instanceof Number n)) continue;

                int mul = n.intValue();
                if (mul == 0) continue;

                int count = validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), blockId);
                if (count != 0) {
                    extra += (long) count * (long) mul;
                }
            }
        }

        long result = base + extra;
        if (result > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (result < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) result;
    }



    public int getCoolant(){
        if (getExternalTank() != null){
            var fluid = getExternalTank().getFluidInTank(0);
            if (fluid.getFluid().getFluidType() == CrazyFluidRegistrar.RESEARCH_FLUID_TYPE.get()){
                return fluid.getAmount();
            }
        }
        return 0;
    }

    private IFluidHandler getExternalTank() {
        if (level == null) return null;
        BlockPos pos = worldPosition.relative(getFacing().getOpposite(), 2).relative(Direction.UP, 4);
        var be = level.getBlockEntity(pos);
        if (be == null) return null;

        Direction sideFromTankTowardController = getFacing();
        var cap = be.getCapability(ForgeCapabilities.FLUID_HANDLER, sideFromTankTowardController);
        if (cap.isPresent()) return cap.orElse(null);

        return be.getCapability(ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
    }

    private Direction getFacing() {
        var state = getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    public boolean doWork(){
        var grid = getGridNode().getGrid();
        if (grid.getEnergyService().extractAEPower(getComputation()*64, Actionable.SIMULATE, PowerMultiplier.CONFIG) != getComputation()*64){
            return false;
        }
        grid.getEnergyService().extractAEPower(getComputation()*64, Actionable.MODULATE, PowerMultiplier.CONFIG);
        if (getExternalTank().drain(getComputation()/4, IFluidHandler.FluidAction.SIMULATE).getAmount() < getComputation()/4){
            return false;
        }
        getExternalTank().drain(getComputation()/4, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }
}
