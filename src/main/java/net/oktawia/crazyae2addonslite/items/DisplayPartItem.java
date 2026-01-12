package net.oktawia.crazyae2addonslite.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addonslite.parts.DisplayPart;


public class DisplayPartItem extends PartItem<DisplayPart> {
    public DisplayPartItem(Properties properties) {
        super(properties, DisplayPart.class, DisplayPart::new);
    }
}