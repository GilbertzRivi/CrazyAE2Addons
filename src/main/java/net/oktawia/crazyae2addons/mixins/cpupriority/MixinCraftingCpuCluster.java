package net.oktawia.crazyae2addons.mixins.cpupriority;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.oktawia.crazyae2addons.logic.interfaces.ICpuPrio;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class MixinCraftingCpuCluster implements ICpuPrio {

    @Unique
    private static final String CRAZY_PRIO_TAG = "CrazyPrio";

    @Shadow
    @Nullable
    public abstract IGrid getGrid();

    @Unique
    private Component crazyae2addons$lastLiveName;

    @Unique
    private int crazyae2addons$prio = 0;

    @Override
    public int getPrio() {
        return this.crazyae2addons$prio;
    }

    @Override
    public void setPrio(int prio) {
        this.crazyae2addons$prio = prio;
    }

    @Inject(method = "writeToNBT(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void crazyae2addons$savePrio(CompoundTag data, CallbackInfo ci) {
        data.putInt(CRAZY_PRIO_TAG, this.crazyae2addons$prio);
    }

    @Inject(method = "readFromNBT(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void crazyae2addons$loadPrio(CompoundTag data, CallbackInfo ci) {
        if (data.contains(CRAZY_PRIO_TAG)) {
            this.crazyae2addons$prio = data.getInt(CRAZY_PRIO_TAG);
        }
    }

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void crazyae2addons$returnLiveOwnerName(CallbackInfoReturnable<Component> cir) {
        var grid = getGrid();
        if (grid == null) {
            return;
        }

        Component fresh = null;

        for (var be : grid.getMachines(CraftingBlockEntity.class)) {
            ICraftingCPU cpu = be.getCluster();
            if (cpu != (Object) this) {
                continue;
            }

            var node = be.getMainNode();
            var owner = node != null ? node.getNode().getOwner() : null;
            if (owner instanceof CraftingBlockEntity craftingBe && craftingBe.getCustomName() != null) {
                fresh = craftingBe.getCustomName();
                break;
            }
        }

        if (fresh != null) {
            this.crazyae2addons$lastLiveName = fresh;
        }

        if (this.crazyae2addons$lastLiveName != null) {
            cir.setReturnValue(this.crazyae2addons$lastLiveName);
        }
    }
}