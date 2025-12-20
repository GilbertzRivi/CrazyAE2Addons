package net.oktawia.crazyae2addons.xei.jei;

import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.xei.common.FabricationEntry;
import net.oktawia.crazyae2addons.xei.common.FabricationPreview;

import java.util.List;

public class FabricationWrapper extends ModularWrapper<FabricationPreview> {
    public final List<ItemStack> input;
    public final ItemStack output;
    public final FluidStack fluidInput;
    public final FluidStack fluidOutput;
    public final ResourceLocation recipeId;

    public FabricationWrapper(FabricationEntry entry) {
        super(new FabricationPreview(
                entry.id(),
                entry.inputs(),
                entry.output(),
                entry.fluidInput(),
                entry.fluidOutput(),
                entry.requiredKey(),
                entry.label()
        ));
        this.input = entry.inputs();
        this.output = entry.output();
        this.fluidInput = entry.fluidInput();
        this.fluidOutput = entry.fluidOutput();
        this.recipeId = entry.id();
    }
}
