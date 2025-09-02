package net.oktawia.crazyae2addons.mixins;

import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.Tooltip;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.me.crafting.CraftingStatusMenu;
import appeng.menu.me.crafting.CraftingStatusMenu.CraftingCpuListEntry;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.oktawia.crazyae2addons.interfaces.ICpuPrio;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mixin(value = CPUSelectionList.class, remap = false)
public abstract class MixinCPUSelectionList {

    @Shadow private CraftingStatusMenu menu;
    @Shadow private Rect2i bounds;
    @Shadow private Blitter buttonBg;
    @Shadow private Scrollbar scrollbar;
    @Unique private CraftingCpuListEntry lastHitCpu;
    @Shadow abstract protected CraftingCpuListEntry hitTestCpu(Point mousePos);

    @Redirect(
            method = "drawBackgroundLayer",
            at = @At(value = "INVOKE", target = "Ljava/util/List;subList(II)Ljava/util/List;")
    )
    private List<CraftingCpuListEntry> sortThenSlice(List<CraftingCpuListEntry> list, int from, int to) {

        var sorted = list.stream()
                .sorted(
                        Comparator
                                .comparingInt((CraftingCpuListEntry e) -> {
                                    Object o = e;
                                    return (o instanceof ICpuPrio p) ? p.getPrio() : 0;
                                })
                                .reversed()
                                .thenComparing((a, b)-> {
                                    var name1 = a.name() == null ? "CPU" : a.name().getString();
                                    var name2 = b.name() == null ? "CPU" : b.name().getString();
                                    return name1.compareToIgnoreCase(name2);
                                })
                                .thenComparingInt(CraftingCpuListEntry::serial)
                )
                .toList();

        int f = Mth.clamp(from, 0, sorted.size());
        int t = Mth.clamp(to, 0, sorted.size());
        return sorted.subList(f, t);
    }

    @Inject(method = "hitTestCpu", at = @At("HEAD"), cancellable = true)
    private void hitTestOnSorted(Point mousePos, CallbackInfoReturnable<CraftingCpuListEntry> cir) {
        int relX = mousePos.getX() - bounds.getX();
        int relY = mousePos.getY() - bounds.getY();

        relX -= 9;
        if (relX < 0 || relX >= buttonBg.getSrcWidth()) {
            cir.setReturnValue(null);
            return;
        }

        relY -= 19;
        int rowH = buttonBg.getSrcHeight() + 1;
        int buttonIdx = scrollbar.getCurrentScroll() + relY / rowH;

        if (relY % rowH == buttonBg.getSrcHeight()) {
            cir.setReturnValue(null);
            return;
        }
        if (relY < 0) {
            cir.setReturnValue(null);
            return;
        }

        List<CraftingCpuListEntry> base = menu.cpuList.cpus();
        if (base.isEmpty()) {
            cir.setReturnValue(null);
            return;
        }

        List<CraftingCpuListEntry> sorted = base.stream()
                .sorted(
                        Comparator
                                .comparingInt((CraftingCpuListEntry e) -> {
                                    Object o = e;
                                    return (o instanceof ICpuPrio p) ? p.getPrio() : 0;
                                })
                                .reversed()
                                .thenComparing((a, b)-> {
                                    var name1 = a.name() == null ? "CPU" : a.name().getString();
                                    var name2 = b.name() == null ? "CPU" : b.name().getString();
                                    return name1.compareToIgnoreCase(name2);
                                })
                                .thenComparingInt(CraftingCpuListEntry::serial)
                )
                .toList();

        if (buttonIdx < 0 || buttonIdx >= sorted.size()) {
            cir.setReturnValue(null);
            return;
        }

        cir.setReturnValue(sorted.get(buttonIdx));
    }

    @Inject(method = "getTooltip", at = @At("HEAD"))
    private void captureCpu(int mouseX, int mouseY,
                                           CallbackInfoReturnable<Tooltip> cir) {
        this.lastHitCpu = hitTestCpu(new Point(mouseX, mouseY));
    }

    @ModifyArg(
            method = "getTooltip",
            at = @At(value = "INVOKE",
                    target = "Lappeng/client/gui/Tooltip;<init>(Ljava/util/List;)V"),
            index = 0
    )
    private List<Component> appendCpuInfo(List<Component> lines) {
        var cpu = this.lastHitCpu;
        if (cpu != null) {
            Object o = cpu;
            int prio = (o instanceof ICpuPrio p) ? p.getPrio() : 0;

            lines.add(Component.literal("Priority: " + prio));
        }
        return lines;
    }

}
