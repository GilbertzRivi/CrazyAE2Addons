package net.oktawia.crazyae2addonslite.mixins;

import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PatternProviderBlockEntity.class)
public interface PatternProviderBlockEntityAccessor {
    @Accessor("logic")
    @Mutable
    void setLogic(PatternProviderLogic logic);
}