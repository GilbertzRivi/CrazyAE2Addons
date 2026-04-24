package net.oktawia.crazyae2addons.xei.common;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.recipes.FabricationRecipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CrazyRecipes {

    private CrazyRecipes() {
    }

    private static FluidStack toLDFluid(net.minecraftforge.fluids.FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FluidStack.empty();
        }

        return FluidStack.create(stack.getFluid(), (long) stack.getAmount(), stack.getTag());
    }

    public static List<FabricationEntry> getFabricationEntries() {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) {
            return List.of();
        }

        var recipeManager = level.getRecipeManager();
        var recipes = recipeManager.getAllRecipesFor(
                net.oktawia.crazyae2addons.defs.regs.CrazyRecipes.FABRICATION_TYPE.get()
        );

        return recipes.stream()
                .map(recipe -> {
                    List<ItemStack> inputs = new ArrayList<>();

                    for (FabricationRecipe.Entry entry : recipe.getInputs()) {
                        ItemStack[] choices = entry.ingredient().getItems();
                        ItemStack in = choices.length > 0 ? choices[0].copy() : ItemStack.EMPTY;
                        if (!in.isEmpty()) {
                            in.setCount(Math.max(1, entry.count()));
                        }
                        inputs.add(in);
                    }

                    ResourceLocation requiredKey = null;
                    String label = null;

                    if (recipe.getRequiredKey() != null && !recipe.getRequiredKey().isBlank()) {
                        requiredKey = new ResourceLocation(recipe.getRequiredKey());
                        label = requiredKey.toString();
                    }

                    return new FabricationEntry(
                            recipe.getId(),
                            List.copyOf(inputs),
                            recipe.getOutput().copy(),
                            toLDFluid(recipe.getFluidInput()),
                            toLDFluid(recipe.getFluidOutput()),
                            requiredKey,
                            label
                    );
                })
                .sorted(Comparator.comparing(entry -> {
                    if (!entry.output().isEmpty()) {
                        return entry.output().getItem().builtInRegistryHolder().key().location().toString();
                    }
                    if (!entry.fluidOutput().isEmpty()) {
                        return entry.fluidOutput().getFluid().builtInRegistryHolder().key().location().toString();
                    }
                    return entry.id().toString();
                }))
                .toList();
    }
}