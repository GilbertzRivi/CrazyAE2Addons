package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addons.parts.EntityTickerPart;

public class EntityTickerPartItem extends PartItem<EntityTickerPart> {
    public EntityTickerPartItem(Properties properties) {
        super(properties, EntityTickerPart.class, EntityTickerPart::new);
    }
}