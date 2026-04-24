package net.oktawia.crazyae2addons.items.part;

import appeng.items.parts.PartItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.parts.EmitterTerminal;
import net.oktawia.crazyae2addons.parts.RedstoneTerminal;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RedstoneTerminalPartItem extends PartItem<RedstoneTerminal> {

    public RedstoneTerminalPartItem(Properties props) {
        super(props, RedstoneTerminal.class, RedstoneTerminal::new);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag advancedTooltips) {
        super.appendHoverText(stack, level, tooltip, advancedTooltips);

        if (!CrazyConfig.COMMON.REDSTONE_EMITTER_TERMINAL_ENABLED.get()) {
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED.getTranslationKey())
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED_CONFIG.getTranslationKey())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}