package net.oktawia.crazyae2addonslite.entities;

import appeng.api.stacks.AEItemKey;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyMenuRegistrar;

public class BrokenPatternProviderBE extends PatternProviderBlockEntity {
    public BrokenPatternProviderBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.BROKEN_PATTERN_PROVIDER_BE.get(), pos, blockState);
        this.getMainNode().setVisualRepresentation(CrazyBlockRegistrar.BROKEN_PATTERN_PROVIDER_BLOCK.get().asItem());
    }

    @Override
    public PatternProviderLogic createLogic() {
        return new PatternProviderLogic(this.getMainNode(), this, 1);
    }

    @Override
    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.BROKEN_PATTERN_PROVIDER_MENU.get(), player, locator);
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(CrazyMenuRegistrar.BROKEN_PATTERN_PROVIDER_MENU.get(), player, subMenu.getLocator());
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(CrazyBlockRegistrar.BROKEN_PATTERN_PROVIDER_BLOCK.get());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyBlockRegistrar.BROKEN_PATTERN_PROVIDER_BLOCK.get().asItem().getDefaultInstance();
    }
}
