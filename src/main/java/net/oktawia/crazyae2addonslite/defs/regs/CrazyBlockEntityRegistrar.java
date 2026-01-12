package net.oktawia.crazyae2addonslite.defs.regs;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addonslite.CrazyAddons;
import net.oktawia.crazyae2addonslite.IsModLoaded;
import net.oktawia.crazyae2addonslite.compat.GregTech.*;
import net.oktawia.crazyae2addonslite.entities.*;

import java.util.ArrayList;
import java.util.List;

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

    public static final RegistryObject<BlockEntityType<EjectorBE>> EJECTOR_BE =
            reg("ejector_be", CrazyBlockRegistrar.EJECTOR_BLOCK, EjectorBE::new, EjectorBE.class);

    public static final RegistryObject<BlockEntityType<CraftingSchedulerBE>> CRAFTING_SHEDULER_BE =
            reg("crafting_scheduler_be", CrazyBlockRegistrar.CRAFTING_SCHEDULER_BLOCK, CraftingSchedulerBE::new, CraftingSchedulerBE.class);

    public static final RegistryObject<BlockEntityType<CrazyPatternProviderBE>> CRAZY_PATTERN_PROVIDER_BE =
            reg("crazy_pattern_provider_be", CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK, CrazyPatternProviderBE::new, CrazyPatternProviderBE.class);

    public static final RegistryObject<BlockEntityType<BrokenPatternProviderBE>> BROKEN_PATTERN_PROVIDER_BE =
            reg("broken_pattern_provider_be", CrazyBlockRegistrar.BROKEN_PATTERN_PROVIDER_BLOCK, BrokenPatternProviderBE::new, BrokenPatternProviderBE.class);

    public static final RegistryObject<BlockEntityType<? extends AmpereMeterBE>> AMPERE_METER_BE =
            BLOCK_ENTITIES.register("ampere_meter_be", () -> {
                var blk = CrazyBlockRegistrar.AMPERE_METER_BLOCK.get();
                if (IsModLoaded.isGTCEuLoaded()) {
                    var type = BlockEntityType.Builder.of(GTAmpereMeterBE::new, blk).build(null);
                    BLOCK_ENTITY_SETUP.add(() -> ((AEBaseEntityBlock) blk).setBlockEntity(GTAmpereMeterBE.class, type, null, null));
                    return type;
                } else {
                    var type = BlockEntityType.Builder.of(AmpereMeterBE::new, blk).build(null);
                    BLOCK_ENTITY_SETUP.add(() -> blk.setBlockEntity(AmpereMeterBE.class, type, null, null));
                    return type;
                }
            });

    public static void setupBlockEntityTypes() {
        for (var runnable : BLOCK_ENTITY_SETUP) {
            runnable.run();
        }
    }

    public static List<? extends BlockEntityType<?>> getEntities() {
        return BLOCK_ENTITIES.getEntries().stream().map(RegistryObject::get).toList();
    }
}