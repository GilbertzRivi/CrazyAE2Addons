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

public class CradleCategory extends ModularUIRecipeCategory<CradleWrapper> {
    public static final RecipeType<CradleWrapper> TYPE =
            RecipeType.create("crazyae2addons", "cradle", CradleWrapper.class);
    private final IDrawable icon;
    private final IDrawable background;

    public CradleCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(160, 200);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(CrazyBlockRegistrar.ENTROPY_CRADLE_CONTROLLER.get()));
    }

    @Override
    public RecipeType<CradleWrapper> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.crazyae2addons.category_cradle");
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