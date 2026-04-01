package net.oktawia.crazyae2addons.interfaces;

import appeng.api.stacks.AEItemKey;

public interface IAutoBuilderLogicHost {
    void addToBuildBuffer(AEItemKey key, long amount);
    void cancelCraftNoFlush();
    void onRedstoneActivate();
}
