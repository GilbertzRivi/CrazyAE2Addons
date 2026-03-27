package net.oktawia.crazyae2addons.screens;

import appeng.api.stacks.AEItemKey;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.definitions.AEItems;
import appeng.menu.SlotSemantics;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.menus.PortableAutobuilderMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.misc.ItemTextButtonBarWidget;
import net.oktawia.crazyae2addons.misc.TemplateUtil;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class PortableAutobuilderScreen<C extends PortableAutobuilderMenu> extends AEBaseScreen<C> {
    private IconButton flipHBtn, flipVBtn, rotateBtn, clearBtn;

    private final StringBuilder programBuf = new StringBuilder();
    private boolean receiving = false;

    private final WidgetGroup root = new WidgetGroup(0, 0, 0, 0);
    private TrackedDummyWorld world = new TrackedDummyWorld();
    private SceneWidget scene;

    private Map<BlockPos, BlockState> blocks = Collections.emptyMap();
    private BlockPos min = BlockPos.ZERO, max = BlockPos.ZERO;
    private boolean rotating = false;
    private double lastMouseX, lastMouseY;
    private float yaw = 0f, pitch = 30f, distance = -20f;
    private int lastSceneW = -1, lastSceneH = -1;
    private final List<ItemTextButtonBarWidget> requirementWidgets = new ArrayList<>();
    private int tick = 0;

    public record RequirementQuad(AEItemKey key, long have, long need, boolean craftable) {}

    private static final int MAX_VISIBLE_REQUIREMENTS = 8;
    private int reqScrollIndex = 0;
    private String lastRequirements = "";

    public static List<RequirementQuad> parseRequirements(String requirements) {
        List<RequirementQuad> result = new ArrayList<>();
        if (requirements == null || requirements.isEmpty()) return result;

        String[] lines = requirements.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(";");
            if (parts.length < 3) continue;

            ResourceLocation rl = ResourceLocation.tryParse(parts[0]);
            if (rl == null) continue;

            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null || item == Items.AIR) continue;

            long need;
            long have;
            try {
                need = Long.parseLong(parts[1]);
                have = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                continue;
            }

            AEItemKey key = AEItemKey.of(item);
            result.add(new RequirementQuad(key, have, need, Objects.equals(parts[3], "1")));
        }
        return result;
    }

    public PortableAutobuilderScreen(C menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);
        setupGui();

        this.widgets.add("flipH", flipHBtn);
        this.widgets.add("flipV", flipVBtn);
        this.widgets.add("rotate", rotateBtn);
        this.widgets.add("clear", clearBtn);
        this.widgets.add("upgrades", new UpgradesPanel(getMenu().getSlots(SlotSemantics.UPGRADE)));
        root.setSize(this.width, this.height);

        getMenu().requestData();
        getMenu().rotateCW(1);
        getMenu().rotateCW(-1);
        getMenu().requestData();
    }

    private void initScene() {
        int pad = 16;
        int viewW = Math.max(16, this.width - pad * 2);
        int viewH = Math.max(16, this.height - pad * 2);

        this.world = new TrackedDummyWorld();
        for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
            this.world.setBlock(e.getKey(), e.getValue(), 3);
        }

        if (this.scene == null) {
            this.scene = new SceneWidget(pad, pad, viewW, viewH, world);
            root.clearAllWidgets();
            root.addWidget(scene);
        } else {
            this.scene.setSize(viewW, viewH);
            this.scene.setSelfPosition(pad, pad);
        }

        scene.clearAllWidgets();
        scene.setRenderedCore(blocks.keySet());

        BlockPos size = max.subtract(min).offset(1, 1, 1);
        BlockPos center = new BlockPos(
                (int) (min.getX() + size.getX() * 0.5),
                (int) (min.getY() + size.getY() * 0.5),
                (int) (min.getZ() + size.getZ() * 0.5)
        );
        scene.setCenter(center.getCenter().toVector3f());
        scene.setCameraYawAndPitch(yaw, pitch);
        scene.setZoom(distance);
    }

    private void setupGui() {
        flipHBtn = new IconButton(Icon.ARROW_RIGHT, (btn) -> {
            getMenu().flipH();
            getMenu().requestData();
        });
        flipHBtn.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.gadget_flip_h")));

        flipVBtn = new IconButton(Icon.ARROW_UP, (btn) -> {
            getMenu().flipV();
            getMenu().requestData();
        });
        flipVBtn.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.gadget_flip_v")));

        rotateBtn = new IconButton(Icon.SCHEDULING_DEFAULT, (btn) -> {
            getMenu().rotateCW(1);
            getMenu().requestData();
        });
        rotateBtn.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.gadget_rotate")));

        clearBtn = new IconButton(Icon.CLEAR, (btn) -> getMenu().clearStructure());
        clearBtn.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.gadget_clear")));
    }

    /**
     * Receives program chunks from the server.
     * Payload is Base64-encoded compressed-NBT (not raw program text).
     */
    public void setProgram(String data) {
        if ("__RESET__".equals(data)) {
            programBuf.setLength(0);
            receiving = true;
            return;
        }
        if ("__END__".equals(data)) {
            receiving = false;
            reloadPreviewNow();
            return;
        }
        if (receiving) {
            programBuf.append(data);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        boolean hasCraftCard = getMenu().host.getUpgrades().isInstalled(AEItems.CRAFTING_CARD);

        if (hasCraftCard) {
            String currentReq = menu.requirements == null ? "" : menu.requirements;
            if (!Objects.equals(currentReq, lastRequirements)) {
                lastRequirements = currentReq;
                rebuildRequirementWidgets();
            } else {
                layoutRequirementWidgets();
            }
        } else {
            clearRequirementWidgets();
            lastRequirements = "";
            reqScrollIndex = 0;
        }

        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTicks);

        if (scene != null) {
            int x = this.leftPos + 8;
            int y = this.topPos + 24;
            int w = this.imageWidth - 16;
            int h = this.imageHeight - 24 - 104;

            w = Math.max(32, w);
            h = Math.max(32, h);

            if (scene.getPositionX() != x || scene.getPositionY() != y
                    || scene.getSizeWidth() != w || scene.getSizeHeight() != h) {
                scene.setSelfPosition(x, y);
                scene.setSize(w, h);

                if (w != lastSceneW || h != lastSceneH) {
                    lastSceneW = w;
                    lastSceneH = h;
                    int sx = max.getX() - min.getX() + 1;
                    int sy = max.getY() - min.getY() + 1;
                    int sz = max.getZ() - min.getZ() + 1;
                    int maxDim = Math.max(sx, Math.max(sy, sz));

                    float scaleW = (float) w / 160f;
                    float scaleH = (float) h / 120f;
                    float scale = Math.max(0.75f, Math.min(scaleW, scaleH));
                    distance = Math.max(6f, (maxDim * 2.2f) / scale);
                    scene.setZoom(distance);
                }
            }

            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            g.flush();
            root.drawInBackground(g, mouseX, mouseY, partialTicks);
            root.drawInForeground(g, mouseX, mouseY, partialTicks);
            root.drawOverlay(g, mouseX, mouseY, partialTicks);
        }
    }

    private void clearRequirementWidgets() {
        if (this.requirementWidgets.isEmpty()) return;
        for (var w : this.requirementWidgets) {
            this.renderables.remove(w);
            w.setBarEnabled(false);
        }
        this.requirementWidgets.clear();
    }

    private void rebuildRequirementWidgets() {
        clearRequirementWidgets();

        List<RequirementQuad> reqs = parseRequirements(menu.requirements);
        for (var record : reqs) {
            long wehave = Math.min(record.have, record.need);
            boolean green = record.have >= record.need;

            IconButton button = new IconButton(
                    record.craftable ? Icon.CRAFT_HAMMER : Icon.BACKGROUND_DUST,
                    record.craftable ? (b) -> {
                        getMenu().craftRequest(
                                String.format("%s|%s", record.key.getId(), record.need - record.have));
                    } : (c) -> {});

            var widget = new ItemTextButtonBarWidget(
                    0, 0, 160, 24,
                    record.key.toStack(),
                    Component.literal(String.format("%s/%s", wehave, record.need)),
                    button
            );

            if (green) widget.setCenterTextColor(0, 255, 0);
            else        widget.setCenterTextColor(255, 0, 0);

            this.addRenderableWidget(widget);
            this.requirementWidgets.add(widget);
        }

        int maxScroll = Math.max(0, requirementWidgets.size() - MAX_VISIBLE_REQUIREMENTS);
        if (reqScrollIndex > maxScroll) reqScrollIndex = maxScroll;
        layoutRequirementWidgets();
    }

    private void layoutRequirementWidgets() {
        if (requirementWidgets.isEmpty()) return;

        int x = getGuiLeft() - 160 - 24;
        int baseY = getGuiTop() + 2;
        int visibleCount = Math.min(MAX_VISIBLE_REQUIREMENTS, requirementWidgets.size());

        for (int i = 0; i < requirementWidgets.size(); i++) {
            ItemTextButtonBarWidget w = requirementWidgets.get(i);
            int relativeIndex = i - reqScrollIndex;
            if (relativeIndex >= 0 && relativeIndex < visibleCount) {
                w.setBarPosition(x, baseY + relativeIndex * 24);
                w.setBarEnabled(true);
            } else {
                w.setBarEnabled(false);
            }
        }
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
            yaw = Math.max(-89f, Math.min(89f, yaw + (float) (dy * 0.5f)));
            scene.setCameraYawAndPitch(yaw, pitch);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) rotating = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        root.updateScreen();
        if (tick % 20 == 0) {
            getMenu().updateRequirements();
            tick = 0;
        }
        tick += 1;
    }

    private void reloadPreviewNow() {
        String base64 = programBuf.toString();
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

            // Convert local positions to display-world positions so block states appear correctly oriented.
            net.minecraft.core.Direction srcFacing = net.oktawia.crazyae2addons.items.PortableSpatialStorage.readSrcFacingFromNbt(
                    net.minecraft.client.Minecraft.getInstance().player.getMainHandItem());
            net.oktawia.crazyae2addons.items.PortableSpatialStorage.Basis basis =
                    net.oktawia.crazyae2addons.items.PortableSpatialStorage.Basis.forFacing(srcFacing);

            HashMap<BlockPos, BlockState> map = new HashMap<>();
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

            for (TemplateUtil.BlockInfo info : blockInfos) {
                BlockPos p = net.oktawia.crazyae2addons.items.PortableSpatialStorage.localToWorld(
                        info.pos(), BlockPos.ZERO, basis);
                map.put(p, info.state());
                if (p.getX() < minX) minX = p.getX();
                if (p.getX() > maxX) maxX = p.getX();
                if (p.getY() < minY) minY = p.getY();
                if (p.getY() > maxY) maxY = p.getY();
                if (p.getZ() < minZ) minZ = p.getZ();
                if (p.getZ() > maxZ) maxZ = p.getZ();
            }

            this.blocks = map;
            this.min = new BlockPos(minX, minY, minZ);
            this.max = new BlockPos(maxX, maxY, maxZ);

            this.world.clear();
            for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
                this.world.setBlock(e.getKey(), e.getValue(), 3);
            }

            if (this.scene == null) {
                this.scene = new SceneWidget(0, 0, 32, 32, world);
                root.clearAllWidgets();
                root.addWidget(scene);
            }
            scene.clearAllWidgets();
            scene.setRenderedCore(blocks.keySet());

            BlockPos size = max.subtract(min).offset(1, 1, 1);
            BlockPos center = new BlockPos(
                    (int) (min.getX() + size.getX() * 0.5),
                    (int) (min.getY() + size.getY() * 0.5),
                    (int) (min.getZ() + size.getZ() * 0.5)
            );

            scene.setCenter(center.getCenter().toVector3f());
            scene.setCameraYawAndPitch(yaw, pitch);
            scene.setZoom(distance);

        } catch (Exception e) {
            clearScene();
        }
    }

    private void clearScene() {
        this.blocks = Collections.emptyMap();
        this.world.clear();
        if (this.scene != null) {
            root.clearAllWidgets();
            this.scene = null;
        }
    }

    private boolean insideScene(double mx, double my) {
        return scene != null &&
                mx >= scene.getPositionX() && my >= scene.getPositionY() &&
                mx < scene.getPositionX() + scene.getSizeWidth() &&
                my < scene.getPositionY() + scene.getSizeHeight();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (scene != null && insideScene(mouseX, mouseY)) {
            float step = 1.0f;
            float minDist = 2.0f;
            float maxDist = 256.0f;
            distance = Math.max(minDist, Math.min(maxDist, distance - (float) delta * step));
            scene.setZoom(distance);
            return true;
        }

        if (!requirementWidgets.isEmpty() && insideRequirementArea(mouseX, mouseY)) {
            if (delta != 0) {
                int maxScroll = Math.max(0, requirementWidgets.size() - MAX_VISIBLE_REQUIREMENTS);
                reqScrollIndex -= (int) Math.signum(delta);
                if (reqScrollIndex < 0) reqScrollIndex = 0;
                if (reqScrollIndex > maxScroll) reqScrollIndex = maxScroll;
                layoutRequirementWidgets();
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean insideRequirementArea(double mx, double my) {
        if (requirementWidgets.isEmpty()) return false;
        int x = getGuiLeft() - 160 - 24;
        int width = 160 + 24;
        int baseY = getGuiTop() + 2;
        int height = MAX_VISIBLE_REQUIREMENTS * 24;
        return mx >= x && mx <= x + width && my >= baseY && my <= baseY + height;
    }
}
