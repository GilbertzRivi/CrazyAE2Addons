package net.oktawia.crazyae2addons.defs.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

public record AutoBuilderPreviewData(
        Optional<BlockPos> ghostRenderPos,
        boolean previewEnabled,
        List<BlockPos> previewPositions,
        List<String> previewPalette,
        int[] previewIndices
) {
    public static final AutoBuilderPreviewData DEFAULT = new AutoBuilderPreviewData(
            Optional.empty(), false, List.of(), List.of(), new int[0]
    );

    private static final Codec<int[]> INT_ARRAY_CODEC = Codec.INT.listOf()
            .xmap(l -> l.stream().mapToInt(Integer::intValue).toArray(),
                    arr -> IntStream.of(arr).boxed().toList());

    public static final Codec<AutoBuilderPreviewData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BlockPos.CODEC.optionalFieldOf("ghostRenderPos").forGetter(AutoBuilderPreviewData::ghostRenderPos),
            Codec.BOOL.optionalFieldOf("previewEnabled", false).forGetter(AutoBuilderPreviewData::previewEnabled),
            BlockPos.CODEC.listOf().optionalFieldOf("previewPositions", List.of()).forGetter(AutoBuilderPreviewData::previewPositions),
            Codec.STRING.listOf().optionalFieldOf("previewPalette", List.of()).forGetter(AutoBuilderPreviewData::previewPalette),
            INT_ARRAY_CODEC.optionalFieldOf("previewIndices", new int[0]).forGetter(AutoBuilderPreviewData::previewIndices)
    ).apply(inst, AutoBuilderPreviewData::new));

    public static final StreamCodec<ByteBuf, AutoBuilderPreviewData> STREAM_CODEC = StreamCodec.of(
            (buf, d) -> {
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC).encode(buf, d.ghostRenderPos());
                ByteBufCodecs.BOOL.encode(buf, d.previewEnabled());
                BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, d.previewPositions());
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, d.previewPalette());
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(buf,
                        IntStream.of(d.previewIndices()).boxed().toList());
            },
            buf -> new AutoBuilderPreviewData(
                    ByteBufCodecs.optional(BlockPos.STREAM_CODEC).decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(buf)
                            .stream().mapToInt(Integer::intValue).toArray()
            )
    );

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AutoBuilderPreviewData d)) return false;
        return previewEnabled == d.previewEnabled
                && ghostRenderPos.equals(d.ghostRenderPos)
                && previewPositions.equals(d.previewPositions)
                && previewPalette.equals(d.previewPalette)
                && Arrays.equals(previewIndices, d.previewIndices);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(ghostRenderPos, previewEnabled, previewPositions, previewPalette);
        result = 31 * result + Arrays.hashCode(previewIndices);
        return result;
    }
}
