package net.oktawia.crazyae2addons.display.compat;

import appeng.api.stacks.AEKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.display.IDisplayKeyResolver;
import net.oktawia.crazyae2addons.mobstorage.MobKey;
import org.jetbrains.annotations.Nullable;

public class MobKeyResolver implements IDisplayKeyResolver {

    @Override
    public String getTypePrefix() { return "mob"; }

    @Override
    public @Nullable AEKey resolve(String id) {
        var entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
        return entityType != null ? MobKey.of(entityType) : null;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable ItemStack getIcon(String id) {
        var key = resolve(id);
        if (key == null) return null;
        var egg = SpawnEggItem.byId(((MobKey) key).getEntityType());
        return egg != null ? new ItemStack(egg) : key.wrapForDisplayOrFilter();
    }
}
