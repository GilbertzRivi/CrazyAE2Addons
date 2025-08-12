package net.oktawia.crazyae2addons.recipes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public class CradleRecipeSerializer implements RecipeSerializer<CradleRecipe> {
    public static final CradleRecipeSerializer INSTANCE = new CradleRecipeSerializer();
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public CradleRecipe fromJson(ResourceLocation id, JsonObject json) {
        CradlePattern pattern = CradlePattern.fromJson(json.getAsJsonObject("pattern"));
        Block resultBlock = ForgeRegistries.BLOCKS.getValue(
                new ResourceLocation(json.get("result_block").getAsString())
        );
        return new CradleRecipe(id, pattern, resultBlock);
    }

    @Override
    public CradleRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        String patternStr = buf.readUtf(32767);
        JsonObject patJson = JsonParser.parseString(patternStr).getAsJsonObject();
        CradlePattern pattern = CradlePattern.fromJson(patJson);

        ResourceLocation blockId = buf.readResourceLocation();
        Block resultBlock = ForgeRegistries.BLOCKS.getValue(blockId);

        return new CradleRecipe(id, pattern, resultBlock);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, CradleRecipe recipe) {
        String patternStr = GSON.toJson(recipe.pattern().toJson());
        buf.writeUtf(patternStr);

        buf.writeResourceLocation(ForgeRegistries.BLOCKS.getKey(recipe.resultBlock()));
    }
}
