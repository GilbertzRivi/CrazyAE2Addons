package net.oktawia.crazyae2addons.logic;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import net.oktawia.crazyae2addons.interfaces.IAutoBuilderLogicHost;

public class AutoBuilderLogic extends PatternProviderLogic {

    private final IAutoBuilderLogicHost builderHost;

    public AutoBuilderLogic(IManagedGridNode mainNode, PatternProviderLogicHost logicHost,
                            IAutoBuilderLogicHost builderHost) {
        super(mainNode, logicHost, 1);
        this.builderHost = builderHost;
        getConfigManager().putSetting(Settings.PATTERN_ACCESS_TERMINAL, YesNo.NO);
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        for (var counter : inputHolder) {
            for (var entry : counter) {
                var key = entry.getKey();
                long amt = entry.getLongValue();
                if (amt <= 0) continue;
                if (key instanceof AEItemKey itemKey) {
                    builderHost.addToBuildBuffer(itemKey, amt);
                }
            }
        }
        builderHost.cancelCraftNoFlush();
        builderHost.onRedstoneActivate();
        return true;
    }
}
