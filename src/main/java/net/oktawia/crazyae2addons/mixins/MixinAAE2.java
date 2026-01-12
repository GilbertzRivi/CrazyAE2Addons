package net.oktawia.crazyae2addons.mixins;

import appeng.api.networking.IGrid;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.interfaces.IAdvPatternProviderCpu;
import net.oktawia.crazyae2addons.interfaces.IPatternProviderCpu;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PatternProviderLogic.class, priority = 1200)
public abstract class MixinAAE2 implements IPatternProviderCpu, IAdvPatternProviderCpu {
    @Unique
    private AdvCraftingCPU cpuLogic = null;

    @Shadow public abstract @Nullable IGrid getGrid();

    @Unique
    @Override
    public void setCpuLogic(AdvCraftingCPU cpu) {
        this.cpuLogic = cpu;
    }
}