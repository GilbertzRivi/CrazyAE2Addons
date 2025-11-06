package net.oktawia.crazyae2addons.recipes;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.defs.regs.CrazyRecipes;
import net.oktawia.crazyae2addons.items.StructureGadgetItem;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ResearchRecipe implements Recipe<Container> {
    public final int duration;
    public final int energyPerTick;
    public final ResourceLocation fluid;
    public final int fluidPerTick;
    public final boolean requiresStabilizer;

    public final boolean gadgetRequired;
    public final boolean driveRequired;
    public final List<Consumable> consumables;
    public final Structure structure;
    public final Unlock unlock;

    private ResourceLocation id;

    public ResearchRecipe(ResourceLocation id,
                          int duration, int ept, ResourceLocation fluid, int fpt, boolean reqStab,
                          boolean gadgetRequired, boolean driveRequired,
                          List<Consumable> consumables, Structure structure, Unlock unlock) {
        this.id = id;
        this.duration = duration;
        this.energyPerTick = ept;
        this.fluid = fluid;
        this.fluidPerTick = fpt;
        this.requiresStabilizer = reqStab;
        this.gadgetRequired = gadgetRequired;
        this.driveRequired = driveRequired;
        this.consumables = List.copyOf(consumables);
        this.structure = structure;
        this.unlock = unlock;
    }

    public long totalEnergy() { return (long) duration * (long) energyPerTick; }
    public long totalFluid()  { return (long) duration * (long) fluidPerTick; }
    public int  sizeX()       { return structure.size[0]; }
    public int  sizeY()       { return structure.size[1]; }
    public int  sizeZ()       { return structure.size[2]; }

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

        ItemStack gadget = ItemStack.EMPTY;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty() && st.getItem() instanceof StructureGadgetItem) {
                gadget = st;
                break;
            }
        }
        boolean hasGadget = !gadget.isEmpty();

        if (this.gadgetRequired && !hasGadget) return false;

        if (this.structure.mode == StructureMode.NONE) {
            return true;
        }

        if (!hasGadget) return false;

        StructureSnapshot snap = StructureGadgetItem.loadSnapshot(gadget, level);
        if (snap == null) {
            return false;
        } else {
            ResearchStructureMatcher matcher = new ResearchStructureMatcher();
            ResearchStructureMatcher.MatchResult res = matcher.match(this.structure, snap);
            return res.ok;
        }
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
        public Consumable(Item item, int count) { this.item = item; this.count = count; }
    }

    public static final class Unlock {
        public final ResourceLocation key;
        public final String label;
        public Unlock(ResourceLocation key, String label) { this.key = key; this.label = label; }
    }

    public enum StructureMode { PATTERN, SIZE_ONLY, NONE }

    public static final class Structure {
        public final StructureMode mode;
        public final int[] size; // [x,y,z]
        // pola wykorzystywane tylko dla PATTERN:
        public final Map<String, List<ResourceLocation>> symbols; // "A" -> [block ids]
        public final List<List<String>> layers; // [y][z] -> "A A . B"

        public Structure(StructureMode mode, int[] size,
                         Map<String, List<ResourceLocation>> symbols,
                         List<List<String>> layers) {
            this.mode = mode;
            this.size = size == null ? new int[]{0,0,0} : size;
            this.symbols = symbols == null ? Map.of() : symbols;
            this.layers  = layers  == null ? List.of() : layers;
        }
    }
}
