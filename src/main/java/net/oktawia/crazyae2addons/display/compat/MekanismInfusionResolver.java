package net.oktawia.crazyae2addons.display.compat;

import appeng.api.stacks.AEKey;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.infuse.InfusionStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.display.IDisplayKeyResolver;
import org.jetbrains.annotations.Nullable;

public class MekanismInfusionResolver implements IDisplayKeyResolver {

    @Override
    public String getTypePrefix() { return "infusion"; }

    @Override
    public @Nullable AEKey resolve(String id) {
        var type = MekanismAPI.infuseTypeRegistry().getValue(new ResourceLocation(id));
        if (type == null) return null;
        return MekanismKey.of(new InfusionStack(type, 1));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable ItemStack getIcon(String id) {
        var key = resolve(id);
        return key != null ? key.wrapForDisplayOrFilter() : null;
    }
}
