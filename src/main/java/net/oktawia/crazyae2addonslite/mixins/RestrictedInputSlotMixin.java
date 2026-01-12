// net.oktawia.crazyae2addons.mixin.RestrictedInputSlot_ViewCellAcceptsAnyMixin
package net.oktawia.crazyae2addonslite.mixins;

import appeng.core.definitions.AEItems;
import appeng.core.definitions.ItemDefinition;
import appeng.items.storage.ViewCellItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = appeng.menu.slot.RestrictedInputSlot.class)
public abstract class RestrictedInputSlotMixin {

    @Redirect(
            method = "mayPlace(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/core/definitions/ItemDefinition;isSameAs(Lnet/minecraft/world/item/ItemStack;)Z",
                    remap = false
            )
    )
    private boolean crazyae2addons$acceptAnyViewCell(ItemDefinition instance, ItemStack stack) {
        if (instance == AEItems.VIEW_CELL) {
            return instance.isSameAs(stack) || (stack.getItem() instanceof ViewCellItem);
        }

        return instance.isSameAs(stack);
    }
}
