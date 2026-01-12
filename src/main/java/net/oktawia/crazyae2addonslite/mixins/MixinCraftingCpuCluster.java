package net.oktawia.crazyae2addonslite.mixins;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import net.minecraft.network.chat.Component;
import net.oktawia.crazyae2addonslite.interfaces.ICraftingClusterPrio;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class MixinCraftingCpuCluster implements ICraftingClusterPrio {

    @Shadow
    @Nullable
    public abstract IGrid getGrid();

    @Unique
    private Component lastLiveName;

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

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void returnLiveOwnerName(CallbackInfoReturnable<Component> cir) {
        var grid = getGrid();
        if (grid == null) return;

        Component fresh = null;
        for (var be : grid.getMachines(CraftingBlockEntity.class)) {
            ICraftingCPU c = be.getCluster();
            if (c == (Object) this) {
                var node = be.getMainNode();
                var owner = (node != null) ? node.getNode().getOwner() : null;
                if (owner instanceof CraftingBlockEntity cbe && cbe.getCustomName() != null) {
                    fresh = cbe.getCustomName();
                    break;
                }
            }
        }

        if (lastLiveName == null || fresh != null && !fresh.getString().equals(lastLiveName.getString())) {
            lastLiveName = fresh;
        }

        if (lastLiveName != null) {
            cir.setReturnValue(lastLiveName);
        }
    }
}
