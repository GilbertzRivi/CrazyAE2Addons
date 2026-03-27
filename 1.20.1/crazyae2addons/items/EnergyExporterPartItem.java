package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.oktawia.crazyae2addons.parts.EnergyExporterPart;

public class EnergyExporterPartItem extends PartItem<EnergyExporterPart> {
    public EnergyExporterPartItem(Properties properties) {
        super(properties, EnergyExporterPart.class, EnergyExporterPart::new);
    }
}