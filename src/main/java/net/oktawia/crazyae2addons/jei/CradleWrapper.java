package net.oktawia.crazyae2addons.jei;

import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CradleWrapper extends ModularWrapper<CradlePreview> {
    public final List<ItemStack> input;
    public final ItemStack output;
    public final ResourceLocation structure;

    public CradleWrapper(CradleEntry entry) {
        super(new CradlePreview(entry.structureId(), entry.inputs(), entry.output()));
        this.input = entry.inputs();
        this.output = entry.output();
        this.structure = entry.structureId();
    }
}