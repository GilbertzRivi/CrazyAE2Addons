package net.oktawia.crazyae2addons.xei.common;

import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.PhantomTankWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public class FabricationPreview extends WidgetGroup {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 100;

    public FabricationPreview(@Nullable ResourceLocation recipeId,
                              List<ItemStack> inputs,
                              ItemStack output,
                              FluidStack fluidInput,
                              FluidStack fluidOutput,
                              @Nullable ResourceLocation requiredKey,
                              @Nullable String requiredLabel) {
        super(0, 0, WIDTH, HEIGHT);
        setClientSideWidget();

        if (inputs == null) inputs = List.of();
        if (output == null) output = ItemStack.EMPTY;
        if (fluidInput == null) fluidInput = FluidStack.empty();
        if (fluidOutput == null) fluidOutput = FluidStack.empty();

        final int x = 10;
        final int yItems = 18;

        addWidget(new LabelWidget(x, 6, "Input"));

        int perRow = 3;
        int spacing = 18;

        int rowCount = 0;
        for (int i = 0; i < inputs.size() && i < 6; i++) {
            ItemStack in = inputs.get(i);
            if (in == null || in.isEmpty()) continue;

            int col = i % perRow;
            int row = i / perRow;
            rowCount = Math.max(rowCount, row + 1);

            int sx = x + col * spacing;
            int sy = yItems + row * spacing;

            addWidget(new SlotWidget(new ItemStackTransfer(in), 0, sx, sy, false, false)
                    .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                    .setIngredientIO(IngredientIO.INPUT));
        }

        if (rowCount == 0) rowCount = 1;

        int middleRowY = yItems + (rowCount - 1) * spacing / 2;

        int arrowX = x + perRow * spacing + 10;
        addWidget(new ButtonWidget(arrowX, middleRowY + 3, 18, 14, new TextTexture("->"), b -> {})
                .appendHoverTooltips("Takes 10 ticks"));

        int outX = arrowX + 26;
        addWidget(new LabelWidget(outX, 6, "Output"));

        if (!output.isEmpty()) {
            addWidget(new SlotWidget(new ItemStackTransfer(output), 0, outX, middleRowY, false, false)
                    .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                    .setIngredientIO(IngredientIO.OUTPUT));
        }

        int yFluids = yItems + rowCount * spacing + 6;
        int tankSize = 18;

        if (!fluidInput.isEmpty()) {
            PreviewFluidTransfer inTank = new PreviewFluidTransfer(fluidInput.copy(), 16000);
            PhantomTankWidget fin = new PhantomTankWidget(inTank, 0, x, yFluids, tankSize, tankSize);
            fin.setIngredientIO(IngredientIO.INPUT);
            addWidget(fin);
            addWidget(new LabelWidget(x, yFluids + tankSize + 2, "Fluid In"));
        }

        if (!fluidOutput.isEmpty()) {
            PreviewFluidTransfer outTank = new PreviewFluidTransfer(fluidOutput.copy(), 16000);
            PhantomTankWidget fout = new PhantomTankWidget(outTank, 0, outX, yFluids, tankSize, tankSize);
            fout.setIngredientIO(IngredientIO.OUTPUT);
            addWidget(fout);
            addWidget(new LabelWidget(outX, yFluids + tankSize + 2, "Fluid Out"));
        }

        // --- TITLE ---
        String labelText = null;
        if (requiredKey != null) {
            if (requiredLabel != null && !requiredLabel.isBlank()) labelText = requiredLabel;
            else labelText = requiredKey.toString();
        }

        String title;
        if (recipeId == null) title = "Fabrication";
        else if (labelText != null) title = "Research: " + labelText;
        else title = "Fabrication";

        addWidget(new LabelWidget(10, HEIGHT - 12, Component.literal(title)));
    }
}
