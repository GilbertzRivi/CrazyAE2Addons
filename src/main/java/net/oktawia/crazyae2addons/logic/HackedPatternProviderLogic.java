package net.oktawia.crazyae2addons.logic;

import appeng.api.config.*;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageHelper;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderTarget;
import net.oktawia.crazyae2addons.blocks.EjectorBlock;
import net.oktawia.crazyae2addons.entities.EjectorBE;
import net.oktawia.crazyae2addons.interfaces.IHackedProvider;

public class HackedPatternProviderLogic extends PatternProviderLogic {
    public PatternProviderLogicHost host;

    public HackedPatternProviderLogic(IManagedGridNode mainNode, PatternProviderLogicHost host) {
        super(mainNode, host, 1);
        this.host = host;
        this.getConfigManager().putSetting(Settings.PATTERN_ACCESS_TERMINAL, YesNo.NO);
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (host.getBlockEntity().getLevel() == null) return true;

        if (host.getBlockEntity() instanceof EjectorBE ejector) {
            var level = ejector.getLevel();
            var direction = ejector.getBlockState().getValue(EjectorBlock.FACING);
            var targetEntity = level.getBlockEntity(ejector.getBlockPos().relative(direction));
            if (targetEntity != null) {
                var target = PatternProviderTarget.get(
                        level,
                        targetEntity.getBlockPos(),
                        targetEntity,
                        direction.getOpposite(),
                        IActionSource.ofMachine(ejector)
                );
                if (target == null) {
                    returnToStorage(patternDetails, inputHolder, ejector);
                } else {
                    patternDetails.pushInputsToExternalInventory(inputHolder, (key, amount) -> {
                        var inserted = target.insert(key, amount, Actionable.MODULATE);
                        var leftover = amount - inserted;
                        if (leftover > 0) {
                            var grid = getGrid();
                            if (grid != null) {
                                StorageHelper.poweredInsert(
                                        grid.getEnergyService(),
                                        grid.getStorageService().getInventory(),
                                        key,
                                        leftover,
                                        IActionSource.ofMachine(ejector)
                                );
                            }
                        }
                    });
                }
            } else {
                returnToStorage(patternDetails, inputHolder, ejector);
            }
        }

        if (host.getBlockEntity() instanceof IHackedProvider hp) {
            hp.cancelCraft();
        }
        return true;
    }

    private void returnToStorage(IPatternDetails patternDetails, KeyCounter[] inputHolder, IActionHost machine) {
        var grid = getGrid();
        if (grid != null) {
            patternDetails.pushInputsToExternalInventory(inputHolder, (what, amt) -> {
                StorageHelper.poweredInsert(
                        grid.getEnergyService(),
                        grid.getStorageService().getInventory(),
                        what,
                        amt,
                        IActionSource.ofMachine(machine)
                );
            });
        }
    }
}
