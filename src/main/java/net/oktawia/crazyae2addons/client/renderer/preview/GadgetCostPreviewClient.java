package net.oktawia.crazyae2addons.client.renderer.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.cutpaste.CutPasteStackState;
import net.oktawia.crazyae2addons.util.TemplateUtil;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GadgetCostPreviewClient {

    private static final int COLOR_OK = 0xFFFFFF;
    private static final int COLOR_CYAN = 0x55FFFF;
    private static final int COLOR_RED = 0xFF4040;

    private static Component currentText = null;
    private static int currentColor = COLOR_OK;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        currentText = null;
        currentColor = COLOR_CYAN;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        ItemStack held = PortableSpatialStorage.findHeld(mc.player);
        if (held.isEmpty()) {
            return;
        }

        int energy = (int) Math.floor(((PortableSpatialStorage) held.getItem()).getAECurrentPower(held));

        if (CutPasteStackState.hasStructure(held)) {
            BlockHitResult hit = PortableSpatialStorage.rayTrace(mc.level, mc.player, 50.0D);
            if (hit.getType() != HitResult.Type.BLOCK) {
                return;
            }

            int cost = computePastePreviewCostAE(held);
            if (cost <= 0) {
                return;
            }

            currentText = Component.translatable(
                    LangDefs.PASTE_COST_PREVIEW.getTranslationKey(),
                    String.format("%,d", cost)
            );
            currentColor = cost > energy ? COLOR_RED : COLOR_CYAN;
            return;
        }

        BlockHitResult hit = PortableSpatialStorage.rayTrace(mc.level, mc.player, 50.0D);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos selectionA = CutPasteStackState.getSelectionA(held);
        if (selectionA == null) {
            return;
        }

        BlockPos selectionB = CutPasteStackState.getSelectionB(held);
        BlockPos previewB = selectionB == null ? hit.getBlockPos().immutable() : selectionB;

        int cost = PortableSpatialStorage.computeCutPreviewCostAE(mc.level, selectionA, previewB, selectionA);

        currentText = Component.translatable(
                LangDefs.CUT_COST_PREVIEW.getTranslationKey(),
                String.format("%,d", cost)
        );
        currentColor = cost > energy ? COLOR_RED : COLOR_CYAN;
    }

    private static int computePastePreviewCostAE(ItemStack stack) {
        String structureId = CutPasteStackState.getStructureId(stack);
        if (structureId == null || structureId.isBlank()) {
            return 0;
        }

        PreviewStructure structure = PortableSpatialStoragePreviewSync.cacheGet(structureId);
        if (structure == null || structure.blocks().isEmpty()) {
            return 0;
        }

        CompoundTag tag = stack.getTag();
        BlockPos energyOrigin = TemplateUtil.getEnergyOrigin(tag);

        double total = 0.0D;

        for (PreviewBlock previewBlock : structure.blocks()) {
            BlockPos pos = previewBlock.pos();

            double dx = pos.getX() - energyOrigin.getX();
            double dy = pos.getY() - energyOrigin.getY();
            double dz = pos.getZ() - energyOrigin.getZ();

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            total += distance;
        }

        return (int) Math.ceil(Math.max(1.0D, total));
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (currentText == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = screenWidth / 2 + 8;
        int y = screenHeight / 2 - 4;

        gui.drawString(mc.font, currentText, x, y, currentColor, true);
    }
}