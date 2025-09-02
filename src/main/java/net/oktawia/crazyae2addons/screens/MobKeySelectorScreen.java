package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.menus.MobKeySelectorMenu;

import java.util.*;

public class MobKeySelectorScreen<C extends MobKeySelectorMenu> extends AEBaseScreen<C> {
    private Scrollbar scroll;
    private final List<Button> btns = new ArrayList<>();
    private final List<String> allIds;
    private List<String> filtered;
    private int lastScroll = -1;
    private boolean initialized;

    public MobKeySelectorScreen(C menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);

        this.allIds = ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .filter(et -> et.getCategory() != MobCategory.MISC)
                .map(ForgeRegistries.ENTITY_TYPES::getKey)
                .filter(Objects::nonNull)
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
        this.filtered = allIds;

        AETextField search = new AETextField(this.style, Minecraft.getInstance().font, 0, 0, 0, 0);
        search.setPlaceholder(Component.literal("Search mob..."));
        search.setResponder(q -> {
            String s = q.toLowerCase().trim();
            this.filtered = s.isEmpty()
                    ? allIds
                    : allIds.stream().filter(id -> id.toLowerCase(Locale.ROOT).contains(s)).toList();
            scroll.setCurrentScroll(0);
            refreshPage();
        });
        search.setBordered(false);
        this.widgets.add("search", search);

        // scroll z JSON
        this.scroll = new Scrollbar();
        this.widgets.add("scroll", this.scroll);
        this.scroll.setRange(0, maxScroll(), 1);

        for (int i = 0; i < 6; i++) {
            int idx = i;
            Button b = Button.builder(Component.empty(), btn -> onPress(idx))
                    .pos(0, 0).size(0, 0).build();
            this.widgets.add("b" + idx, b);
            btns.add(b);
        }
    }

    private int maxScroll() {
        int visible = btns.size();
        int total = filtered.size();
        return Math.max(0, total - visible);
    }

    private void onPress(int visibleIndex) {
        int idx = scroll.getCurrentScroll() + visibleIndex;
        if (idx < 0 || idx >= filtered.size()) return;
        String id = filtered.get(idx);
        menu.choose(id);
        refreshPage();
    }

    private void refreshPage() {
        int offset = scroll.getCurrentScroll();
        for (int i = 0; i < btns.size(); i++) {
            Button b = btns.get(i);
            int idx = offset + i;
            if (idx < filtered.size()) {
                String id = filtered.get(idx);
                b.active = true;
                b.setMessage(Component.literal(id));
                b.setTooltip(id.equals(menu.selectedKey) ? Tooltip.create(Component.literal("Selected")) : null);
            } else {
                b.active = false;
                b.setMessage(Component.empty());
                b.setTooltip(null);
            }
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        int max = maxScroll();
        if (scroll.getCurrentScroll() > max) scroll.setCurrentScroll(max);
        scroll.setRange(0, max, 1);

        int cur = scroll.getCurrentScroll();
        if (cur != lastScroll) {
            lastScroll = cur;
            refreshPage();
        }

        if (!initialized) {
            initialized = true;
            refreshPage();
        }
    }
}
