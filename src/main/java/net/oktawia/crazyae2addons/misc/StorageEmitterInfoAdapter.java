package net.oktawia.crazyae2addons.misc;

import appeng.api.stacks.GenericStack;
import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.oktawia.crazyae2addons.menus.EmitterTerminalMenu;

import java.lang.reflect.Type;

public class StorageEmitterInfoAdapter implements
        JsonSerializer<EmitterTerminalMenu.StorageEmitterInfo>,
        JsonDeserializer<EmitterTerminalMenu.StorageEmitterInfo> {

    @Override
    public JsonElement serialize(EmitterTerminalMenu.StorageEmitterInfo src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        obj.addProperty("uuid", src.uuid());

        String name = src.name() == null ? "" : src.name().getString();
        obj.addProperty("name", name);

        if (src.config() == null) {
            obj.add("config", JsonNull.INSTANCE);
        } else {
            CompoundTag tag = GenericStack.writeTag(src.config());
            obj.addProperty("config", tag.toString());
        }

        if (src.value() == null) {
            obj.add("value", JsonNull.INSTANCE);
        } else {
            obj.addProperty("value", src.value());
        }

        return obj;
    }

    @Override
    public EmitterTerminalMenu.StorageEmitterInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        String uuid = obj.has("uuid") && !obj.get("uuid").isJsonNull()
                ? obj.get("uuid").getAsString()
                : "";

        String nameString = obj.has("name") && !obj.get("name").isJsonNull()
                ? obj.get("name").getAsString()
                : "";

        Component name = nameString.isEmpty()
                ? Component.empty()
                : Component.literal(nameString);

        GenericStack config = null;
        if (obj.has("config") && !obj.get("config").isJsonNull()) {
            String snbt = obj.get("config").getAsString();
            try {
                CompoundTag tag = TagParser.parseTag(snbt);
                config = GenericStack.readTag(tag);
            } catch (Exception e) {
                throw new JsonParseException("Failed to parse GenericStack from SNBT: " + snbt, e);
            }
        }

        Long value = null;
        if (obj.has("value") && !obj.get("value").isJsonNull()) {
            try {
                value = obj.get("value").getAsLong();
            } catch (Exception e) {
                throw new JsonParseException("Failed to parse value as Long: " + obj.get("value"), e);
            }
        }

        return new EmitterTerminalMenu.StorageEmitterInfo(uuid, name, config, value);
    }
}