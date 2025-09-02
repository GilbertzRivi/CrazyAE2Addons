package net.oktawia.crazyae2addons.xei.jei;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;
import net.oktawia.crazyae2addons.mobstorage.MobKeyIng;
import net.oktawia.crazyae2addons.mobstorage.MobKeyIngType;
import org.jetbrains.annotations.Nullable;

public class MobKeyIngHelper implements IIngredientHelper<MobKeyIng> {

    @Override
    public IIngredientType<MobKeyIng> getIngredientType() {
        return MobKeyIngType.TYPE;
    }

    @Override
    public String getUniqueId(MobKeyIng ingredient, UidContext context) {
        return "crazyae2addons:mobkey/" + ingredient.id();
    }

    @Override
    public ResourceLocation getResourceLocation(MobKeyIng ingredient) {
        // RL entita (np. minecraft:zombie)
        return ingredient.id();
    }

    @Override
    public String getDisplayName(MobKeyIng ingredient) {
        return ingredient.key().getDisplayName().getString();
    }

    @Override
    public MobKeyIng copyIngredient(MobKeyIng ingredient) {
        return new MobKeyIng(ingredient.key(), ingredient.id());
    }

    @Override
    public String getErrorInfo(@Nullable MobKeyIng ingredient) {
        return ingredient == null ? "MobKeyIng[null]" : "MobKeyIng[" + ingredient.id() + "]";
    }
}
