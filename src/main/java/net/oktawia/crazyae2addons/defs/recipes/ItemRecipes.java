package net.oktawia.crazyae2addons.defs.recipes;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ItemRecipes {

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
        recipe("display")
                .shaped("TL/L ")
                .define('T', AEParts.SEMI_DARK_MONITOR)
                .define('L', AEItems.ADVANCED_CARD)
                .output(CrazyItemRegistrar.DISPLAY.get())
                .register();
        recipe("emitter_terminal")
                .shaped("ATR")
                .define('A', AEItems.ADVANCED_CARD)
                .define('T', AEParts.TERMINAL)
                .define('R', AEParts.LEVEL_EMITTER)
                .output(CrazyItemRegistrar.EMITTER_TERMINAL.get())
                .register();
        recipe("wireless_emitter_terminal")
                .shaped("R/T/D")
                .define('R', AEItems.WIRELESS_RECEIVER)
                .define('T', CrazyItemRegistrar.EMITTER_TERMINAL.get())
                .define('D', AEBlocks.DENSE_ENERGY_CELL)
                .output(CrazyItemRegistrar.WIRELESS_EMITTER_TERMINAL.get())
                .register();
        recipe("redstone_terminal")
                .shaped("ATR")
                .define('A', AEItems.ADVANCED_CARD)
                .define('T', AEParts.TERMINAL)
                .define('R', CrazyItemRegistrar.REDSTONE_EMITTER.get())
                .output(CrazyItemRegistrar.REDSTONE_TERMINAL.get())
                .register();
        recipe("wireless_redstone_terminal")
                .shaped("R/T/D")
                .define('R', AEItems.WIRELESS_RECEIVER)
                .define('T', CrazyItemRegistrar.REDSTONE_TERMINAL.get())
                .define('D', AEBlocks.DENSE_ENERGY_CELL)
                .output(CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get())
                .register();
        recipe("wireless_notification_terminal")
                .shaped("ATC")
                .define('A', AEItems.ADVANCED_CARD)
                .define('T', CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get())
                .define('C', AEBlocks.CONTROLLER)
                .output(CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get())
                .register();
        recipe("round_robin_item_p2p_tunnel")
                .shaped("PC")
                .define('P', AEParts.ITEM_P2P_TUNNEL)
                .define('C', AEItems.EQUAL_DISTRIBUTION_CARD)
                .output(CrazyItemRegistrar.RR_ITEM_P2P.get())
                .register();
        recipe("round_robin_fluid_p2p_tunnel")
                .shaped("PC")
                .define('P', AEParts.FLUID_P2P_TUNNEL)
                .define('C', AEItems.EQUAL_DISTRIBUTION_CARD)
                .output(CrazyItemRegistrar.RR_FLUID_P2P.get())
                .register();
        recipe("tag_view_cell")
                .shaped("DI")
                .define('D', AEItems.VIEW_CELL)
                .define('I', Items.BOOK)
                .output(CrazyItemRegistrar.TAG_VIEW_CELL.get())
                .register();
        recipe("crazy_upgrade")
                .shaped("PPP/PDP/PPP")
                .define('P', AEBlocks.PATTERN_PROVIDER)
                .define('D', AEItems.ADVANCED_CARD)
                .output(CrazyItemRegistrar.CRAZY_UPGRADE.get())
                .register();
        recipe("crazy_pattern_provider_part")
                .shapeless(CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get())
                .output(CrazyItemRegistrar.CRAZY_PATTERN_PROVIDER_PART.get())
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
