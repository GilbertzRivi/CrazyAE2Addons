package net.oktawia.crazyae2addonslite.mixins;

import appeng.api.networking.security.IActionSource;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.me.helpers.MachineSource;
import appeng.me.helpers.PlayerSource;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addonslite.entities.CrazyPatternProviderBE;
import net.oktawia.crazyae2addonslite.interfaces.ICrazyProviderSourceFilter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

@Mixin(value = PatternProviderLogic.class, priority = 1100)
public abstract class MixinPatternProviderLogic implements ICrazyProviderSourceFilter {

    @Shadow @Final private PatternProviderLogicHost host;

    @Unique
    @Override
    public boolean allowSource(@Nullable IActionSource src) {
        if (src == null) return true;
        var be = host != null ? host.getBlockEntity() : null;
        if (!(be instanceof CrazyPatternProviderBE cpp)) return true;
        var upgrades = cpp.getUpgrades();
        if (upgrades == null) return true;
        if (upgrades.isInstalled(CrazyItemRegistrar.AUTOMATION_UPGRADE_CARD.get())) {
            return src instanceof MachineSource;
        }
        if (upgrades.isInstalled(CrazyItemRegistrar.PLAYER_UPGRADE_CARD.get())) {
            return src instanceof PlayerSource;
        }
        return true;
    }

}
