package net.oktawia.crazyae2addons.logic.display.keytypes;

import appeng.api.stacks.AEKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public interface IDisplayKeyResolver {
    String getTypePrefix();

    @Nullable AEKey resolve(String id);

    @OnlyIn(Dist.CLIENT)
    @Nullable ItemStack getIcon(String id);
}
