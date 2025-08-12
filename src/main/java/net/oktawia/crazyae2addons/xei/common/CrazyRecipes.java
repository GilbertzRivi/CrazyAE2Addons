package net.oktawia.crazyae2addons.xei.common;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.xei.jei.ReinforcedCondenserEntry;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static net.oktawia.crazyae2addons.defs.regs.CrazyRecipes.CRADLE_TYPE;

public class CrazyRecipes {

    public static List<CrazyEntry> getCrazyEntries() {
        return List.of(
                new CrazyEntry(
                        new ResourceLocation("crazyae2addons", "penrose_sphere.nbt"),
                        Component.literal("Penrose Sphere"),
                        List.of(
                                new ItemStack(CrazyBlockRegistrar.PENROSE_FRAME.get()).copyWithCount(1298),
                                new ItemStack(CrazyBlockRegistrar.PENROSE_COIL.get()).copyWithCount(307),
                                new ItemStack(CrazyBlockRegistrar.PENROSE_PORT.get()).copyWithCount(4),
                                new ItemStack(CrazyBlockRegistrar.PENROSE_CONTROLLER.get()).copyWithCount(1)
                        ),
                        new ItemStack(CrazyBlockRegistrar.PENROSE_CONTROLLER.get()).copyWithCount(1)
                ),

                new CrazyEntry(
                        new ResourceLocation("crazyae2addons", "energy_storage.nbt"),
                        Component.literal("Energy Storage"),
                        List.of(
                                new ItemStack(CrazyBlockRegistrar.DENSE_ENERGY_STORAGE_256K_BLOCK.get()).copyWithCount(279),
                                new ItemStack(AEBlocks.QUARTZ_VIBRANT_GLASS).copyWithCount(216),
                                new ItemStack(CrazyBlockRegistrar.ENERGY_STORAGE_FRAME_BLOCK.get()).copyWithCount(202),
                                new ItemStack(CrazyBlockRegistrar.ENERGY_STORAGE_PORT_BLOCK.get()).copyWithCount(3),
                                new ItemStack(CrazyBlockRegistrar.ENERGY_STORAGE_CONTROLLER_BLOCK.get()).copyWithCount(1)
                        ),
                        new ItemStack(CrazyBlockRegistrar.ENERGY_STORAGE_CONTROLLER_BLOCK.get()).copyWithCount(1)
                ),

                new CrazyEntry(
                        new ResourceLocation("crazyae2addons", "entropy_cradle.nbt"),
                        Component.literal("Entropy Cradle"),
                        List.of(
                                new ItemStack(CrazyBlockRegistrar.ENTROPY_CRADLE.get()).copyWithCount(236),
                                new ItemStack(CrazyBlockRegistrar.ENTROPY_CRADLE_CAPACITOR.get()).copyWithCount(24),
                                new ItemStack(CrazyBlockRegistrar.ENTROPY_CRADLE_CONTROLLER.get()).copyWithCount(1)
                        ),
                        new ItemStack(CrazyBlockRegistrar.ENTROPY_CRADLE_CONTROLLER.get()).copyWithCount(1)
                ),

                new CrazyEntry(
                        new ResourceLocation("crazyae2addons", "spawner_extractor.nbt"),
                        Component.literal("Spawner Extractor"),
                        List.of(
                                new ItemStack(CrazyBlockRegistrar.SPAWNER_EXTRACTOR_WALL.get()).copyWithCount(101),
                                new ItemStack(AEBlocks.QUARTZ_VIBRANT_GLASS).copyWithCount(36),
                                new ItemStack(CrazyBlockRegistrar.SPAWNER_EXTRACTOR_CONTROLLER.get()).copyWithCount(1)
                        ),
                        new ItemStack(CrazyBlockRegistrar.SPAWNER_EXTRACTOR_CONTROLLER.get()).copyWithCount(1)
                ),

                new CrazyEntry(
                        new ResourceLocation("crazyae2addons", "mob_farm.nbt"),
                        Component.literal("Mob Farm"),
                        List.of(
                                new ItemStack(CrazyBlockRegistrar.MOB_FARM_WALL.get()).copyWithCount(113),
                                new ItemStack(CrazyBlockRegistrar.MOB_FARM_COLLECTOR.get()).copyWithCount(18),
                                new ItemStack(CrazyBlockRegistrar.MOB_FARM_DAMAGE.get()).copyWithCount(16),
                                new ItemStack(CrazyBlockRegistrar.MOB_FARM_INPUT.get()).copyWithCount(2),
                                new ItemStack(CrazyBlockRegistrar.MOB_FARM_CONTROLLER.get()).copyWithCount(1)
                        ),
                        new ItemStack(CrazyBlockRegistrar.MOB_FARM_CONTROLLER.get()).copyWithCount(1)
                ),

                new CrazyEntry(
                        new ResourceLocation("crazyae2addons", "pattern_unit.nbt"),
                        Component.literal("Pattern Management Unit"),
                        List.of(
                                new ItemStack(CrazyBlockRegistrar.PATTERN_MANAGEMENT_UNIT_WALL_BLOCK.get()).copyWithCount(54),
                                new ItemStack(CrazyBlockRegistrar.PATTERN_MANAGEMENT_UNIT_FRAME_BLOCK.get()).copyWithCount(44),
                                new ItemStack(CrazyBlockRegistrar.PATTERN_MANAGEMENT_UNIT_BLOCK.get()).copyWithCount(27),
                                new ItemStack(CrazyBlockRegistrar.PATTERN_MANAGEMENT_UNIT_CONTROLLER_BLOCK.get()).copyWithCount(1)
                        ),
                        new ItemStack(CrazyBlockRegistrar.PATTERN_MANAGEMENT_UNIT_CONTROLLER_BLOCK.get()).copyWithCount(1)
                )
        );
    }

