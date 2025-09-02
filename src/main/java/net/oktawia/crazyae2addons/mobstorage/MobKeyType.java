package net.oktawia.crazyae2addons.mobstorage;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import com.google.common.collect.Streams;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.items.MobKeySelectorItem;
import org.jetbrains.annotations.Nullable;
import net.minecraft.tags.TagKey;

import java.util.Optional;
import java.util.stream.Stream;

public class MobKeyType extends AEKeyType {

    public static final AEKeyType TYPE = new MobKeyType();

    private MobKeyType() {
        super(CrazyAddons.makeId("mob"), MobKey.class, Component.literal("mobs"));
    }

    @Nullable
    @Override
    public AEKey readFromPacket(FriendlyByteBuf input) {
        DataResult<MobKey> result = MobKey.CODEC.parse(NbtOps.INSTANCE, input.readNbt());
        Optional<MobKey> dataOptional = result.result();
        return dataOptional.get();
    }

    @Nullable
    @Override
    public AEKey loadKeyFromTag(CompoundTag tag) {
        DataResult<MobKey> result = MobKey.CODEC.parse(NbtOps.INSTANCE, tag);
        Optional<MobKey> dataOptional = result.result();
        return dataOptional.get();
    }

    @Override
    public Stream<TagKey<?>> getTagNames() {
        return Stream.empty();
    }

    @Override
    public String getUnitSymbol() {
        return "Mobs";
    }

    public static void registerContainerItemStrategies(){
        ContainerItemStrategies.register(
            MobKeyType.TYPE,
            MobKey.class,
                new ContainerItemStrategy() {
                    @Override
                    public @Nullable GenericStack getContainedStack(ItemStack stack) {
                        if (!(stack.getItem() instanceof MobKeySelectorItem)) return null;
                        String id = MobKeySelectorItem.getSelectedKeyId(stack);
                        if (id.isEmpty()) return null;
                        ResourceLocation rl = ResourceLocation.tryParse(id);
                        if (rl == null) return null;
                        EntityType<?> et = ForgeRegistries.ENTITY_TYPES.getValue(rl);
                        if (et == null) return null;
                        MobKey mk = MobKey.of(et);
                        return new GenericStack(mk, 1);
                    }

                    @Override
                    public @Nullable MobKey findCarriedContext(Player player, AbstractContainerMenu menu) {
                        ItemStack carried = player.containerMenu.getCarried();
                        if (!(carried.getItem() instanceof MobKeySelectorItem)) return null;
                        String id = MobKeySelectorItem.getSelectedKeyId(carried);
                        if (id.isEmpty()) return null;
                        ResourceLocation rl = ResourceLocation.tryParse(id);
                        if (rl == null) return null;
                        EntityType<?> et = ForgeRegistries.ENTITY_TYPES.getValue(rl);
                        return et != null ? MobKey.of(et) : null;
                    }
                    @Override public @Nullable GenericStack getExtractableContent(Object context) { return null; }
                    @Override public long insert(Object ctx, AEKey what, long amount, Actionable mode)   { return 0; }
                    @Override public long extract(Object ctx, AEKey what, long amount, Actionable mode)  { return 0; }
                    @Override public void playFillSound(Player player, AEKey what)  {}
                    @Override public void playEmptySound(Player player, AEKey what) {}
            }
        );
    }
}
