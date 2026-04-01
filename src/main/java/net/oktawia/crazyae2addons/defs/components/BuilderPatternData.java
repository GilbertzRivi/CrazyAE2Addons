package net.oktawia.crazyae2addons.defs.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;

import java.util.Optional;

public record BuilderPatternData(String programId, int delay, Optional<Direction> srcFacing) {

    public static final BuilderPatternData DEFAULT = new BuilderPatternData("", 20, Optional.empty());

    public boolean hasCode() {
        return !programId.isEmpty();
    }

    public static final Codec<BuilderPatternData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("program_id", "").forGetter(BuilderPatternData::programId),
            Codec.INT.optionalFieldOf("delay", 20).forGetter(BuilderPatternData::delay),
            Direction.CODEC.optionalFieldOf("src_facing").forGetter(BuilderPatternData::srcFacing)
    ).apply(inst, BuilderPatternData::new));
}
