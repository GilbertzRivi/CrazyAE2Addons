package net.oktawia.crazyae2addons.compat.Apotheosis;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import dev.shadowsoffire.apotheosis.ench.objects.TreasureShelfBlock;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.entities.AutoEnchanterBE;
import net.oktawia.crazyae2addons.items.XpShardItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApothAutoEnchanterBE extends AutoEnchanterBE {

    public ApothAutoEnchanterBE(BlockPos pos, BlockState blockState) {
        super(pos, blockState);
    }

    public record EnchStats(float eterna, float quanta, float arcana, int clues, boolean treasure) {}

    public EnchStats getEnchantStats(BlockPos tablePos) {
        int radius = 2;
        float eterna = 0;
        float quanta = 0;
        float arcana = 0;
        int clues = 0;
        boolean treasure = false;

        for (BlockPos pos : BlockPos.betweenClosed(
                tablePos.offset(-radius, 0, -radius),
                tablePos.offset(radius, 1, radius)
        )) {
            BlockState state = getLevel().getBlockState(pos);
            eterna += EnchantingStatRegistry.getEterna(state, getLevel(), pos);
            quanta += EnchantingStatRegistry.getQuanta(state, getLevel(), pos);
            arcana += EnchantingStatRegistry.getArcana(state, getLevel(), pos);
            clues += EnchantingStatRegistry.getBonusClues(state, getLevel(), pos);

            if (!treasure && state.getBlock() instanceof TreasureShelfBlock) {
                treasure = true;
            }
        }

        return new EnchStats(eterna, quanta, arcana, clues, treasure);
    }

    private static long levelToXpLong(int level) {
        if (level <= 16) {
            return (long) level * (long) level + 6L * level;
        } else if (level <= 31) {
            return (long) (2.5d * level * level - 40.5d * level + 360d);
        } else {
            return (long) (4.5d * level * level - 162.5d * level + 2220d);
        }
    }

    private static long safeMul(long a, long b) {
        if (a == 0 || b == 0) return 0;
        if (a > Long.MAX_VALUE / b) return Long.MAX_VALUE;
        return a * b;
    }

    public int getXpCostForEnchant(ItemStack input, int option) {
        if (input.isEmpty() || (!input.isEnchantable() && input.getItem() != Items.BOOK)) {
            return 0;
        }

        BlockPos tablePos = this.getBlockPos().above().above();
        EnchStats stats = getEnchantStats(tablePos);

        RandomSource random = RandomSource.create();
        int enchantLevel = RealEnchantmentHelper.getEnchantmentCost(random, option, stats.eterna(), input);
        return Math.max(enchantLevel, 0);
    }

    private boolean consumeXpFromNetworkAtomically(long xpToConsume) {
        IGridNode node = getGridNode();
        if (node == null || !node.isActive() || node.getGrid() == null) return false;

        var grid = node.getGrid();
        var energy = grid.getEnergyService();
        var storage = grid.getStorageService().getInventory();
        var source = IActionSource.ofMachine(this);

        long xpLeft = xpToConsume;

        List<AEFluidKey> fluids = new ArrayList<>(getAvailableXpFluids());

        long shardAvail = storage.extract(
                AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()),
                Long.MAX_VALUE,
                Actionable.SIMULATE,
                source
        );

        long shardsPlanned = Math.min(xpLeft / (long) XpShardItem.XP_VAL, shardAvail);

        long shardsSim = StorageHelper.poweredExtraction(
                energy,
                storage,
                AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()),
                shardsPlanned,
                source,
                Actionable.SIMULATE
        );
        if (shardsSim < shardsPlanned) return false;

        xpLeft -= shardsPlanned * (long) XpShardItem.XP_VAL;

        Map<AEFluidKey, Long> fluidsPlanned = new LinkedHashMap<>();
        for (AEFluidKey fluid : fluids) {
            if (xpLeft <= 0) break;

            long availableMb = storage.extract(fluid, Long.MAX_VALUE, Actionable.SIMULATE, source);

            long needMbRaw = safeMul(xpLeft, 20L);
            long toExtractMb = Math.min(needMbRaw, availableMb);
            toExtractMb = (toExtractMb / 20L) * 20L;

            if (toExtractMb <= 0) continue;

            long simMb = StorageHelper.poweredExtraction(
                    energy,
                    storage,
                    fluid,
                    toExtractMb,
                    source,
                    Actionable.SIMULATE
            );
            if (simMb < toExtractMb) return false;

            fluidsPlanned.put(fluid, toExtractMb);
            xpLeft -= (toExtractMb / 20L);
        }

        if (xpLeft > 0) return false;

        long shardsDone = StorageHelper.poweredExtraction(
                energy,
                storage,
                AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()),
                shardsPlanned,
                source,
                Actionable.MODULATE
        );
        if (shardsDone < shardsPlanned) return false;

        for (var e : fluidsPlanned.entrySet()) {
            long doneMb = StorageHelper.poweredExtraction(
                    energy,
                    storage,
                    e.getKey(),
                    e.getValue(),
                    source,
                    Actionable.MODULATE
            );
            if (doneMb < e.getValue()) return false;
        }

        return true;
    }

    @Override
    public void refreshXpForMenu() {
        if (this.menu == null) return;

        IGridNode node = getGridNode();
        if (node == null || node.getGrid() == null) return;

        var storage = node.getGrid().getStorageService().getInventory();
        var source = IActionSource.ofMachine(this);

        long totalXp;

        long shardCount = storage.extract(
                AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()),
                Long.MAX_VALUE,
                Actionable.SIMULATE,
                source
        );

        totalXp = safeMul(shardCount, XpShardItem.XP_VAL);

        for (AEFluidKey fluid : getAvailableXpFluids()) {
            long fluidAmount = storage.extract(fluid, Long.MAX_VALUE, Actionable.SIMULATE, source);
            long xpFromFluid = fluidAmount / 20L;

            long newTotal = totalXp + xpFromFluid;
            totalXp = (newTotal < totalXp) ? Long.MAX_VALUE : newTotal;
        }

        this.xp = (int) Math.min(totalXp, Integer.MAX_VALUE);
        this.menu.xp = this.xp;
    }

    @Override
    public ItemStack performEnchant(ItemStack input, int option) {
        ItemStack lapis = lapisInv.getStackInSlot(0);

        if (input.isEmpty()
                || (!input.isEnchantable() && input.getItem() != Items.BOOK)
                || lapis.isEmpty()
                || lapis.getItem() != Items.LAPIS_LAZULI
                || lapis.getCount() < option) {
            return input;
        }

        IGridNode node = getGridNode();
        if (node == null || !node.isActive() || node.getGrid() == null) {
            return input;
        }

        BlockPos tablePos = this.getBlockPos().above().above();
        EnchStats stats = getEnchantStats(tablePos);

        RandomSource random = RandomSource.create();

        int enchantLevel = RealEnchantmentHelper.getEnchantmentCost(random, option, stats.eterna(), input);
        if (enchantLevel <= 0) return input;

        List<EnchantmentInstance> enchantments = RealEnchantmentHelper.selectEnchantment(
                random,
                input,
                enchantLevel,
                stats.quanta(),
                stats.arcana(),
                stats.eterna(),
                stats.treasure(),
                Set.of()
        );
        if (enchantments.isEmpty()) {
            return input;
        }

        long fullXpRequired = levelToXpLong(enchantLevel);
        long base = Math.max(1L, fullXpRequired / 100L);
        long mult = (long) CrazyConfig.COMMON.AutoEnchanterCost.get();
        long xpToConsume = safeMul(base, mult);

        if (!consumeXpFromNetworkAtomically(xpToConsume)) {
            return input;
        }

        refreshXpForMenu();

        ItemStack result;
        if (input.getItem() == Items.BOOK) {
            result = new ItemStack(Items.ENCHANTED_BOOK);
            for (EnchantmentInstance inst : enchantments) {
                EnchantedBookItem.addEnchantment(result, inst);
            }
        } else {
            result = input.copy();
            for (EnchantmentInstance inst : enchantments) {
                result.enchant(inst.enchantment, inst.level);
            }
        }

        lapis.shrink(option);
        return result;
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        super.tickingRequest(node, ticksSinceLastCall);

        int enchLevel = getXpCostForEnchant(this.inputInv.getStackInSlot(0), this.option);
        long display = safeMul(levelToXpLong(enchLevel), (long) CrazyConfig.COMMON.AutoEnchanterCost.get());

        this.levelCost = Utils.shortenNumber(display);

        if (this.menu != null) {
            this.menu.levelCost = this.levelCost;
        }

        return TickRateModulation.IDLE;
    }
}
