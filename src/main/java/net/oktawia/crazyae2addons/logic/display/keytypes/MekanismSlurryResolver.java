package net.oktawia.crazyae2addons.logic.display.keytypes;

import appeng.api.stacks.AEKey;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class MekanismSlurryResolver implements IDisplayKeyResolver {

    @Override
    public String getTypePrefix() {
        return "slurry";
    }

    @Override
    public @Nullable AEKey resolve(String id) {
        Slurry slurry = MekanismAPI.slurryRegistry().getValue(new ResourceLocation(id));
        if (slurry == null) {
            return null;
        }
        return MekanismKey.of(new SlurryStack(slurry, 1));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable ItemStack getIcon(String id) {
        AEKey key = resolve(id);
        return key != null ? key.wrapForDisplayOrFilter() : null;
    }
}