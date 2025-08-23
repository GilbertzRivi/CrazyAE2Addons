package net.oktawia.crazyae2addons.xei.common;

import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.defs.regs.CrazyRecipes;
import net.oktawia.crazyae2addons.recipes.ResearchRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.*;

public class ResearchPreview extends WidgetGroup {

    private final TrackedDummyWorld world = new TrackedDummyWorld();
    private static final int WIDTH = 160;
    private static final int HEIGHT = 200;

    private final SceneWidget sceneAll;
    private final SceneWidget sceneLayer;

    private final @Nullable ResourceLocation recipeId;
    private final @Nullable ResearchRecipe recipe;

    private int layer = -1;
    private int minY = 0;
    private int maxY = 0;

    private Set<BlockPos> allPositions = new HashSet<>();

    private Map<BlockPos, BlockInfo> baseBlocks = new HashMap<>();

    public ResearchPreview(@Nullable ResourceLocation recipeId,
                           @Nullable ResearchRecipe recipe,
                           List<ItemStack> inputs,
                           ItemStack driveOrOutput,
                           @Nullable ResourceLocation overlayNbt) {
        super(0, 0, WIDTH, HEIGHT);
        setClientSideWidget();

        this.recipeId = recipeId;
        this.recipe = recipe;

        int sceneX = 5;
        int sceneY = 5;
        int sceneSize = 100;

        sceneAll = new SceneWidget(sceneX, sceneY, sceneSize, sceneSize, world).setRenderFacing(false);
        sceneLayer = new SceneWidget(sceneX, sceneY, sceneSize, sceneSize, world).setRenderFacing(false);
        addWidget(sceneAll);
        addWidget(sceneLayer);
        sceneAll.setVisible(true);
        sceneLayer.setVisible(false);
        addWidget(new ButtonWidget(sceneX + sceneSize - 25, sceneY, 20, 20,
                new TextTexture("ALL"), b -> switchLayer())
                .appendHoverTooltips("Change layer visibility"));

        addInfoButton(sceneX, sceneY, sceneSize);

        int inputX = 125;
        int inputY = 20;
        addWidget(new LabelWidget(inputX - 8, inputY - 15, "Inputs"));

        for (int i = 0; i < inputs.size(); i++) {
            ItemStack stack = inputs.get(i);
            addWidget(new SlotWidget(new ItemStackTransfer(stack), 0, inputX, inputY, false, false)
                    .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                    .setIngredientIO(IngredientIO.INPUT));

            if (i == 0) {
                addWidget(new ButtonWidget(inputX + 20, inputY + 2, 12, 12,
                        new TextTexture("!"), b -> {})
                        .appendHoverTooltips("Nokia 3310 with the scanned structure"));
            }

            inputY += 22;
        }

        int driveY = inputY + 10;
        String driveLabel = "Drive";
        int driveLabelX = inputX + (18 - Minecraft.getInstance().font.width(driveLabel)) / 2;
        addWidget(new LabelWidget(driveLabelX, driveY - 12, driveLabel));

        addWidget(new SlotWidget(new ItemStackTransfer(driveOrOutput), 0, inputX, driveY, false, false)
                .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.OUTPUT));

        addWidget(new ButtonWidget(inputX + 20, driveY + 2, 12, 12,
                new TextTexture("!"), b -> {})
                .appendHoverTooltips("Data Drive: stores the unlocked recipe", "Can hold many recipes at once"));

        ResearchRecipe r = resolveRecipe();
        if (r != null) {
            String sizeText = "Size: %dx%dx%d".formatted(r.structure.size[0], r.structure.size[1], r.structure.size[2]);
            int sizeX = sceneX + (sceneSize - Minecraft.getInstance().font.width(sizeText)) / 2;
            addWidget(new LabelWidget(sizeX, sceneY + sceneSize + 5, sizeText));
        }

        addProcessInfoPanel();
        loadStructure();

