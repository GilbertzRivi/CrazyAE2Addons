package net.oktawia.crazyae2addons.xei.common;

import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.blocks.*;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.*;


public class CrazyPreview extends WidgetGroup {

    private final TrackedDummyWorld world = new TrackedDummyWorld();
    private static final int WIDTH = 160;
    private static final int HEIGHT = 200;

    private final SceneWidget sceneWidgetAll;
    private final SceneWidget sceneWidgetLayer;
    private final ResourceLocation structureId;
    private final ItemStackTransfer selectedStackTransfer;
    private int layer = -1;
    private int minY = 0;
    private int maxY = 0;
    private Set<BlockPos> allPositions = new HashSet<>();
    private final SlotWidget selectedBlockSlot;
    private ItemStack selectedStack = ItemStack.EMPTY;

    public CrazyPreview(ResourceLocation structureId, List<ItemStack> ingredients, String displayName) {
        super(0, 0, WIDTH, HEIGHT);
        setClientSideWidget();
        this.structureId = structureId;

        sceneWidgetAll = new SceneWidget(5, 20, 150, 150, world).setRenderFacing(false);
        sceneWidgetLayer = new SceneWidget(5, 20, 150, 150, world).setRenderFacing(false);
        LabelWidget label = new LabelWidget(5, 5, displayName);

        sceneWidgetAll.setOnSelected((pos, facing) -> {
            updateSelectedBlock(pos);
        });

        sceneWidgetLayer.setOnSelected((pos, facing) -> {
            updateSelectedBlock(pos);
        });

        addWidget(sceneWidgetAll);
        addWidget(sceneWidgetLayer);
        addWidget(label);

        this.selectedStackTransfer = new ItemStackTransfer(ItemStack.EMPTY);

        selectedBlockSlot = new SlotWidget(selectedStackTransfer, 0, 5, 25, false, false)
                .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE);

        addWidget(selectedBlockSlot);

        sceneWidgetAll.setVisible(true);
        sceneWidgetLayer.setVisible(false);

        addWidget(new ButtonWidget(130, 20, 20, 20,
                new com.lowdragmc.lowdraglib.gui.texture.TextTexture("L")
                        .setSupplier(() -> layer >= 0
                                ? Component.translatable("gui.crazyae2addons.crazy_preview_layer_prefix").getString() + layer
                                : Component.translatable("gui.crazyae2addons.crazy_preview_layer_all").getString()),
                b -> switchLayer()));

        int x = 5;
        int y = 160;
        for (ItemStack stack : ingredients) {
            addWidget(new SlotWidget(new ItemStackTransfer(stack), 0, x, y, false, false)
                    .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                    .setIngredientIO(IngredientIO.INPUT));
            y -= 18;
        }

        loadStructure();
    }

    private void updateSelectedBlock(BlockPos pos) {
        if (pos == null) {
            selectedStack = ItemStack.EMPTY;
        } else {
            BlockState state = world.getBlockState(pos);
            selectedStack = state.isAir() ? ItemStack.EMPTY : new ItemStack(state.getBlock());
        }
        selectedStackTransfer.setStackInSlot(0, selectedStack);
    }

    private void switchLayer() {
        layer++;
        if (layer > (maxY - minY)) {
            layer = -1;
        }
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
                if (pos.getY() == targetY) {
                    filtered.add(pos);
                }
            }
            sceneWidgetLayer.setRenderedCore(filtered, null);
            sceneWidgetAll.setVisible(false);
            sceneWidgetLayer.setVisible(true);
        }
    }

    private void loadStructure() {
        try {
            ResourceLocation assetRl = new ResourceLocation(structureId.getNamespace(), "structures/" + structureId.getPath());
            InputStream stream = Minecraft.getInstance().getResourceManager().getResource(assetRl).orElseThrow().open();
            CompoundTag tag = NbtIo.readCompressed(stream);

            List<BlockState> palette = new ArrayList<>();
            ListTag paletteTag = tag.getList("palette", Tag.TAG_COMPOUND);
            for (Tag t : paletteTag) {
                CompoundTag entry = (CompoundTag) t;
                Block block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getString("Name")));
                BlockState state = block != null ? block.defaultBlockState() : Blocks.AIR.defaultBlockState();
                palette.add(state);
            }

            Map<BlockPos, BlockInfo> blockMap = new HashMap<>();
            ListTag blocksTag = tag.getList("blocks", Tag.TAG_COMPOUND);
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (Tag t : blocksTag) {
                CompoundTag blockTag = (CompoundTag) t;
                ListTag posList = blockTag.getList("pos", Tag.TAG_INT);
                BlockPos pos = new BlockPos(posList.getInt(0), posList.getInt(1), posList.getInt(2));
                BlockState state = palette.get(blockTag.getInt("state"));
                if (state.getBlock() instanceof MobFarmWallBlock) {
                    state = state.setValue(MobFarmWallBlock.FORMED, true);
                } else if (state.getBlock() instanceof SpawnerExtractorWallBlock) {
                    state = state.setValue(SpawnerExtractorWallBlock.FORMED, true);
                } else if (state.getBlock() instanceof EntropyCradle) {
                    state = state.setValue(EntropyCradle.FORMED, true);
                } else if (state.getBlock() instanceof EntropyCradleCapacitor) {
                    state = state.setValue(EntropyCradleCapacitor.FORMED, true);
                } else if (state.getBlock() instanceof PenroseFrameBlock) {
                    state = state.setValue(PenroseFrameBlock.FORMED, true);
                }
                blockMap.put(pos, BlockInfo.fromBlockState(state));
                min = Math.min(min, pos.getY());
                max = Math.max(max, pos.getY());
            }

            minY = min;
            maxY = max;
            allPositions = blockMap.keySet();

            world.clear();
            world.addBlocks(blockMap);

            sceneWidgetAll.setRenderedCore(allPositions, null);
            updateRenderedLayer();

        } catch (Exception e) {
            LogUtils.getLogger().info(e.toString());
        }
    }

    public static CrazyPreview getPreviewWidget(ResourceLocation id, List<ItemStack> ingredients, String name) {
        return new CrazyPreview(id, ingredients, name);
    }
}