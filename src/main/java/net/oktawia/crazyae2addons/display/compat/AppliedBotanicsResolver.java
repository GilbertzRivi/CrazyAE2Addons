package net.oktawia.crazyae2addons.display.compat;

import appeng.api.stacks.AEKey;
import appbot.ae2.ManaKey;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.display.IDisplayKeyResolver;
import org.jetbrains.annotations.Nullable;

public class AppliedBotanicsResolver implements IDisplayKeyResolver {

    @Override
    public String getTypePrefix() { return "mana"; }

    @Override
    public @Nullable AEKey resolve(String id) {
        return ManaKey.KEY;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable ItemStack getIcon(String id) {
        return ManaKey.KEY.wrapForDisplayOrFilter();
    }
}
