package net.oktawia.crazyae2addons.logic.display.keytypes;

import appeng.api.stacks.AEKey;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class MekanismChemicalResolver implements IDisplayKeyResolver {

    @Override
    public String getTypePrefix() { return "gas"; }

    @Override
    public @Nullable AEKey resolve(String id) {
        var gas = MekanismAPI.CHEMICAL_REGISTRY.getHolder(ResourceLocation.parse(id));
        return gas.map(chemicalReference -> MekanismKey.of(new ChemicalStack(chemicalReference, 1))).orElse(null);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable ItemStack getIcon(String id) {
        var key = resolve(id);
        return key != null ? key.wrapForDisplayOrFilter() : null;
    }
}
