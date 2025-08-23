package net.oktawia.crazyae2addons.xei.common;

import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;

import javax.annotation.Nullable;

public class FabricationPreview extends WidgetGroup {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 80;

    public FabricationPreview(@Nullable ResourceLocation recipeId,
                              ItemStack input,
                              ItemStack output,
                              @Nullable ResourceLocation requiredKey,
                              @Nullable String requiredLabel) {
        super(0, 0, WIDTH, HEIGHT);
        setClientSideWidget();

        int x = 10;
        int y = 18;

        addWidget(new LabelWidget(x, 6, "Input"));
        addWidget(new SlotWidget(new ItemStackTransfer(input), 0, x, y, false, false)
                .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.INPUT));

        int arrowX = x + 30;
        addWidget(new ButtonWidget(arrowX, y + 3, 18, 14, new TextTexture("->"), b -> {})
                .appendHoverTooltips("Takes 10 ticks"));

        int outX = arrowX + 26;
        addWidget(new LabelWidget(outX, 6, "Output"));
        addWidget(new SlotWidget(new ItemStackTransfer(output), 0, outX, y, false, false)
                .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.OUTPUT));

        String labelText = (requiredLabel != null && !requiredLabel.isBlank())
                ? requiredLabel
                : requiredKey.toString();

        if (requiredKey != null) {
            int driveX = outX + 48;
            addWidget(new LabelWidget(driveX, 6, "Drive"));
            ItemStack drive = new ItemStack(CrazyItemRegistrar.DATA_DRIVE.get());

            addWidget(new SlotWidget(new ItemStackTransfer(drive), 0, driveX, y, false, false)
                    .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                    .setIngredientIO(IngredientIO.INPUT)
                    .appendHoverTooltips(
                            "Requires research:",
                            labelText,
                            "unlocked on the Data Drive"
                    ));
        }

        String title = recipeId == null ? "Fabrication" : "Research: " + labelText;
        addWidget(new LabelWidget(10, HEIGHT - 12, Component.literal(title)));
    }
}
