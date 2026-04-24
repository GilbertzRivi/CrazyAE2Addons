package net.oktawia.crazyae2addons.defs.regs;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addons.recipes.FabricationRecipe;
import net.oktawia.crazyae2addons.recipes.FabricationRecipeSerializer;
import net.oktawia.crazyae2addons.recipes.FabricationRecipeType;

import static net.oktawia.crazyae2addons.CrazyAddons.MODID;

public final class CrazyRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MODID);

    public static final RegistryObject<RecipeSerializer<FabricationRecipe>> FABRICATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("fabrication", () -> FabricationRecipeSerializer.INSTANCE);

    public static final RegistryObject<RecipeType<FabricationRecipe>> FABRICATION_TYPE =
            RECIPE_TYPES.register("fabrication", () -> FabricationRecipeType.INSTANCE);

    private CrazyRecipes() {
    }
}