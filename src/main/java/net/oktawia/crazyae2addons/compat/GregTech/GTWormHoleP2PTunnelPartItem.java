package net.oktawia.crazyae2addons.compat.GregTech;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addons.parts.WormholeP2PTunnelPart;

public class GTWormHoleP2PTunnelPartItem extends PartItem<GTWormholeP2PTunnelPart> {
    public GTWormHoleP2PTunnelPartItem(Properties properties) {
        super(properties, GTWormholeP2PTunnelPart.class, GTWormholeP2PTunnelPart::new);
    }
}