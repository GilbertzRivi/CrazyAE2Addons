package net.oktawia.crazyae2addonslite.mixins;

import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.implementations.PatternProviderMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PatternProviderMenu.class)
public interface PatternProviderMenuAccessor {
    @Accessor("logic")
    PatternProviderLogic getLogic();
}