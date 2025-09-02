package net.oktawia.crazyae2addons.xei.jei;

import appeng.api.integrations.jei.IngredientConverter;
import appeng.api.stacks.GenericStack;
import net.oktawia.crazyae2addons.mobstorage.MobKeyIng;
import net.oktawia.crazyae2addons.mobstorage.MobKeyIngType;
import org.jetbrains.annotations.Nullable;
import net.oktawia.crazyae2addons.mobstorage.MobKey;

public class MobKeyIngredientConverter implements IngredientConverter<MobKeyIng> {

    @Override
    public mezz.jei.api.ingredients.IIngredientType<MobKeyIng> getIngredientType() {
        return MobKeyIngType.TYPE;
    }

    @Override
    public @Nullable GenericStack getStackFromIngredient(MobKeyIng ingredient) {
        return new GenericStack(ingredient.key(), 1);
    }

    @Override
    public @Nullable MobKeyIng getIngredientFromStack(GenericStack stack) {
        if (stack.what() instanceof MobKey key) {
            return MobKeyIng.of(key);
        }
        return null;
    }
}
