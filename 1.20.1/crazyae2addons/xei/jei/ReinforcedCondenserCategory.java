package net.oktawia.crazyae2addons.xei.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;

import java.util.List;

public class ReinforcedCondenserCategory implements IRecipeCategory<ReinforcedCondenserEntry> {
    public static final ResourceLocation UID = new ResourceLocation("crazyae2addons", "reinforced_condenser");
    public static final RecipeType<ReinforcedCondenserEntry> TYPE =
            RecipeType.create("crazyae2addons", "reinforced_condenser", ReinforcedCondenserEntry.class);

    private final IDrawable background;
    private final IDrawable icon;

    public ReinforcedCondenserCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(130, 80);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(CrazyBlockRegistrar.REINFORCED_MATTER_CONDENSER_BLOCK.get()));
    }

    @Override public RecipeType<ReinforcedCondenserEntry> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.literal("Reinforced Condenser"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ReinforcedCondenserEntry recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 40, 11).addItemStack(recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 80, 11).addItemStack(recipe.output());
    }

    @Override
    public void draw(ReinforcedCondenserEntry recipe, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, "→", 64, 16, 0x999999, false);

        List<String> lines = List.of(
                "You have to fill the",
                "condenser with 64 times",
                "256k Storage Components",
                "for it to start operating"
        );


        int guiWidth = 130;
        int centerX = guiWidth / 2;
        int startY = 35;

        for (int i = 0; i < lines.size(); i++) {
            String text = lines.get(i);
            int width = font.width(text);
            int x = centerX - width / 2;
            graphics.drawString(font, text, x, startY + i * 10, 0x555555, false);
        }

    }
}
