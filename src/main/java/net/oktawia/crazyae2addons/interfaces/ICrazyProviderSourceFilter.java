package net.oktawia.crazyae2addons.interfaces;

import appeng.api.networking.security.IActionSource;
import org.jetbrains.annotations.Nullable;

public interface ICrazyProviderSourceFilter {
    boolean allowSource(@Nullable IActionSource src);
}
