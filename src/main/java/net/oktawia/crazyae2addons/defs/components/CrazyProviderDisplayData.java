package net.oktawia.crazyae2addons.defs.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CrazyProviderDisplayData(int added, int filled) {

    public static final CrazyProviderDisplayData DEFAULT = new CrazyProviderDisplayData(0, 0);

    public static final Codec<CrazyProviderDisplayData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.optionalFieldOf("added", 0).forGetter(CrazyProviderDisplayData::added),
            Codec.INT.optionalFieldOf("filled", 0).forGetter(CrazyProviderDisplayData::filled)
    ).apply(inst, CrazyProviderDisplayData::new));

    public static final StreamCodec<ByteBuf, CrazyProviderDisplayData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CrazyProviderDisplayData::added,
            ByteBufCodecs.VAR_INT, CrazyProviderDisplayData::filled,
            CrazyProviderDisplayData::new
    );
}
