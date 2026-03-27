package net.oktawia.crazyae2addons.menus;

import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.RecipeFabricatorBE;
import net.oktawia.crazyae2addons.misc.AppEngFilteredSlot;

public class RecipeFabricatorMenu extends UpgradeableMenu<RecipeFabricatorBE> {

    @GuiSync(801) public Integer progress = 0;
    @GuiSync(802) public Integer duration = 10;

    // ==== SYNC FLUIDÓW (UI) ====
    @GuiSync(803) public Integer fluidInRawId = 0;
    @GuiSync(804) public Integer fluidInAmount = 0;
    @GuiSync(805) public Integer fluidOutRawId = 0;
    @GuiSync(806) public Integer fluidOutAmount = 0;

    private static final int BUCKET_MB = 1000;

    // dwa “puste” sloty (tylko UI)
    private final SimpleContainer fluidUi = new SimpleContainer(2);
    private Slot fluidInSlot;
    private Slot fluidOutSlot;

    public RecipeFabricatorMenu(int id, Inventory ip, RecipeFabricatorBE host) {
        super(CrazyMenuRegistrar.RECIPE_FABRICATOR_MENU.get(), id, ip, host);

        // 6 item input
        for (int i = 0; i < 6; i++) {
            this.addSlot(new AppEngSlot(host.input, i), SlotSemantics.MACHINE_INPUT);
        }

        // fluid input slot
        this.fluidInSlot = this.addSlot(new FluidClickSlot(fluidUi, 0), SlotSemantics.MACHINE_INPUT);

        // drive
        this.addSlot(new AppEngFilteredSlot(host.drive, 0, CrazyItemRegistrar.DATA_DRIVE.get()),
                SlotSemantics.CONFIG);

        // item output
        this.addSlot(new AppEngFilteredSlot(host.output, 0, Items.AIR),
                SlotSemantics.MACHINE_OUTPUT);

        // fluid output slot
        this.fluidOutSlot = this.addSlot(new FluidClickSlot(fluidUi, 1), SlotSemantics.MACHINE_OUTPUT);
    }

    public Slot getFluidInSlot() { return fluidInSlot; }
    public Slot getFluidOutSlot() { return fluidOutSlot; }

    // ====== GETTERY dla screena (z SYNC) ======
    public FluidStack getSyncedFluidIn() {
        return fromSync(fluidInRawId, fluidInAmount);
    }

    public FluidStack getSyncedFluidOut() {
        return fromSync(fluidOutRawId, fluidOutAmount);
    }

    private static int toRawId(FluidStack fs) {
        if (fs == null || fs.isEmpty()) return BuiltInRegistries.FLUID.getId(Fluids.EMPTY);
        return BuiltInRegistries.FLUID.getId(fs.getFluid());
    }

    private static FluidStack fromSync(Integer rawId, Integer amount) {
        int rid = rawId != null ? rawId : BuiltInRegistries.FLUID.getId(Fluids.EMPTY);
        int amt = amount != null ? amount : 0;

        if (amt <= 0) return FluidStack.EMPTY;

        var fluid = BuiltInRegistries.FLUID.byId(rid);
        if (fluid == null || fluid == Fluids.EMPTY) return FluidStack.EMPTY;

        return new FluidStack(fluid, amt);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        var h = getHost();
        if (h != null) {
            this.progress = h.getProgress();
            this.duration = h.getDuration();

            FluidStack fin = h.getFluidIn();
            this.fluidInRawId = toRawId(fin);
            this.fluidInAmount = fin.isEmpty() ? 0 : fin.getAmount();

            FluidStack fout = h.getFluidOut();
            this.fluidOutRawId = toRawId(fout);
            this.fluidOutAmount = fout.isEmpty() ? 0 : fout.getAmount();
        }
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < this.slots.size() && clickType == ClickType.PICKUP) {
            Slot s = this.slots.get(slotId);

            if (s == fluidInSlot) {
                handleFluidInClick(player);
                return;
            }
            if (s == fluidOutSlot) {
                handleFluidOutClick(player);
                return;
            }
        }

        super.clicked(slotId, dragType, clickType, player);
    }

    private void handleFluidInClick(Player player) {
        RecipeFabricatorBE host = getHost();
        if (host == null) return;

        IFluidHandler tank = host.getMenuInputHandler();

        ItemStack carried = getCarried();
        if (carried.isEmpty()) return;

        ItemStack one = carried.copy();
        one.setCount(1);

        // jeśli pojemnik ma fluid -> wlej do INPUT
        var contained = FluidUtil.getFluidContained(one);
        if (contained.isPresent() && !contained.get().isEmpty()) {
            FluidActionResult res = FluidUtil.tryEmptyContainer(one, tank, BUCKET_MB, player, true);
            if (res.isSuccess()) {
                applyCarriedResult(player, carried, res.getResult());
            }
            return;
        }

        // jeśli pojemnik pusty -> nabierz z INPUT
        FluidActionResult res = FluidUtil.tryFillContainer(one, tank, BUCKET_MB, player, true);
        if (res.isSuccess()) {
            applyCarriedResult(player, carried, res.getResult());
        }
    }

    private void handleFluidOutClick(Player player) {
        RecipeFabricatorBE host = getHost();
        if (host == null) return;

        IFluidHandler tank = host.getMenuOutputHandler();

        ItemStack carried = getCarried();
        if (carried.isEmpty()) return;

        ItemStack one = carried.copy();
        one.setCount(1);

        // OUTPUT: tylko wyciąganie, więc jeśli niesiesz pełny pojemnik -> nic
        var contained = FluidUtil.getFluidContained(one);
        if (contained.isPresent() && !contained.get().isEmpty()) {
            return;
        }

        FluidActionResult res = FluidUtil.tryFillContainer(one, tank, BUCKET_MB, player, true);
        if (res.isSuccess()) {
            applyCarriedResult(player, carried, res.getResult());
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

    // slot “klikany” – nic nie wkładasz, nic nie wyciągasz normalnie
    private static class FluidClickSlot extends Slot {
        public FluidClickSlot(SimpleContainer inv, int index) {
            super(inv, index, 0, 0);
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public void set(ItemStack stack) { }
        @Override public ItemStack getItem() { return ItemStack.EMPTY; } // render płynu robi screen
        @Override public int getMaxStackSize() { return 1; }
    }
}
