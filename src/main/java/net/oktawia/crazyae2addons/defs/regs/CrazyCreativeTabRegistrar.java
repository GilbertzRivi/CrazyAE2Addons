package net.oktawia.crazyae2addons.defs.regs;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseBlockItem;
import appeng.items.AEBaseItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.CrazyAddons;

public final class CrazyCreativeTabRegistrar {

    public static final ResourceLocation ID = CrazyAddons.makeId("tab");

    public static final CreativeModeTab TAB = CreativeModeTab.builder()
            .title(Component.literal("Crazy AE2 Addons"))
            .icon(() -> new ItemStack(
                    CrazyBlockRegistrar.DATA_PROCESSOR_BLOCK.get()))
            .displayItems(CrazyCreativeTabRegistrar::populate)
            .build();

    private static void populate(CreativeModeTab.ItemDisplayParameters ignored,
                                 CreativeModeTab.Output out) {

        CrazyItemRegistrar.ITEMS.getEntries().forEach(ro -> push(out, ro.get()));

        CrazyBlockRegistrar.BLOCK_ITEMS.getEntries()
                .forEach(ro -> push(out, ro.get()));
    }

    private static void push(CreativeModeTab.Output out, net.minecraft.world.item.Item item) {
        if (item instanceof AEBaseBlockItem bItem && bItem.getBlock() instanceof AEBaseBlock blk) {
            blk.addToMainCreativeTab(out);
        } else if (item instanceof AEBaseItem baseItem) {
            baseItem.addToMainCreativeTab(out);
        } else {
            out.accept(item);
        }
    }

    private CrazyCreativeTabRegistrar() {}
}
