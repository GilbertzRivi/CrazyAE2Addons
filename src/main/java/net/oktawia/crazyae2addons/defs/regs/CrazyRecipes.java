package net.oktawia.crazyae2addons.defs.regs;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.oktawia.crazyae2addons.recipes.*;

import static net.oktawia.crazyae2addons.CrazyAddons.MODID;

public final class CrazyRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CradleRecipe>> CRADLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("cradle", () -> CradleRecipeSerializer.INSTANCE);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CradleRecipe>> CRADLE_TYPE =
            RECIPE_TYPES.register("cradle", () -> CradleRecipeType.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ResearchRecipe>> RESEARCH_SERIALIZER =
            RECIPE_SERIALIZERS.register("research", () -> ResearchRecipeSerializer.INSTANCE);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ResearchRecipe>> RESEARCH_TYPE =
            RECIPE_TYPES.register("research", () -> ResearchRecipeType.INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FabricationRecipe>> FABRICATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("fabrication", () -> FabricationRecipeSerializer.INSTANCE);

    public static final DeferredHolder<RecipeType<?>, RecipeType<FabricationRecipe>> FABRICATION_TYPE =
            RECIPE_TYPES.register("fabrication", () -> FabricationRecipeType.INSTANCE);

    private CrazyRecipes() {}
}
