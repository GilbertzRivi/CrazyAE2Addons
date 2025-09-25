package net.oktawia.crazyae2addons.defs.regs;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseBlockItem;
import appeng.items.AEBaseItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.oktawia.crazyae2addons.CrazyAddons;

public final class CrazyCreativeTabRegistrar {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CrazyAddons.MODID);

    public static final ResourceLocation ID = CrazyAddons.makeId("tab");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            TABS.register("tab", () -> CreativeModeTab.builder()
                    .title(Component.literal("Crazy AE2 Addons"))
                    .icon(() -> new ItemStack(CrazyBlockRegistrar.DATA_PROCESSOR_BLOCK.get()))
                    .displayItems(CrazyCreativeTabRegistrar::populate)
                    .build()
            );

    private static void populate(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output out) {
        CrazyItemRegistrar.ITEMS.getEntries().forEach(holder -> {
            Item item = holder.get();
            if (!item.equals(CrazyItemRegistrar.MOB_KEY_ITEM.get())) {
                push(params, out, item);
            }
        });

        CrazyBlockRegistrar.BLOCK_ITEMS.getEntries().forEach(holder -> push(params, out, holder.get()));

        push(params, out, CrazyFluidRegistrar.RESEARCH_FLUID_BUCKET.get());
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
