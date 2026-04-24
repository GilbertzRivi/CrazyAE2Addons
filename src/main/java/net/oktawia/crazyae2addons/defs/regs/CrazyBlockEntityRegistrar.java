package net.oktawia.crazyae2addons.defs.regs;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.entities.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class CrazyBlockEntityRegistrar {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CrazyAddons.MODID);

    private static final List<Runnable> BLOCK_ENTITY_SETUP = new ArrayList<>();

    private static <T extends AEBaseBlockEntity> RegistryObject<BlockEntityType<T>> reg(
            String id,
            RegistryObject<? extends AEBaseEntityBlock<?>> block,
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

    private static <T extends AEBaseBlockEntity, S extends T> RegistryObject<BlockEntityType<T>> regConditional(
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
                BlockEntityType<S> type = BlockEntityType.Builder.of(trueFactory.get(), blk).build(null);
                BLOCK_ENTITY_SETUP.add(() -> blk.setBlockEntity(
                        (Class) trueClass.get(),
                        (BlockEntityType) type,
                        null,
                        null
                ));
                return (BlockEntityType<T>) type;
            } else {
                BlockEntityType<T> type = BlockEntityType.Builder.of(falseFactory, blk).build(null);
                BLOCK_ENTITY_SETUP.add(() -> blk.setBlockEntity(
                        (Class) falseClass,
                        (BlockEntityType) type,
                        null,
                        null
                ));
                return type;
            }
        });
    }

    @SafeVarargs
    private static <T extends AEBaseBlockEntity> RegistryObject<BlockEntityType<T>> regMulti(
            String id,
            BlockEntityType.BlockEntitySupplier<T> factory,
            Class<T> blockEntityClass,
            Supplier<? extends AEBaseEntityBlock<?>>... blocks
    ) {
        return BLOCK_ENTITIES.register(id, () -> {
            var resolved = Arrays.stream(blocks)
                    .map(Supplier::get)
                    .toArray(Block[]::new);

            BlockEntityType<T> type = BlockEntityType.Builder.of(factory, resolved).build(null);

            for (var blk : resolved) {
                var aeBlk = (AEBaseEntityBlock<?>) blk;
                BLOCK_ENTITY_SETUP.add(() -> aeBlk.setBlockEntity(
                        (Class) blockEntityClass,
                        (BlockEntityType) type,
                        null,
                        null
                ));
            }

            return type;
        });
    }

    public static void setupBlockEntityTypes() {
        for (var runnable : BLOCK_ENTITY_SETUP) {
            runnable.run();
        }
    }

    public static List<? extends BlockEntityType<?>> getEntities() {
        return BLOCK_ENTITIES.getEntries().stream().map(RegistryObject::get).toList();
    }

    public static final RegistryObject<BlockEntityType<CrazyPatternProviderBE>> CRAZY_PATTERN_PROVIDER_BE =
            reg("crazy_pattern_provider_be", CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK, CrazyPatternProviderBE::new, CrazyPatternProviderBE.class);

    public static final RegistryObject<BlockEntityType<EjectorBE>> EJECTOR_BE =
            reg("ejector_be", CrazyBlockRegistrar.EJECTOR_BLOCK, EjectorBE::new, EjectorBE.class);

    public static final RegistryObject<BlockEntityType<RecipeFabricatorBE>> RECIPE_FABRICATOR_BE =
            reg("recipe_fabricator_be", CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK, RecipeFabricatorBE::new, RecipeFabricatorBE.class);

    private CrazyBlockEntityRegistrar() {}
}