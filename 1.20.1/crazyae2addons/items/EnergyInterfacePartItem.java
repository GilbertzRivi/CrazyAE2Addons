package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addons.parts.EnergyInterfacePart;

public class EnergyInterfacePartItem extends PartItem<EnergyInterfacePart> {
    public EnergyInterfacePartItem(Properties properties) {
        super(properties, EnergyInterfacePart.class, EnergyInterfacePart::new);
    }
}