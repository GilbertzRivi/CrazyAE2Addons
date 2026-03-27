package net.oktawia.crazyae2addons.xei.emi;

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.xei.common.CrazyEntry;
import net.oktawia.crazyae2addons.xei.common.CrazyPreview;

public class CrazyEmiRecipe extends ModularEmiRecipe<WidgetGroup> {

    private final EmiRecipeCategory category;
    private final CrazyEntry entry;

    public CrazyEmiRecipe(CrazyEntry entry, EmiRecipeCategory category) {
        super(() -> CrazyPreview.getPreviewWidget(
                entry.structureId(), entry.requiredItems(), entry.name().getString()
        ));
        this.category = category;
        this.entry = entry;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return CrazyAddons.makeId("/" + entry.name().getString().toLowerCase().replace(" ", "_"));
    }
}

