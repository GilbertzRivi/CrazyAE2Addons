package net.oktawia.crazyae2addons.recipes;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class CradleRecipe implements Recipe<CradleContext> {
    private final ResourceLocation id;
    private final CradlePattern pattern;
    private final Block resultBlock;
    private final String description;

    public CradleRecipe(ResourceLocation id, CradlePattern pattern, Block resultBlock, String description) {
        this.id = id;
        this.pattern = pattern;
        this.resultBlock = resultBlock;
        this.description = description;
    }

    public CradlePattern pattern() { return pattern; }
    public Block resultBlock() { return resultBlock; }
    public String description() { return description; }

    @Override public boolean matches(CradleContext ctx, Level level) {
        return pattern.matches(level, ctx.origin(), ctx.facing());
    }

    @Override public ItemStack assemble(CradleContext ctx, RegistryAccess regs) {
        return ItemStack.EMPTY;
    }

    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess regs) { return ItemStack.EMPTY; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return CradleRecipeSerializer.INSTANCE; }
    @Override public net.minecraft.world.item.crafting.RecipeType<?> getType() { return CradleRecipeType.INSTANCE; }
    @Override public boolean isSpecial() { return true; }
}
