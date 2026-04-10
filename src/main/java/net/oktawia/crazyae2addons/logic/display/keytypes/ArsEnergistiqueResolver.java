package net.oktawia.crazyae2addons.logic.display.keytypes;

import appeng.api.stacks.AEKey;
import gripe._90.arseng.me.key.SourceKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class ArsEnergistiqueResolver implements IDisplayKeyResolver {

    @Override
    public String getTypePrefix() { return "source"; }

    @Override
    public @Nullable AEKey resolve(String id) {
        return SourceKey.KEY;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable ItemStack getIcon(String id) {
        return SourceKey.KEY.wrapForDisplayOrFilter();
    }
}
