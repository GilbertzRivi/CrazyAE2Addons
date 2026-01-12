package net.oktawia.crazyae2addonslite.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addonslite.parts.WormholeP2PTunnelPart;

public class WormHoleP2PTunnelPartItem extends PartItem<WormholeP2PTunnelPart> {
    public WormHoleP2PTunnelPartItem(Properties properties) {
        super(properties, WormholeP2PTunnelPart.class, WormholeP2PTunnelPart::new);
    }
}