package net.oktawia.crazyae2addons.entities;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.blocks.ResearchCableBlock;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.ICableMachine;
import net.oktawia.crazyae2addons.menus.ResearchPedestalMenu;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ResearchPedestalBottomBE extends AEBaseBlockEntity implements ICableMachine, MenuProvider {

    public ResearchPedestalBottomBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.RESEARCH_PEDESTAL_BOTTOM_BE.get(), pos, blockState);
    }

    public List<BlockPos> getConnectedMachines() {
        if (this.level == null || this.level.isClientSide) {
            return Collections.emptyList();
        }

        List<BlockPos> machines = ResearchCableBlock.findConnectedMachines(this.level, this.worldPosition);
        machines.remove(this.worldPosition);
        return machines;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new ResearchPedestalMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.crazyae2addons.research_pedestal_bottom");
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.RESEARCH_PEDESTAL_MENU.get(), player, locator);
    }

    public int getConnectedComputation(){
        if (getConnectedMachines().size() != 1 || getLevel() == null){
            return 0;
        } else {
            var machine = getLevel().getBlockEntity(getConnectedMachines().get(0));
            if (!(machine instanceof ResearchUnitBE ru)){
                return 0;
            } else {
                return ru.getComputation();
            }
        }
    }

    public boolean isValidConnection(){
        return getLevel() != null && getConnectedMachines().size() == 1 && getLevel().getBlockEntity(getConnectedMachines().get(0)) instanceof ResearchUnitBE;
    }

    public boolean doWork(){
        if (getConnectedMachines().size() != 1 || getLevel() == null){
            return false;
        } else {
            var machine = getLevel().getBlockEntity(getConnectedMachines().get(0));
            if (!(machine instanceof ResearchUnitBE ru)){
                return false;
            } else {
                return ru.doWork();
            }
        }
    }
}
