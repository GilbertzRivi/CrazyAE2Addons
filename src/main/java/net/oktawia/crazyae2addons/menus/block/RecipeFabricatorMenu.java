package net.oktawia.crazyae2addons.menus.block;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.AppEngSlot;
import lombok.Getter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.RecipeFabricatorBE;

public class RecipeFabricatorMenu extends AEBaseMenu {

    private static final int BUCKET_MB = 1000;

    private final SimpleContainer fluidUi = new SimpleContainer(2);
    @Getter
    private final Slot fluidInSlot;
    @Getter
    private final Slot fluidOutSlot;
    @Getter
    private final RecipeFabricatorBE host;

    public RecipeFabricatorMenu(int id, Inventory playerInventory, RecipeFabricatorBE host) {
        super(CrazyMenuRegistrar.RECIPE_FABRICATOR_MENU.get(), id, playerInventory, host);
        this.host = host;

        for (int i = 0; i < 6; i++) {
            this.addSlot(new AppEngSlot(host.input, i), SlotSemantics.MACHINE_INPUT);
        }

        this.fluidInSlot = this.addSlot(new FluidClickSlot(fluidUi, 0), SlotSemantics.MACHINE_INPUT);

        this.addSlot(new AppEngSlot(host.output, 0){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        }, SlotSemantics.MACHINE_OUTPUT);

        this.fluidOutSlot = this.addSlot(new FluidClickSlot(fluidUi, 1), SlotSemantics.MACHINE_OUTPUT);

        this.createPlayerInventorySlots(playerInventory);
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < this.slots.size() && clickType == ClickType.PICKUP) {
            Slot slot = this.slots.get(slotId);

            if (slot == fluidInSlot) {
                handleFluidInClick(player);
                return;
            }

            if (slot == fluidOutSlot) {
                handleFluidOutClick(player);
                return;
            }
        }

        super.clicked(slotId, dragType, clickType, player);
    }

    private void handleFluidInClick(Player player) {
        RecipeFabricatorBE host = getHost();
        if (host == null) {
            return;
        }

        IFluidHandler tank = host.getMenuInputHandler();
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            return;
        }

        ItemStack single = carried.copy();
        single.setCount(1);

        var contained = FluidUtil.getFluidContained(single);
        if (contained.isPresent() && !contained.get().isEmpty()) {
            FluidActionResult result = FluidUtil.tryEmptyContainer(single, tank, BUCKET_MB, player, true);
            if (result.isSuccess()) {
                applyCarriedResult(player, carried, result.getResult());
            }
            return;
        }

        FluidActionResult result = FluidUtil.tryFillContainer(single, tank, BUCKET_MB, player, true);
        if (result.isSuccess()) {
            applyCarriedResult(player, carried, result.getResult());
        }
    }

    private void handleFluidOutClick(Player player) {
        RecipeFabricatorBE host = getHost();
        if (host == null) {
            return;
        }

        IFluidHandler tank = host.getMenuOutputHandler();
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            return;
        }

        ItemStack single = carried.copy();
        single.setCount(1);

        var contained = FluidUtil.getFluidContained(single);
        if (contained.isPresent() && !contained.get().isEmpty()) {
            return;
        }

        FluidActionResult result = FluidUtil.tryFillContainer(single, tank, BUCKET_MB, player, true);
        if (result.isSuccess()) {
            applyCarriedResult(player, carried, result.getResult());
        }
    }

    private static void applyCarriedResult(Player player, ItemStack carried, ItemStack result) {
        if (carried.getCount() <= 1) {
            player.containerMenu.setCarried(result);
            return;
        }

        carried.shrink(1);
        player.containerMenu.setCarried(carried);

        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
    }

    private static class FluidClickSlot extends Slot {
        public FluidClickSlot(SimpleContainer inv, int index) {
            super(inv, index, 0, 0);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public void set(ItemStack stack) {
        }

        @Override
        public ItemStack getItem() {
            return ItemStack.EMPTY;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}