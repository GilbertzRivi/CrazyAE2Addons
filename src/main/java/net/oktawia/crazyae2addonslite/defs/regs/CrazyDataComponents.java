package net.oktawia.crazyae2addonslite.defs.regs;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;

public final class CrazyDataComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CrazyAddonslite.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> CRAZY_PROVIDER_DATA =
            COMPONENTS.registerComponentType(
                    "crazy_provider_data",
                    builder -> builder.persistent(CustomData.CODEC)
            );

    private CrazyDataComponents() {}
}
