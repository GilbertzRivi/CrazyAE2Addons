package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.oktawia.crazyae2addons.menus.RecipeFabricatorMenu;

import java.util.List;

public class RecipeFabricatorScreen<C extends RecipeFabricatorMenu> extends UpgradeableScreen<C> {

    public RecipeFabricatorScreen(C menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        int p = getMenu().progress != null ? getMenu().progress : 0;
        int d = getMenu().duration != null ? getMenu().duration : 10;
        int pct = d > 0 ? (int) Math.round(100.0 * p / d) : 0;

        setTextContent("progress", Component.literal("Progress: " + pct + "%"));
    }

    @Override
    public void drawBG(GuiGraphics gg, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(gg, offsetX, offsetY, mouseX, mouseY, partialTicks);

        // render z wartości zsynchronizowanych w MENU
        renderFluidInSlot(gg, menu.getSyncedFluidIn(), menu.getFluidInSlot());
        renderFluidInSlot(gg, menu.getSyncedFluidOut(), menu.getFluidOutSlot());
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);

        if (isHovering(menu.getFluidInSlot().x, menu.getFluidInSlot().y, 16, 16, mouseX, mouseY)) {
            renderFluidTooltip(gg, menu.getSyncedFluidIn(), mouseX, mouseY, Component.literal("Fluid In"));
        }

        if (isHovering(menu.getFluidOutSlot().x, menu.getFluidOutSlot().y, 16, 16, mouseX, mouseY)) {
            renderFluidTooltip(gg, menu.getSyncedFluidOut(), mouseX, mouseY, Component.literal("Fluid Out"));
        }
    }

    private void renderFluidTooltip(GuiGraphics gg, FluidStack fs, int mouseX, int mouseY, Component title) {
        if (fs == null || fs.isEmpty()) {
            gg.renderComponentTooltip(this.font, List.of(title, Component.literal("Empty")), mouseX, mouseY);
            return;
        }
        gg.renderComponentTooltip(this.font, List.of(
                title,
                fs.getDisplayName(),
                Component.literal(fs.getAmount() + " mB")
        ), mouseX, mouseY);
    }

    private void renderFluidInSlot(GuiGraphics gg, FluidStack fs, net.minecraft.world.inventory.Slot slot) {
        if (fs == null || fs.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        var ext = IClientFluidTypeExtensions.of(fs.getFluid());

        ResourceLocation still = ext.getStillTexture();
        if (still == null) return;

        int tint = ext.getTintColor();

        TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);

        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;

        float a = ((tint >> 24) & 0xFF) / 255f;
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(r, g, b, a == 0 ? 1f : a);

        gg.blit(x, y, 0, 16, 16, sprite);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
