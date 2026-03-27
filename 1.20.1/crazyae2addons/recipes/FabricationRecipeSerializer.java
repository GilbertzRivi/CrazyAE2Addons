package net.oktawia.crazyae2addons.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class FabricationRecipeSerializer implements RecipeSerializer<FabricationRecipe> {

    public static final FabricationRecipeSerializer INSTANCE = new FabricationRecipeSerializer();

    @Override
    public FabricationRecipe fromJson(ResourceLocation id, JsonObject json) throws JsonSyntaxException {
        List<FabricationRecipe.Entry> inputs = new ArrayList<>();

        // --- ITEMS INPUT ---
        if (json.has("input") && json.get("input").isJsonArray()) {
            JsonArray arr = GsonHelper.getAsJsonArray(json, "input");
            if (arr.size() == 0) throw new JsonSyntaxException("Fabrication recipe " + id + " has empty input array");

            for (JsonElement el : arr) {
                JsonObject inObj = GsonHelper.convertToJsonObject(el, "input");
                Ingredient ing = Ingredient.fromJson(inObj);
                int count = GsonHelper.getAsInt(inObj, "count", 1);
                inputs.add(new FabricationRecipe.Entry(ing, count));
            }
        } else {
            JsonObject inObj = GsonHelper.getAsJsonObject(json, "input");
            Ingredient ing = Ingredient.fromJson(inObj);
            int inCount = GsonHelper.getAsInt(json, "input_count", 1);
            inputs.add(new FabricationRecipe.Entry(ing, inCount));
        }

        // --- ITEM OUTPUT (opcjonalny) ---
        ItemStack out = ItemStack.EMPTY;
        if (json.has("output") && json.get("output").isJsonObject()) {
            JsonObject outObj = GsonHelper.getAsJsonObject(json, "output");
            ResourceLocation outRL = new ResourceLocation(GsonHelper.getAsString(outObj, "item"));
            int outCount = GsonHelper.getAsInt(outObj, "count", 1);

            var outItem = ForgeRegistries.ITEMS.getValue(outRL);
            if (outItem == null) throw new JsonSyntaxException("Unknown output item: " + outRL);

            out = new ItemStack(outItem, Math.max(1, outCount));
        }

        // --- FLUID INPUT/OUTPUT (opcjonalne) ---
        FluidStack fin = parseFluidOptional(json, "fluid_input");
        FluidStack fout = parseFluidOptional(json, "fluid_output");

        // musi być coś na wyjściu (item albo fluid)
        if (out.isEmpty() && fout.isEmpty()) {
            throw new JsonSyntaxException("Fabrication recipe " + id + " must have either 'output' or 'fluid_output'");
        }

        String key = json.has("required_key") ? GsonHelper.getAsString(json, "required_key") : null;

        return new FabricationRecipe(id, inputs, out, key, fin, fout);
    }

    private static FluidStack parseFluidOptional(JsonObject root, String field) {
        if (!root.has(field) || !root.get(field).isJsonObject()) {
            return FluidStack.EMPTY;
        }

        JsonObject obj = GsonHelper.getAsJsonObject(root, field);

        if (!obj.has("fluid")) return FluidStack.EMPTY;

        ResourceLocation fluidRL = new ResourceLocation(GsonHelper.getAsString(obj, "fluid"));
        int amount = GsonHelper.getAsInt(obj, "amount", 1000);
        if (amount <= 0) return FluidStack.EMPTY;

        var fluid = ForgeRegistries.FLUIDS.getValue(fluidRL);
        if (fluid == null) throw new JsonSyntaxException("Unknown fluid: " + fluidRL);

        return new FluidStack(fluid, amount);
    }

    @Override
    public FabricationRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        int inputSize = buf.readVarInt();
        List<FabricationRecipe.Entry> inputs = new ArrayList<>(inputSize);
        for (int i = 0; i < inputSize; i++) {
            Ingredient ing = Ingredient.fromNetwork(buf);
            int count = buf.readVarInt();
            inputs.add(new FabricationRecipe.Entry(ing, count));
        }

        boolean hasItemOut = buf.readBoolean();
        ItemStack out = hasItemOut ? buf.readItem() : ItemStack.EMPTY;

        boolean hasKey = buf.readBoolean();
        String key = hasKey ? buf.readUtf() : null;

        FluidStack fin = buf.readBoolean() ? readFluid(buf) : FluidStack.EMPTY;
        FluidStack fout = buf.readBoolean() ? readFluid(buf) : FluidStack.EMPTY;

        return new FabricationRecipe(id, inputs, out, key, fin, fout);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, FabricationRecipe r) {
        buf.writeVarInt(r.getInputs().size());
        for (FabricationRecipe.Entry entry : r.getInputs()) {
            entry.ingredient().toNetwork(buf);
            buf.writeVarInt(entry.count());
        }

        ItemStack out = r.getOutput();
        boolean hasItemOut = out != null && !out.isEmpty();
        buf.writeBoolean(hasItemOut);
        if (hasItemOut) buf.writeItem(out);

        boolean hasKey = r.getRequiredKey() != null;
        buf.writeBoolean(hasKey);
        if (hasKey) buf.writeUtf(r.getRequiredKey());

        FluidStack fin = r.getFluidInput();
        buf.writeBoolean(!fin.isEmpty());
        if (!fin.isEmpty()) writeFluid(buf, fin);

        FluidStack fout = r.getFluidOutput();
        buf.writeBoolean(!fout.isEmpty());
        if (!fout.isEmpty()) writeFluid(buf, fout);
    }

    private static FluidStack readFluid(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        int amount = buf.readVarInt();
        boolean hasTag = buf.readBoolean();
        CompoundTag tag = hasTag ? buf.readNbt() : null;

        var fluid = ForgeRegistries.FLUIDS.getValue(id);
        if (fluid == null) return FluidStack.EMPTY;
        if (amount <= 0) return FluidStack.EMPTY;

        return tag == null ? new FluidStack(fluid, amount) : new FluidStack(fluid, amount, tag);
    }

    private static void writeFluid(FriendlyByteBuf buf, FluidStack fs) {
        var key = ForgeRegistries.FLUIDS.getKey(fs.getFluid());
        if (key == null) key = new ResourceLocation("minecraft", "empty");

        buf.writeResourceLocation(key);
        buf.writeVarInt(fs.getAmount());

        CompoundTag tag = fs.getTag();
        buf.writeBoolean(tag != null);
        if (tag != null) buf.writeNbt(tag);
    }
}