    public static List<CradleEntry> getCradleEntries() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return List.of();

        var rm = level.getRecipeManager();
        var recipes = rm.getAllRecipesFor(CRADLE_TYPE.get());

        return recipes.stream()
                .map(r -> new CradleEntry(
                        r.getId(),
                        buildInputsFromPattern(r.pattern()),
                        new ItemStack(r.resultBlock().asItem())
                ))
                .sorted(Comparator.comparing(e ->
                        e.output().getItem().builtInRegistryHolder().key().location().toString()))
                .toList();
    }

    private static List<ItemStack> buildInputsFromPattern(net.oktawia.crazyae2addons.recipes.CradlePattern pattern) {
        final int SIZE = 5;
        Map<Block, Integer> counts = new java.util.LinkedHashMap<>();

        var symbols = pattern.symbolMap();
        var layers  = pattern.layers();  

        for (int y = 0; y < SIZE; y++) {
            String[][] layer = layers.get(y);
            for (int z = 0; z < SIZE; z++) {
                String[] row = layer[z];
                for (int x = 0; x < SIZE; x++) {
                    String sym = row[x];
                    if (sym.equals(".")) continue;

                    var opts = symbols.get(sym);
                    if (opts == null || opts.isEmpty()) continue;
                    var block = opts.get(0);
                    if (block == net.minecraft.world.level.block.Blocks.AIR) continue;

                    counts.merge(block, 1, Integer::sum);
                }
            }
        }

        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        counts.forEach((b, n) -> {
            ItemStack s = new ItemStack(b.asItem());
            s.setCount(n);
            stacks.add(s);
        });
        return stacks;
    }


    public static List<ReinforcedCondenserEntry> getCondenserEntried() {
        return List.of(
                new ReinforcedCondenserEntry(
                        new ItemStack(AEItems.SINGULARITY).copyWithCount(8192),
                        new ItemStack(CrazyItemRegistrar.SUPER_SINGULARITY.get())
                )
        );
    }
}