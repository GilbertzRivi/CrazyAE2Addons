package net.oktawia.crazyae2addons.logic.display.keytypes;

import appeng.api.stacks.AEKey;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class MekanismPigmentResolver implements IDisplayKeyResolver {

    @Override
    public String getTypePrefix() {
        return "pigment";
    }

    @Override
    public @Nullable AEKey resolve(String id) {
        Pigment pigment = MekanismAPI.pigmentRegistry().getValue(new ResourceLocation(id));
        if (pigment == null) {
            return null;
        }
        return MekanismKey.of(new PigmentStack(pigment, 1));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable ItemStack getIcon(String id) {
        AEKey key = resolve(id);
        return key != null ? key.wrapForDisplayOrFilter() : null;
    }
}