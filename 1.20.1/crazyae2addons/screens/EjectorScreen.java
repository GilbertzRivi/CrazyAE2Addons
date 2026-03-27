package net.oktawia.crazyae2addons.screens;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.menu.SlotSemantics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.components.Renderable; // ← ważne
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.EjectorMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.SetConfigAmountPacket;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EjectorScreen<C extends EjectorMenu> extends AEBaseScreen<C> {

    private ItemStack missingIcon = ItemStack.EMPTY;
    private String missingCountText = "";
    private boolean parsedOk = false;

    private static final int MISSING_ICON_X = 80;
    private static final int MISSING_ICON_Y = 22;
    private String lastCantCraft = null;
    private boolean started = false;

    private static final Pattern AMOUNT_PREFIX =
            Pattern.compile("^\\s*([0-9][0-9_.,\\skKmMbBtT]*)\\s*[x×]\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern RL_FINDER =
            Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)", Pattern.CASE_INSENSITIVE);

    public EjectorScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        var btn = new IconButton(Icon.ENTER, b -> {
            getMenu().applyPatternToConfig();
        });
        btn.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.ejector_load_pattern")));
        this.widgets.add("load", btn);

        this.addRenderableOnly((gg, mouseX, mouseY, partialTicks) -> {
            if (parsedOk && !missingIcon.isEmpty()) {
                int x = leftPos + MISSING_ICON_X;
                int y = topPos + MISSING_ICON_Y;
                gg.renderItem(missingIcon, x, y);
                if (!missingCountText.isEmpty()) {
                    gg.renderItemDecorations(font, missingIcon, x, y, missingCountText);
                }
            }
        });
    }

    @Override
    public void updateBeforeRender() {
        super.updateBeforeRender();

        parseCantCraft(getMenu().cantCraft);

        if (parsedOk && !missingIcon.isEmpty()) {
            setTextContent("missing", Component.empty());
        } else {
            setTextContent("missing", getMenu().cantCraft == null
                    ? Component.translatable("gui.crazyae2addons.ejector_nothing")
                    : Component.literal(getMenu().cantCraft));
        }
    }

    private void parseCantCraft(String s) {
        parsedOk = false;
        missingIcon = ItemStack.EMPTY;
        missingCountText = "";

        if (s == null || s.isEmpty() || "nothing".equalsIgnoreCase(s)) return;

        s = s.replace('\u00A0', ' ').trim();

        String rest = s;

        Matcher mAmt = AMOUNT_PREFIX.matcher(s);
        if (mAmt.find()) {
            missingCountText = mAmt.group(1).trim();
            rest = s.substring(mAmt.end()).trim();
        }

        String cand = firstWord(rest);
        ResourceLocation rl = tryExtractRL(cand);

        if (rl == null) {
            rl = scanForRL(rest);
        }
        if (rl == null) {
            rl = scanForRL(s);
        }

        if (rl == null) return;

        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == Items.AIR) {
            item = BuiltInRegistries.ITEM.get(rl);
        }
        if (item == null || item == Items.AIR) {
            var block = ForgeRegistries.BLOCKS.getValue(rl);
            if (block == null || block == Blocks.AIR) {
                block = BuiltInRegistries.BLOCK.get(rl);
            }
            if (block != null && block != Blocks.AIR) {
                item = block.asItem();
            }
        }
        if (item == null || item == Items.AIR) return;

        missingIcon = new ItemStack(item);
        parsedOk = true;
    }

    private boolean isMouseOverMissingIcon(int mouseX, int mouseY) {
        int x = leftPos + MISSING_ICON_X;
        int y = topPos + MISSING_ICON_Y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private static String firstWord(String txt) {
        if (txt == null) return "";
        int i = 0, n = txt.length();
        while (i < n && Character.isWhitespace(txt.charAt(i))) i++;
        int j = i;
        while (j < n && !Character.isWhitespace(txt.charAt(j))) j++;
        return txt.substring(i, j);
    }

    private static ResourceLocation tryExtractRL(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String t = sanitizeRL(raw);
        if (!t.contains(":")) return null;
        return ResourceLocation.tryParse(t);
    }

    private static ResourceLocation scanForRL(String txt) {
        if (txt == null) return null;
        int idx = txt.indexOf(':');
        if (idx < 0) return null;

        java.util.function.IntPredicate ok = c ->
                Character.isLetterOrDigit(c) ||
                        c == '_' || c == '-' || c == '.' || c == '/' || c == ':';

        int l = idx - 1;
        int r = idx + 1;
        while (l >= 0 && ok.test(txt.charAt(l))) l--;
        while (r < txt.length() && ok.test(txt.charAt(r))) r++;

        String candidate = txt.substring(l + 1, r);
        return tryExtractRL(candidate);
    }

    private static String sanitizeRL(String raw) {
        String t = raw.trim();
        int start = 0, end = t.length();
        while (start < end && !isRlChar(t.charAt(start))) start++;
        while (end > start && !isRlChar(t.charAt(end - 1))) end--;
        t = t.substring(start, end);
        while (!t.isEmpty()) {
            char c = t.charAt(t.length() - 1);
            if (isRlChar(c)) break;
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    private static boolean isRlChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == '/' || c == ':';
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 2) {
            var slot = this.getSlotUnderMouse();
            if (slot != null && getMenu().canModifyAmountForSlot(slot)) {
                var gs = GenericStack.fromItemStack(slot.getItem());
                if (gs != null) {
                    this.setSlotsHidden(SlotSemantics.CONFIG, true);
                    this.setSlotsHidden(SlotSemantics.PLAYER_HOTBAR, true);
                    this.setSlotsHidden(SlotSemantics.PLAYER_INVENTORY, true);
                    this.minecraft.setScreen(new SetConfigAmountScreen<>(
                            this,
                            gs,
                            newStack -> {
                                if (newStack == null) {
                                    NetworkHandler.INSTANCE.sendToServer(
                                            new SetConfigAmountPacket(slot.index, 0L));
                                } else {
                                    NetworkHandler.INSTANCE.sendToServer(
                                            new SetConfigAmountPacket(slot.index, newStack.amount()));
                                }
                                this.setSlotsHidden(SlotSemantics.CONFIG, false);
                                this.setSlotsHidden(SlotSemantics.PLAYER_HOTBAR, false);
                                this.setSlotsHidden(SlotSemantics.PLAYER_INVENTORY, false);
                            }
                    ));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    protected void renderTooltip(net.minecraft.client.gui.GuiGraphics gg, int x, int y) {
        if (parsedOk && !missingIcon.isEmpty() && isMouseOverMissingIcon(x, y)) {
            gg.renderTooltip(this.font, missingIcon, x, y);
            return;
        }

        if (this.menu.getCarried().isEmpty() && this.menu.canModifyAmountForSlot(this.hoveredSlot)) {
            var lines = new java.util.ArrayList<>(getTooltipFromContainerItem(this.hoveredSlot.getItem()));
            lines.add(Component.translatable("gui.crazyae2addons.ejector_middle_click").withStyle(net.minecraft.ChatFormatting.GRAY));
            drawTooltip(gg, x, y, lines);
            return;
        }
        super.renderTooltip(gg, x, y);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        String s = getMenu().cantCraft;
        if (!java.util.Objects.equals(s, lastCantCraft)) {
            lastCantCraft = s;
            parseCantCraft(s);

            if (parsedOk && !missingIcon.isEmpty()) {
                setTextContent("missing", Component.empty());
            } else {
                setTextContent("missing", s == null
                        ? Component.translatable("gui.crazyae2addons.ejector_nothing")
                        : Component.literal(s));
            }
        }
    }
}