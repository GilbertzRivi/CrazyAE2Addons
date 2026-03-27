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

    @Shadow
    @Final
    private PatternProviderLogicHost host;

    @Unique
    private AdvCraftingCPU cpuLogic = null;

    @Unique
    private CompoundTag logicTag = null;

    @Shadow public abstract @Nullable IGrid getGrid();

    @Unique
    @Override
    public void setCpuLogic(AdvCraftingCPU cpu) {
        this.cpuLogic = cpu;
    }

    public void failAdvCrafting(){
        if (this.cpuLogic != null) {
            this.cpuLogic.cancelJob();
            this.cpuLogic = null;
        }
    }

    @Override
    public void advSaveNbt(CompoundTag tag){
        if (this.cpuLogic != null) {
            CompoundTag logicTag = new CompoundTag();
            this.cpuLogic.writeToNBT(logicTag);
            tag.put("cpuLogic", logicTag);
        }
    }

    @Override
    public void advReadNbt(CompoundTag tag){
        this.logicTag = tag.getCompound("cpuLogic");
    }

    public void loadTag(){
        if (this.getGrid() != null && logicTag != null){
            for (var cpu : this.getGrid().getCraftingService().getCpus()) {
                if (cpu instanceof AdvCraftingCPU advCpu) {
                    CompoundTag newTag = new CompoundTag();
                    advCpu.writeToNBT(newTag);
                    var job = logicTag.getCompound("job");
                    var link = job.getCompound("link");
                    var id = link.getIntArray("craftId");

                    var newJob = newTag.getCompound("job");
                    var newLink = newJob.getCompound("link");
                    var newId = newLink.getIntArray("craftId");

                    try {
                        if (newId[0] == id[0] && newId[1] == id[1] && newId[2] == id[2] && newId[3] == id[3]) {
                            this.cpuLogic = advCpu;
                            this.logicTag = null;
                            return;
                        }
                    } catch (Exception e) {
                        LogUtils.getLogger().info(e.toString());
                    }
                }
            }
        }
    }
}