package net.oktawia.crazyae2addons.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.screens.CrazyPatternProviderScreen;
import net.oktawia.crazyae2addons.screens.DataflowPatternScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TagPacket {
    private final CompoundTag tag;

    public TagPacket(CompoundTag tag) {
        this.tag = tag;
    }

    public static void encode(TagPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.tag);
    }

    public static TagPacket decode(FriendlyByteBuf buf) {
        return new TagPacket(buf.readNbt());
    }

    public static void handle(TagPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof DataflowPatternScreen<?> screen) {
                screen.loadFromTag(packet.tag);
            }
        });
        ctx.setPacketHandled(true);
    }
}
