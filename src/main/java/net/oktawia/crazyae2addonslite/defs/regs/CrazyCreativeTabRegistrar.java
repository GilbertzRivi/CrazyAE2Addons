package net.oktawia.crazyae2addonslite.defs.regs;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseBlockItem;
import appeng.items.AEBaseItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;

public final class CrazyCreativeTabRegistrar {

    public static final ResourceLocation ID = CrazyAddonslite.makeId("tab");

    public static final CreativeModeTab TAB = CreativeModeTab.builder()
            .title(Component.literal("Crazy AE2 Addons (lite)"))
            .icon(() -> new ItemStack(CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get()))
            .displayItems(CrazyCreativeTabRegistrar::populate)
            .build();

    private static void populate(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output out) {
        CrazyItemRegistrar.ITEMS.getEntries().forEach(h -> push(params, out, h.get()));
        CrazyBlockRegistrar.BLOCK_ITEMS.getEntries().forEach(h -> push(params, out, h.get()));
    }

    private static void push(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output out, Item item) {
        if (item instanceof AEBaseBlockItem bItem && bItem.getBlock() instanceof AEBaseBlock blk) {
            blk.addToMainCreativeTab(params, out);
        } else if (item instanceof AEBaseItem baseItem) {
            baseItem.addToMainCreativeTab(params, out);
        } else {
            out.accept(item);
        }
    }

    private CrazyCreativeTabRegistrar() {}
}
