package net.oktawia.crazyae2addons.recipes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** Matcher porównujący snapshot z gadżetu z definicją struktury w ResearchRecipe.Structure. */
public final class ResearchStructureMatcher {

    /** Prosty wynik dopasowania – bez statycznych fabryk. */
    public final class MatchResult {
        public final boolean ok;
        public final List<String> reasons;
        public MatchResult(boolean ok, List<String> reasons) {
            this.ok = ok;
            this.reasons = List.copyOf(reasons);
        }
    }

    /** Główny punkt wejścia – metoda instancyjna. */
    public MatchResult match(ResearchRecipe.Structure def, StructureSnapshot snap) {
        List<String> reasons = new ArrayList<>();

        // 1) Rozmiar
        int dx = def.size[0], dy = def.size[1], dz = def.size[2];
        if (snap.sizeX() != dx || snap.sizeY() != dy || snap.sizeZ() != dz) {
            reasons.add("Size mismatch: gadget=%dx%dx%d vs recipe=%dx%dx%d"
                    .formatted(snap.sizeX(), snap.sizeY(), snap.sizeZ(), dx, dy, dz));
            return new MatchResult(false, reasons);
        }

        // 2) SIZE_ONLY – sam rozmiar wystarcza
        if (def.mode == ResearchRecipe.StructureMode.SIZE_ONLY) {
            return new MatchResult(true, List.of());
        }

        // 3) PATTERN → symbol -> dopuszczalne bloki
        Map<String, Set<Block>> symbolBlocks = new HashMap<>();
        for (var e : def.symbols.entrySet()) {
            Set<Block> set = new HashSet<>();
            for (ResourceLocation rl : e.getValue()) {
                Block b = ForgeRegistries.BLOCKS.getValue(rl);
                if (b != null && b != Blocks.AIR) set.add(b);
            }
            symbolBlocks.put(e.getKey(), set);
        }

        // 4) Sprawdzenie każdej komórki
        int mismatches = 0;
        for (int y = 0; y < dy; y++) {
            var rows = def.layers.get(y);
            for (int z = 0; z < dz; z++) {
                String[] cols = rows.get(z).trim().split("\\s+");
                for (int x = 0; x < dx; x++) {
                    String sym = cols[x];

                    // "." lub spacja = oczekujemy AIR
                    if (sym.equals(".") || sym.equals(" ")) {
                        BlockState st = snap.get(x, y, z);
                        if (st != null && !st.isAir()) {
                            if (mismatches < 10) {
                                reasons.add("(%d,%d,%d): expected AIR (.), got %s"
                                        .formatted(x, y, z, st.getBlock().builtInRegistryHolder().key().location()));
                            }
                            mismatches++;
                        }
                        continue;
                    }

                    Set<Block> allowed = symbolBlocks.getOrDefault(sym, Set.of());
                    BlockState st = snap.get(x, y, z);
                    Block got = (st == null || st.isAir()) ? Blocks.AIR : st.getBlock();

                    if (!allowed.contains(got)) {
                        if (mismatches < 10) {
                            reasons.add("(%d,%d,%d): expected %s, got %s".formatted(
                                    x, y, z, summarize(allowed), got.builtInRegistryHolder().key().location()));
                        }
                        mismatches++;
                    }
                }
            }
        }

        if (mismatches == 0) {
            return new MatchResult(true, List.of());
        }
        reasons.add("Total mismatches: " + mismatches);
        return new MatchResult(false, reasons);
    }

    private String summarize(Set<Block> allowed) {
        if (allowed.isEmpty()) return "<nothing>";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Block b : allowed) {
            if (i++ > 0) sb.append(" | ");
            sb.append(b.builtInRegistryHolder().key().location());
            if (i >= 3 && allowed.size() > 3) { sb.append(" | ..."); break; }
        }
        return sb.toString();
    }
}
