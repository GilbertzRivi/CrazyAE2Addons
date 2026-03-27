package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addons.parts.NBTExportBusPart;

public class NBTExportBusPartItem extends PartItem<NBTExportBusPart> {
    public NBTExportBusPartItem(Properties properties) {
        super(properties, NBTExportBusPart.class, NBTExportBusPart::new);
    }
}