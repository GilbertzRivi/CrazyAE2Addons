package net.oktawia.crazyae2addons.xei.common;

import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyRecipes;
import net.oktawia.crazyae2addons.recipes.ResearchRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ResearchPreview extends WidgetGroup {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 200;

    @Nullable
    private final ResourceLocation recipeId;
    @Nullable
    private final ResearchRecipe recipe;

    public ResearchPreview(@Nullable ResourceLocation recipeId,
                           @Nullable ResearchRecipe recipe,
                           List<ItemStack> inputs,
                           ItemStack driveOrOutput,
                           @Nullable ResourceLocation overlayNbt) {
        super(0, 0, WIDTH, HEIGHT);
        setClientSideWidget();

        this.recipeId = recipeId;
        this.recipe = recipe;

        int centerX = WIDTH / 2;
        int centerY = 60;

        addStationIcon(centerX, centerY);
        addPedestalsCircle(centerX, centerY);
        addOutputPanel(driveOrOutput);
        addProcessInfoPanel();
    }

    private void addStationIcon(int centerX, int centerY) {
        ItemStack stationStack = new ItemStack(CrazyBlockRegistrar.RESEARCH_STATION.get().asItem());

        int slotX = centerX - 9; // SlotWidget 18x18
        int slotY = centerY - 9;

        addWidget(new SlotWidget(new ItemStackTransfer(stationStack), 0,
                slotX, slotY, false, false)
                .setIngredientIO(IngredientIO.RENDER_ONLY));
    }

    private void addPedestalsCircle(int centerX, int centerY) {
        ResearchRecipe r = resolveRecipe();
        if (r == null || r.consumables == null || r.consumables.isEmpty()) {
            return;
        }

        int radius = 46;
        int radiusX = radius;
        int radiusY = radius;

        int maxSlots = 8;
        int count = Math.min(maxSlots, r.consumables.size());

        double angleStep = 2.0 * Math.PI / (double) count;
        double startAngle = -Math.PI / 2.0;

        var font = Minecraft.getInstance().font;

        for (int i = 0; i < count; i++) {
            ResearchRecipe.Consumable c = r.consumables.get(i);

            double angle = startAngle + angleStep * i;

            int slotCenterX = centerX + (int) Math.round(Math.cos(angle) * radiusX);
            int slotCenterY = centerY + (int) Math.round(Math.sin(angle) * radiusY);

            int slotX = slotCenterX - 9;
            int slotY = slotCenterY - 9;

            ItemStack stack = new ItemStack(c.item, Math.max(1, c.count));
            if (stack.getCount() > stack.getMaxStackSize()) {
                stack.setCount(stack.getMaxStackSize());
            }

            addWidget(new SlotWidget(new ItemStackTransfer(stack), 0,
                    slotX, slotY, false, false)
                    .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                    .setIngredientIO(IngredientIO.INPUT));

            String compText = Component.translatable(
                    "gui.crazyae2addons.research_pedestal_compact", c.computation
            ).getString();

            int textWidth = font.width(compText);
            int textX = slotCenterX - textWidth / 2;
            int textY = slotY + 18 + 2;

            addWidget(new LabelWidget(textX, textY, compText));
        }
    }

    private void addOutputPanel(ItemStack driveOrOutput) {
        int x = 5;
        int labelY = 135;
        int slotY = labelY + 10;

        String outLabel = Component
                .translatable("gui.crazyae2addons.research_output_label")
                .getString();
        addWidget(new LabelWidget(x, labelY, outLabel));

        addWidget(new SlotWidget(new ItemStackTransfer(driveOrOutput), 0,
                x, slotY, false, false)
                .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.OUTPUT));

        addWidget(new ButtonWidget(x + 20, slotY + 3, 12, 12,
                new TextTexture("?"), b -> {})
                .appendHoverTooltips(
                        Component.translatable("gui.crazyae2addons.research_drive_tooltip_1").getString(),
                        Component.translatable("gui.crazyae2addons.research_drive_tooltip_2").getString()
                ));

        String diskNote = Component
                .translatable("gui.crazyae2addons.research_output_disk_note")
                .getString();
        addWidget(new LabelWidget(x + 35, slotY + 4, diskNote));
    }

    private int getTotalPedestalComputation(ResearchRecipe r) {
        int sum = 0;
        if (r.consumables != null) {
            for (ResearchRecipe.Consumable c : r.consumables) {
                if (c != null && c.computation > 0) {
                    sum += c.computation;
                }
            }
        }
        return sum;
    }

    private void addProcessInfoPanel() {
        try {
            ResearchRecipe r = resolveRecipe();
            if (r == null) return;

            long requiredComputation = r.duration;
            int minCompPerTick = getTotalPedestalComputation(r);

            long ticksMin = 1;
            long seconds = 0;

            if (minCompPerTick > 0) {
                ticksMin = (requiredComputation + minCompPerTick - 1L) / (long) minCompPerTick;
                seconds = ticksMin / 20L;
            }

            String line3 = Component
                    .translatable("gui.crazyae2addons.research_duration", seconds)
                    .getString();

            String unlockName = (r.unlock.label == null || r.unlock.label.isEmpty()
                    ? r.unlock.key.toString()
                    : r.unlock.label);
            String line5 = Component
                    .translatable("gui.crazyae2addons.research_unlocks", unlockName)
                    .getString();

            int startY = 165;
            int lineH = Minecraft.getInstance().font.lineHeight + 2;
            int x = 5;

            addWidget(new LabelWidget(x, startY, line3));
            addWidget(new LabelWidget(x, startY + lineH, line5));
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private ResearchRecipe resolveRecipe() {
        if (recipe != null) return recipe;
        if (recipeId == null) return null;

        var level = Minecraft.getInstance().level;
        if (level == null) {
            LogUtils.getLogger().warn("No client level available to load research recipe {}", recipeId);
            return null;
        }
        return level.getRecipeManager()
                .getAllRecipesFor(CrazyRecipes.RESEARCH_TYPE.get())
                .stream()
                .filter(rr -> rr.getId().equals(recipeId))
                .findFirst()
                .orElse(null);
    }
}
