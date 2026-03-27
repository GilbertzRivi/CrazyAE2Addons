package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addons.parts.NBTExportBusPart;
import net.oktawia.crazyae2addons.parts.NBTStorageBusPart;

public class NBTStorageBusPartItem extends PartItem<NBTStorageBusPart> {
    public NBTStorageBusPartItem(Properties properties) {
        super(properties, NBTStorageBusPart.class, NBTStorageBusPart::new);
    }
}