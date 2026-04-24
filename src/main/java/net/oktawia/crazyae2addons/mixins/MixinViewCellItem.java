package net.oktawia.crazyae2addons.mixins;

import appeng.api.storage.AEKeyFilter;
import appeng.items.storage.ViewCellItem;
import appeng.util.prioritylist.IPartitionList;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.items.TagViewCellItem;
import net.oktawia.crazyae2addons.logic.viewcell.TagPriorityList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(value = ViewCellItem.class, remap = false)
public abstract class MixinViewCellItem {

    @Inject(
            method = "createFilter(Lappeng/api/storage/AEKeyFilter;Ljava/util/Collection;)Lappeng/util/prioritylist/IPartitionList;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void crazyae2addons$useTagPriorityList(
            AEKeyFilter filter,
            Collection<ItemStack> list,
            CallbackInfoReturnable<IPartitionList> cir
    ) {
        List<String> tagFilters = new ArrayList<>();

        for (ItemStack stack : list) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof TagViewCellItem && stack.getOrCreateTag().contains("filter")) {
                String value = stack.getOrCreateTag().getString("filter");
                if (!value.isBlank()) {
                    tagFilters.add(value);
                }
            }
        }

        if (!tagFilters.isEmpty()) {
            cir.setReturnValue(new TagPriorityList(String.join("\n", tagFilters)));
        }
    }
}