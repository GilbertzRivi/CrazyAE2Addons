package net.oktawia.crazyae2addons.defs.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DisplayPartData(String textValue, byte spin, boolean mode, boolean margin, boolean center) {

    public static final DisplayPartData DEFAULT = new DisplayPartData("", (byte) 0, true, false, false);

    public static final Codec<DisplayPartData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("textvalue", "").forGetter(DisplayPartData::textValue),
            Codec.BYTE.optionalFieldOf("spin", (byte) 0).forGetter(DisplayPartData::spin),
            Codec.BOOL.optionalFieldOf("mode", true).forGetter(DisplayPartData::mode),
            Codec.BOOL.optionalFieldOf("margin", false).forGetter(DisplayPartData::margin),
            Codec.BOOL.optionalFieldOf("center", false).forGetter(DisplayPartData::center)
    ).apply(inst, DisplayPartData::new));

    public static final StreamCodec<ByteBuf, DisplayPartData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DisplayPartData::textValue,
            ByteBufCodecs.BYTE, DisplayPartData::spin,
            ByteBufCodecs.BOOL, DisplayPartData::mode,
            ByteBufCodecs.BOOL, DisplayPartData::margin,
            ByteBufCodecs.BOOL, DisplayPartData::center,
            DisplayPartData::new
    );
}
