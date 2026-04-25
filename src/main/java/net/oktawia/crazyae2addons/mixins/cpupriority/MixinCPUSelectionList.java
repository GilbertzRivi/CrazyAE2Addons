package net.oktawia.crazyae2addons.mixins.cpupriority;

import appeng.client.Point;
import appeng.client.gui.Tooltip;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.me.crafting.CraftingStatusMenu;
import appeng.menu.me.crafting.CraftingStatusMenu.CraftingCpuListEntry;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.logic.cpupriority.CpuPriorityHelper;
import net.oktawia.crazyae2addons.logic.interfaces.ICpuPrio;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = CPUSelectionList.class, remap = false)
public abstract class MixinCPUSelectionList {

    @Final
    @Shadow
    private CraftingStatusMenu menu;

    @Shadow
    private Rect2i bounds;

    @Final
    @Shadow
    private Blitter buttonBg;

    @Final
    @Shadow
    private Scrollbar scrollbar;

    @Shadow
    protected abstract CraftingCpuListEntry hitTestCpu(Point mousePos);

    @Unique
    private CraftingCpuListEntry crazyae2addons$lastHitCpu;

    @Redirect(
            method = "drawBackgroundLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;subList(II)Ljava/util/List;"
            )
    )
    private List<CraftingCpuListEntry> crazyae2addons$sortThenSlice(
            List<CraftingCpuListEntry> list,
            int from,
            int to
    ) {
        if (!CrazyConfig.COMMON.CPU_PRIORITIES_ENABLED.get()) {
            return list.subList(from, to);
        }

        var sorted = CpuPriorityHelper.sortEntries(list);

        int safeFrom = Mth.clamp(from, 0, sorted.size());
        int safeTo = Mth.clamp(to, 0, sorted.size());

        return sorted.subList(safeFrom, safeTo);
    }

    @Inject(method = "hitTestCpu", at = @At("HEAD"), cancellable = true)
    private void crazyae2addons$hitTestOnSorted(Point mousePos, CallbackInfoReturnable<CraftingCpuListEntry> cir) {
        if (!CrazyConfig.COMMON.CPU_PRIORITIES_ENABLED.get()) {
            return;
        }

        int relX = mousePos.getX() - bounds.getX() - 9;
        int relY = mousePos.getY() - bounds.getY() - 19;

        if (relX < 0 || relX >= buttonBg.getSrcWidth()) {
            cir.setReturnValue(null);
            return;
        }

        if (relY < 0) {
            cir.setReturnValue(null);
            return;
        }

        int rowHeight = buttonBg.getSrcHeight() + 1;
        if (relY % rowHeight == buttonBg.getSrcHeight()) {
            cir.setReturnValue(null);
            return;
        }

        var base = menu.cpuList.cpus();
        if (base.isEmpty()) {
            cir.setReturnValue(null);
            return;
        }

        var sorted = CpuPriorityHelper.sortEntries(base);
        int index = scrollbar.getCurrentScroll() + relY / rowHeight;

        if (index < 0 || index >= sorted.size()) {
            cir.setReturnValue(null);
            return;
        }

        cir.setReturnValue(sorted.get(index));
    }

    @Inject(method = "getTooltip", at = @At("HEAD"))
    private void crazyae2addons$captureCpu(
            int mouseX,
            int mouseY,
            CallbackInfoReturnable<Tooltip> cir
    ) {
        this.crazyae2addons$lastHitCpu = hitTestCpu(new Point(mouseX, mouseY));
    }

    @ModifyArg(
            method = "getTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/client/gui/Tooltip;<init>(Ljava/util/List;)V"
            ),
            index = 0
    )
    private List<Component> crazyae2addons$appendCpuInfo(List<Component> lines) {
        if (!CrazyConfig.COMMON.CPU_PRIORITIES_ENABLED.get()) {
            return lines;
        }

        var result = new ArrayList<>(lines);
        var cpu = this.crazyae2addons$lastHitCpu;

        if (cpu != null) {
            int prio = ((Object) cpu instanceof ICpuPrio p) ? p.getPrio() : 0;
            result.add(Component.literal("Priority: " + prio));
        }

        return result;
    }
}