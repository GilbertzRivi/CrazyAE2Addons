package net.oktawia.crazyae2addons.compat.GregTech;

import appeng.items.parts.PartItem;

public class GTEnergyExporterPartItem extends PartItem<GTEnergyExporterPart> {
    public GTEnergyExporterPartItem(Properties properties) {
        super(properties, GTEnergyExporterPart.class, GTEnergyExporterPart::new);
    }
}