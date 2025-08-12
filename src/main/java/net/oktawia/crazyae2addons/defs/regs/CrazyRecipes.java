package net.oktawia.crazyae2addons.defs.regs;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addons.recipes.CradleRecipe;
import net.oktawia.crazyae2addons.recipes.CradleRecipeSerializer;
import net.oktawia.crazyae2addons.recipes.CradleRecipeType;

import static net.oktawia.crazyae2addons.CrazyAddons.MODID;

public final class CrazyRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MODID);

    public static final RegistryObject<RecipeSerializer<CradleRecipe>> CRADLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("cradle", () -> CradleRecipeSerializer.INSTANCE);

    public static final RegistryObject<RecipeType<CradleRecipe>> CRADLE_TYPE =
            RECIPE_TYPES.register("cradle", () -> CradleRecipeType.INSTANCE);

    private CrazyRecipes() {}
}
