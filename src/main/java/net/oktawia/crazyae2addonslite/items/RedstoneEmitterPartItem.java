package net.oktawia.crazyae2addonslite.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addonslite.parts.RedstoneEmitterPart;

public class RedstoneEmitterPartItem extends PartItem<RedstoneEmitterPart> {
    public RedstoneEmitterPartItem(Properties properties) {
        super(properties, RedstoneEmitterPart.class, RedstoneEmitterPart::new);
    }
}