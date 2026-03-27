package net.oktawia.crazyae2addons.mobstorage;

import appeng.items.parts.PartItem;

public class MobExportBusItem extends PartItem<MobExportBus> {
    public MobExportBusItem(Properties properties) {
        super(properties, MobExportBus.class, MobExportBus::new);
    }
}