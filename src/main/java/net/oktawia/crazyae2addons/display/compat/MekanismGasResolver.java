package net.oktawia.crazyae2addons.display.compat;

import appeng.api.stacks.AEKey;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.GasStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.display.IDisplayKeyResolver;
import org.jetbrains.annotations.Nullable;

public class MekanismGasResolver implements IDisplayKeyResolver {

    @Override
    public String getTypePrefix() { return "gas"; }

    @Override
    public @Nullable AEKey resolve(String id) {
        var gas = MekanismAPI.gasRegistry().getValue(new ResourceLocation(id));
        if (gas == null) return null;
        return MekanismKey.of(new GasStack(gas, 1));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable ItemStack getIcon(String id) {
        var key = resolve(id);
        return key != null ? key.wrapForDisplayOrFilter() : null;
    }
}