        if (overlayNbt != null) {
            loadOverlayFromNbt(overlayNbt, new BlockPos(-3, -1, -3));
        }
    }

    private void addInfoButton(int sceneX, int sceneY, int sceneSize) {
        List<String> tt = new ArrayList<>();

        // najpierw instrukcja
        tt.add("Structure must be scanned with the Nokia 3310");
        tt.add("and inserted into the Research Station.");
        tt.add(""); // pusta linia jako separator

        // potem dopiero lista bloków
        ResearchRecipe r = resolveRecipe();
        if (r != null) {
            tt.addAll(buildStructureTooltipLines(r));
        }

        ButtonWidget infoBtn = new ButtonWidget(sceneX + sceneSize - 45, sceneY, 20, 20,
                new TextTexture("!"), b -> {});
        infoBtn.appendHoverTooltips(tt.toArray(new String[0]));
        addWidget(infoBtn);
    }



    private List<String> buildStructureTooltipLines(ResearchRecipe r) {
        List<String> lines = new ArrayList<>();

        int sx = r.structure.size[0];
        int sy = r.structure.size[1];
        int sz = r.structure.size[2];

        Map<String, Integer> symbolCounts = new LinkedHashMap<>();
        for (int y = 0; y < sy; y++) {
            List<String> rows = r.structure.layers.get(y);
            for (int z = 0; z < sz; z++) {
                String[] cols = rows.get(z).trim().split("\\s+");
                for (int x = 0; x < sx; x++) {
                    String sym = cols[x];
                    if (sym.equals(".") || sym.equals(" ")) continue;
                    symbolCounts.merge(sym, 1, Integer::sum);
                }
            }
        }

        int total = symbolCounts.values().stream().mapToInt(i -> i).sum();
        lines.add("Total blocks: " + total);

        for (var e : symbolCounts.entrySet()) {
            String sym = e.getKey();
            int count = e.getValue();
            List<ResourceLocation> allowed = r.structure.symbols.getOrDefault(sym, List.of());
            String allowedStr;
            if (allowed.isEmpty()) {
                allowedStr = "<none>";
            } else {
                int maxShow = Math.min(3, allowed.size());
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < maxShow; i++) {
                    if (i > 0) sb.append(" | ");
                    sb.append(allowed.get(i));
                }
                if (allowed.size() > 3) sb.append(" | ...");
                allowedStr = sb.toString();
            }
            lines.add("%s × %d → %s".formatted(sym, count, allowedStr));
        }

        return lines;
    }


    private void addProcessInfoPanel() {
        try {
            ResearchRecipe r = resolveRecipe();
            if (r == null) return;

            int seconds = r.duration / 20;
            String line1 = "Research duration: " + seconds + "s";

            String line2 = "FE/t: " + r.energyPerTick + " Fluid/t: " + r.fluidPerTick;

            long totalEnergy = (long) r.duration * r.energyPerTick;
            long totalFluid = (long) r.duration * r.fluidPerTick;
            String line3 = "Total Energy: " + totalEnergy + " FE";
            String line4 = "Total Fluid: " + totalFluid + " mB";

            String line5 = "Unlocks: " + (r.unlock.label == null || r.unlock.label.isEmpty()
                    ? r.unlock.key.toString()
                    : r.unlock.label);

            int startY = 128;
            int lineH = Minecraft.getInstance().font.lineHeight + 2;

            int cx1 = (WIDTH - Minecraft.getInstance().font.width(line1)) / 2;
            int cx2 = (WIDTH - Minecraft.getInstance().font.width(line2)) / 2;
            int cx3 = (WIDTH - Minecraft.getInstance().font.width(line3)) / 2;
            int cx4 = (WIDTH - Minecraft.getInstance().font.width(line4)) / 2;
            int cx5 = (WIDTH - Minecraft.getInstance().font.width(line5)) / 2;

            addWidget(new LabelWidget(cx1, startY, line1));
            addWidget(new LabelWidget(cx2, startY + lineH, line2));
            addWidget(new LabelWidget(cx3, startY + lineH * 2, line3));
            addWidget(new LabelWidget(cx4, startY + lineH * 3, line4));
            addWidget(new LabelWidget(cx5, startY + lineH * 4, line5));
        } catch (Exception ignored) {}
    }


    private void switchLayer() {
        layer++;
        if (layer > (maxY - minY)) layer = -1;
        updateRenderedLayer();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void updateRenderedLayer() {
        if (layer == -1) {
            sceneAll.setVisible(true);
            sceneLayer.setVisible(false);
        } else {
            int targetY = minY + layer;
            Set<BlockPos> filtered = new HashSet<>();
            for (BlockPos pos : allPositions) {
                if (pos.getY() == targetY) filtered.add(pos);
            }
            sceneLayer.setRenderedCore(filtered, null);
            sceneAll.setVisible(false);
            sceneLayer.setVisible(true);
        }
    }

    private void loadStructure() {
        try {
            ResearchRecipe r = resolveRecipe();
            if (r == null) return;

            int sx = r.structure.size[0];
            int sy = r.structure.size[1];
            int sz = r.structure.size[2];

            Map<BlockPos, BlockInfo> blockMap = buildBlocksFromPattern(r);

            minY = 0;
            maxY = sy - 1;
            allPositions = blockMap.keySet();

            baseBlocks.clear();
            baseBlocks.putAll(blockMap);

            world.clear();
            world.addBlocks(blockMap);
            sceneAll.setRenderedCore(allPositions, null);
            updateRenderedLayer();
        } catch (Exception e) {
            LogUtils.getLogger().warn("Failed to load research structure {}: {}", recipeId, e.toString());
        }
    }


    private void loadOverlayFromNbt(ResourceLocation rl, BlockPos offset) {
        try {
            InputStream is = Minecraft.getInstance().getResourceManager().getResource(rl).orElseThrow().open();
            CompoundTag tag = NbtIo.readCompressed(is);

            List<BlockState> palette = new ArrayList<>();
            for (Tag t : tag.getList("palette", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) t;
                Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getString("Name")));
                palette.add(b != null ? b.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }

            Map<BlockPos, BlockInfo> map = new HashMap<>();
            for (Tag t : tag.getList("blocks", Tag.TAG_COMPOUND)) {
                CompoundTag b = (CompoundTag) t;
                ListTag posList = b.getList("pos", Tag.TAG_INT);
                BlockPos pos = new BlockPos(posList.getInt(0), posList.getInt(1), posList.getInt(2));
                BlockState state = palette.get(b.getInt("state"));
                if (!state.isAir()) map.put(pos.offset(offset), BlockInfo.fromBlockState(state));
            }
        } catch (Exception e) {
            LogUtils.getLogger().warn("Failed to load overlay {}: {}", rl, e.toString());
        }
    }

    @Nullable
    private ResearchRecipe resolveRecipe() {
        if (recipe != null) return recipe;
        if (recipeId == null) return null;

        var level = Minecraft.getInstance().level;
        if (level == null) {
            LogUtils.getLogger().warn("No client level available to load research recipe {}", recipeId);
            return null;
        }
        return level.getRecipeManager()
                .getAllRecipesFor(CrazyRecipes.RESEARCH_TYPE.get())
                .stream()
                .filter(rr -> rr.getId().equals(recipeId))
                .findFirst()
                .orElse(null);
    }

    @NotNull
    private Map<BlockPos, BlockInfo> buildBlocksFromPattern(ResearchRecipe r) {
        int sx = r.structure.size[0];
        int sy = r.structure.size[1];
        int sz = r.structure.size[2];

        Map<String, Block> symbolBlock = new HashMap<>();
        for (var e : r.structure.symbols.entrySet()) {
            Block chosen = Blocks.AIR;
            var list = e.getValue();
            if (list != null && !list.isEmpty()) {
                Block b = ForgeRegistries.BLOCKS.getValue(list.get(0));
                if (b != null) chosen = b;
            }
            symbolBlock.put(e.getKey(), chosen);
        }

        Map<BlockPos, BlockInfo> out = new HashMap<>();
        for (int y = 0; y < sy; y++) {
            List<String> rows = r.structure.layers.get(y);
            for (int z = 0; z < sz; z++) {
                String[] cols = rows.get(z).trim().split("\\s+");
                for (int x = 0; x < sx; x++) {
                    String sym = cols[x];
                    if (sym.equals(".") || sym.equals(" ")) continue;
                    Block b = symbolBlock.getOrDefault(sym, Blocks.AIR);
                    if (b == Blocks.AIR) continue;
                    out.put(new BlockPos(x, y, z), BlockInfo.fromBlockState(b.defaultBlockState()));
                }
            }
        }
        return out;
    }
}
