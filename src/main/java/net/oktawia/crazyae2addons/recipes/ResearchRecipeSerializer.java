package net.oktawia.crazyae2addons.recipes;

import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ResearchRecipeSerializer implements RecipeSerializer<ResearchRecipe> {

    public static final ResearchRecipeSerializer INSTANCE = new ResearchRecipeSerializer();

    @Override
    public ResearchRecipe fromJson(ResourceLocation id, JsonObject json) throws JsonSyntaxException {
        final int duration = reqInt(json, "duration");
        final int ept      = reqInt(json, "energy_per_tick");
        final ResourceLocation fluid = new ResourceLocation(reqString(json, "fluid"));
        final int fpt      = optInt(json, "fluid_per_tick", optInt(json, "water_per_tick", 0));
        final boolean reqStab = GsonHelper.getAsBoolean(json, "requires_stabilizer", false);

        final boolean gadgetRequired = parsePresenceAsBool(json, "gadget");
        final boolean driveRequired  = parsePresenceAsBool(json, "drive");

        final java.util.List<ResearchRecipe.Consumable> consumables = new ArrayList<>();
        if (json.has("consumables")) {
            JsonArray cons = GsonHelper.getAsJsonArray(json, "consumables");
            for (JsonElement el : cons) {
                JsonObject o = el.getAsJsonObject();
                Item item = readItem(reqString(o, "item"));
                int count = GsonHelper.getAsInt(o, "count", 1);
                consumables.add(new ResearchRecipe.Consumable(item, Math.max(1, count)));
            }
        }

        final JsonObject structJson = json.has("structure") ? GsonHelper.getAsJsonObject(json, "structure") : null;
        final ResearchRecipe.Structure struct = readStructure(structJson);

        // unlock
        JsonObject un = GsonHelper.getAsJsonObject(json, "unlock");
        final ResourceLocation unlockKey = new ResourceLocation(reqString(un, "key"));
        final String unlockLabel = GsonHelper.getAsString(un, "label", "");

        return new ResearchRecipe(
                id,
                duration, ept, fluid, fpt, reqStab,
                gadgetRequired, driveRequired,
                consumables,
                struct,
                new ResearchRecipe.Unlock(unlockKey, unlockLabel)
        );
    }

    // -------------------- NETWORK --------------------

    @Override
    public ResearchRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        // parametry
        int duration = buf.readVarInt();
        int ept      = buf.readVarInt();
        ResourceLocation fluid = buf.readResourceLocation();
        int fpt       = buf.readVarInt();
        boolean reqStab = buf.readBoolean();

        // wejścia
        boolean gadgetRequired = buf.readBoolean();
        boolean driveRequired  = buf.readBoolean();

        int consN = buf.readVarInt();
        java.util.List<ResearchRecipe.Consumable> consumables = new ArrayList<>(consN);
        for (int i = 0; i < consN; i++) {
            Item item = buf.readRegistryId();
            int count = buf.readVarInt();
            consumables.add(new ResearchRecipe.Consumable(item, count));
        }

        // struktura
        ResearchRecipe.Structure struct = readStructure(buf);

        // unlock
        ResourceLocation unlockKey = buf.readResourceLocation();
        String unlockLabel = buf.readUtf(256);

        return new ResearchRecipe(
                id,
                duration, ept, fluid, fpt, reqStab,
                gadgetRequired, driveRequired,
                consumables,
                struct,
                new ResearchRecipe.Unlock(unlockKey, unlockLabel)
        );
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, ResearchRecipe r) {
        // parametry
        buf.writeVarInt(r.duration);
        buf.writeVarInt(r.energyPerTick);
        buf.writeResourceLocation(r.fluid);
        buf.writeVarInt(r.fluidPerTick);
        buf.writeBoolean(r.requiresStabilizer);

        // wejścia
        buf.writeBoolean(r.gadgetRequired);
        buf.writeBoolean(r.driveRequired);

        buf.writeVarInt(r.consumables.size());
        for (var c : r.consumables) {
            buf.writeRegistryId(ForgeRegistries.ITEMS, c.item);
            buf.writeVarInt(c.count);
        }

        // struktura (zapisujemy enum + size; dla NONE size będzie [0,0,0])
        writeStructure(buf, r.structure);

        // unlock
        buf.writeResourceLocation(r.unlock.key);
        buf.writeUtf(r.unlock.label == null ? "" : r.unlock.label);
    }

    private static ResearchRecipe.Structure readStructure(@Nullable JsonObject obj) {
        if (obj == null) {
            return new ResearchRecipe.Structure(ResearchRecipe.StructureMode.NONE, new int[]{0,0,0}, Map.of(), List.of());
        }

        String modeStr = GsonHelper.getAsString(obj, "mode", "none");
        ResearchRecipe.StructureMode mode;
        if (modeStr.equalsIgnoreCase("pattern")) {
            mode = ResearchRecipe.StructureMode.PATTERN;
        } else if (modeStr.equalsIgnoreCase("size_only")) {
            mode = ResearchRecipe.StructureMode.SIZE_ONLY;
        } else {
            mode = ResearchRecipe.StructureMode.NONE;
        }

        int[] size = (mode == ResearchRecipe.StructureMode.NONE)
                ? new int[]{0,0,0}
                : readSize3(reqArray(obj, "size"));

        Map<String, java.util.List<ResourceLocation>> symbols = Map.of();
        java.util.List<java.util.List<String>> layers = List.of();

        if (mode == ResearchRecipe.StructureMode.PATTERN) {
            symbols = new LinkedHashMap<>();
            if (obj.has("symbols")) {
                JsonObject sym = obj.getAsJsonObject("symbols");
                for (var e : sym.entrySet()) {
                    java.util.List<ResourceLocation> lst = new ArrayList<>();
                    for (JsonElement el : e.getValue().getAsJsonArray()) {
                        lst.add(new ResourceLocation(el.getAsString()));
                    }
                    symbols.put(e.getKey(), lst);
                }
            }
            layers = new ArrayList<>();
            if (obj.has("layers")) {
                JsonArray ly = obj.getAsJsonArray("layers");
                for (JsonElement layerEl : ly) {
                    java.util.List<String> rows = new ArrayList<>();
                    for (JsonElement rowEl : layerEl.getAsJsonArray()) {
                        rows.add(rowEl.getAsString());
                    }
                    layers.add(rows);
                }
            }
        }

        return new ResearchRecipe.Structure(mode, size, symbols, layers);
    }

    private static void writeStructure(FriendlyByteBuf buf, ResearchRecipe.Structure s) {
        buf.writeEnum(s.mode);
        buf.writeVarInt(s.size[0]); buf.writeVarInt(s.size[1]); buf.writeVarInt(s.size[2]);

        if (s.mode == ResearchRecipe.StructureMode.PATTERN) {
            buf.writeVarInt(s.symbols.size());
            for (var e : s.symbols.entrySet()) {
                buf.writeUtf(e.getKey());
                buf.writeVarInt(e.getValue().size());
                for (var rl : e.getValue()) buf.writeResourceLocation(rl);
            }

            buf.writeVarInt(s.layers.size());
            for (var layer : s.layers) {
                buf.writeVarInt(layer.size());
                for (String row : layer) buf.writeUtf(row);
            }
        }
    }

    private static ResearchRecipe.Structure readStructure(FriendlyByteBuf buf) {
        ResearchRecipe.StructureMode mode = buf.readEnum(ResearchRecipe.StructureMode.class);
        int[] size = new int[]{ buf.readVarInt(), buf.readVarInt(), buf.readVarInt() };

        Map<String, java.util.List<ResourceLocation>> symbols = Map.of();
        java.util.List<java.util.List<String>> layers = List.of();

        if (mode == ResearchRecipe.StructureMode.PATTERN) {
            int sN = buf.readVarInt();
            Map<String, java.util.List<ResourceLocation>> map = new LinkedHashMap<>();
            for (int i=0;i<sN;i++) {
                String sym = buf.readUtf();
                int m = buf.readVarInt();
                java.util.List<ResourceLocation> lst = new ArrayList<>(m);
                for (int j=0;j<m;j++) lst.add(buf.readResourceLocation());
                map.put(sym, lst);
            }
            symbols = map;

            int ly = buf.readVarInt();
            java.util.List<java.util.List<String>> ll = new ArrayList<>(ly);
            for (int y=0;y<ly;y++) {
                int rz = buf.readVarInt();
                java.util.List<String> rows = new ArrayList<>(rz);
                for (int z=0;z<rz;z++) rows.add(buf.readUtf());
                ll.add(rows);
            }
            layers = ll;
        }
        return new ResearchRecipe.Structure(mode, size, symbols, layers);
    }

    private static String reqString(JsonObject o, String key) {
        if (!o.has(key)) throw new JsonSyntaxException("Missing '" + key + "'");
        return o.get(key).getAsString();
    }

    private static int reqInt(JsonObject o, String key) {
        if (!o.has(key)) throw new JsonSyntaxException("Missing '" + key + "'");
        return o.get(key).getAsInt();
    }

    private static int optInt(JsonObject o, String key, int def) {
        return o.has(key) ? o.get(key).getAsInt() : def;
    }

    private static JsonArray reqArray(JsonObject o, String key) {
        if (!o.has(key)) throw new JsonSyntaxException("Missing '" + key + "'");
        return o.getAsJsonArray(key);
    }

    private static Item readItem(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null) throw new JsonSyntaxException("Unknown item: " + id);
        return item;
    }

    private static boolean parsePresenceAsBool(JsonObject json, String key) {
        if (!json.has(key)) return false;
        if (json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isBoolean()) {
            return json.get(key).getAsBoolean();
        }
        return json.get(key).isJsonObject();
    }

    private static int[] readSize3(JsonArray arr) throws JsonSyntaxException {
        if (arr == null) {
            throw new JsonSyntaxException("Missing 'structure.size' (expected [x,y,z])");
        }
        if (arr.size() != 3) {
            throw new JsonSyntaxException("structure.size must have exactly 3 integers [x,y,z]");
        }

        try {
            int x = arr.get(0).getAsInt();
            int y = arr.get(1).getAsInt();
            int z = arr.get(2).getAsInt();
            return new int[]{ x, y, z };
        } catch (ClassCastException | IllegalStateException e) {
            throw new JsonSyntaxException("structure.size entries must be integers [x,y,z]", e);
        }
    }

}
