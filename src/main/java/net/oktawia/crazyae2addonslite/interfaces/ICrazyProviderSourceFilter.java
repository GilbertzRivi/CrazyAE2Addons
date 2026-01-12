package net.oktawia.crazyae2addonslite.interfaces;

import appeng.api.networking.security.IActionSource;
import org.jetbrains.annotations.Nullable;

public interface ICrazyProviderSourceFilter {
    boolean allowSource(@Nullable IActionSource src);
}
