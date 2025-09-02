package net.oktawia.crazyae2addons.renderer;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.GenericStack;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.oktawia.crazyae2addons.mobstorage.MobKeyIng;

import java.util.List;

public class MobKeyIngDelegatingRenderer implements IIngredientRenderer<MobKeyIng> {

    @Override
    public void render(GuiGraphics guiGraphics, MobKeyIng ingredient) {
        AEKeyRendering.drawInGui(Minecraft.getInstance(), guiGraphics, 0, 0, ingredient.key());
    }

    @Override
    public List<Component> getTooltip(MobKeyIng ingredient, TooltipFlag tooltipFlag) {
        return List.of(ingredient.key().getDisplayName());
    }

    @Override
    public Font getFontRenderer(Minecraft minecraft, MobKeyIng ingredient) {
        return minecraft.font;
    }
}
