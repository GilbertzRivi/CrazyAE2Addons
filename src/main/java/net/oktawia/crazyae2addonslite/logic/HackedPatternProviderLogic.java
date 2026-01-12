package net.oktawia.crazyae2addonslite.logic;

import appeng.api.config.*;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageHelper;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderTarget;
import net.oktawia.crazyae2addonslite.blocks.EjectorBlock;
import net.oktawia.crazyae2addonslite.entities.EjectorBE;
import net.oktawia.crazyae2addonslite.interfaces.IHackedProvider;

public class HackedPatternProviderLogic extends PatternProviderLogic {
    public PatternProviderLogicHost host;

    public HackedPatternProviderLogic(IManagedGridNode mainNode, PatternProviderLogicHost host) {
        super(mainNode, host, 1);
        this.host = host;
        this.getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.getConfigManager().registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        this.getConfigManager().putSetting(Settings.PATTERN_ACCESS_TERMINAL, YesNo.NO);
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (host.getBlockEntity() instanceof EjectorBE ejector) {
            var level = ejector.getLevel();
            var direction = ejector.getBlockState().getValue(EjectorBlock.FACING);
            var targetEntity = ejector.getLevel().getBlockEntity(ejector.getBlockPos().relative(direction));
            if (targetEntity != null) {
                var target = PatternProviderTarget.get(
                        level,
                        targetEntity.getBlockPos(),
                        targetEntity,
                        direction.getOpposite(),
                        IActionSource.ofMachine(ejector)
                );
                patternDetails.pushInputsToExternalInventory(inputHolder, (what, amt) -> {
                    var inserted = target.insert(what, amt, Actionable.MODULATE);
                    if (inserted < amt) {
                        StorageHelper.poweredInsert(
                            getGrid().getEnergyService(),
                            getGrid().getStorageService().getInventory(),
                            what,
                            amt - inserted,
                            IActionSource.ofMachine(ejector)
                        );
                    }
                });
            } else {
                var grid = getGrid();
                if (grid != null) {
                    patternDetails.pushInputsToExternalInventory(inputHolder, (what, amt) -> {
                        StorageHelper.poweredInsert(
                            grid.getEnergyService(),
                            grid.getStorageService().getInventory(),
                            what,
                            amt,
                            IActionSource.ofMachine(ejector)
                        );
                    });
                }
            }
            if (host.getBlockEntity() instanceof IHackedProvider hp) {
                hp.cancelCraft();
            }
            return true;
        }

        if (host.getBlockEntity() instanceof IHackedProvider hp) {
            hp.cancelCraft();
        }
        return true;
    }
}
