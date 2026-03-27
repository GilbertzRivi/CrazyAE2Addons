package net.oktawia.crazyae2addons.interfaces;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import net.pedroksl.advanced_ae.common.logic.AdvCraftingCPULogic;

public interface IPatternProviderCpu {
    void setCpuCluster(CraftingCPUCluster cpu);
    CraftingCPUCluster getCpuCluster();
    void setPatternDetails(IPatternDetails iPatternDetails);
    IPatternDetails getPatternDetails();
}