package net.oktawia.crazyae2addons.logic.buffer;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;

public class ManagedBufferLogic extends PatternProviderLogic {

    private final ManagedBuffer buffer;

    public ManagedBufferLogic(IManagedGridNode mainNode, PatternProviderLogicHost host, ManagedBuffer buffer) {
        super(mainNode, host, 1);
        this.buffer = buffer;
        getConfigManager().putSetting(Settings.PATTERN_ACCESS_TERMINAL, YesNo.NO);
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        for (var counter : inputHolder) {
            for (var entry : counter) {
                if (entry.getLongValue() > 0 && entry.getKey() != null) {
                    buffer.add(entry.getKey(), entry.getLongValue());
                }
            }
        }
        buffer.onPushPatternComplete();
        return true;
    }
}
