package net.oktawia.crazyae2addonslite.misc;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.core.BlockPos;

import java.io.IOException;

public class BlockPosAdapter extends TypeAdapter<BlockPos> {

    @Override
    public void write(JsonWriter out, BlockPos pos) throws IOException {
        out.beginObject();
        out.name("x").value(pos.getX());
        out.name("y").value(pos.getY());
        out.name("z").value(pos.getZ());
        out.endObject();
    }

    @Override
    public BlockPos read(JsonReader in) throws IOException {
        in.beginObject();
        int x = 0, y = 0, z = 0;
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "x" -> x = in.nextInt();
                case "y" -> y = in.nextInt();
                case "z" -> z = in.nextInt();
            }
        }
        in.endObject();
        return new BlockPos(x, y, z);
    }
}