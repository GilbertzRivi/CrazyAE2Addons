package net.oktawia.crazyae2addons.xei.jei;

import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.xei.common.CrazyEntry;
import net.oktawia.crazyae2addons.xei.common.CrazyPreview;

import java.util.List;

public class CrazyWrapper extends ModularWrapper<CrazyPreview> {
    public final ResourceLocation structureId;
    public final List<ItemStack> requiredItems;
    public final String name;

    public CrazyWrapper(CrazyEntry entry) {
        super(CrazyPreview.getPreviewWidget(entry.structureId(), entry.requiredItems(), entry.name().getString()));
        this.structureId = entry.structureId();
        this.requiredItems = entry.requiredItems();
        this.name = entry.name().getString();
        CrazyJeiPlugin.currentEntry = entry;
    }
}

