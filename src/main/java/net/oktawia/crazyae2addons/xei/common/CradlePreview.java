package net.oktawia.crazyae2addons.xei.common;

import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.blocks.EntropyCradle;
import net.oktawia.crazyae2addons.blocks.EntropyCradleCapacitor;
import net.oktawia.crazyae2addons.defs.regs.CrazyRecipes;
import net.oktawia.crazyae2addons.recipes.CradlePattern;
import net.oktawia.crazyae2addons.recipes.CradleRecipe;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.*;

public class CradlePreview extends WidgetGroup {

    private final TrackedDummyWorld world = new TrackedDummyWorld();
    private static final int WIDTH = 160;
    private static final int HEIGHT = 200;

    private final SceneWidget sceneWidgetAll;
    private final SceneWidget sceneWidgetLayer;
    private final ResourceLocation structureId;
    private int layer = -1;
    private int minY = 0;
    private int maxY = 0;
    private Set<BlockPos> allPositions = new HashSet<>();
    private boolean showCradle = false;
    private Map<BlockPos, BlockInfo> cradleBlocks = null;
    private Map<BlockPos, BlockInfo> baseBlocks = new HashMap<>();

    public CradlePreview(ResourceLocation structureId, List<ItemStack> inputs, ItemStack output) {
        super(0, 0, WIDTH, HEIGHT);
        setClientSideWidget();
        this.structureId = structureId;

        int sceneX = 5;
        int sceneY = 5;
        int sceneSize = 100;

        sceneWidgetAll = new SceneWidget(sceneX, sceneY, sceneSize, sceneSize, world).setRenderFacing(false);
        sceneWidgetLayer = new SceneWidget(sceneX, sceneY, sceneSize, sceneSize, world).setRenderFacing(false);

        addWidget(sceneWidgetAll);
        addWidget(sceneWidgetLayer);

        sceneWidgetAll.setVisible(true);
        sceneWidgetLayer.setVisible(false);

        addWidget(new ButtonWidget(sceneX + sceneSize - 25, sceneY, 20, 20,
                new TextTexture("L").setSupplier(() -> layer >= 0 ? "L:" + layer : "ALL"),
                b -> switchLayer()).appendHoverTooltips("Change layer visibility"));
        addWidget(new ButtonWidget(sceneX + sceneSize - 50, sceneY, 20, 20,
                new TextTexture("Cradle").setSupplier(() -> showCradle ? "ON" : "OFF"),
                b -> toggleCradle()).appendHoverTooltips("Toggle cradle visibility"));

        int inputX = 125;
        int inputY = 20;
        addWidget(new LabelWidget(inputX - 8, inputY - 15, "Inputs"));

        for (ItemStack stack : inputs) {
            addWidget(new SlotWidget(new ItemStackTransfer(stack), 0, inputX, inputY, false, false)
                    .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                    .setIngredientIO(IngredientIO.INPUT));
            inputY += 22;
        }

        int outputSlotX = sceneX + sceneSize / 2 - 9;
        int outputSlotY = sceneY + sceneSize + 15;
        addWidget(new LabelWidget(outputSlotX - 8, outputSlotY - 12, "Output"));
        addWidget(new SlotWidget(new ItemStackTransfer(output), 0, outputSlotX, outputSlotY, false, false)
                .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.OUTPUT));

        loadStructure();

