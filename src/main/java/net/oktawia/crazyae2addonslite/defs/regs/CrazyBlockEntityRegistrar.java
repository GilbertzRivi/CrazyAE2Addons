package net.oktawia.crazyae2addonslite.defs.regs;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;
import net.oktawia.crazyae2addonslite.entities.CrazyPatternProviderBE;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CrazyBlockEntityRegistrar {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CrazyAddonslite.MODID);

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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrazyPatternProviderBE>> CRAZY_PATTERN_PROVIDER_BE =
            reg("crazy_pattern_provider_be", CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK, CrazyPatternProviderBE::new, CrazyPatternProviderBE.class);

//    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EjectorBE>> EJECTOR_BE =
//            reg("ejector_be", CrazyBlockRegistrar.EJECTOR_BLOCK, EjectorBE::new, EjectorBE.class);
//
//    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingSchedulerBE>> CRAFTING_SHEDULER_BE =
//            reg("crafting_scheduler_be", CrazyBlockRegistrar.CRAFTING_SCHEDULER_BLOCK, CraftingSchedulerBE::new, CraftingSchedulerBE.class);
//
//    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrokenPatternProviderBE>> BROKEN_PATTERN_PROVIDER_BE =
//            reg("broken_pattern_provider_be", CrazyBlockRegistrar.BROKEN_PATTERN_PROVIDER_BLOCK, BrokenPatternProviderBE::new, BrokenPatternProviderBE.class);
//
//    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<? extends AmpereMeterBE>> AMPERE_METER_BE =
//            reg("ampere_meter_be", CrazyBlockRegistrar.AMPERE_METER_BLOCK, AmpereMeterBE::new, AmpereMeterBE.class);

    public static void setupBlockEntityTypes() {
        for (var runnable : BLOCK_ENTITY_SETUP) {
            runnable.run();
        }
    }

    public static List<? extends BlockEntityType<?>> getEntities() {
        return BLOCK_ENTITIES.getEntries().stream().map(DeferredHolder::get).toList();
    }
}
