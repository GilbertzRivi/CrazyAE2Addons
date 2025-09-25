package net.oktawia.crazyae2addons.defs.regs;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.fluid.ResearchFluid;
import net.oktawia.crazyae2addons.fluid.ResearchFluidBlock;
import net.oktawia.crazyae2addons.fluid.ResearchFluidType;

public final class CrazyFluidRegistrar {
    private CrazyFluidRegistrar() {}

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CrazyAddons.MODID);

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, CrazyAddons.MODID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, CrazyAddons.MODID);

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, CrazyAddons.MODID);

    public static final DeferredHolder<FluidType, FluidType> RESEARCH_FLUID_TYPE = FLUID_TYPES.register(
            "research_fluid_type",
            () -> new ResearchFluidType(
                    FluidType.Properties.create()
                            .density(300)
                            .viscosity(1000)
                            .canSwim(true)
                            .canDrown(true)
                            .sound(SoundActions.BUCKET_FILL, net.minecraft.sounds.SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY)
            )
    );

    public static final DeferredHolder<Fluid, FlowingFluid> RESEARCH_FLUID_SOURCE =
            FLUIDS.register("research_fluid", ResearchFluid.Source::new);

    public static final DeferredHolder<Fluid, FlowingFluid> RESEARCH_FLUID_FLOWING =
            FLUIDS.register("research_fluid_flowing", ResearchFluid.Flowing::new);

    public static final DeferredHolder<Block, LiquidBlock> RESEARCH_FLUID_BLOCK =
            BLOCKS.register("research_fluid_block",
                    () -> new ResearchFluidBlock(
                            RESEARCH_FLUID_SOURCE.get(),
                            BlockBehaviour.Properties.of()
                                    .liquid()
                                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                                    .noLootTable()
                                    .replaceable()
                                    .noCollission()
                                    .strength(20.0F)
                                    .pushReaction(PushReaction.DESTROY)
                    )
            );

    public static final DeferredHolder<Item, Item> RESEARCH_FLUID_BUCKET =
            ITEMS.register("research_fluid_bucket",
                    () -> new BucketItem(RESEARCH_FLUID_SOURCE.get(),
                            new Item.Properties()
                                    .stacksTo(1)
                                    .craftRemainder(Items.BUCKET)));

    public static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
    }
}
