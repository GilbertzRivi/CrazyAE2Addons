package net.oktawia.crazyae2addonslite.mixins;

import appeng.parts.p2p.P2PTunnelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(P2PTunnelPart.class)
public interface P2PTunnelPartAccessor {
    @Accessor("output")
    void setOutput(boolean output);
}