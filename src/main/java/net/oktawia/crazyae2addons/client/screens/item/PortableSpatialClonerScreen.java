package net.oktawia.crazyae2addons.client.screens.item;

import appeng.api.upgrades.Upgrades;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.client.misc.ClonerMaterialListWidget;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.client.misc.PortableSpatialClonerRequirementSync;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.items.PortableSpatialCloner;
import net.oktawia.crazyae2addons.menus.item.PortableSpatialClonerMenu;

import java.util.ArrayList;
import java.util.List;

public class PortableSpatialClonerScreen<C extends PortableSpatialClonerMenu>
        extends AbstractPortableStructureToolScreen<C> {

    private static final int PREVIEW_LEFT = 104;
    private static final int PREVIEW_TOP = 26;
    private static final int PREVIEW_WIDTH = 144;
    private static final int PREVIEW_HEIGHT = 135;

    private final ClonerMaterialListWidget materialList;

    public PortableSpatialClonerScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        initCommonWidgets(style, getCompatibleUpgrades());

        IconButton forgetButton = new IconButton(Icon.CLEAR, button -> getMenu().forgetStructure());
        forgetButton.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.FORGET_STRUCTURE.getTranslationKey())
        ));
        this.widgets.add("forget", forgetButton);

        this.materialList = new ClonerMaterialListWidget(0, 0, 72, 135);
        this.materialList.setCraftRequestHandler(entry -> {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.stack().getItem());
            if (itemId == null) {
                return;
            }

            long missing = entry.missing();
            if (missing <= 0) {
                return;
            }

            getMenu().craftRequest(itemId + "|" + missing);
        });

        this.widgets.add("materials", this.materialList);

        finishInit();
    }

    @Override
    protected PreviewRect getPreviewRect() {
        return new PreviewRect(
                this.leftPos + PREVIEW_LEFT,
                this.topPos + PREVIEW_TOP,
                PREVIEW_WIDTH,
                PREVIEW_HEIGHT
        );
    }

    @Override
    protected ItemStack findRelevantStack() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = PortableSpatialCloner.findActive(player);
        if (stack.isEmpty()) {
            stack = PortableSpatialCloner.findHeld(player);
        }

        return stack;
    }

    @Override
    protected void onClearExtraState() {
        this.materialList.setEntries(List.of());
    }

    @Override
    public void removed() {
        super.removed();
        PortableSpatialClonerRequirementSync.clear(getMenu().containerId);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        syncRequirementEntries();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.materialList.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderExtraOverlays(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderMaterialTooltip(graphics, mouseX, mouseY);
    }

    private void syncRequirementEntries() {
        boolean hasCraftingCard = getMenu().getStructureHost().getUpgrades().isInstalled(AEItems.CRAFTING_CARD);

        this.materialList.setCraftButtonsEnabled(hasCraftingCard);
        this.materialList.setEntries(
                PortableSpatialClonerRequirementSync.getEntries(getMenu().containerId)
        );
    }

    private void renderMaterialTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.materialList.isHoveringCraftButton(mouseX, mouseY)) {
            return;
        }

        ClonerMaterialListWidget.MaterialEntry hovered = this.materialList.getHoveredEntry(mouseX, mouseY);
        if (hovered == null) {
            return;
        }

        List<Component> lines = new ArrayList<>(
                hovered.stack().getTooltipLines(Minecraft.getInstance().player, TooltipFlag.Default.NORMAL)
        );

        lines.add(Component.translatable(
                LangDefs.AVAILABLE_COUNT.getTranslationKey(),
                String.format("%,d", hovered.available())
        ));

        lines.add(Component.translatable(
                LangDefs.REQUIRED_COUNT.getTranslationKey(),
                String.format("%,d", hovered.required())
        ));

        lines.add(Component.translatable(
                hovered.complete()
                        ? LangDefs.STATUS_OK.getTranslationKey()
                        : LangDefs.STATUS_MISSING.getTranslationKey()
        ));

        graphics.renderTooltip(
                Minecraft.getInstance().font,
                lines,
                hovered.stack().getTooltipImage(),
                mouseX,
                mouseY
        );
    }

    private List<Component> getCompatibleUpgrades() {
        var list = new ArrayList<Component>();
        list.add(GuiText.CompatibleUpgrades.text());
        list.addAll(Upgrades.getTooltipLinesForMachine(CrazyItemRegistrar.PORTABLE_SPATIAL_CLONER.get()));
        return list;
    }
}