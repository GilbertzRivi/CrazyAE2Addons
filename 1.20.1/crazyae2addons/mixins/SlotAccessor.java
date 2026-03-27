package net.oktawia.crazyae2addons.mixins;

import net.oktawia.crazyae2addons.interfaces.IMovableSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.inventory.Slot;

@Mixin(Slot.class)
public abstract class SlotAccessor implements IMovableSlot {
    @Final
    @Shadow
    @Mutable
    public int x;

    @Final
    @Shadow
    @Mutable
    public int y;

    public void setX(int x){
        this.x = x;
    }

    public void setY(int y){
        this.y = y;
    }
}
