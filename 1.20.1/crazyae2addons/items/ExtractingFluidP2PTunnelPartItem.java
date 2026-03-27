package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addons.parts.ExtractingFluidP2PTunnelPart;
import net.oktawia.crazyae2addons.parts.ExtractingItemP2PTunnelPart;

public class ExtractingFluidP2PTunnelPartItem extends PartItem<ExtractingFluidP2PTunnelPart> {
    public ExtractingFluidP2PTunnelPartItem(Properties properties) {
        super(properties, ExtractingFluidP2PTunnelPart.class, ExtractingFluidP2PTunnelPart::new);
    }
}