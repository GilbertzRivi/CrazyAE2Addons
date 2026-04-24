package net.oktawia.crazyae2addons.defs.recipes;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FabricationRecipes {

    public record InputEntry(String item, int count) {}
    public record FluidEntry(String fluid, int amount) {}

    public record RecipeDef(
            String id,
            List<InputEntry> inputs,
            @Nullable String outputItem,
            int outputCount,
            @Nullable FluidEntry fluidInput,
            @Nullable FluidEntry fluidOutput
    ) {}

    private static final List<RecipeDef> RECIPES = new ArrayList<>();

    public static List<RecipeDef> getRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static Builder recipe(String id) {
        return new Builder(id);
    }

    public static void registerRecipes() {
        recipe("crazy_provider")
                .input("ae2:pattern_provider", 8)
                .input("minecraft:diamond", 1)
                .input("ae2:engineering_processor", 2)
                .input("ae2:logic_processor", 2)
                .input("ae2:calculation_processor", 2)
                .output("crazyae2addons:crazy_pattern_provider", 1)
                .register();
        recipe("cpu_priority_tunner")
                .input("ae2:certus_quartz_wrench", 1)
                .input("ae2:crafting_monitor", 1)
                .output("crazyae2addons:cpu_priority_tuner", 1)
                .register();
        recipe("ejector")
                .input("ae2:pattern_provider", 1)
                .input("minecraft:piston", 1)
                .input("minecraft:stone_button", 1)
                .output("crazyae2addons:ejector", 1)
                .register();
        recipe("multi_level_emitter")
                .input("ae2:level_emitter", 8)
                .input("ae2:engineering_processor", 2)
                .output("crazyae2addons:multi_level_emitter", 1)
                .register();
        recipe("pattern_multiplier")
                .input("ae2:blank_pattern", 1)
                .input("ae2:engineering_processor", 2)
                .input("ae2:logic_processor", 2)
                .input("minecraft:chest", 1)
                .output("crazyae2addons:pattern_multiplier", 1)
                .register();
        recipe("tag_level_emitter")
                .input("crazyae2addons:multi_level_emitter", 1)
                .input("crazyae2addons:tag_view_cell", 1)
                .input("ae2:logic_processor", 1)
                .output("crazyae2addons:tag_level_emitter", 1)
                .register();
        recipe("redstone_emitter")
                .input("ae2:level_emitter", 1)
                .input("minecraft:redstone", 4)
                .input("ae2:logic_processor", 1)
                .output("crazyae2addons:redstone_emitter", 1)
                .register();
        recipe("wormhole")
                .input("ae2:me_p2p_tunnel", 1)
                .input("ae2:item_p2p_tunnel", 1)
                .input("ae2:fluid_p2p_tunnel", 1)
                .input("ae2:fe_p2p_tunnel", 1)
                .input("minecraft:nether_star", 1)
                .input("minecraft:diamond", 4)
                .output("crazyae2addons:wormhole", 1)
                .register();
        recipe("portable_spatial_storage")
                .input("ae2:spatial_io_port", 1)
                .input("ae2:dense_energy_cell", 1)
                .input("ae2:spatial_pylon", 16)
                .input("ae2:import_bus", 4)
                .input("ae2:export_bus", 4)
                .input("ae2:wireless_terminal", 1)
                .output("crazyae2addons:portable_spatial_storage", 1)
                .register();
        recipe("portable_spatial_cloner")
                .input("crazyae2addons:portable_spatial_storage", 1)
                .input("ae2:dense_energy_cell", 2)
                .input("ae2:wireless_receiver", 1)
                .input("ae2:wireless_crafting_terminal", 1)
                .output("crazyae2addons:portable_spatial_cloner", 1)
                .register();
    }

    public static class Builder {
        private final String id;
        private final List<InputEntry> inputs = new ArrayList<>();
        private String outputItem = null;
        private int outputCount = 1;
        private FluidEntry fluidInput = null;
        private FluidEntry fluidOutput = null;
        private Builder(String id) {
            this.id = id;
        }

        public Builder input(String item, int count) {
            inputs.add(new InputEntry(item, count));
            return this;
        }

        public Builder output(String item, int count) {
            this.outputItem = item;
            this.outputCount = count;
            return this;
        }

        public Builder fluidInput(String fluid, int amount) {
            this.fluidInput = new FluidEntry(fluid, amount);
            return this;
        }

        public Builder fluidOutput(String fluid, int amount) {
            this.fluidOutput = new FluidEntry(fluid, amount);
            return this;
        }

        public void register() {
            RECIPES.add(new RecipeDef(id, List.copyOf(inputs), outputItem, outputCount, fluidInput, fluidOutput));
        }
    }
}
