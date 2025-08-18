package net.oktawia.crazyae2addons.mixins;

import net.oktawia.crazyae2addons.interfaces.ICraftingClusterPrio;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = appeng.me.cluster.implementations.CraftingCPUCluster.class, remap = false)
public class MixinCraftingCpuCluster implements ICraftingClusterPrio {

    @Unique private int prio = 0;

    @Override
    public int getPrio() {
        return this.prio;
    }

    @Override
    public void setPrio(int prio) {
        this.prio = prio;
    }

    @Inject(
            method = "writeToNBT(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("TAIL")
    )
    private void savePrio(net.minecraft.nbt.CompoundTag data, CallbackInfo ci) {
        data.putInt("CrazyPrio", this.prio);
    }

    @Inject(
            method = "readFromNBT(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("TAIL")
    )
    private void loadPrio(net.minecraft.nbt.CompoundTag data, CallbackInfo ci) {
        if (data.contains("CrazyPrio")) {
            this.prio = data.getInt("CrazyPrio");
        }
    }
}
