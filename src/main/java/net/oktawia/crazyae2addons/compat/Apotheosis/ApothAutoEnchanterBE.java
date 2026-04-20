package net.oktawia.crazyae2addons.compat.Apotheosis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.entities.AutoEnchanterBE;

import java.util.List;

public class ApothAutoEnchanterBE extends AutoEnchanterBE {

    public ApothAutoEnchanterBE(BlockPos pos, BlockState blockState) {
        super(pos, blockState);
    }

    @Override
    protected int computeEnchantLevel(ItemStack input, int option) {
        if (!isValidEnchantingInput(input) || !hasEnchantingTable()) {
            return 0;
        }

        EnchantmentTableStats stats = EnchantmentTableStats.gatherStats(
                getLevel(),
                getEnchantingTablePos(),
                input.getEnchantmentValue()
        );

        return Math.max(
                ApothEnchantmentHelper.getEnchantmentCost(RandomSource.create(), option, stats.eterna(), input),
                0
        );
    }

    @Override
    protected List<EnchantmentInstance> selectEnchantments(ItemStack input, int option, int enchantLevel) {
        EnchantmentTableStats stats = EnchantmentTableStats.gatherStats(
                getLevel(),
                getEnchantingTablePos(),
                input.getEnchantmentValue()
        );

        return ApothEnchantmentHelper.selectEnchantment(
                RandomSource.create(),
                input,
                enchantLevel,
                stats,
                getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
        );
    }

    @Override
    protected ItemStack buildEnchantResult(ItemStack input, List<EnchantmentInstance> enchantments) {
        ItemStack result = input.getItem() == Items.BOOK
                ? new ItemStack(Items.ENCHANTED_BOOK)
                : input.copyWithCount(1);

        for (EnchantmentInstance inst : enchantments) {
            result.enchant(inst.enchantment, inst.level);
        }

        return result;
    }
}