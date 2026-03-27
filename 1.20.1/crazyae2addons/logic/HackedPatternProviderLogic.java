package net.oktawia.crazyae2addons.logic;

import appeng.api.config.*;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageHelper;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderTarget;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.blocks.EjectorBlock;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;
import net.oktawia.crazyae2addons.entities.EjectorBE;
import net.oktawia.crazyae2addons.interfaces.IHackedProvider;

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
        if (host.getBlockEntity().getLevel() == null) return true;
        if (host.getBlockEntity() instanceof AutoBuilderBE ab) {
            for (var inputList : inputHolder) {
                for (var input : inputList) {
                    var key = input.getKey();
                    long amt = input.getLongValue();
                    if (amt <= 0) continue;

                    if (key instanceof AEItemKey itemKey) {
                        ab.addToBuildBuffer(itemKey, amt);
                    } else if (getGrid() != null) {
                        StorageHelper.poweredInsert(
                                getGrid().getEnergyService(),
                                getGrid().getStorageService().getInventory(),
                                key,
                                amt,
                                IActionSource.empty()
                        );
                    }
                }
            }

            ab.cancelCraftNoFlush();
            ab.onRedstoneActivate();
            return true;
        }

        else if (host.getBlockEntity() instanceof EjectorBE ejector) {
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
                };
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
