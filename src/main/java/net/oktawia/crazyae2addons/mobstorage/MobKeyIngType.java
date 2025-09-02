package net.oktawia.crazyae2addons.mobstorage;

import mezz.jei.api.ingredients.IIngredientType;

public final class MobKeyIngType {
    private MobKeyIngType() {}
    public static final IIngredientType<MobKeyIng> TYPE = () -> MobKeyIng.class;
}
