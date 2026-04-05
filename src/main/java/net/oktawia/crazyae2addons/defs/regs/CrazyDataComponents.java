package net.oktawia.crazyae2addons.defs.regs;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.components.AmpereMeterData;
import net.oktawia.crazyae2addons.defs.components.BuilderPatternData;
import net.oktawia.crazyae2addons.defs.components.CrazyProviderDisplayData;
import net.oktawia.crazyae2addons.defs.components.EjectorData;

public final class CrazyDataComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CrazyAddons.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> CRAZY_PROVIDER_DATA =
            COMPONENTS.registerComponentType(
                    "crazy_provider_data",
                    builder -> builder.persistent(CustomData.CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BuilderPatternData>> BUILDER_PATTERN_DATA =
            COMPONENTS.registerComponentType(
                    "builder_pattern_data",
                    builder -> builder.persistent(BuilderPatternData.CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CrazyProviderDisplayData>> CRAZY_PROVIDER_DISPLAY =
            COMPONENTS.registerComponentType(
                    "crazy_provider_display",
                    builder -> builder.persistent(CrazyProviderDisplayData.CODEC)
                                      .networkSynchronized(CrazyProviderDisplayData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AmpereMeterData>> AMPERE_METER_DATA =
            COMPONENTS.registerComponentType(
                    "ampere_meter_data",
                    builder -> builder.persistent(AmpereMeterData.CODEC)
                                      .networkSynchronized(AmpereMeterData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EjectorData>> EJECTOR_DATA =
            COMPONENTS.registerComponentType(
                    "ejector_data",
                    builder -> builder.persistent(EjectorData.CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> RR_ITEM_P2P_TYPE =
            COMPONENTS.registerComponentType(
                    "rr_item_p2p_type",
                    builder -> builder.persistent(Codec.STRING)
            );

    private CrazyDataComponents() {}
}
