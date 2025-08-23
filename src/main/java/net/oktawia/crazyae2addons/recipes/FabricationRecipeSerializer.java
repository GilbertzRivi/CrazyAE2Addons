package net.oktawia.crazyae2addons.recipes;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;

public class FabricationRecipeSerializer implements RecipeSerializer<FabricationRecipe> {

    public static final FabricationRecipeSerializer INSTANCE = new FabricationRecipeSerializer();

    @Override
    public FabricationRecipe fromJson(ResourceLocation id, JsonObject json) throws JsonSyntaxException {
        Ingredient ing = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "input"));
        int inCount = GsonHelper.getAsInt(json, "input_count", 1);

        JsonObject outObj = GsonHelper.getAsJsonObject(json, "output");
        ResourceLocation outRL = new ResourceLocation(GsonHelper.getAsString(outObj, "item"));
        int outCount = GsonHelper.getAsInt(outObj, "count", 1);
        var outItem = ForgeRegistries.ITEMS.getValue(outRL);
        if (outItem == null) throw new JsonSyntaxException("Unknown output item: " + outRL);
        ItemStack out = new ItemStack(outItem, Math.max(1, outCount));

        String key = json.has("required_key") ? GsonHelper.getAsString(json, "required_key") : null;

        return new FabricationRecipe(id, ing, inCount, out, key);
    }

    @Override
    public FabricationRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        Ingredient ing = Ingredient.fromNetwork(buf);
        int inCount = buf.readVarInt();
        ItemStack out = buf.readItem();
        String key = buf.readBoolean() ? buf.readUtf() : null;
        return new FabricationRecipe(id, ing, inCount, out, key);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, FabricationRecipe r) {
        r.input.toNetwork(buf);
        buf.writeVarInt(r.getInputCount());
        buf.writeItem(r.getOutput());
        boolean hasKey = r.getRequiredKey() != null;
        buf.writeBoolean(hasKey);
        if (hasKey) buf.writeUtf(r.getRequiredKey());
    }
}
