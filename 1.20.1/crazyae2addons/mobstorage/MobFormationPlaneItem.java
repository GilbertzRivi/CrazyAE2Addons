package net.oktawia.crazyae2addons.mobstorage;

import appeng.items.parts.PartItem;

public class MobFormationPlaneItem extends PartItem<MobFormationPlane> {
    public MobFormationPlaneItem(Properties properties) {
        super(properties, MobFormationPlane.class, MobFormationPlane::new);
    }
}