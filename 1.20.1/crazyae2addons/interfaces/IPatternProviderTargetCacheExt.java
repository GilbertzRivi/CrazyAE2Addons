package net.oktawia.crazyae2addons.interfaces;

import appeng.api.crafting.IPatternDetails;
import appeng.helpers.patternprovider.PatternProviderTarget;

public interface IPatternProviderTargetCacheExt {
    PatternProviderTarget find(IPatternDetails patternDetails);
    void setDetails(IPatternDetails patternDetails);
}
