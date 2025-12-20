package net.oktawia.crazyae2addons.xei.jei;

import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;

public class CrazyCategory extends ModularUIRecipeCategory<CrazyWrapper> {

    public static final ResourceLocation UID = new ResourceLocation("crazyae2addons", "crazy_multiblocks");
    public static final RecipeType<CrazyWrapper> TYPE = new RecipeType<>(UID, CrazyWrapper.class);

    private final IDrawable background;
    private final IDrawable icon;

    public CrazyCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(160, 180);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(CrazyItemRegistrar.SUPER_SINGULARITY.get()));
    }

    @Override
    public RecipeType<CrazyWrapper> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.crazyae2addons.category_crazy_multiblocks");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }
}