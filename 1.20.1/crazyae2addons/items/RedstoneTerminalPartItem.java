package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addons.parts.RedstoneEmitterPart;
import net.oktawia.crazyae2addons.parts.RedstoneTerminalPart;

public class RedstoneTerminalPartItem extends PartItem<RedstoneTerminalPart> {
    public RedstoneTerminalPartItem(Properties properties) {
        super(properties, RedstoneTerminalPart.class, RedstoneTerminalPart::new);
    }
}