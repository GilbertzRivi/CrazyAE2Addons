package net.oktawia.crazyae2addons.compat.Apotheosis;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import dev.shadowsoffire.apotheosis.ench.objects.TreasureShelfBlock;
import dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApothAutoEnchanterBE extends AutoEnchanterBE {

    public ApothAutoEnchanterBE(BlockPos pos, BlockState blockState) {
        super(pos, blockState);
    }

    public record EnchStats(float eterna, float quanta, float arcana, int clues, boolean treasure){}

    public EnchStats getEnchantStats(BlockPos tablePos) {
        int radius = 2;
        float eterna = 0;
        float quanta = 0;
        float arcana = 0;
        int clues = 0;
        boolean treasure = false;

        for (BlockPos pos : BlockPos.betweenClosed(tablePos.offset(-radius, 0, -radius), tablePos.offset(radius, 1, radius))) {
            BlockState state = getLevel().getBlockState(pos);
            eterna += EnchantingStatRegistry.getEterna(state, getLevel(), pos);
            quanta += EnchantingStatRegistry.getQuanta(state, getLevel(), pos);
            arcana += EnchantingStatRegistry.getArcana(state, getLevel(), pos);
            clues += EnchantingStatRegistry.getBonusClues(state, getLevel(), pos);
            if (!treasure && getLevel().getBlockState(pos).getBlock() instanceof TreasureShelfBlock) {
                treasure = true;
            }
        }

        return new EnchStats(eterna, quanta, arcana, clues, treasure);
    }

    public int getXpCostForEnchant(ItemStack input) {
        if (input.isEmpty() || (!input.isEnchantable() && input.getItem() != Items.BOOK)) {
            return 0;
        }

        EnchStats stats = getEnchantStats(this.getBlockPos().above());
        RandomSource random = RandomSource.create();

        int enchantLevel = RealEnchantmentHelper.getEnchantmentCost(random, this.option, stats.eterna(), input);
        return Math.max(enchantLevel, 0);
    }

    @Override
    public ItemStack performEnchant(ItemStack input, int option) {
        ItemStack lapis = lapisInv.getStackInSlot(0);

        if (input.isEmpty()
                || (!input.isEnchantable() && input.getItem() != Items.BOOK)
                || lapis.getCount() <= 0
                || lapis.getItem() != Items.LAPIS_LAZULI) {
            return input;
        }

        EnchStats stats = getEnchantStats(this.getBlockPos().above().above());
        RandomSource random = RandomSource.create();

        int enchantLevel = getXpCostForEnchant(input);
        int fullXpRequired = levelToXp(enchantLevel);
        int xpToConsume = Math.max(1, fullXpRequired / 100) * CrazyConfig.COMMON.AutoEnchanterCost.get();

        var grid = getGridNode().getGrid();
        var energy = grid.getEnergyService();
        var storage = grid.getStorageService().getInventory();
        var source = IActionSource.ofMachine(this);

        long shardCount = storage.extract(
                AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()),
                Integer.MAX_VALUE,
                Actionable.SIMULATE,
                source
        );
        int xpFromShards = (int) Math.min(shardCount, Integer.MAX_VALUE) * XpShardItem.XP_VAL;

        int xpFromFluids = 0;
        Map<AEFluidKey, Long> candidateFluids = new HashMap<>();

        for (AEFluidKey fluid : getAvailableXpFluids()) {
            long mB = storage.extract(fluid, Integer.MAX_VALUE, Actionable.SIMULATE, source);
            xpFromFluids += (int) (mB / 20);
            candidateFluids.put(fluid, mB);
        }

        int totalXpAvailable = xpFromShards + xpFromFluids;

        if (totalXpAvailable < fullXpRequired * CrazyConfig.COMMON.AutoEnchanterCost.get()) {
            return input;
        }

        int xpLeft = xpToConsume;

        int shardsToExtract = Math.min(xpLeft / XpShardItem.XP_VAL, (int) shardCount);
        long extractedShards = StorageHelper.poweredExtraction(
                energy,
                storage,
                AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()),
                shardsToExtract,
                source,
                Actionable.MODULATE
        );
        xpLeft -= extractedShards * XpShardItem.XP_VAL;

        if (xpLeft > 0) {
            int fluidMbLeft = xpLeft * 20;

            for (Map.Entry<AEFluidKey, Long> entry : candidateFluids.entrySet()) {
                AEFluidKey fluidKey = entry.getKey();
                long availableMb = entry.getValue();

                long toExtractMb = Math.min(fluidMbLeft, availableMb);
                long extractedMb = StorageHelper.poweredExtraction(
                        energy,
                        storage,
                        fluidKey,
                        toExtractMb,
                        source,
                        Actionable.MODULATE
                );

                fluidMbLeft -= extractedMb;
                if (fluidMbLeft <= 0) break;
            }
        }

        if (this.menu != null) {
            long totalXp = shardCount * XpShardItem.XP_VAL;
            for (Map.Entry<AEFluidKey, Long> entry : candidateFluids.entrySet()) {
                totalXp += entry.getValue() / 20;
            }
            this.xp = (int) Math.min(totalXp, Integer.MAX_VALUE);
            this.menu.xp = this.xp;
        }

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
        this.levelCost = Utils.shortenNumber(levelToXp(getXpCostForEnchant(this.inputInv.getStackInSlot(0))) * CrazyConfig.COMMON.AutoEnchanterCost.get());
        if (this.menu != null){
            this.menu.levelCost = this.levelCost;
        }
        return TickRateModulation.IDLE;
    }
}
