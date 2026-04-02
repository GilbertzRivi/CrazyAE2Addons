package net.oktawia.crazyae2addons.display.compat;

import appeng.api.stacks.AEKey;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.display.IDisplayKeyResolver;
import org.jetbrains.annotations.Nullable;

public class AppFluxResolver implements IDisplayKeyResolver {

    @Override
    public String getTypePrefix() { return "flux"; }

    @Override
    public @Nullable AEKey resolve(String id) {
        var rl = new ResourceLocation(id);
        for (var type : EnergyType.values()) {
            if (type.id().equals(rl)) return FluxKey.of(type);
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable ItemStack getIcon(String id) {
        var key = resolve(id);
        return key != null ? key.wrapForDisplayOrFilter() : null;
    }
}
