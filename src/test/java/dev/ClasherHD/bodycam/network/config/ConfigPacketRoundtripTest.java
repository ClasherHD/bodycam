package dev.ClasherHD.bodycam.network.config;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class ConfigPacketRoundtripTest {

    @Test
    @DisplayName("OpenServerConfigS2CPacket should encode and decode all 12 configuration options accurately")
    void openServerConfigS2CPacket_roundtrip_preservesAllOptions() {
        OpenServerConfigS2CPacket original = new OpenServerConfigS2CPacket(
                128, 5, 20,
                true, true, true,
                true, true, true,
                true, true, true
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        OpenServerConfigS2CPacket.encode(original, buf);
        OpenServerConfigS2CPacket decoded = OpenServerConfigS2CPacket.decode(buf);

        assertThat(decoded.maxMonitorDistance).isEqualTo(128);
        assertThat(decoded.maxWatchPointsPerPlayer).isEqualTo(5);
        assertThat(decoded.maxWatchPointsTotal).isEqualTo(20);
        assertThat(decoded.enableReachEnchantment).isTrue();
        assertThat(decoded.enableJammer).isTrue();
        assertThat(decoded.enableDimensionLocator).isTrue();
        assertThat(decoded.enableHologramBlock).isTrue();
        assertThat(decoded.enableAnonymizer).isTrue();
        assertThat(decoded.enablePlayerLocator).isTrue();
        assertThat(decoded.enableWatchPoint).isTrue();
        assertThat(decoded.enablePodest).isTrue();
        assertThat(decoded.opOnlyMode).isTrue();
    }

    @Test
    @DisplayName("OpenServerConfigS2CPacket should handle false boolean flags and boundary integer limits")
    void openServerConfigS2CPacket_roundtrip_withBoundaryValues() {
        OpenServerConfigS2CPacket original = new OpenServerConfigS2CPacket(
                Integer.MAX_VALUE, 0, Integer.MIN_VALUE,
                false, false, false,
                false, false, false,
                false, false, false
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        OpenServerConfigS2CPacket.encode(original, buf);
        OpenServerConfigS2CPacket decoded = OpenServerConfigS2CPacket.decode(buf);

        assertThat(decoded.maxMonitorDistance).isEqualTo(Integer.MAX_VALUE);
        assertThat(decoded.maxWatchPointsPerPlayer).isEqualTo(0);
        assertThat(decoded.maxWatchPointsTotal).isEqualTo(Integer.MIN_VALUE);
        assertThat(decoded.enableReachEnchantment).isFalse();
        assertThat(decoded.enableJammer).isFalse();
        assertThat(decoded.enableDimensionLocator).isFalse();
        assertThat(decoded.enableHologramBlock).isFalse();
        assertThat(decoded.enableAnonymizer).isFalse();
        assertThat(decoded.enablePlayerLocator).isFalse();
        assertThat(decoded.enableWatchPoint).isFalse();
        assertThat(decoded.enablePodest).isFalse();
        assertThat(decoded.opOnlyMode).isFalse();
    }

    @Test
    @DisplayName("ReloadClientConfigS2CPacket should encode and decode empty payload")
    void reloadClientConfigS2CPacket_roundtrip_createsValidInstance() {
        ReloadClientConfigS2CPacket original = new ReloadClientConfigS2CPacket();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        ReloadClientConfigS2CPacket.encode(original, buf);
        ReloadClientConfigS2CPacket decoded = ReloadClientConfigS2CPacket.decode(buf);

        assertThat(decoded).isNotNull();
    }

    @Test
    @DisplayName("SaveServerConfigC2SPacket should encode and decode all 12 configuration options accurately")
    void saveServerConfigC2SPacket_roundtrip_preservesAllOptions() {
        SaveServerConfigC2SPacket original = new SaveServerConfigC2SPacket(
                256, 10, 50,
                true, false, true,
                false, true, false,
                true, false, true
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SaveServerConfigC2SPacket.encode(original, buf);
        SaveServerConfigC2SPacket decoded = SaveServerConfigC2SPacket.decode(buf);

        assertThat(decoded.maxMonitorDistance).isEqualTo(256);
        assertThat(decoded.maxWatchPointsPerPlayer).isEqualTo(10);
        assertThat(decoded.maxWatchPointsTotal).isEqualTo(50);
        assertThat(decoded.enableReachEnchantment).isTrue();
        assertThat(decoded.enableJammer).isFalse();
        assertThat(decoded.enableDimensionLocator).isTrue();
        assertThat(decoded.enableHologramBlock).isFalse();
        assertThat(decoded.enableAnonymizer).isTrue();
        assertThat(decoded.enablePlayerLocator).isFalse();
        assertThat(decoded.enableWatchPoint).isTrue();
        assertThat(decoded.enablePodest).isFalse();
        assertThat(decoded.opOnlyMode).isTrue();
    }

    @Test
    @DisplayName("SaveServerConfigC2SPacket should handle boundary integer values")
    void saveServerConfigC2SPacket_roundtrip_withBoundaryValues() {
        SaveServerConfigC2SPacket original = new SaveServerConfigC2SPacket(
                1, 1, 1,
                false, true, false,
                true, false, true,
                false, true, false
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SaveServerConfigC2SPacket.encode(original, buf);
        SaveServerConfigC2SPacket decoded = SaveServerConfigC2SPacket.decode(buf);

        assertThat(decoded.maxMonitorDistance).isEqualTo(1);
        assertThat(decoded.maxWatchPointsPerPlayer).isEqualTo(1);
        assertThat(decoded.maxWatchPointsTotal).isEqualTo(1);
        assertThat(decoded.enableReachEnchantment).isFalse();
        assertThat(decoded.enableJammer).isTrue();
    }
}
