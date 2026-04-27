package net.oktawia.crazyae2addons.mixins.cancelallcrafting;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.me.crafting.CraftingCPUMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.CancelAllCraftingPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingCPUScreen.class, remap = false)
public abstract class MixinCraftingCPUScreenCL<T extends CraftingCPUMenu> extends AEBaseScreen<T> {

    public MixinCraftingCPUScreenCL(T menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(
            method = {"init()V", "m_7856_()V"},
            at = @At("TAIL")
    )
    private void crazyae2addons$addCancelAllAfterForkInit(CallbackInfo ci) {
        Button cancelAll = Button.builder(
                Component.translatable(LangDefs.CANCEL_ALL_CRAFTING.getTranslationKey()),
                btn -> NetworkHandler.sendToServer(new CancelAllCraftingPacket())
        ).build();

        cancelAll.setPosition(getGuiLeft() + 8, getGuiTop() + getYSize() + 5);
        cancelAll.setWidth(140);
        cancelAll.setHeight(20);

        addRenderableWidget(cancelAll);
    }
}