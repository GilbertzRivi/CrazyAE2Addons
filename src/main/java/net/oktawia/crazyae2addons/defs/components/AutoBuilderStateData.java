package net.oktawia.crazyae2addons.defs.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record AutoBuilderStateData(
        int currentInstruction,
        int tickDelayLeft,
        boolean isRunning,
        BlockPos offset,
        boolean skipEmpty,
        boolean energyPrepaid,
        boolean isCrafting
) {
    public static final AutoBuilderStateData DEFAULT = new AutoBuilderStateData(
            0, 0, false, BlockPos.ZERO, false, false, false
    );

    public static final Codec<AutoBuilderStateData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.optionalFieldOf("currentInstruction", 0).forGetter(AutoBuilderStateData::currentInstruction),
            Codec.INT.optionalFieldOf("tickDelayLeft", 0).forGetter(AutoBuilderStateData::tickDelayLeft),
            Codec.BOOL.optionalFieldOf("isRunning", false).forGetter(AutoBuilderStateData::isRunning),
            BlockPos.CODEC.optionalFieldOf("offset", BlockPos.ZERO).forGetter(AutoBuilderStateData::offset),
            Codec.BOOL.optionalFieldOf("skipEmpty", false).forGetter(AutoBuilderStateData::skipEmpty),
            Codec.BOOL.optionalFieldOf("energyPrepaid", false).forGetter(AutoBuilderStateData::energyPrepaid),
            Codec.BOOL.optionalFieldOf("isCrafting", false).forGetter(AutoBuilderStateData::isCrafting)
    ).apply(inst, AutoBuilderStateData::new));
}
