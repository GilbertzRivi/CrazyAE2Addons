package net.oktawia.crazyae2addons.recipes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public final class ResearchStructureMatcher {

    public final class MatchResult {
        public final boolean ok;
        public final List<String> reasons;
        public MatchResult(boolean ok, List<String> reasons) {
            this.ok = ok;
            this.reasons = List.copyOf(reasons);
        }
    }

    public MatchResult match(ResearchRecipe.Structure def, StructureSnapshot snap) {
        Map<String, Set<Block>> symbolBlocks = new HashMap<>();
        for (var e : def.symbols.entrySet()) {
            Set<Block> set = new HashSet<>();
            for (ResourceLocation rl : e.getValue()) {
                Block b = ForgeRegistries.BLOCKS.getValue(rl);
                if (b != null && b != Blocks.AIR) set.add(b);
            }
            symbolBlocks.put(e.getKey(), set);
        }

        int dx = def.size[0], dy = def.size[1], dz = def.size[2];

        List<Transform> transforms = Transform.all();

        if (def.mode == ResearchRecipe.StructureMode.SIZE_ONLY) {
            for (Transform t : transforms) {
                Dim td = t.applyDims(dx, dy, dz);
                if (snap.sizeX() == td.x && snap.sizeY() == td.y && snap.sizeZ() == td.z) {
                    return new MatchResult(true, List.of("Matched by size using transform: " + t.label()));
                }
            }
            String expected = transforms.stream()
                    .map(t -> {
                        Dim d = t.applyDims(dx, dy, dz);
                        return "%dx%dx%d (%s)".formatted(d.x, d.y, d.z, t.label());
                    })
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(", "));
            return new MatchResult(false, List.of(
                    "Size mismatch for all transforms.",
                    "Gadget: %dx%dx%d".formatted(snap.sizeX(), snap.sizeY(), snap.sizeZ()),
                    "Recipe possible sizes: " + expected
            ));
        }

        Transform bestT = null;
        int bestMismatches = Integer.MAX_VALUE;
        List<String> bestReasons = null;

        for (Transform t : transforms) {
            Dim td = t.applyDims(dx, dy, dz);
            if (snap.sizeX() != td.x || snap.sizeY() != td.y || snap.sizeZ() != td.z) {
                continue;
            }

            List<String> reasons = new ArrayList<>();
            int mismatches = 0;

            for (int y = 0; y < dy; y++) {
                var rows = def.layers.get(y);
                for (int z = 0; z < dz; z++) {
                    String[] cols = rows.get(z).trim().split("\\s+");
                    for (int x = 0; x < dx; x++) {
                        String sym = cols[x];

                        Vec p = t.map(x, y, z, dx, dy, dz);

                        BlockState st = snap.get(p.x, p.y, p.z);

                        if (sym.equals(".") || sym.equals(" ")) {
                            if (st != null && !st.isAir()) {
                                if (mismatches < 10) {
                                    reasons.add("(%d,%d,%d)->(%d,%d,%d) [%s]: expected AIR (.), got %s"
                                            .formatted(x, y, z, p.x, p.y, p.z, t.label(),
                                                    st.getBlock().builtInRegistryHolder().key().location()));
                                }
                                mismatches++;
                            }
                            continue;
                        }

                        Set<Block> allowed = symbolBlocks.getOrDefault(sym, Set.of());
                        Block got = (st == null || st.isAir()) ? Blocks.AIR : st.getBlock();

                        if (!allowed.contains(got)) {
                            if (mismatches < 10) {
                                reasons.add("(%d,%d,%d)->(%d,%d,%d) [%s]: expected %s, got %s".formatted(
                                        x, y, z, p.x, p.y, p.z, t.label(), summarize(allowed),
                                        got.builtInRegistryHolder().key().location()));
                            }
                            mismatches++;
                        }
                    }
                }
            }

            if (mismatches == 0) {
                return new MatchResult(true, List.of("Transform used: " + t.label()));
            }

            if (mismatches < bestMismatches) {
                bestMismatches = mismatches;
                bestReasons = reasons;
                bestT = t;
            }
        }

        List<String> reasons = new ArrayList<>();
        if (bestT != null) {
            reasons.add("Best transform: " + bestT.label());
            reasons.addAll(bestReasons);
            reasons.add("Total mismatches: " + bestMismatches);
            return new MatchResult(false, reasons);
        } else {
            String expected = transforms.stream()
                    .map(t -> {
                        Dim d = t.applyDims(dx, dy, dz);
                        return "%dx%dx%d (%s)".formatted(d.x, d.y, d.z, t.label());
                    })
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(", "));
            reasons.add("Size mismatch: gadget=%dx%dx%d, recipe options=%s"
                    .formatted(snap.sizeX(), snap.sizeY(), snap.sizeZ(), expected));
            return new MatchResult(false, reasons);
        }
    }

    private record Dim(int x, int y, int z) {}

    private record Vec(int x, int y, int z) {}

    private enum RotY { R0, R90, R180, R270 }

    private record Transform(RotY rot, boolean mirrorX, boolean mirrorZ, boolean flipY) {

        static List<Transform> all() {
                List<Transform> list = new ArrayList<>();
                for (RotY r : RotY.values()) {
                    for (boolean mx : new boolean[]{false, true}) {
                        for (boolean mz : new boolean[]{false, true}) {
                            for (boolean fy : new boolean[]{false, true}) {
                                list.add(new Transform(r, mx, mz, fy));
                            }
                        }
                    }
                }
                return list;
            }

            String label() {
                StringBuilder sb = new StringBuilder(rot.name());
                if (mirrorX) sb.append("+MX");
                if (mirrorZ) sb.append("+MZ");
                if (flipY) sb.append("+MY");
                return sb.toString();
            }

            Dim applyDims(int dx, int dy, int dz) {
                int rx, rz;
                switch (rot) {
                    case R0, R180 -> {
                        rx = dx;
                        rz = dz;
                    }
                    case R90, R270 -> {
                        rx = dz;
                        rz = dx;
                    }
                    default -> throw new IllegalStateException();
                }
                return new Dim(rx, dy, rz);
            }

            Vec map(int x, int y, int z, int dx, int dy, int dz) {
                int xr, zr;
                switch (rot) {
                    case R0 -> {
                        xr = x;
                        zr = z;
                    }
                    case R90 -> {
                        xr = z;
                        zr = (dx - 1) - x;
                    }
                    case R180 -> {
                        xr = (dx - 1) - x;
                        zr = (dz - 1) - z;
                    }
                    case R270 -> {
                        xr = (dz - 1) - z;
                        zr = x;
                    }
                    default -> throw new IllegalStateException();
                }
                int yr = flipY ? (dy - 1) - y : y;

                Dim d = applyDims(dx, dy, dz);
                if (mirrorX) xr = (d.x - 1) - xr;
                if (mirrorZ) zr = (d.z - 1) - zr;

                return new Vec(xr, yr, zr);
            }
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
