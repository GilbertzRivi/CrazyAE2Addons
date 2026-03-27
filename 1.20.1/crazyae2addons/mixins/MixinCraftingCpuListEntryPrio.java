package net.oktawia.crazyae2addons.mixins;

import appeng.menu.me.crafting.CraftingStatusMenu.CraftingCpuListEntry;
import net.oktawia.crazyae2addons.interfaces.ICpuPrio;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingCpuListEntry.class, remap = false)
public class MixinCraftingCpuListEntryPrio implements ICpuPrio {

    @Unique
    private int prio;

    @Override
    public int getPrio() {
        return prio;
    }

    @Override
    public void setPrio(int prio) {
        this.prio = prio;
    }

    @Inject(method = "writeToPacket", at = @At("TAIL"))
    private void writePrio(FriendlyByteBuf buf, CallbackInfo ci) {
        buf.writeVarInt(prio);
    }

    @Inject(method = "readFromPacket", at = @At("TAIL"))
    private static void readPrio(FriendlyByteBuf buf,
                                                CallbackInfoReturnable<CraftingCpuListEntry> cir) {
        var ret = cir.getReturnValue();
        ((ICpuPrio) (Object) ret).setPrio(buf.readVarInt());
    }
}
