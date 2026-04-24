package net.oktawia.crazyae2addons.defs.recipes;

import appeng.core.definitions.AEBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BlockRecipes {

    public record RecipeDef(
            String id,
            @Nullable String pattern,
            Map<Character, Item> keys,
            List<Item> shapelessIngredients,
            Item output,
            int count
    ) {}

    private static final List<RecipeDef> RECIPES = new ArrayList<>();

    public static List<RecipeDef> getRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static Builder recipe(String id) {
        return new Builder(id);
    }

    public static void registerRecipes() {
        recipe("crazy_pattern_provider")
                .shapeless(CrazyItemRegistrar.CRAZY_PATTERN_PROVIDER_PART.get())
                .output(CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get())
                .register();
        recipe("recipe_fabricator")
                .shaped("LCR")
                .define('L', AEBlocks.PATTERN_PROVIDER)
                .define('C', Blocks.CRAFTING_TABLE)
                .define('R', AEBlocks.CONTROLLER)
                .output(CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK.get())
                .register();
    }

    public static class Builder {
        private final String id;
        private String pattern = null;
        private final Map<Character, Item> keys = new LinkedHashMap<>();
        private final List<Item> shapelessIngredients = new ArrayList<>();
        private Item output = null;
        private int count = 1;

        private Builder(String id) {
            this.id = id;
        }

        public Builder shaped(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder define(char key, ItemLike item) {
            keys.put(key, item.asItem());
            return this;
        }

        public Builder shapeless(ItemLike... ingredients) {
            for (var i : ingredients) shapelessIngredients.add(i.asItem());
            return this;
        }

        public Builder output(ItemLike item) {
            this.output = item.asItem();
            return this;
        }

        public Builder output(ItemLike item, int count) {
            this.output = item.asItem();
            this.count = count;
            return this;
        }

        public void register() {
            RECIPES.add(new RecipeDef(id, pattern, Map.copyOf(keys), List.copyOf(shapelessIngredients), output, count));
        }
    }
}
