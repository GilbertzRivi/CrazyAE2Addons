package net.oktawia.crazyae2addons.mobstorage;

import appeng.items.AEBaseItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MobKeyItem extends AEBaseItem {

    public static final String TAG_MOB = "mob";

    public MobKeyItem(Properties props) {
        super(props);
    }

    public static ItemStack of(EntityType<?> type) {
        ItemStack stack = new ItemStack(CrazyItemRegistrar.MOB_KEY_ITEM.get());
        var id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id != null) stack.getOrCreateTag().putString(TAG_MOB, id.toString());
        return stack;
    }

    @Nullable
    public static EntityType<?> getEntityType(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_MOB)) return null;
        var id = new ResourceLocation(stack.getTag().getString(TAG_MOB));
        return ForgeRegistries.ENTITY_TYPES.getValue(id);
    }

    public static boolean isSupported(EntityType<?> t) {
        return t.getCategory() != MobCategory.MISC && t.canSummon();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        var type = getEntityType(stack);
        if (type != null) {
            var id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (id != null) tooltip.add(Component.literal(id.toString()));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
