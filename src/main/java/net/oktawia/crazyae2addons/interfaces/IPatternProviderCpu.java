package net.oktawia.crazyae2addons.interfaces;

import appeng.me.cluster.implementations.CraftingCPUCluster;

public interface IPatternProviderCpu {
    void setCpuCluster(CraftingCPUCluster cpu);
    CraftingCPUCluster getCpuCluster();
}