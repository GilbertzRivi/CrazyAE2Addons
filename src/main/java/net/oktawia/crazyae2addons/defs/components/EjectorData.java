package net.oktawia.crazyae2addons.defs.components;

import appeng.api.stacks.GenericStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record EjectorData(
        List<@Nullable GenericStack> config,
        List<@Nullable GenericStack> storage,
        ItemStack pattern
) {
    public static final EjectorData DEFAULT = new EjectorData(
            new ArrayList<>(Collections.nCopies(36, null)),
            new ArrayList<>(Collections.nCopies(36, null)),
            ItemStack.EMPTY
    );

    public static final Codec<EjectorData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            GenericStack.FAULT_TOLERANT_NULLABLE_LIST_CODEC.fieldOf("config").forGetter(EjectorData::config),
            GenericStack.FAULT_TOLERANT_NULLABLE_LIST_CODEC.fieldOf("storage").forGetter(EjectorData::storage),
            ItemStack.OPTIONAL_CODEC.fieldOf("pattern").forGetter(EjectorData::pattern)
    ).apply(inst, EjectorData::new));
}
