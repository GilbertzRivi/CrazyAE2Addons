package net.oktawia.crazyae2addons.mobstorage;

import net.minecraft.resources.ResourceLocation;
import net.oktawia.crazyae2addons.mobstorage.MobKey;

public record MobKeyIng(MobKey key, ResourceLocation id) {
    public static MobKeyIng of(MobKey key) {
        return new MobKeyIng(key, key.getId());
    }
}
