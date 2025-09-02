package net.oktawia.crazyae2addons.network;

import appeng.api.parts.IPartHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.parts.DisplayPart;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class DisplayValuePacket {
    public final BlockPos pos;
    public final String textValue;
    public final Direction direction;
    public final byte spin;
    public final String variables;
    public final int fontSize;
    public final boolean mode;
    public final boolean margin;
    public final boolean center;

    public DisplayValuePacket(BlockPos blockPos, String textValue, Direction side, byte spin, String variables, int fontSize, boolean mode, boolean margin, boolean center) {
        this.pos = blockPos;
        this.textValue = textValue;
        this.direction = side;
        this.spin = spin;
        this.variables = variables == null ? "" : variables;
        this.fontSize = fontSize;
        this.mode = mode;
        this.margin = margin;
        this.center = center;
    }

    public static void encode(DisplayValuePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeUtf(packet.direction.getSerializedName());
        buf.writeUtf(packet.textValue);
        buf.writeByte(packet.spin);
        buf.writeUtf(packet.variables);
        buf.writeBoolean(packet.mode);
        buf.writeInt(packet.fontSize);
        buf.writeBoolean(packet.margin);
        buf.writeBoolean(packet.center);
    }

    public static DisplayValuePacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String dirName = buf.readUtf();
        String textValue = buf.readUtf();
        byte spin = buf.readByte();
        String variables = buf.readUtf();
        boolean mode = buf.readBoolean();
        int fontSize = buf.readInt();
        boolean margin = buf.readBoolean();
        boolean center = buf.readBoolean();

        Direction dir = Direction.byName(dirName);
        if (dir == null) dir = Direction.SOUTH;

        return new DisplayValuePacket(pos, textValue, dir, spin, variables, fontSize, mode, margin, center);
    }

    public static void handle(DisplayValuePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                BlockEntity te = level.getBlockEntity(packet.pos);
                if (te instanceof IPartHost host) {
                    DisplayPart displayPart = (DisplayPart) host.getPart(packet.direction);
                    if (displayPart != null) {
                        displayPart.textValue = packet.textValue;
                        displayPart.spin = packet.spin;
                        displayPart.mode = packet.mode;
                        displayPart.fontSize = packet.fontSize;
                        displayPart.margin = packet.margin;
                        displayPart.center = packet.center;

                        if (packet.variables != null && !packet.variables.isEmpty()) {
                            HashMap<String, String> variablesMap = new HashMap<>();
                            for (String s : packet.variables.split("\\|")) {
                                if (s.isEmpty()) continue;
                                String[] arr = s.split("=", 2);
                                if (arr.length == 2 && !arr[0].isEmpty()) {
                                    variablesMap.put(arr[0], arr[1]);
                                }
                            }
                            if (!variablesMap.isEmpty()) {
                                displayPart.variables = variablesMap;
                            }
                        }

                        host.markForUpdate();
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }


}
