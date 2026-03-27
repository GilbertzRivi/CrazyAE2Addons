package net.oktawia.crazyae2addons.xei.jei;

import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import org.jetbrains.annotations.Nullable;

public class FabricationCategory extends ModularUIRecipeCategory<FabricationWrapper> {
    public static final RecipeType<FabricationWrapper> TYPE =
            RecipeType.create("crazyae2addons", "fabrication", FabricationWrapper.class);

    private final IDrawable background;
    private final IDrawable icon;

    public FabricationCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(160, 100);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK.get().asItem()));
    }

    @Override
    public RecipeType<FabricationWrapper> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Recipe Fabricator");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }
}
