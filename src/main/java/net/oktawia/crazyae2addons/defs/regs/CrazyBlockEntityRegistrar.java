package net.oktawia.crazyae2addons.defs.regs;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.compat.Apotheosis.ApothAutoEnchanterBE;
import net.oktawia.crazyae2addons.entities.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class CrazyBlockEntityRegistrar {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CrazyAddons.MODID);

    private static final List<Runnable> BLOCK_ENTITY_SETUP = new ArrayList<>();

    private static <T extends AEBaseBlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> reg(
            String id,
            Supplier<? extends AEBaseEntityBlock<?>> block,
            BlockEntityType.BlockEntitySupplier<T> factory,
            Class<T> blockEntityClass
    ) {
        return BLOCK_ENTITIES.register(id, () -> {
            var blk = block.get();
            var type = BlockEntityType.Builder.of(factory, blk).build(null);

            BLOCK_ENTITY_SETUP.add(() -> blk.setBlockEntity(
                    (Class) blockEntityClass, (BlockEntityType) type, null, null
            ));

            return type;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends AEBaseBlockEntity, S extends T> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> regConditional(
            String id,
            Supplier<? extends AEBaseEntityBlock<?>> block,
            BooleanSupplier condition,
            Supplier<BlockEntityType.BlockEntitySupplier<S>> trueFactory,
            Supplier<Class<S>> trueClass,
            BlockEntityType.BlockEntitySupplier<T> falseFactory,
            Class<T> falseClass
    ) {
        return BLOCK_ENTITIES.register(id, () -> {
            var blk = block.get();
            if (condition.getAsBoolean()) {
                var type = BlockEntityType.Builder.of(trueFactory.get(), blk).build(null);
                BLOCK_ENTITY_SETUP.add(() -> blk.setBlockEntity((Class) trueClass.get(), (BlockEntityType) type, null, null));
                return (BlockEntityType<T>) type;
            } else {
                var type = BlockEntityType.Builder.of(falseFactory, blk).build(null);
                BLOCK_ENTITY_SETUP.add(() -> blk.setBlockEntity((Class) falseClass, (BlockEntityType) type, null, null));
                return type;
            }
        });
    }

    @SafeVarargs
    private static <T extends AEBaseBlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> regMulti(
            String id,
            BlockEntityType.BlockEntitySupplier<T> factory,
            Class<T> blockEntityClass,
            Supplier<? extends AEBaseEntityBlock<?>>... blocks
    ) {
        return BLOCK_ENTITIES.register(id, () -> {
            var resolved = Arrays.stream(blocks)
                    .map(Supplier::get)
                    .toArray(Block[]::new);
            var type = BlockEntityType.Builder.of(factory, resolved).build(null);

            for (var blk : resolved) {
                var aeBlk = (AEBaseEntityBlock<?>) blk;
                BLOCK_ENTITY_SETUP.add(() -> aeBlk.setBlockEntity(
                        (Class) blockEntityClass, (BlockEntityType) type, null, null
                ));
            }

            return type;
        });
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrazyPatternProviderBE>> CRAZY_PATTERN_PROVIDER_BE =
            reg("crazy_pattern_provider_be", CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK, CrazyPatternProviderBE::new, CrazyPatternProviderBE.class);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrokenPatternProviderBE>> BROKEN_PATTERN_PROVIDER_BE =
            reg("broken_pattern_provider_be", CrazyBlockRegistrar.BROKEN_PATTERN_PROVIDER_BLOCK, BrokenPatternProviderBE::new, BrokenPatternProviderBE.class);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingSchedulerBE>> CRAFTING_SCHEDULER_BE =
            reg("crafting_scheduler_be", CrazyBlockRegistrar.CRAFTING_SCHEDULER_BLOCK, CraftingSchedulerBE::new, CraftingSchedulerBE.class);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyStorageBE>> ENERGY_STORAGE_BE =
            regMulti("energy_storage_be", EnergyStorageBE::new, EnergyStorageBE.class,
                    CrazyBlockRegistrar.ENERGY_STORAGE_1K,
                    CrazyBlockRegistrar.ENERGY_STORAGE_4K,
                    CrazyBlockRegistrar.ENERGY_STORAGE_16K,
                    CrazyBlockRegistrar.ENERGY_STORAGE_64K,
                    CrazyBlockRegistrar.ENERGY_STORAGE_256K,
                    CrazyBlockRegistrar.ENERGY_STORAGE_1M,
                    CrazyBlockRegistrar.ENERGY_STORAGE_4M,
                    CrazyBlockRegistrar.ENERGY_STORAGE_16M,
                    CrazyBlockRegistrar.ENERGY_STORAGE_64M,
                    CrazyBlockRegistrar.ENERGY_STORAGE_256M
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoBuilderCreativeSupplyBE>> AUTO_BUILDER_CREATIVE_SUPPLY_BE =
            reg("auto_builder_creative_supply_be", CrazyBlockRegistrar.AUTO_BUILDER_CREATIVE_SUPPLY_BLOCK, AutoBuilderCreativeSupplyBE::new, AutoBuilderCreativeSupplyBE.class);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoBuilderBE>> AUTO_BUILDER_BE =
            reg("auto_builder_be", CrazyBlockRegistrar.AUTO_BUILDER_BLOCK, AutoBuilderBE::new, AutoBuilderBE.class);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EjectorBE>> EJECTOR_BE =
            reg("ejector_be", CrazyBlockRegistrar.EJECTOR_BLOCK, EjectorBE::new, EjectorBE.class);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmpereMeterBE>> AMPERE_METER_BE =
            reg("ampere_meter_be", CrazyBlockRegistrar.AMPERE_METER_BLOCK, AmpereMeterBE::new, AmpereMeterBE.class);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoEnchanterBE>> AUTO_ENCHANTER_BE =
            regConditional(
                    "auto_enchanter_be",
                    CrazyBlockRegistrar.AUTO_ENCHANTER_BLOCK,
                    IsModLoaded::isApothEnchLoaded,
                    () -> ApothAutoEnchanterBE::new,
                    () -> ApothAutoEnchanterBE.class,
                    AutoEnchanterBE::new,
                    AutoEnchanterBE.class
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PenroseControllerBE>> PENROSE_CONTROLLER_BE =
            reg("penrose_controller_be",
                    CrazyBlockRegistrar.PENROSE_CONTROLLER,
                    PenroseControllerBE::new,
                    PenroseControllerBE.class);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestMultiblockControllerBE>> TEST_MULTIBLOCK_CONTROLLER_BE =
            reg("test_multiblock_controller_be",
                    CrazyBlockRegistrar.TEST_MULTIBLOCK_CONTROLLER,
                    TestMultiblockControllerBE::new,
                    TestMultiblockControllerBE.class);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestMultiblockFrameBE>> TEST_MULTIBLOCK_FRAME_BE =
            reg("test_multiblock_frame_be",
                    CrazyBlockRegistrar.TEST_MULTIBLOCK_FRAME,
                    TestMultiblockFrameBE::new,
                    TestMultiblockFrameBE.class);

    public static void setupBlockEntityTypes() {
        for (var runnable : BLOCK_ENTITY_SETUP) {
            runnable.run();
        }
    }

    public static List<? extends BlockEntityType<?>> getEntities() {
        return BLOCK_ENTITIES.getEntries().stream().map(DeferredHolder::get).toList();
    }
}
