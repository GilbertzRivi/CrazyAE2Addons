package net.oktawia.crazyae2addons.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.oktawia.crazyae2addons.CrazyAddons;

import java.util.Optional;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CrazyAddons.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void registerMessages() {
        int id = 0;

        // PLAY_TO_CLIENT
        INSTANCE.registerMessage(id++, DisplayValuePacket.class,  DisplayValuePacket::encode,  DisplayValuePacket::decode,  DisplayValuePacket::handle,  Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, UpdatePatternsPacket.class,UpdatePatternsPacket::encode,UpdatePatternsPacket::decode,UpdatePatternsPacket::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, SyncBlockClientPacket.class,SyncBlockClientPacket::encode,SyncBlockClientPacket::decode,SyncBlockClientPacket::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, SendLongStringToClientPacket.class,SendLongStringToClientPacket::encode,SendLongStringToClientPacket::decode,SendLongStringToClientPacket::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, ClipboardPacket.class,ClipboardPacket::encode,ClipboardPacket::decode,ClipboardPacket::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, StockThresholdToastPacket.class,StockThresholdToastPacket::encode,StockThresholdToastPacket::decode,StockThresholdToastPacket::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, NotificationHudPacket.class,NotificationHudPacket::encode,NotificationHudPacket::decode,NotificationHudPacket::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // PLAY_TO_SERVER
        INSTANCE.registerMessage(id++, SendLongStringToServerPacket.class,SendLongStringToServerPacket::encode,SendLongStringToServerPacket::decode,SendLongStringToServerPacket::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++, SetConfigAmountPacket.class,SetConfigAmountPacket::encode,SetConfigAmountPacket::decode,SetConfigAmountPacket::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++, CancellAllCraftingPacket.class,CancellAllCraftingPacket::encode,CancellAllCraftingPacket::decode,CancellAllCraftingPacket::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
