package net.oktawia.crazyae2addons.xei.jei;

import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.xei.common.ResearchEntry;
import net.oktawia.crazyae2addons.xei.common.ResearchPreview;

import java.util.List;

public class ResearchWrapper extends ModularWrapper<ResearchPreview> {
    public final List<ItemStack> inputs;
    public final ItemStack drive;
    public final ResourceLocation recipeId;

    public ResearchWrapper(ResearchEntry entry) {
        super(new ResearchPreview(entry.recipeId(), null, entry.inputs(), entry.driveOrOutput(), null));
        this.inputs = entry.inputs();
        this.drive = entry.driveOrOutput();
        this.recipeId = entry.recipeId();
    }
}
