package dev.ClasherHD.bodycam.network.portal;

import dev.ClasherHD.bodycam.bodycam;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class PortablePortalActionC2SPacket implements CustomPacketPayload {
    public static final Type<PortablePortalActionC2SPacket> TYPE = new Type<>(bodycam.id("portable_portal_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PortablePortalActionC2SPacket> STREAM_CODEC =
            StreamCodec.of(PortablePortalActionC2SPacket::encode, PortablePortalActionC2SPacket::decode);

    public final boolean complete;

    public PortablePortalActionC2SPacket(boolean complete) {
        this.complete = complete;
    }

    public static void encode(RegistryFriendlyByteBuf buf, PortablePortalActionC2SPacket msg) {
        buf.writeBoolean(msg.complete);
    }

    public static PortablePortalActionC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new PortablePortalActionC2SPacket(buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PortablePortalActionC2SPacket msg, NetworkManager.PacketContext context) {
        // Portal teleportation is completed server-side via PortableNetherPortalItem#finishUsingItem.
    }
}
