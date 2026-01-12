package net.oktawia.crazyae2addonslite.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addonslite.parts.RRFluidP2PTunnelPart;
import net.oktawia.crazyae2addonslite.parts.RRItemP2PTunnelPart;

public class RRFluidP2PTunnelPartItem extends PartItem<RRFluidP2PTunnelPart> {
    public RRFluidP2PTunnelPartItem(Properties properties) {
        super(properties, RRFluidP2PTunnelPart.class, RRFluidP2PTunnelPart::new);
    }
}