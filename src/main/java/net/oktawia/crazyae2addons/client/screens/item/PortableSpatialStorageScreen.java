package net.oktawia.crazyae2addons.client.screens.item;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.menu.SlotSemantics;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.item.PortableSpatialStorageMenu;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PortableSpatialStorageScreen<C extends PortableSpatialStorageMenu> extends UpgradeableScreen<C> {

    private static final Direction[] DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST,
            Direction.UP, Direction.DOWN
    };

    private final StringBuilder previewBuffer = new StringBuilder();
    private boolean receivingPreview = false;

    private final WidgetGroup root = new WidgetGroup(0, 0, 0, 0);
    private final TrackedDummyWorld world = new TrackedDummyWorld();
    private SceneWidget scene;

    private Map<BlockPos, BlockState> blocks = Collections.emptyMap();
    private Set<BlockPos> renderedCore = Collections.emptySet();
    private BlockPos min = BlockPos.ZERO;
    private BlockPos max = BlockPos.ZERO;

    private boolean rotating = false;
    private double lastMouseX;
    private double lastMouseY;
    private float yaw = 0.0f;
    private float pitch = 30.0f;
    private float distance = -20.0f;

    private int lastSceneWidth = -1;
    private int lastSceneHeight = -1;

    public PortableSpatialStorageScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        IconButton flipHorizontalButton = new IconButton(Icon.ARROW_RIGHT, button -> {
            getMenu().flipHorizontal();
            getMenu().requestPreview();
        });
        flipHorizontalButton.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.FLIP_HORIZONTAL.getTranslationKey())
        ));

        IconButton flipVerticalButton = new IconButton(Icon.ARROW_UP, button -> {
            getMenu().flipVertical();
            getMenu().requestPreview();
        });
        flipVerticalButton.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.FLIP_VERTICAL.getTranslationKey())
        ));

        IconButton rotateButton = new IconButton(Icon.SCHEDULING_DEFAULT, button -> {
            getMenu().rotateClockwise(1);
            getMenu().requestPreview();
        });
        rotateButton.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.ROTATE_CLOCKWISE.getTranslationKey())
        ));

        this.widgets.add("flipH", flipHorizontalButton);
        this.widgets.add("flipV", flipVerticalButton);
        this.widgets.add("rotate", rotateButton);

        this.root.setSize(this.width, this.height);
        getMenu().requestPreview();
    }

    public void setProgram(String data) {
        if ("__RESET__".equals(data)) {
            previewBuffer.setLength(0);
            receivingPreview = true;
            return;
        }

        if ("__END__".equals(data)) {
            receivingPreview = false;
            reloadPreviewNow();
            return;
        }

        if (receivingPreview) {
            previewBuffer.append(data);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (scene != null) {
            int x = this.leftPos + 8;
            int y = this.topPos + 24;
            int width = Math.max(32, this.imageWidth - 16);
            int height = Math.max(32, this.imageHeight - 24 - 104);

            if (scene.getPositionX() != x || scene.getPositionY() != y
                    || scene.getSizeWidth() != width || scene.getSizeHeight() != height) {
                scene.setSelfPosition(x, y);
                scene.setSize(width, height);

                if (width != lastSceneWidth || height != lastSceneHeight) {
                    lastSceneWidth = width;
                    lastSceneHeight = height;

                    int sizeX = max.getX() - min.getX() + 1;
                    int sizeY = max.getY() - min.getY() + 1;
                    int sizeZ = max.getZ() - min.getZ() + 1;
                    int maxDim = Math.max(sizeX, Math.max(sizeY, sizeZ));

                    float scaleW = (float) width / 160.0f;
                    float scaleH = (float) height / 120.0f;
                    float scale = Math.max(0.75f, Math.min(scaleW, scaleH));

                    distance = Math.max(6.0f, (maxDim * 2.2f) / scale);
                    scene.setZoom(distance);
                }
            }
        }

        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        graphics.flush();

        root.drawInBackground(graphics, mouseX, mouseY, partialTick);
        root.drawInForeground(graphics, mouseX, mouseY, partialTick);
        root.drawOverlay(graphics, mouseX, mouseY, partialTick);

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scene != null && insideScene(mouseX, mouseY) && button == 0) {
            rotating = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (rotating) {
            double dx = mouseX - lastMouseX;
            double dy = mouseY - lastMouseY;

            lastMouseX = mouseX;
            lastMouseY = mouseY;

            pitch += (float) (dx * 0.5f);
            yaw = Math.max(-89.0f, Math.min(89.0f, yaw + (float) (dy * 0.5f)));

            scene.setCameraYawAndPitch(yaw, pitch);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            rotating = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (scene != null && insideScene(mouseX, mouseY)) {
            float step = 1.0f;
            float minDistance = 2.0f;
            float maxDistance = 256.0f;

            distance = Math.max(minDistance, Math.min(maxDistance, distance - (float) delta * step));
            scene.setZoom(distance);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        root.updateScreen();
    }

    private void reloadPreviewNow() {
        String base64 = previewBuffer.toString();
        if (base64.isEmpty()) {
            clearScene();
            return;
        }

        try {
            byte[] bytes = TemplateUtil.fromBase64(base64);
            CompoundTag templateTag = TemplateUtil.decompressNbt(bytes);
            List<TemplateUtil.BlockInfo> blockInfos = TemplateUtil.parseBlocksFromTag(templateTag);

            if (blockInfos.isEmpty()) {
                clearScene();
                return;
            }

            HashMap<BlockPos, BlockState> newBlocks = new HashMap<>();

            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (TemplateUtil.BlockInfo info : blockInfos) {
                BlockPos pos = info.pos();
                newBlocks.put(pos, info.state());

                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }

            this.blocks = newBlocks;
            this.min = new BlockPos(minX, minY, minZ);
            this.max = new BlockPos(maxX, maxY, maxZ);

            this.world.clear();
            for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
                this.world.setBlock(entry.getKey(), entry.getValue(), 3);
            }

            if (this.scene == null) {
                this.scene = new SceneWidget(0, 0, 32, 32, world);
                this.root.clearAllWidgets();
                this.root.addWidget(scene);
            }

            scene.clearAllWidgets();
            this.renderedCore = computeSurface(this.blocks);
            scene.setRenderedCore(this.renderedCore);

            BlockPos size = max.subtract(min).offset(1, 1, 1);
            BlockPos center = new BlockPos(
                    (int) (min.getX() + size.getX() * 0.5),
                    (int) (min.getY() + size.getY() * 0.5),
                    (int) (min.getZ() + size.getZ() * 0.5)
            );

            scene.setCenter(center.getCenter().toVector3f());
            scene.setCameraYawAndPitch(yaw, pitch);
            scene.setZoom(distance);
        } catch (Exception ignored) {
            clearScene();
        }
    }

    private void clearScene() {
        this.blocks = Collections.emptyMap();
        this.renderedCore = Collections.emptySet();
        this.world.clear();

        if (this.scene != null) {
            this.root.clearAllWidgets();
            this.scene = null;
        }
    }

    private boolean insideScene(double mouseX, double mouseY) {
        return scene != null
                && mouseX >= scene.getPositionX()
                && mouseY >= scene.getPositionY()
                && mouseX < scene.getPositionX() + scene.getSizeWidth()
                && mouseY < scene.getPositionY() + scene.getSizeHeight();
    }

    private static Set<BlockPos> computeSurface(Map<BlockPos, BlockState> map) {
        java.util.HashSet<BlockPos> out = new java.util.HashSet<>();
        if (map == null || map.isEmpty()) {
            return out;
        }

        for (BlockPos pos : map.keySet()) {
            for (Direction direction : DIRECTIONS) {
                if (!map.containsKey(pos.relative(direction))) {
                    out.add(pos);
                    break;
                }
            }
        }

        return out;
    }
}