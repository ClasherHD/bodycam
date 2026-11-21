package dev.ClasherHD.bodycam.network;

import dev.ClasherHD.bodycam.network.portal.PortablePortalActionC2SPacket;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@SuppressWarnings("null")
class PortablePortalPacketTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
    }

    @Test
    void streamCodec_roundtripsCompleteFlag() {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(new FriendlyByteBuf(Unpooled.buffer()), RegistryAccess.EMPTY);
        PortablePortalActionC2SPacket.STREAM_CODEC.encode(buf, new PortablePortalActionC2SPacket(true));
        PortablePortalActionC2SPacket.STREAM_CODEC.encode(buf, new PortablePortalActionC2SPacket(false));

        PortablePortalActionC2SPacket first = PortablePortalActionC2SPacket.STREAM_CODEC.decode(buf);
        PortablePortalActionC2SPacket second = PortablePortalActionC2SPacket.STREAM_CODEC.decode(buf);

        assertThat(first.complete).isTrue();
        assertThat(second.complete).isFalse();
        assertThat(buf.readableBytes()).isZero();
    }

    @Test
    void handle_ignoresClientTeleportRequests() {
        NetworkManager.PacketContext context = mock(NetworkManager.PacketContext.class);
        PortablePortalActionC2SPacket.handle(new PortablePortalActionC2SPacket(true), context);
        verifyNoInteractions(context);
    }
}
