package net.oktawia.crazyae2addons.defs.regs;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.entities.BrokenPatternProviderBE;
import net.oktawia.crazyae2addons.entities.CrazyPatternProviderBE;
import net.oktawia.crazyae2addons.entities.EnergyStorageBE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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


    public static void setupBlockEntityTypes() {
        for (var runnable : BLOCK_ENTITY_SETUP) {
            runnable.run();
        }
    }

    public static List<? extends BlockEntityType<?>> getEntities() {
        return BLOCK_ENTITIES.getEntries().stream().map(DeferredHolder::get).toList();
    }
}
