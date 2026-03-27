package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.defs.regs.CrazyRecipes;
import net.oktawia.crazyae2addons.items.DataDrive;
import net.oktawia.crazyae2addons.menus.DataDriveMenu;
import net.oktawia.crazyae2addons.recipes.ResearchRecipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class DataDriveScreen<C extends DataDriveMenu> extends AEBaseScreen<C> {

    private final Scrollbar pageScroll;

    private final ResearchCardWidget card1 = new ResearchCardWidget();
    private final ResearchCardWidget card2 = new ResearchCardWidget();
    private final ResearchCardWidget card3 = new ResearchCardWidget();
    private final ResearchCardWidget card4 = new ResearchCardWidget();

    private final List<Entry> entries = new ArrayList<>();
    private boolean initialized = false;

    private static final int CARDS_PER_PAGE = 4;

    public DataDriveScreen(C menu, net.minecraft.world.entity.player.Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);

        pageScroll = new Scrollbar();
        widgets.add("page_scroll", pageScroll);

        widgets.add("card1", card1);
        widgets.add("card2", card2);
        widgets.add("card3", card3);
        widgets.add("card4", card4);
    }

    @Override
    public void updateBeforeRender() {
        super.updateBeforeRender();

        if (!initialized) {
            buildOnce();
            setupPager();
            initialized = true;
        } else {
            setupPager();
        }

        int startIndex = Math.min(pageScroll.getCurrentScroll(), maxStart());
        fillFrom(startIndex);
    }

    private void buildOnce() {
        entries.clear();

        ItemStack drive = menu.host.getItemStack();
        Set<ResourceLocation> unlocked = DataDrive.getUnlocked(drive);

        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        RecipeManager rm = level.getRecipeManager();
        var type = CrazyRecipes.RESEARCH_TYPE.get();
        List<ResearchRecipe> list = rm.getAllRecipesFor(type);

        for (ResearchRecipe r : list) {
            Entry e = new Entry();
            e.key = r.unlock.key;
            e.label = r.unlock.label == null ? "" : r.unlock.label;
            e.unlocked = unlocked.contains(e.key);
            entries.add(e);
        }

        entries.sort(Comparator
                .comparing((Entry e) -> !e.unlocked)
                .thenComparing(e -> e.label == null || e.label.isEmpty() ? e.key.toString() : e.label,
                        String.CASE_INSENSITIVE_ORDER));
    }

    private void setupPager() {
        int max = Math.max(0, maxStart());
        pageScroll.setRange(0, max, 1);
        if (pageScroll.getCurrentScroll() > max) pageScroll.setCurrentScroll(max);
    }

    private int maxStart() {
        return Math.max(0, entries.size() - CARDS_PER_PAGE);
    }

    private void fillFrom(int start) {
        ResearchCardWidget[] cards = { card1, card2, card3, card4 };

        for (var c : cards) c.clear();

        for (int i = 0; i < CARDS_PER_PAGE; i++) {
            int idx = start + i;
            if (idx < entries.size()) {
                cards[i].set(entries.get(idx));
            }
        }
    }

    private static final class Entry {
        ResourceLocation key;
        String label;
        boolean unlocked;
    }

    private static final class ResearchCardWidget extends AbstractWidget {

        private String title = "";
        private int titleColor = 0xFF000000;
        private boolean empty = true;

        ResearchCardWidget() {
            super(0, 0, 0, 0, Component.empty());
            setFocused(false);
        }

        void clear() {
            empty = true;
            title = "";
            titleColor = 0xFF000000;
        }

        void set(Entry e) {
            empty = false;

            String name = (e.label == null || e.label.isEmpty()) ? e.key.toString() : e.label;
            if (e.unlocked) {
                title = "\u2714 " + name;
                titleColor = 0xFF20C020;
            } else {
                title = "\u2716 " + name;
                titleColor = 0xFFE04A4A;
            }
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x1 = getX(), y1 = getY(), x2 = x1 + getWidth(), y2 = y1 + getHeight();

            // tło + ramka
            g.fill(x1, y1, x2, y2, 0x7F101010);
            g.fill(x1, y1, x2, y1 + 1, 0xFF606060);
            g.fill(x1, y2 - 1, x2, y2, 0xFF606060);
            g.fill(x1, y1, x1 + 1, y2, 0xFF606060);
            g.fill(x2 - 1, y1, x2, y2, 0xFF606060);

            if (empty) return;

            var font = Minecraft.getInstance().font;
            int x = x1 + 6;
            int y = y1 + 4;
            g.drawString(font, title, x, y, titleColor, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
        }
    }
}
