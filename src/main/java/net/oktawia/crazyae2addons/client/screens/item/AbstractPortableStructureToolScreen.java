package net.oktawia.crazyae2addons.client.screens.item;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.client.renderer.preview.PortableSpatialStorageDummyWorld;
import net.oktawia.crazyae2addons.client.renderer.preview.PortableSpatialStorageSceneWidget;
import net.oktawia.crazyae2addons.client.renderer.preview.PortableSpatialStoragePreviewSync;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewBlock;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewStructure;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.menus.item.AbstractPortableStructureToolMenu;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractPortableStructureToolScreen<M extends AbstractPortableStructureToolMenu> extends AEBaseScreen<M> {

    protected static final Direction[] DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST,
            Direction.UP, Direction.DOWN
    };

    protected static final int DIRECTION_COMPASS_SIZE = 48;
    protected static final float DIRECTION_COMPASS_ORTHO_RANGE = 18.5F;

    protected final WidgetGroup directionCompassRoot = new WidgetGroup(0, 0, 0, 0);
    protected TrackedDummyWorld directionCompassWorld;
    protected SceneWidget directionCompassScene;
    protected Set<BlockPos> directionCompassCore = Collections.emptySet();

    protected boolean transformAroundOriginMode = false;
    protected boolean previewStructureFromSharedCache = false;
    protected boolean initialCameraAlignedToPlayer = false;

    protected final WidgetGroup root = new WidgetGroup(0, 0, 0, 0);
    protected final PortableSpatialStorageDummyWorld world = new PortableSpatialStorageDummyWorld();
    protected PortableSpatialStorageSceneWidget scene;

    protected AETextField offsetDisplayX;
    protected AETextField offsetDisplayY;
    protected AETextField offsetDisplayZ;

    protected IconButton flipEastWestButton;
    protected IconButton flipNorthSouthButton;
    protected IconButton flipVerticalButton;
    protected IconButton rotateButton;

    protected PreviewStructure previewStructure;
    protected Set<BlockPos> renderedCore = Collections.emptySet();
    protected BlockPos min = BlockPos.ZERO;
    protected BlockPos max = BlockPos.ZERO;

    protected boolean rotating = false;
    protected double lastMouseX;
    protected double lastMouseY;
    protected float yaw = 0.0f;
    protected float pitch = 90.0f;
    protected float distance = -20.0f;
    protected int lastSceneWidth = -1;
    protected int lastSceneHeight = -1;
    protected int previewReloadDelay = -1;

    protected AbstractPortableStructureToolScreen(M menu, Inventory playerInventory, net.minecraft.network.chat.Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    protected final void initCommonWidgets(ScreenStyle style, List<net.minecraft.network.chat.Component> compatibleUpgrades) {
        this.widgets.add("upgrades", new UpgradesPanel(getMenu().getSlots(SlotSemantics.UPGRADE), () -> compatibleUpgrades));
        this.flipEastWestButton = new IconButton(Icon.ARROW_LEFT, button -> {
            if (this.transformAroundOriginMode) {
                getMenu().flipEastWestAroundOrigin();
            } else {
                getMenu().flipEastWest();
            }
        });

        this.flipNorthSouthButton = new IconButton(Icon.ARROW_RIGHT, button -> {
            if (this.transformAroundOriginMode) {
                getMenu().flipNorthSouthAroundOrigin();
            } else {
                getMenu().flipNorthSouth();
            }
        });

        this.flipVerticalButton = new IconButton(Icon.ARROW_UP, button -> {
            if (this.transformAroundOriginMode) {
                getMenu().flipVerticalAroundOrigin();
            } else {
                getMenu().flipVertical();
            }
        });

        this.rotateButton = new IconButton(Icon.SCHEDULING_DEFAULT, button -> {
            if (this.transformAroundOriginMode) {
                getMenu().rotateClockwiseAroundOrigin(1);
            } else {
                getMenu().rotateClockwise(1);
            }
        });

        refreshTransformAroundOriginMode();
        updateTransformButtonTooltips();

        this.widgets.add("flipEW", this.flipEastWestButton);
        this.widgets.add("flipH", this.flipNorthSouthButton);
        this.widgets.add("flipV", this.flipVerticalButton);
        this.widgets.add("rotate", this.rotateButton);

        IconButton offsetLeftButton = new IconButton(Icon.ARROW_LEFT, button -> getMenu().offsetLeft());
        offsetLeftButton.setTooltip(Tooltip.create(
                net.minecraft.network.chat.Component.translatable(LangDefs.OFFSET_LEFT_TOOLTIP.getTranslationKey())
        ));
        this.widgets.add("offsetLeft", offsetLeftButton);

        IconButton offsetRightButton = new IconButton(Icon.ARROW_RIGHT, button -> getMenu().offsetRight());
        offsetRightButton.setTooltip(Tooltip.create(
                net.minecraft.network.chat.Component.translatable(LangDefs.OFFSET_RIGHT_TOOLTIP.getTranslationKey())
        ));
        this.widgets.add("offsetRight", offsetRightButton);

        IconButton offsetUpButton = new IconButton(Icon.ARROW_UP, button -> getMenu().offsetUp());
        offsetUpButton.setTooltip(Tooltip.create(
                net.minecraft.network.chat.Component.translatable(LangDefs.OFFSET_UP_TOOLTIP.getTranslationKey())
        ));
        this.widgets.add("offsetUp", offsetUpButton);

        IconButton offsetDownButton = new IconButton(Icon.ARROW_DOWN, button -> getMenu().offsetDown());
        offsetDownButton.setTooltip(Tooltip.create(
                net.minecraft.network.chat.Component.translatable(LangDefs.OFFSET_DOWN_TOOLTIP.getTranslationKey())
        ));
        this.widgets.add("offsetDown", offsetDownButton);

        IconButton offsetFrontButton = new IconButton(Icon.SCHEDULING_DEFAULT, button -> getMenu().offsetFront());
        offsetFrontButton.setTooltip(Tooltip.create(
                net.minecraft.network.chat.Component.translatable(LangDefs.OFFSET_FRONT_TOOLTIP.getTranslationKey())
        ));
        this.widgets.add("offsetFront", offsetFrontButton);

        IconButton offsetBackButton = new IconButton(Icon.SCHEDULING_DEFAULT, button -> getMenu().offsetBack());
        offsetBackButton.setTooltip(Tooltip.create(
                net.minecraft.network.chat.Component.translatable(LangDefs.OFFSET_BACK_TOOLTIP.getTranslationKey())
        ));
        this.widgets.add("offsetBack", offsetBackButton);

        this.offsetDisplayX = new AETextField(style, Minecraft.getInstance().font, 0, 0, 36, 12);
        this.offsetDisplayX.setEditable(false);
        this.offsetDisplayX.setBordered(false);
        this.offsetDisplayX.setMaxLength(8);
        this.offsetDisplayX.setTooltipMessage(List.of(
                net.minecraft.network.chat.Component.translatable(LangDefs.OFFSET_X_TOOLTIP.getTranslationKey())
        ));
        this.offsetDisplayX.setValue("X:0");
        this.widgets.add("offsetDisplayX", this.offsetDisplayX);

        this.offsetDisplayY = new AETextField(style, Minecraft.getInstance().font, 0, 0, 36, 12);
        this.offsetDisplayY.setEditable(false);
        this.offsetDisplayY.setBordered(false);
        this.offsetDisplayY.setMaxLength(8);
        this.offsetDisplayY.setTooltipMessage(List.of(
                net.minecraft.network.chat.Component.translatable(LangDefs.OFFSET_Y_TOOLTIP.getTranslationKey())
        ));
        this.offsetDisplayY.setValue("Y:0");
        this.widgets.add("offsetDisplayY", this.offsetDisplayY);

        this.offsetDisplayZ = new AETextField(style, Minecraft.getInstance().font, 0, 0, 36, 12);
        this.offsetDisplayZ.setEditable(false);
        this.offsetDisplayZ.setBordered(false);
        this.offsetDisplayZ.setMaxLength(8);
        this.offsetDisplayZ.setTooltipMessage(List.of(
                net.minecraft.network.chat.Component.translatable(LangDefs.OFFSET_Z_TOOLTIP.getTranslationKey())
        ));
        this.offsetDisplayZ.setValue("Z:0");
        this.widgets.add("offsetDisplayZ", this.offsetDisplayZ);
    }

    protected final void finishInit() {
        this.root.setSize(this.width, this.height);
        this.directionCompassRoot.setSize(this.width, this.height);

        ensureDirectionCompassScene();

        reloadPreviewNow();
        getMenu().requestPreview();
    }

    protected record PreviewRect(int x, int y, int width, int height) {}

    protected abstract PreviewRect getPreviewRect();

    protected abstract ItemStack findRelevantStack();

    protected void onPreviewTagLoaded(CompoundTag syncedTag) {
    }

    protected void onClearExtraState() {
    }

    protected void renderExtraOverlays(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    protected boolean isShiftPhysicallyDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    protected void refreshTransformAroundOriginMode() {
        this.transformAroundOriginMode = isShiftPhysicallyDown();
    }

    protected void updateTransformButtonTooltips() {
        boolean aroundOrigin = this.transformAroundOriginMode;

        this.flipEastWestButton.setTooltip(Tooltip.create(
                net.minecraft.network.chat.Component.translatable(
                        aroundOrigin
                                ? LangDefs.FLIP_EAST_WEST_AROUND_ORIGIN.getTranslationKey()
                                : LangDefs.FLIP_EAST_WEST.getTranslationKey()
                )
        ));

        this.flipNorthSouthButton.setTooltip(Tooltip.create(
                net.minecraft.network.chat.Component.translatable(
                        aroundOrigin
                                ? LangDefs.FLIP_NORTH_SOUTH_AROUND_ORIGIN.getTranslationKey()
                                : LangDefs.FLIP_NORTH_SOUTH.getTranslationKey()
                )
        ));

        this.flipVerticalButton.setTooltip(Tooltip.create(
                net.minecraft.network.chat.Component.translatable(
                        aroundOrigin
                                ? LangDefs.FLIP_VERTICAL_AROUND_ORIGIN.getTranslationKey()
                                : LangDefs.FLIP_VERTICAL.getTranslationKey()
                )
        ));

        this.rotateButton.setTooltip(Tooltip.create(
                net.minecraft.network.chat.Component.translatable(
                        aroundOrigin
                                ? LangDefs.ROTATE_CLOCKWISE_AROUND_ORIGIN.getTranslationKey()
                                : LangDefs.ROTATE_CLOCKWISE.getTranslationKey()
                )
        ));
    }

    protected void syncOffsetDisplays() {
        BlockPos offset = readCurrentOffset();

        String xValue = "X:" + offset.getX();
        String yValue = "Y:" + offset.getY();
        String zValue = "Z:" + offset.getZ();

        if (!xValue.equals(this.offsetDisplayX.getValue())) {
            this.offsetDisplayX.setValue(xValue);
        }
        if (!yValue.equals(this.offsetDisplayY.getValue())) {
            this.offsetDisplayY.setValue(yValue);
        }
        if (!zValue.equals(this.offsetDisplayZ.getValue())) {
            this.offsetDisplayZ.setValue(zValue);
        }
    }

    protected BlockPos readCurrentOffset() {
        ItemStack stack = findRelevantStack();
        if (stack.isEmpty()) {
            return BlockPos.ZERO;
        }

        CompoundTag tag = stack.getTag();
        return TemplateUtil.getTemplateOffset(tag);
    }

    public void markPreviewDirty() {
        this.previewReloadDelay = 1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (scene != null) {
            PreviewRect rect = getPreviewRect();

            if (scene.getPositionX() != rect.x() || scene.getPositionY() != rect.y()
                    || scene.getSizeWidth() != rect.width() || scene.getSizeHeight() != rect.height()) {
                scene.setSelfPosition(rect.x(), rect.y());
                scene.setSize(rect.width(), rect.height());

                if (rect.width() != lastSceneWidth || rect.height() != lastSceneHeight) {
                    lastSceneWidth = rect.width();
                    lastSceneHeight = rect.height();

                    int sizeX = max.getX() - min.getX() + 1;
                    int sizeY = max.getY() - min.getY() + 1;
                    int sizeZ = max.getZ() - min.getZ() + 1;
                    int maxDim = Math.max(sizeX, Math.max(sizeY, sizeZ));

                    float scaleW = (float) rect.width() / 160.0f;
                    float scaleH = (float) rect.height() / 120.0f;
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

        updateDirectionCompassSceneLayoutAndCamera();

        root.drawInBackground(graphics, mouseX, mouseY, partialTick);
        root.drawInForeground(graphics, mouseX, mouseY, partialTick);
        root.drawOverlay(graphics, mouseX, mouseY, partialTick);

        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        graphics.flush();

        directionCompassRoot.drawInBackground(graphics, mouseX, mouseY, partialTick);
        directionCompassRoot.drawInForeground(graphics, mouseX, mouseY, partialTick);
        directionCompassRoot.drawOverlay(graphics, mouseX, mouseY, partialTick);

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        renderExtraOverlays(graphics, mouseX, mouseY, partialTick);
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
        if (rotating && scene != null) {
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
        directionCompassRoot.updateScreen();

        syncOffsetDisplays();

        refreshTransformAroundOriginMode();
        updateTransformButtonTooltips();

        updateDirectionCompassSceneLayoutAndCamera();

        if (previewReloadDelay >= 0) {
            if (previewReloadDelay == 0) {
                previewReloadDelay = -1;
                reloadPreviewNow();
            } else {
                previewReloadDelay--;
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        clearScene();
        clearDirectionCompassScene();
    }

    public void reloadPreviewNow() {
        ItemStack stack = findRelevantStack();
        if (stack.isEmpty()) {
            clearScene();
            return;
        }

        String structureId = StructureToolStackState.getStructureId(stack);

        PreviewStructure newStructure = null;
        boolean fromSharedCache = false;

        if (!structureId.isBlank()) {
            newStructure = PortableSpatialStoragePreviewSync.cacheGet(structureId);
            fromSharedCache = newStructure != null;
        }

        if (newStructure == null || newStructure.blocks().isEmpty()) {
            clearScene();
            return;
        }

        if (this.previewStructure != null && !this.previewStructureFromSharedCache) {
            this.previewStructure.close();
        }

        this.previewStructure = newStructure;
        this.previewStructureFromSharedCache = fromSharedCache;

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (PreviewBlock block : newStructure.blocks()) {
            BlockPos pos = block.pos();
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        this.min = new BlockPos(minX, minY, minZ);
        this.max = new BlockPos(maxX, maxY, maxZ);

        this.world.loadPreviewStructure(newStructure);

        if (this.scene == null) {
            this.scene = new PortableSpatialStorageSceneWidget(0, 0, 32, 32, world);
            this.root.clearAllWidgets();
            this.root.addWidget(scene);
        }

        BlockPos originMarkerPos = BlockPos.ZERO;
        BlockPos floorAnchorPos = BlockPos.ZERO;
        CompoundTag stackTag = stack.getTag();
        if (stackTag != null) {
            originMarkerPos = TemplateUtil.getEnergyOrigin(stackTag);
            floorAnchorPos = TemplateUtil.getEnergyOrigin(stackTag);
        }

        this.renderedCore = computeSurface(newStructure);
        this.scene.setPreview(
                newStructure,
                StructureToolStackState.getPreviewSideMap(stack),
                this.renderedCore,
                originMarkerPos,
                floorAnchorPos
        );

        CompoundTag syncedTag = PortableSpatialStoragePreviewSync.cacheGetRawTag(structureId);
        onPreviewTagLoaded(syncedTag);

        BlockPos size = max.subtract(min).offset(1, 1, 1);
        BlockPos center = new BlockPos(
                (int) (min.getX() + size.getX() * 0.5),
                (int) (min.getY() + size.getY() * 0.5),
                (int) (min.getZ() + size.getZ() * 0.5)
        );

        this.scene.setCenter(center.getCenter().toVector3f());

        if (!this.initialCameraAlignedToPlayer) {
            alignInitialCameraToPlayer();
        }

        this.scene.setCameraYawAndPitch(this.yaw, this.pitch);
        this.scene.setZoom(this.distance);

        this.lastSceneWidth = -1;
        this.lastSceneHeight = -1;
    }

    protected void clearScene() {
        this.renderedCore = Collections.emptySet();
        this.world.loadPreviewStructure(null);

        if (this.previewStructure != null && !this.previewStructureFromSharedCache) {
            this.previewStructure.close();
        }
        this.previewStructure = null;
        this.previewStructureFromSharedCache = false;

        if (this.scene != null) {
            this.root.clearAllWidgets();
            this.scene = null;
        }

        this.initialCameraAlignedToPlayer = false;
        this.min = BlockPos.ZERO;
        this.max = BlockPos.ZERO;
        this.lastSceneWidth = -1;
        this.lastSceneHeight = -1;

        onClearExtraState();
    }

    protected boolean insideScene(double mouseX, double mouseY) {
        return scene != null
                && mouseX >= scene.getPositionX()
                && mouseY >= scene.getPositionY()
                && mouseX < scene.getPositionX() + scene.getSizeWidth()
                && mouseY < scene.getPositionY() + scene.getSizeHeight();
    }

    protected static Set<BlockPos> computeSurface(PreviewStructure structure) {
        HashSet<BlockPos> out = new HashSet<>();
        if (structure == null || structure.blocks().isEmpty()) {
            return out;
        }

        Set<BlockPos> all = new HashSet<>();
        for (PreviewBlock block : structure.blocks()) {
            all.add(block.pos());
        }

        for (PreviewBlock block : structure.blocks()) {
            BlockPos pos = block.pos();
            for (Direction direction : DIRECTIONS) {
                if (!all.contains(pos.relative(direction))) {
                    out.add(pos);
                    break;
                }
            }
        }

        return out;
    }

    protected void alignInitialCameraToPlayer() {
        if (this.initialCameraAlignedToPlayer) {
            return;
        }

        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        this.yaw = 25.0f;
        this.pitch = Mth.wrapDegrees(player.getYRot() - 90.0f);
        this.initialCameraAlignedToPlayer = true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean result = super.keyPressed(keyCode, scanCode, modifiers);
        refreshTransformAroundOriginMode();
        updateTransformButtonTooltips();
        return result;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        boolean result = super.keyReleased(keyCode, scanCode, modifiers);
        refreshTransformAroundOriginMode();
        updateTransformButtonTooltips();
        return result;
    }

    protected PreviewRect getDirectionCompassRect() {
        PreviewRect preview = getPreviewRect();

        return new PreviewRect(
                preview.x() + preview.width() - DIRECTION_COMPASS_SIZE - 4,
                preview.y() + 4,
                DIRECTION_COMPASS_SIZE,
                DIRECTION_COMPASS_SIZE
        );
    }

    protected void ensureDirectionCompassScene() {
        if (this.directionCompassScene != null) {
            return;
        }

        this.directionCompassWorld = new TrackedDummyWorld();
        this.directionCompassCore = buildDirectionCompassStructure(this.directionCompassWorld);

        this.directionCompassScene = new SceneWidget(
                0,
                0,
                DIRECTION_COMPASS_SIZE,
                DIRECTION_COMPASS_SIZE,
                this.directionCompassWorld
        );

        this.directionCompassScene
                .useOrtho(true)
                .setRenderFacing(false)
                .setRenderSelect(false)
                .setDraggable(false)
                .setScalable(false)
                .setIntractable(false)
                .setHoverTips(false)
                .setClearColor(0x00000000);

        this.directionCompassScene.setRenderedCore(this.directionCompassCore);
        this.directionCompassScene.setOrthoRange(DIRECTION_COMPASS_ORTHO_RANGE);
        this.directionCompassScene.setZoom(1.0F);
        this.directionCompassScene.setCenter(new org.joml.Vector3f(0.5F, 0.75F, 0.5F));
        this.directionCompassScene.setCameraYawAndPitch(this.yaw, this.pitch);

        this.directionCompassRoot.clearAllWidgets();
        this.directionCompassRoot.addWidget(this.directionCompassScene);
    }

    protected void updateDirectionCompassSceneLayoutAndCamera() {
        if (this.directionCompassScene == null) {
            ensureDirectionCompassScene();
        }

        if (this.directionCompassScene == null) {
            return;
        }

        PreviewRect rect = getDirectionCompassRect();

        if (this.directionCompassScene.getPositionX() != rect.x()
                || this.directionCompassScene.getPositionY() != rect.y()
                || this.directionCompassScene.getSizeWidth() != rect.width()
                || this.directionCompassScene.getSizeHeight() != rect.height()) {
            this.directionCompassScene.setSelfPosition(rect.x(), rect.y());
            this.directionCompassScene.setSize(rect.width(), rect.height());
        }

        this.directionCompassScene.setOrthoRange(DIRECTION_COMPASS_ORTHO_RANGE);
        this.directionCompassScene.setZoom(1.0F);
        this.directionCompassScene.setCameraYawAndPitch(this.yaw, this.pitch);
    }

    protected void clearDirectionCompassScene() {
        this.directionCompassRoot.clearAllWidgets();
        this.directionCompassScene = null;
        this.directionCompassWorld = null;
        this.directionCompassCore = Collections.emptySet();
    }

    private static Set<BlockPos> buildDirectionCompassStructure(TrackedDummyWorld world) {
        Set<BlockPos> core = new HashSet<>();

        BlockState center = Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
        BlockState arrow = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState north = Blocks.RED_CONCRETE.defaultBlockState();
        BlockState south = Blocks.BLUE_CONCRETE.defaultBlockState();
        BlockState east = Blocks.YELLOW_CONCRETE.defaultBlockState();
        BlockState west = Blocks.GREEN_CONCRETE.defaultBlockState();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                placeCompassPillar(world, core, x, z, center);
            }
        }

        // North arrow
        for (int z = -5; z <= -3; z++) {
            placeCompassPillar(world, core, 0, z, arrow);
        }

        for (int x = -2; x <= 2; x++) {
            placeCompassPillar(world, core, x, -6, north);
        }
        for (int x = -1; x <= 1; x++) {
            placeCompassPillar(world, core, x, -7, north);
        }
        placeCompassPillar(world, core, 0, -8, north);

        // South arrow
        for (int z = 3; z <= 5; z++) {
            placeCompassPillar(world, core, 0, z, arrow);
        }

        for (int x = -2; x <= 2; x++) {
            placeCompassPillar(world, core, x, 6, south);
        }
        for (int x = -1; x <= 1; x++) {
            placeCompassPillar(world, core, x, 7, south);
        }
        placeCompassPillar(world, core, 0, 8, south);

        // East arrow
        for (int x = 3; x <= 5; x++) {
            placeCompassPillar(world, core, x, 0, arrow);
        }

        for (int z = -2; z <= 2; z++) {
            placeCompassPillar(world, core, 6, z, east);
        }
        for (int z = -1; z <= 1; z++) {
            placeCompassPillar(world, core, 7, z, east);
        }
        placeCompassPillar(world, core, 8, 0, east);

        // West arrow
        for (int x = -5; x <= -3; x++) {
            placeCompassPillar(world, core, x, 0, arrow);
        }

        for (int z = -2; z <= 2; z++) {
            placeCompassPillar(world, core, -6, z, west);
        }
        for (int z = -1; z <= 1; z++) {
            placeCompassPillar(world, core, -7, z, west);
        }
        placeCompassPillar(world, core, -8, 0, west);

        placeCompassGlyph(world, core, -2, -14, north, new String[]{
                "#...#",
                "##..#",
                "#.#.#",
                "#..##",
                "#...#"
        });

        placeCompassGlyph(world, core, -2, 10, south, new String[]{
                "#####",
                "#....",
                "#####",
                "....#",
                "#####"
        });

        placeCompassGlyph(world, core, 10, -2, east, new String[]{
                "#####",
                "#....",
                "####.",
                "#....",
                "#####"
        });

        placeCompassGlyph(world, core, -14, -2, west, new String[]{
                "#...#",
                "#...#",
                "#.#.#",
                "##.##",
                "#...#"
        });

        return core;
    }

    private static void placeCompassGlyph(
            TrackedDummyWorld world,
            Set<BlockPos> core,
            int startX,
            int startZ,
            BlockState state,
            String[] rows
    ) {
        for (int dz = 0; dz < rows.length; dz++) {
            String row = rows[dz];

            for (int dx = 0; dx < row.length(); dx++) {
                if (row.charAt(dx) == '#') {
                    placeCompassPillar(world, core, startX + dx, startZ + dz, state);
                }
            }
        }
    }

    private static void placeCompassPillar(
            TrackedDummyWorld world,
            Set<BlockPos> core,
            int x,
            int z,
            BlockState state
    ) {
        placeCompassBlock(world, core, new BlockPos(x, 0, z), state);
        placeCompassBlock(world, core, new BlockPos(x, 1, z), state);
    }

    private static void placeCompassBlock(
            TrackedDummyWorld world,
            Set<BlockPos> core,
            BlockPos pos,
            BlockState state
    ) {
        world.setBlock(pos, state, 3, 0);
        core.add(pos);
    }
}