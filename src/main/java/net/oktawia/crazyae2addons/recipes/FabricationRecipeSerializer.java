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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FabricationRecipeSerializer implements RecipeSerializer<FabricationRecipe> {

    public static final FabricationRecipeSerializer INSTANCE = new FabricationRecipeSerializer();

    @Override
    public FabricationRecipe fromJson(ResourceLocation id, JsonObject json) throws JsonSyntaxException {
        List<FabricationRecipe.Entry> inputs = new ArrayList<>();

        if (json.has("input") && json.get("input").isJsonArray()) {
            JsonArray array = GsonHelper.getAsJsonArray(json, "input");
            if (array.size() == 0) {
                throw new JsonSyntaxException("Fabrication recipe " + id + " has empty input array");
            }

            for (JsonElement element : array) {
                JsonObject inputObject = GsonHelper.convertToJsonObject(element, "input");
                Ingredient ingredient = Ingredient.fromJson(inputObject);
                int count = GsonHelper.getAsInt(inputObject, "count", 1);
                inputs.add(new FabricationRecipe.Entry(ingredient, count));
            }
        } else {
            JsonObject inputObject = GsonHelper.getAsJsonObject(json, "input");
            Ingredient ingredient = Ingredient.fromJson(inputObject);
            int count = GsonHelper.getAsInt(json, "input_count", 1);
            inputs.add(new FabricationRecipe.Entry(ingredient, count));
        }

        ItemStack output = ItemStack.EMPTY;
        if (json.has("output") && json.get("output").isJsonObject()) {
            JsonObject outputObject = GsonHelper.getAsJsonObject(json, "output");
            ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(outputObject, "item"));
            int count = GsonHelper.getAsInt(outputObject, "count", 1);

            var item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                throw new JsonSyntaxException("Unknown output item: " + itemId);
            }

            output = new ItemStack(item, Math.max(1, count));
        }

        FluidStack fluidInput = parseFluidOptional(json, "fluid_input");
        FluidStack fluidOutput = parseFluidOptional(json, "fluid_output");

        if (output.isEmpty() && fluidOutput.isEmpty()) {
            throw new JsonSyntaxException("Fabrication recipe " + id + " must have either 'output' or 'fluid_output'");
        }

        @Nullable String requiredKey = json.has("required_key")
                ? GsonHelper.getAsString(json, "required_key")
                : null;

        return new FabricationRecipe(id, inputs, output, requiredKey, fluidInput, fluidOutput);
    }

    private static FluidStack parseFluidOptional(JsonObject root, String field) {
        if (!root.has(field) || !root.get(field).isJsonObject()) {
            return FluidStack.EMPTY;
        }

        JsonObject object = GsonHelper.getAsJsonObject(root, field);
        if (!object.has("fluid")) {
            return FluidStack.EMPTY;
        }

        ResourceLocation fluidId = new ResourceLocation(GsonHelper.getAsString(object, "fluid"));
        int amount = GsonHelper.getAsInt(object, "amount", 1000);
        if (amount <= 0) {
            return FluidStack.EMPTY;
        }

        var fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
        if (fluid == null) {
            throw new JsonSyntaxException("Unknown fluid: " + fluidId);
        }

        return new FluidStack(fluid, amount);
    }

    @Override
    public FabricationRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<FabricationRecipe.Entry> inputs = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            Ingredient ingredient = Ingredient.fromNetwork(buf);
            int count = buf.readVarInt();
            inputs.add(new FabricationRecipe.Entry(ingredient, count));
        }

        ItemStack output = buf.readBoolean() ? buf.readItem() : ItemStack.EMPTY;

        String requiredKey = buf.readBoolean() ? buf.readUtf() : null;

        FluidStack fluidInput = buf.readBoolean() ? readFluid(buf) : FluidStack.EMPTY;
        FluidStack fluidOutput = buf.readBoolean() ? readFluid(buf) : FluidStack.EMPTY;

        return new FabricationRecipe(id, inputs, output, requiredKey, fluidInput, fluidOutput);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, FabricationRecipe recipe) {
        buf.writeVarInt(recipe.getInputs().size());
        for (FabricationRecipe.Entry entry : recipe.getInputs()) {
            entry.ingredient().toNetwork(buf);
            buf.writeVarInt(entry.count());
        }

        ItemStack output = recipe.getOutput();
        buf.writeBoolean(!output.isEmpty());
        if (!output.isEmpty()) {
            buf.writeItem(output);
        }

        String requiredKey = recipe.getRequiredKey();
        buf.writeBoolean(requiredKey != null);
        if (requiredKey != null) {
            buf.writeUtf(requiredKey);
        }

        FluidStack fluidInput = recipe.getFluidInput();
        buf.writeBoolean(!fluidInput.isEmpty());
        if (!fluidInput.isEmpty()) {
            writeFluid(buf, fluidInput);
        }

        FluidStack fluidOutput = recipe.getFluidOutput();
        buf.writeBoolean(!fluidOutput.isEmpty());
        if (!fluidOutput.isEmpty()) {
            writeFluid(buf, fluidOutput);
        }
    }

    private static FluidStack readFluid(FriendlyByteBuf buf) {
        ResourceLocation fluidId = buf.readResourceLocation();
        int amount = buf.readVarInt();
        CompoundTag tag = buf.readBoolean() ? buf.readNbt() : null;

        var fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
        if (fluid == null || amount <= 0) {
            return FluidStack.EMPTY;
        }

        return tag == null
                ? new FluidStack(fluid, amount)
                : new FluidStack(fluid, amount, tag);
    }

    private static void writeFluid(FriendlyByteBuf buf, FluidStack stack) {
        ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        if (fluidId == null) {
            fluidId = new ResourceLocation("minecraft", "empty");
        }

        buf.writeResourceLocation(fluidId);
        buf.writeVarInt(stack.getAmount());

        CompoundTag tag = stack.getTag();
        buf.writeBoolean(tag != null);
        if (tag != null) {
            buf.writeNbt(tag);
        }
    }
}