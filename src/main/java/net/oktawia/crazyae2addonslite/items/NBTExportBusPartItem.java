package net.oktawia.crazyae2addonslite.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addonslite.parts.NBTExportBusPart;

public class NBTExportBusPartItem extends PartItem<NBTExportBusPart> {
    public NBTExportBusPartItem(Properties properties) {
        super(properties, NBTExportBusPart.class, NBTExportBusPart::new);
    }
}