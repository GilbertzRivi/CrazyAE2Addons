package net.oktawia.crazyae2addons.xei.emi;

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.xei.common.ResearchEntry;
import net.oktawia.crazyae2addons.xei.common.ResearchPreview;

public class ResearchEmiRecipe extends ModularEmiRecipe<WidgetGroup> {

    private final EmiRecipeCategory category;
    private final ResearchEntry entry;

    public ResearchEmiRecipe(ResearchEntry entry, EmiRecipeCategory category) {
        super(() -> new ResearchPreview(
                entry.recipeId(),
                null,
                entry.inputs(),
                entry.driveOrOutput(),
                null
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
        return CrazyAddons.makeId("/research/" + entry.unlockKey().toString().replace(':', '/'));
    }
}
