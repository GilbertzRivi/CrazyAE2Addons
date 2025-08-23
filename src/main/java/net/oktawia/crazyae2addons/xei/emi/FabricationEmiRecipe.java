package net.oktawia.crazyae2addons.xei.emi;

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.xei.common.FabricationEntry;
import net.oktawia.crazyae2addons.xei.common.FabricationPreview;

public class FabricationEmiRecipe extends ModularEmiRecipe<WidgetGroup> {

    private final EmiRecipeCategory category;
    private final FabricationEntry entry;

    public FabricationEmiRecipe(FabricationEntry entry, EmiRecipeCategory category) {
        super(() -> new FabricationPreview(
                entry.recipeId(),
                entry.input(),
                entry.output(),
                entry.requiredKey(),
                entry.requiredLabel()
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
        // unikalny ID oparty o ID recepty
        return CrazyAddons.makeId("/fabrication/" + entry.recipeId().toString().replace(':', '/'));
    }
}
