package net.oktawia.crazyae2addons.screens;

import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageCells;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ToggleButton;
import appeng.core.definitions.AEItems;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.entities.PenroseControllerBE;
import net.oktawia.crazyae2addons.menus.PenroseControllerMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

import java.util.List;

public class PenroseControllerScreen<C extends PenroseControllerMenu> extends AEBaseScreen<C> implements CrazyScreen {
    private static final String NAME = "penrose_controller";
    public ToggleButton powerMode;

    static {
        CrazyScreen.i18n(NAME, "extract", "Extract items from the cell");
        CrazyScreen.i18n(NAME, "insert", "Insert items into the cell");
        CrazyScreen.i18n(NAME, "store_power_ae", "Store power in AE network");
        CrazyScreen.i18n(NAME, "store_power_network", "Store power in the network");
        CrazyScreen.i18n(NAME, "store_power_fe", "Store power in FE network");
        CrazyScreen.i18n(NAME, "store_power_multiblock", "Store power in the multiblock");
        CrazyScreen.i18n(NAME, "preview_toggle", "Toggle preview mode");
        CrazyScreen.i18n(NAME, "change_tier", "Change preview tier");
        CrazyScreen.i18n(NAME, "power_generation", "Power generation");
        CrazyScreen.i18n(NAME, "fe_per_tick", "FE per tick: %s");
        CrazyScreen.i18n(NAME, "tier", "Tier: %s");
        CrazyScreen.i18n(NAME, "preview", "Preview: %s (Tier %s)");
    }

    public PenroseControllerScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        var extractBtn = new IconButton(Icon.ARROW_UP, (btn) -> getMenu().extractFromCell());
        var insertBtn = new IconButton(Icon.ARROW_DOWN, (btn) -> getMenu().insertToCell());
        this.powerMode = new ToggleButton(Icon.POWER_UNIT_AE, Icon.POWER_UNIT_RF, this::changePowerMode);
        extractBtn.setTooltip(Tooltip.create(l10n(NAME, "extract")));
        insertBtn.setTooltip(Tooltip.create(l10n(NAME, "insert")));
        this.powerMode.setTooltipOn(List.of(
                l10n(NAME, "store_power_ae"),
                l10n(NAME, "store_power_network")
        ));
        this.powerMode.setTooltipOff(List.of(
                l10n(NAME, "store_power_fe"),
                l10n(NAME, "store_power_multiblock")
        ));
        this.widgets.add("add", extractBtn);
        this.widgets.add("remove", insertBtn);
        this.widgets.add("energy", powerMode);
        var prevBtn = new IconButton(Icon.ENTER, btn -> getMenu().changePreview(!getMenu().preview));
        prevBtn.setTooltip(Tooltip.create(l10n(NAME, "preview_toggle")));
        var tierBtn = new IconButton(Icon.ENTER, btn -> getMenu().changePrevTier((getMenu().previewTier + 1) % 4));
        tierBtn.setTooltip(Tooltip.create(l10n(NAME, "change_tier")));
        this.widgets.add("prevbtn", prevBtn);
        this.widgets.add("tierbtn", tierBtn);
    }

    private void changePowerMode(boolean b) {
        this.powerMode.setState(!b);
        this.getMenu().changeEnergyMode(b);
    }

    @Override
    public void updateBeforeRender() {
        super.updateBeforeRender();
        setTextContent("generation", l10n(NAME, "power_generation"));
        var disk0 = StorageCells.getCellInventory(getMenu().diskSlot0.getItem(), null);
        var disk1 = StorageCells.getCellInventory(getMenu().diskSlot1.getItem(), null);
        var disk2 = StorageCells.getCellInventory(getMenu().diskSlot2.getItem(), null);
        var disk3 = StorageCells.getCellInventory(getMenu().diskSlot3.getItem(), null);
        long generated;
        var tier = getMenu().tier;
        int count = 0;
        if (disk0 != null && tier >= 0) {
            count += (int) disk0.getAvailableStacks().get(AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get()));
        } if (disk1 != null && tier >= 1) {
            count += (int) disk1.getAvailableStacks().get(AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get()));
        } if (disk2 != null && tier >= 2) {
            count += (int) disk2.getAvailableStacks().get(AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get()));
        } if (disk3 != null && tier >= 3) {
            count += (int) disk3.getAvailableStacks().get(AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get()));
        }

        generated = PenroseControllerBE.energyGenerated(count, getMenu().tier);

        if (AEItems.MATTER_BALL.isSameAs(getMenu().configSlot.getItem())){
            generated *= 8;
        } else if (AEItems.SINGULARITY.isSameAs(getMenu().configSlot.getItem())){
            generated *= 64;
        }

        this.powerMode.setState(getMenu().powerMode);
        setTextContent("amount", l10n(NAME, "fe_per_tick", Utils.shortenNumber(generated)));
        setTextContent("tier", l10n(NAME, "tier", getMenu().tier));
        setTextContent("prev", l10n(NAME, "preview", getMenu().preview, getMenu().previewTier));
    }
}
