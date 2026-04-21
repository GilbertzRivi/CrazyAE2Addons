package net.oktawia.crazyae2addons.menus.block;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.Getter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.EjectorBE;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class EjectorMenu extends AEBaseMenu {

    public final Player player;
    public static final String APPLY_PATTERN = "ActionPattern";

    @Getter
    private final EjectorBE host;

    private final Int2IntMap configIndexBySlot = new Int2IntOpenHashMap();

    public EjectorMenu(int id, Inventory ip, EjectorBE host) {
        super(CrazyMenuRegistrar.EJECTOR_MENU.get(), id, ip, host);
        this.createPlayerInventorySlots(ip);

        this.player = ip.player;
        this.host = host;

        this.addSlot(new AppEngSlot(host.pattern, 0), SlotSemantics.ENCODED_PATTERN);

        for (int i = 0; i < host.config.size(); i++) {
            var slot = new FakeSlot(host.config.createMenuWrapper(), i);
            this.addSlot(slot, SlotSemantics.CONFIG);
            configIndexBySlot.put(slot.index, i);
        }

        registerClientAction(APPLY_PATTERN, this::applyPatternToConfig);
    }

    @Contract("null -> false")
    public boolean canModifyAmountForSlot(@Nullable Slot slot) {
        return slot != null
                && configIndexBySlot.containsKey(slot.index)
                && slot.hasItem();
    }

    public void setConfigAmount(int slotIndex, long amount) {
        int local = configIndexBySlot.getOrDefault(slotIndex, -1);
        if (local < 0) return;

        var inv = this.host.config;
        if (amount <= 0) {
            inv.setStack(local, null);
        } else {
            var gs = inv.getStack(local);
            if (gs == null) return;
            inv.setStack(local, new GenericStack(gs.what(), amount));
        }

        this.broadcastChanges();
    }

    public void applyPatternToConfig() {
        if (isClientSide()) {
            sendClientAction(APPLY_PATTERN);
        } else {
            var pg = host.pattern;
            ItemStack pattern = pg.getStackInSlot(0);
            if (pattern.isEmpty()) return;

            IPatternDetails details;
            try {
                details = PatternDetailsHelper.decodePattern(pattern, player.level());
            } catch (Exception ex) {
                return;
            }

            if (details == null) return;

            for (int i = 0; i < host.config.size(); i++) {
                host.config.setStack(i, null);
            }

            int ci = 0;
            for (var input : details.getInputs()) {
                var possibles = input.getPossibleInputs();
                if (possibles.length == 0) continue;

                AEKey key = possibles[0].what();
                long amount = Math.max(1, input.getMultiplier());

                if (ci < host.config.size()) {
                    host.config.setStack(ci, new GenericStack(key, amount));
                    ci++;
                } else {
                    break;
                }
            }

            this.broadcastChanges();
        }
    }
}