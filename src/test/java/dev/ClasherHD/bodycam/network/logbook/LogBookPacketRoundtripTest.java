package dev.ClasherHD.bodycam.network.logbook;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class LogBookPacketRoundtripTest {

    @Test
    @DisplayName("LogBookActionC2SPacket should encode and decode action, offHand, and PINs")
    void logBookActionC2SPacket_roundtrip_preservesFields() {
        LogBookActionC2SPacket originalUnlock = new LogBookActionC2SPacket(
                LogBookActionC2SPacket.ACTION_UNLOCK,
                false,
                "1234",
                ""
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        LogBookActionC2SPacket.encode(originalUnlock, buf);
        LogBookActionC2SPacket decodedUnlock = LogBookActionC2SPacket.decode(buf);

        assertThat(decodedUnlock.action).isEqualTo(LogBookActionC2SPacket.ACTION_UNLOCK);
        assertThat(decodedUnlock.offHand).isFalse();
        assertThat(decodedUnlock.pin).isEqualTo("1234");
        assertThat(decodedUnlock.newPin).isEmpty();

        LogBookActionC2SPacket originalSet = new LogBookActionC2SPacket(
                LogBookActionC2SPacket.ACTION_SET_PIN,
                true,
                "1234",
                "5678"
        );
        FriendlyByteBuf buf2 = new FriendlyByteBuf(Unpooled.buffer());
        LogBookActionC2SPacket.encode(originalSet, buf2);
        LogBookActionC2SPacket decodedSet = LogBookActionC2SPacket.decode(buf2);

        assertThat(decodedSet.action).isEqualTo(LogBookActionC2SPacket.ACTION_SET_PIN);
        assertThat(decodedSet.offHand).isTrue();
        assertThat(decodedSet.pin).isEqualTo("1234");
        assertThat(decodedSet.newPin).isEqualTo("5678");
    }

    @Test
    @DisplayName("OpenLogBookPinScreenS2CPacket should encode and decode mode and offHand flag")
    void openLogBookPinScreenS2CPacket_roundtrip_preservesFields() {
        OpenLogBookPinScreenS2CPacket original = new OpenLogBookPinScreenS2CPacket(
                OpenLogBookPinScreenS2CPacket.MODE_CHANGE,
                true
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        OpenLogBookPinScreenS2CPacket.encode(original, buf);
        OpenLogBookPinScreenS2CPacket decoded = OpenLogBookPinScreenS2CPacket.decode(buf);

        assertThat(decoded.mode).isEqualTo(OpenLogBookPinScreenS2CPacket.MODE_CHANGE);
        assertThat(decoded.offHand).isTrue();
    }

    @Test
    @DisplayName("OpenLogBookS2CPacket should encode and decode CompoundTag payload")
    void openLogBookS2CPacket_roundtrip_preservesData() {
        CompoundTag tag = new CompoundTag();
        tag.putString("TestKey", "TestValue");

        OpenLogBookS2CPacket original = new OpenLogBookS2CPacket(tag);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        OpenLogBookS2CPacket.encode(original, buf);
        OpenLogBookS2CPacket decoded = OpenLogBookS2CPacket.decode(buf);

        assertThat(decoded.data).isNotNull();
        assertThat(decoded.data.getString("TestKey")).isEqualTo("TestValue");
    }
}
