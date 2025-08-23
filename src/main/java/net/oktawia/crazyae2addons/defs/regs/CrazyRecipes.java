package net.oktawia.crazyae2addons.defs.regs;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addons.recipes.*;

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

    public static final RegistryObject<RecipeSerializer<ResearchRecipe>> RESEARCH_SERIALIZER =
            RECIPE_SERIALIZERS.register("research", () -> ResearchRecipeSerializer.INSTANCE);

    public static final RegistryObject<RecipeType<ResearchRecipe>> RESEARCH_TYPE =
            RECIPE_TYPES.register("research", () -> ResearchRecipeType.INSTANCE);

    public static final RegistryObject<RecipeSerializer<FabricationRecipe>> FABRICATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("fabrication", () -> FabricationRecipeSerializer.INSTANCE);

    public static final RegistryObject<RecipeType<FabricationRecipe>> FABRICATION_TYPE =
            RECIPE_TYPES.register("fabrication", () -> FabricationRecipeType.INSTANCE);


    private CrazyRecipes() {}
}