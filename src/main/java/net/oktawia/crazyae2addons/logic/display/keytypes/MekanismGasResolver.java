package net.oktawia.crazyae2addons.logic.display.keytypes;

import appeng.api.stacks.AEKey;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class MekanismGasResolver implements IDisplayKeyResolver {

    @Override
    public String getTypePrefix() {
        return "gas";
    }

    @Override
    public @Nullable AEKey resolve(String id) {
        Gas gas = MekanismAPI.gasRegistry().getValue(new ResourceLocation(id));
        if (gas == null) {
            return null;
        }
        return MekanismKey.of(new GasStack(gas, 1));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable ItemStack getIcon(String id) {
        AEKey key = resolve(id);
        return key != null ? key.wrapForDisplayOrFilter() : null;
    }
}