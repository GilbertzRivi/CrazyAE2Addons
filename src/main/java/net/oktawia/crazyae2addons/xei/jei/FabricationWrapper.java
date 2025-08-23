package net.oktawia.crazyae2addons.xei.jei;

import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.xei.common.FabricationEntry;
import net.oktawia.crazyae2addons.xei.common.FabricationPreview;

public class FabricationWrapper extends ModularWrapper<FabricationPreview> {
    public final ItemStack input;
    public final ItemStack output;
    public final ResourceLocation recipeId;

    public FabricationWrapper(FabricationEntry entry) {
        super(new FabricationPreview(
                entry.recipeId(),
                entry.input(),
                entry.output(),
                entry.requiredKey(),
                entry.requiredLabel()
        ));
        this.input = entry.input();
        this.output = entry.output();
        this.recipeId = entry.recipeId();
    }
}
