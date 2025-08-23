package net.oktawia.crazyae2addons.xei.jei;

import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import org.jetbrains.annotations.Nullable;

public class ResearchCategory extends ModularUIRecipeCategory<ResearchWrapper> {
    public static final RecipeType<ResearchWrapper> TYPE =
            RecipeType.create("crazyae2addons", "research", ResearchWrapper.class);

    private final IDrawable background;
    private final IDrawable icon;

    public ResearchCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(160, 200);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(CrazyItemRegistrar.DATA_DRIVE.get()));
    }

    @Override
    public RecipeType<ResearchWrapper> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Research Station");
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
