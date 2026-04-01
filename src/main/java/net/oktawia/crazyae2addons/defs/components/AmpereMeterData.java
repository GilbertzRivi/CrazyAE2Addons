package net.oktawia.crazyae2addons.defs.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record AmpereMeterData(boolean direction, int minFePerTick, int maxFePerTick) {

    public static final AmpereMeterData DEFAULT = new AmpereMeterData(false, 0, 1000);

    public static final Codec<AmpereMeterData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.optionalFieldOf("direction", false).forGetter(AmpereMeterData::direction),
            Codec.INT.optionalFieldOf("minFePerTick", 0).forGetter(AmpereMeterData::minFePerTick),
            Codec.INT.optionalFieldOf("maxFePerTick", 1000).forGetter(AmpereMeterData::maxFePerTick)
    ).apply(inst, AmpereMeterData::new));

    public static final StreamCodec<ByteBuf, AmpereMeterData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AmpereMeterData::direction,
            ByteBufCodecs.VAR_INT, AmpereMeterData::minFePerTick,
            ByteBufCodecs.VAR_INT, AmpereMeterData::maxFePerTick,
            AmpereMeterData::new
    );
}
