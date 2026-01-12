package net.oktawia.crazyae2addonslite.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addonslite.parts.RedstoneTerminalPart;

public class RedstoneTerminalPartItem extends PartItem<RedstoneTerminalPart> {
    public RedstoneTerminalPartItem(Properties properties) {
        super(properties, RedstoneTerminalPart.class, RedstoneTerminalPart::new);
    }
}