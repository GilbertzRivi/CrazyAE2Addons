package net.oktawia.crazyae2addonslite.misc;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class StockThresholdToast implements Toast {
    private static final long TIME_VISIBLE = 3000;
    private static final int TITLE_COLOR = 0xFF404040;
    private static final int TEXT_COLOR = 0xFF000000;

    private static final String TR_TITLE = "gui.crazyae2addons.toast.stock_alert_title";
    private static final String TR_ABOVE = "gui.crazyae2addons.toast.stock_alert_above";
    private static final String TR_BELOW = "gui.crazyae2addons.toast.stock_alert_below";

    private static final String TR_AMOUNT_UNIT_INT = "gui.crazyae2addons.amount.unit_int";
    private static final String TR_AMOUNT_UNIT_DEC_INTERNAL = "gui.crazyae2addons.amount.unit_decimal_with_internal";

    private final AEKey what;
    private final List<FormattedCharSequence> lines;
    private final int height;

    public StockThresholdToast(AEKey what, boolean wentAbove, long threshold, long amountNow) {
        this.what = what;

        var minecraft = Minecraft.getInstance();
        var font = minecraft.font;

        String thStr = formatAmount(threshold, what);
        String nowStr = formatAmount(amountNow, what);

        Component text = Component.translatable(
                wentAbove ? TR_ABOVE : TR_BELOW,
                thStr,
                nowStr
        );

        this.lines = font.split(text, width() - 30 - 5);
        this.height = Toast.super.height() + (lines.size() - 1) * font.lineHeight;
    }

    private static String formatAmount(long technical, AEKey key) {
        String sym = key.getUnitSymbol();
        long per = key.getAmountPerUnit();

        String techStr = String.format("%,d", technical);

        if (sym == null || sym.isBlank() || per <= 1) {
            return techStr;
        }

        if (technical % per == 0) {
            String unitNum = String.format("%,d", (technical / per));
            return Component.translatable(TR_AMOUNT_UNIT_INT, unitNum, sym).getString();
        }

        BigDecimal v = BigDecimal.valueOf(technical)
                .divide(BigDecimal.valueOf(per), 3, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        return Component.translatable(TR_AMOUNT_UNIT_DEC_INTERNAL, v.toPlainString(), sym, techStr).getString();
    }

    @Override
    public Visibility render(GuiGraphics gg, ToastComponent tc, long timeSinceLastVisible) {
        var minecraft = Minecraft.getInstance();
        var font = minecraft.font;

        gg.blit(TEXTURE, 0, 0, 0, 32, this.width(), 8);
        int middleHeight = height - 16;
        for (int middleY = 0; middleY < middleHeight; middleY += 16) {
            int tileHeight = Math.min(middleHeight - middleY, 16);
            gg.blit(TEXTURE, 0, 8 + middleY, 0, 32 + 8, this.width(), tileHeight);
        }
        gg.blit(TEXTURE, 0, height - 8, 0, 32 + 32 - 8, this.width(), 8);

        gg.drawString(tc.getMinecraft().font, Component.translatable(TR_TITLE), 30, 7, TITLE_COLOR, false);

        int lineY = 18;
        for (var line : lines) {
            gg.drawString(tc.getMinecraft().font, line, 30, lineY, TEXT_COLOR, false);
            lineY += font.lineHeight;
        }

        AEKeyRendering.drawInGui(minecraft, gg, 8, 8, what);

        return timeSinceLastVisible >= TIME_VISIBLE ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public int height() {
        return height;
    }
}
