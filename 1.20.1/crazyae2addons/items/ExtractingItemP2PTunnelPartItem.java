package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addons.parts.ExtractingItemP2PTunnelPart;
import net.oktawia.crazyae2addons.parts.RRItemP2PTunnelPart;

public class ExtractingItemP2PTunnelPartItem extends PartItem<ExtractingItemP2PTunnelPart> {
    public ExtractingItemP2PTunnelPartItem(Properties properties) {
        super(properties, ExtractingItemP2PTunnelPart.class, ExtractingItemP2PTunnelPart::new);
    }
}