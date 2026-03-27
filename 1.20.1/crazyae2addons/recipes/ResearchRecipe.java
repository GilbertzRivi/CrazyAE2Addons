package net.oktawia.crazyae2addons.recipes;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.defs.regs.CrazyRecipes;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ResearchRecipe implements Recipe<Container> {
    public final int duration;
    public final int energyPerTick;
    public final boolean driveRequired;
    public final List<Consumable> consumables;
    public final Unlock unlock;

    private ResourceLocation id;

    public ResearchRecipe(ResourceLocation id,
                          int duration, int ept,
                          boolean driveRequired,
                          List<Consumable> consumables, Unlock unlock) {
        this.id = id;
        this.duration = duration;
        this.energyPerTick = ept;
        this.driveRequired = driveRequired;
        this.consumables = List.copyOf(consumables);
        this.unlock = unlock;
    }

    public long totalEnergy() { return (long) duration * (long) energyPerTick; }

    @Override
    public boolean matches(@NotNull Container inv, @NotNull Level level) {
        for (Consumable c : this.consumables) {
            int have = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack st = inv.getItem(i);
                if (!st.isEmpty() && st.getItem() == c.item) {
                    have += st.getCount();
                    if (have >= c.count) break;
                }
            }
            if (have < c.count) return false;
        }
        return true;
    }

    @Override public ItemStack assemble(Container inv, RegistryAccess reg) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int w, int h) { return false; }
    @Override public ItemStack getResultItem(RegistryAccess reg) { return ItemStack.EMPTY; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return CrazyRecipes.RESEARCH_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return CrazyRecipes.RESEARCH_TYPE.get(); }

    public static final class Consumable {
        public final Item item;
        public final int count;
        public final int computation;
        public Consumable(Item item, int count, int computation) { this.item = item; this.count = count; this.computation = computation; }
    }

    public static final class Unlock {
        public final ResourceLocation key;
        public final String label;
        public final String item;
        public Unlock(ResourceLocation key, String label, String item) { this.key = key; this.label = label; this.item = item; }
    }
}
