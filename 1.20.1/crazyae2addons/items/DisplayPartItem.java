package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addons.parts.DisplayPart;


public class DisplayPartItem extends PartItem<DisplayPart> {
    public DisplayPartItem(Properties properties) {
        super(properties, DisplayPart.class, DisplayPart::new);
    }
}