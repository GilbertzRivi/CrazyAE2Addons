package net.oktawia.crazyae2addons.defs.regs;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.oktawia.crazyae2addons.CrazyAddons;

public final class CrazyDataComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CrazyAddons.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> CRAZY_PROVIDER_DATA =
            COMPONENTS.registerComponentType(
                    "crazy_provider_data",
                    builder -> builder.persistent(CustomData.CODEC)
            );

    private CrazyDataComponents() {}
}
