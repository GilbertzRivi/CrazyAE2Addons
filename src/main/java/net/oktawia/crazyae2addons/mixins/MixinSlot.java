package net.oktawia.crazyae2addons.mixins;

import lombok.Setter;
import net.oktawia.crazyae2addons.logic.interfaces.IMovableSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.inventory.Slot;

@Setter
@Mixin(value = Slot.class, remap = false)
public abstract class MixinSlot implements IMovableSlot {
    @Final
    @Shadow
    @Mutable
    public int x;

    @Final
    @Shadow
    @Mutable
    public int y;

}