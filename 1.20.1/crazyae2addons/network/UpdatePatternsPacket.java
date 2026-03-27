package net.oktawia.crazyae2addons.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.screens.CrazyPatternModifierScreenPP;
import net.oktawia.crazyae2addons.screens.CrazyPatternProviderScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class UpdatePatternsPacket {
    private final int startIndex;
    private final List<ItemStack> patterns;

    public UpdatePatternsPacket(int startIndex, List<ItemStack> patterns) {
        this.startIndex = startIndex;
        this.patterns = patterns;
    }

    public static void encode(UpdatePatternsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.startIndex);
        buf.writeVarInt(packet.patterns.size());
        for (ItemStack stack : packet.patterns) {
            buf.writeItem(stack);
        }
    }

    public static UpdatePatternsPacket decode(FriendlyByteBuf buf) {
        int start = buf.readVarInt();
        int size = buf.readVarInt();
        var patterns = new ArrayList<ItemStack>(Math.max(size, 0));
        for (int i = 0; i < size; i++) {
            patterns.add(buf.readItem());
        }
        return new UpdatePatternsPacket(start, patterns);
    }

    public static void handle(UpdatePatternsPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof CrazyPatternProviderScreen<?> screen) {
                screen.updatePatternsFromServer(packet.startIndex, packet.patterns);
            } else if (mc.screen instanceof CrazyPatternModifierScreenPP<?> screen) {
                screen.updatePatternsFromServer(packet.startIndex, packet.patterns);
            }
        });
        ctx.setPacketHandled(true);
    }
}
