package net.oktawia.crazyae2addons.mixins;

import appeng.api.storage.AEKeyFilter;
import appeng.items.storage.ViewCellItem;
import appeng.util.prioritylist.IPartitionList;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.items.NbtViewCellItem;
import net.oktawia.crazyae2addons.items.TagViewCellItem;
import net.oktawia.crazyae2addons.misc.NBTPriorityList;
import net.oktawia.crazyae2addons.misc.TagPriorityList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(value = ViewCellItem.class)
public abstract class ViewCellItemMixin {
    @Inject(
            method = "createFilter(Lappeng/api/storage/AEKeyFilter;Ljava/util/Collection;)Lappeng/util/prioritylist/IPartitionList;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void crazyae2addons$useNbtPriorityList(
            AEKeyFilter filter, Collection<ItemStack> list, CallbackInfoReturnable<IPartitionList> cir
    ) {
        for (ItemStack is : list) {
            if (is != null && is.getItem() instanceof NbtViewCellItem && is.getOrCreateTag().contains("filter")) {
                cir.setReturnValue(new NBTPriorityList(is.getOrCreateTag().getString("filter")));
                return;
            }
            else if (is != null && is.getItem() instanceof TagViewCellItem && is.getOrCreateTag().contains("filter")) {
                cir.setReturnValue(new TagPriorityList(is.getOrCreateTag().getString("filter")));
                return;
            }
        }
    }
}
