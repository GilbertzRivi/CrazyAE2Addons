package net.oktawia.crazyae2addons.interfaces;

import net.minecraft.nbt.CompoundTag;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;

public interface IAdvPatternProviderCpu {
    void setCpuLogic(AdvCraftingCPU cpu);
    void failAdvCrafting();
    void advSaveNbt(CompoundTag tag);
    void advReadNbt(CompoundTag tag);
    void loadTag();
}