package net.oktawia.crazyae2addonslite.mixins;

import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.WidgetContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin(WidgetContainer.class)
public interface WidgetContainerAccessor {
    @Accessor("compositeWidgets")
    Map<String, ICompositeWidget> getCompositeWidgets();
}
