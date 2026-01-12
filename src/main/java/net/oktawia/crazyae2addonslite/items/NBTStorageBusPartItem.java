package net.oktawia.crazyae2addonslite.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addonslite.parts.NBTStorageBusPart;

public class NBTStorageBusPartItem extends PartItem<NBTStorageBusPart> {
    public NBTStorageBusPartItem(Properties properties) {
        super(properties, NBTStorageBusPart.class, NBTStorageBusPart::new);
    }
}