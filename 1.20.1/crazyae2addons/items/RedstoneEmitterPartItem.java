package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addons.parts.RedstoneEmitterPart;

public class RedstoneEmitterPartItem extends PartItem<RedstoneEmitterPart> {
    public RedstoneEmitterPartItem(Properties properties) {
        super(properties, RedstoneEmitterPart.class, RedstoneEmitterPart::new);
    }
}