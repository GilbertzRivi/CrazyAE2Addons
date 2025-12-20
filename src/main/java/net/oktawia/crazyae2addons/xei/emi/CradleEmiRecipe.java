package net.oktawia.crazyae2addons.xei.emi;

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.xei.common.CradleEntry;
import net.oktawia.crazyae2addons.xei.common.CradlePreview;
import net.oktawia.crazyae2addons.xei.common.CrazyEntry;
import net.oktawia.crazyae2addons.xei.common.CrazyPreview;

public class CradleEmiRecipe extends ModularEmiRecipe<WidgetGroup> {

    private final EmiRecipeCategory category;
    private final CradleEntry entry;

    public CradleEmiRecipe(CradleEntry entry, EmiRecipeCategory category) {
        super(() -> new CradlePreview(
                entry.structureId(), entry.inputs(), entry.output(), entry.description()
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
        return CrazyAddons.makeId("/" + entry.output().toString().toLowerCase().replace(" ", "_"));
    }
}

