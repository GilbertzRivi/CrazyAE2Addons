package net.oktawia.crazyae2addons.mixins;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.me.crafting.CraftingCPUMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.network.CancellAllCraftingPacket;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = CraftingCPUScreen.class, remap = false)
public abstract class MixinCraftingCPUScreen<T extends CraftingCPUMenu> extends AEBaseScreen<T> {

    @Unique Button cancellAll = null;

    public MixinCraftingCPUScreen(T menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Override
    public void init() {
        super.init();

        this.cancellAll = Button.builder(
                Component.literal("Cancel All Crafting Jobs"),
                btn -> NetworkHandler.INSTANCE.sendToServer(new CancellAllCraftingPacket())
        ).build();

        this.cancellAll.setPosition(getGuiLeft() + 8, getGuiTop() + getYSize() - 25);
        this.cancellAll.setWidth(140);
        this.cancellAll.setHeight(20);

        addRenderableWidget(this.cancellAll);
    }

}
