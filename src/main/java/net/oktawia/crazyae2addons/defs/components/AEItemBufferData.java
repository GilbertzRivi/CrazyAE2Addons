package net.oktawia.crazyae2addons.defs.components;

import appeng.api.stacks.GenericStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record AEItemBufferData(
        List<GenericStack> entries,
        boolean flushPending,
        int flushTickAcc,
        List<CompoundTag> links,
        ItemStack patternSlot
) {
    public static final AEItemBufferData DEFAULT = new AEItemBufferData(List.of(), false, 0, List.of(), ItemStack.EMPTY);

    public static final Codec<AEItemBufferData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            GenericStack.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(AEItemBufferData::entries),
            Codec.BOOL.optionalFieldOf("flushPending", false).forGetter(AEItemBufferData::flushPending),
            Codec.INT.optionalFieldOf("flushTickAcc", 0).forGetter(AEItemBufferData::flushTickAcc),
            CompoundTag.CODEC.listOf().optionalFieldOf("links", List.of()).forGetter(AEItemBufferData::links),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("patternSlot", ItemStack.EMPTY).forGetter(AEItemBufferData::patternSlot)
    ).apply(inst, AEItemBufferData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AEItemBufferData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);
}