        try {
            var otp = String.valueOf(output.getItem());
            boolean dense = false;
            if (otp.contains("dense")) {
                dense = true;
            }
            var split = otp.split("_");
            int base = Integer.parseInt((split[split.length - 1]).substring(0, (split[split.length - 1].length() - 1)));
            long cost = 1024L * 1024 * 8 * base;
            if (dense) cost *= 1024;
            String line1 = dense ? "%sk Dense Energy Cell can hold".formatted(base) : "%sk Energy Cell can hold".formatted(base);
            String line2 = "up to %s AE".formatted(Utils.shortenNumber(cost));
            String line3 = "or %s FE".formatted(Utils.shortenNumber(cost * 2));

            int textHeight = Minecraft.getInstance().font.lineHeight * 2 + 2;
            int cx1 = (WIDTH - Minecraft.getInstance().font.width(line1)) / 2;
            int cx2 = (WIDTH - Minecraft.getInstance().font.width(line2)) / 2;
            int cx3 = (WIDTH - Minecraft.getInstance().font.width(line3)) / 2;
            int cy = (HEIGHT - textHeight) / 2 + 60;

            addWidget(new LabelWidget(cx1, cy, line1));
            addWidget(new LabelWidget(cx2, cy + Minecraft.getInstance().font.lineHeight + 2, line2));
            addWidget(new LabelWidget(cx3, cy + (Minecraft.getInstance().font.lineHeight + 2) * 2, line3));
        } catch (Exception ignored) {
            String line1 = "Quite a lot for one block,";
            String line2 = "and don't you need 1600?";

            int textHeight = Minecraft.getInstance().font.lineHeight * 2 + 2;
            int cx1 = (WIDTH - Minecraft.getInstance().font.width(line1)) / 2;
            int cx2 = (WIDTH - Minecraft.getInstance().font.width(line2)) / 2;
            int cy = (HEIGHT - textHeight) / 2 + 60;

            addWidget(new LabelWidget(cx1, cy, line1));
            addWidget(new LabelWidget(cx2, cy + Minecraft.getInstance().font.lineHeight + 2, line2));
        }
    }

    private void toggleCradle() {
        var rotationp = sceneWidgetAll.getRotationPitch();
        var rotationy = sceneWidgetAll.getRotationYaw();
        var scale = sceneWidgetAll.getZoom();
        var center = sceneWidgetAll.getCenter();
        showCradle = !showCradle;

        Map<BlockPos, BlockInfo> combined = new HashMap<>(baseBlocks);
        if (showCradle && cradleBlocks != null) {
            combined.putAll(cradleBlocks);
        }
        world.clear();
        world.addBlocks(combined);

        Set<BlockPos> combinedPositions = new HashSet<>(baseBlocks.keySet());
        if (showCradle && cradleBlocks != null) {
            combinedPositions.addAll(cradleBlocks.keySet());
        }
        allPositions = combinedPositions;

        sceneWidgetAll.setRenderedCore(allPositions, null);
        sceneWidgetAll.setCameraYawAndPitch(rotationy, rotationp);
        sceneWidgetAll.setZoom(scale);
        sceneWidgetAll.setCenter(center);
        updateRenderedLayer();
    }

    private void switchLayer() {
        layer++;
        if (layer > (maxY - minY)) layer = -1;
        updateRenderedLayer();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void updateRenderedLayer() {
        if (layer == -1) {
            sceneWidgetAll.setVisible(true);
            sceneWidgetLayer.setVisible(false);
        } else {
            int targetY = minY + layer;
            Set<BlockPos> filtered = new HashSet<>();
            for (BlockPos pos : allPositions) {
                if (pos.getY() == targetY) filtered.add(pos);
            }
            sceneWidgetLayer.setRenderedCore(filtered, null);
            sceneWidgetAll.setVisible(false);
            sceneWidgetLayer.setVisible(true);
        }
    }

    private void loadStructure() {
        try {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                LogUtils.getLogger().warn("No client level available to load recipe {}", structureId);
                return;
            }

            var mgr = level.getRecipeManager();

            CradleRecipe r = mgr
                    .getAllRecipesFor(CrazyRecipes.CRADLE_TYPE.get())
                    .stream()
                    .filter(cr -> cr.getId().equals(structureId))
                    .findFirst()
                    .orElse(null);

            if (r == null) {
                LogUtils.getLogger().warn("Cradle recipe not found for id {}", structureId);
                return;
            }

            var pattern = r.pattern();

            Map<BlockPos, BlockInfo> innerBlockMap = buildBlocksFromPattern(pattern);

            minY = 0;
            maxY = 4;
            allPositions = innerBlockMap.keySet();

            baseBlocks.clear();
            baseBlocks.putAll(innerBlockMap);

            world.clear();
            world.addBlocks(innerBlockMap);
            sceneWidgetAll.setRenderedCore(allPositions, null);
            updateRenderedLayer();

        } catch (Exception e) {
            LogUtils.getLogger().warn("Failed to load inner structure from recipe {}: {}", structureId, e.toString());
        }

        if (cradleBlocks == null) {
            cradleBlocks = new HashMap<>();
            try {
                ResourceLocation cradleRl = new ResourceLocation("crazyae2addons", "structures/entropy_cradle.nbt");
                InputStream cradleStream = Minecraft.getInstance().getResourceManager().getResource(cradleRl).orElseThrow().open();
                CompoundTag cradleTag = NbtIo.readCompressed(cradleStream);

                List<BlockState> cradlePalette = new ArrayList<>();
                for (Tag t : cradleTag.getList("palette", Tag.TAG_COMPOUND)) {
                    CompoundTag entry = (CompoundTag) t;
                    Block block = net.minecraftforge.registries.ForgeRegistries.BLOCKS
                            .getValue(new ResourceLocation(entry.getString("Name")));
                    cradlePalette.add(block != null ? block.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }

                for (Tag t : cradleTag.getList("blocks", Tag.TAG_COMPOUND)) {
                    CompoundTag b = (CompoundTag) t;
                    ListTag posList = b.getList("pos", Tag.TAG_INT);
                    BlockPos pos = new BlockPos(posList.getInt(0), posList.getInt(1), posList.getInt(2));
                    BlockState state = cradlePalette.get(b.getInt("state"));
                    if (state.getBlock() instanceof EntropyCradle){
                        state = state.setValue(EntropyCradle.FORMED, true);
                    } else if (state.getBlock() instanceof EntropyCradleCapacitor){
                        state = state.setValue(EntropyCradleCapacitor.FORMED, true);
                    }
                    if (!state.isAir()) {
                        cradleBlocks.put(pos.offset(-3, -1, -3), BlockInfo.fromBlockState(state));
                    }
                }
            } catch (Exception e) {
                LogUtils.getLogger().warn("Failed to load cradle overlay: {}", e.toString());
            }
        }
    }

    @NotNull
    private Map<BlockPos, BlockInfo> buildBlocksFromPattern(net.oktawia.crazyae2addons.recipes.CradlePattern pattern) {
        final int SIZE = 5;
        Map<BlockPos, BlockInfo> blockMap = new HashMap<>();

        Map<String, List<Block>> symbols = pattern.symbolMap();
        List<String[][]> layers = pattern.layers();

        for (int y = 0; y < SIZE; y++) {
            String[][] layer = layers.get(y);
            for (int z = 0; z < SIZE; z++) {
                String[] row = layer[z];
                for (int x = 0; x < SIZE; x++) {
                    String sym = row[x];
                    if (sym.equals(".")) continue;

                    var options = symbols.get(sym);
                    Block chosen = (options != null && !options.isEmpty()) ? options.get(0) : Blocks.AIR;

                    if (chosen != Blocks.AIR) {
                        BlockState state = chosen.defaultBlockState();
                        BlockPos pos = new BlockPos(x, y, z);
                        blockMap.put(pos, BlockInfo.fromBlockState(state));
                    }
                }
            }
        }
        return blockMap;
    }
}
