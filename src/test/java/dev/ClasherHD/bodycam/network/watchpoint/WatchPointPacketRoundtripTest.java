package dev.ClasherHD.bodycam.network.watchpoint;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WatchPointPacketRoundtripTest {

    @Test
    @DisplayName("OpenWatchPointScreenS2CPacket should encode and decode BlockPos and name")
    void openWatchPointScreenS2CPacket_roundtrip_preservesFields() {
        BlockPos pos = new BlockPos(100, 64, -200);
        String name = "Main Camera Point";

        OpenWatchPointScreenS2CPacket original = new OpenWatchPointScreenS2CPacket(pos, name);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        OpenWatchPointScreenS2CPacket.encode(original, buf);
        OpenWatchPointScreenS2CPacket decoded = OpenWatchPointScreenS2CPacket.decode(buf);

        assertThat(decoded.pos).isEqualTo(pos);
        assertThat(decoded.currentName).isEqualTo(name);
    }

    @Test
    @DisplayName("OpenWatchPointScreenS2CPacket should convert null name to empty string")
    void openWatchPointScreenS2CPacket_withNullName_convertsToEmptyString() {
        BlockPos pos = new BlockPos(0, 0, 0);

        OpenWatchPointScreenS2CPacket packet = new OpenWatchPointScreenS2CPacket(pos, null);
        assertThat(packet.currentName).isEqualTo("");

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        OpenWatchPointScreenS2CPacket.encode(packet, buf);
        OpenWatchPointScreenS2CPacket decoded = OpenWatchPointScreenS2CPacket.decode(buf);

        assertThat(decoded.currentName).isEqualTo("");
    }

    @Test
    @DisplayName("OpenWatchPointScreenS2CPacket should support exactly 32-character name")
    void openWatchPointScreenS2CPacket_with32CharName_encodesAndDecodesSuccessfully() {
        BlockPos pos = new BlockPos(10, 20, 30);
        String exact32Chars = "12345678901234567890123456789012";
        assertThat(exact32Chars).hasSize(32);

        OpenWatchPointScreenS2CPacket original = new OpenWatchPointScreenS2CPacket(pos, exact32Chars);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        OpenWatchPointScreenS2CPacket.encode(original, buf);
        OpenWatchPointScreenS2CPacket decoded = OpenWatchPointScreenS2CPacket.decode(buf);

        assertThat(decoded.currentName).isEqualTo(exact32Chars);
    }

    @Test
    @DisplayName("OpenWatchPointScreenS2CPacket encoding string exceeding 32 characters should throw exception")
    void openWatchPointScreenS2CPacket_withNameExceeding32Chars_throwsExceptionOnEncode() {
        BlockPos pos = new BlockPos(10, 20, 30);
        String tooLongName = "123456789012345678901234567890123";
        assertThat(tooLongName.length()).isGreaterThan(32);

        OpenWatchPointScreenS2CPacket packet = new OpenWatchPointScreenS2CPacket(pos, tooLongName);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        assertThatThrownBy(() -> OpenWatchPointScreenS2CPacket.encode(packet, buf))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("RenameWatchPointC2SPacket should encode and decode BlockPos and new name")
    void renameWatchPointC2SPacket_roundtrip_preservesFields() {
        BlockPos pos = new BlockPos(-50, 120, 300);
        String newName = "Rear Base Cam";

        RenameWatchPointC2SPacket original = new RenameWatchPointC2SPacket(pos, newName);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        RenameWatchPointC2SPacket.encode(original, buf);
        RenameWatchPointC2SPacket decoded = RenameWatchPointC2SPacket.decode(buf);

        assertThat(decoded.pos).isEqualTo(pos);
        assertThat(decoded.newName).isEqualTo(newName);
    }

    @Test
    @DisplayName("RenameWatchPointC2SPacket should convert null new name to empty string")
    void renameWatchPointC2SPacket_withNullName_convertsToEmptyString() {
        BlockPos pos = new BlockPos(0, 0, 0);

        RenameWatchPointC2SPacket packet = new RenameWatchPointC2SPacket(pos, null);
        assertThat(packet.newName).isEqualTo("");

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RenameWatchPointC2SPacket.encode(packet, buf);
        RenameWatchPointC2SPacket decoded = RenameWatchPointC2SPacket.decode(buf);

        assertThat(decoded.newName).isEqualTo("");
    }

    @Test
    @DisplayName("RenameWatchPointC2SPacket should support exactly 32-character new name")
    void renameWatchPointC2SPacket_with32CharName_encodesAndDecodesSuccessfully() {
        BlockPos pos = new BlockPos(1, 2, 3);
        String exact32Chars = "abcdefghijklmnopqrstuvwxyz123456";
        assertThat(exact32Chars).hasSize(32);

        RenameWatchPointC2SPacket original = new RenameWatchPointC2SPacket(pos, exact32Chars);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        RenameWatchPointC2SPacket.encode(original, buf);
        RenameWatchPointC2SPacket decoded = RenameWatchPointC2SPacket.decode(buf);

        assertThat(decoded.newName).isEqualTo(exact32Chars);
    }

    @Test
    @DisplayName("RenameWatchPointC2SPacket encoding string exceeding 32 characters should throw exception")
    void renameWatchPointC2SPacket_withNameExceeding32Chars_throwsExceptionOnEncode() {
        BlockPos pos = new BlockPos(1, 2, 3);
        String tooLong = "abcdefghijklmnopqrstuvwxyz1234567";
        assertThat(tooLong.length()).isGreaterThan(32);

        RenameWatchPointC2SPacket packet = new RenameWatchPointC2SPacket(pos, tooLong);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        assertThatThrownBy(() -> RenameWatchPointC2SPacket.encode(packet, buf))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("WatchPointLookC2SPacket should encode and decode yaw and pitch rotation floats")
    void watchPointLookC2SPacket_roundtrip_preservesRotationFloats() {
        float yRot = 120.5F;
        float xRot = -45.0F;

        WatchPointLookC2SPacket original = new WatchPointLookC2SPacket(yRot, xRot);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        WatchPointLookC2SPacket.encode(original, buf);
        WatchPointLookC2SPacket decoded = WatchPointLookC2SPacket.decode(buf);

        assertThat(decoded.yRot).isEqualTo(yRot);
        assertThat(decoded.xRot).isEqualTo(xRot);
    }

    @Test
    @DisplayName("WatchPointLookC2SPacket should handle boundary and extreme float values")
    void watchPointLookC2SPacket_roundtrip_withBoundaryValues() {
        WatchPointLookC2SPacket original = new WatchPointLookC2SPacket(0.0F, 0.0F);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        WatchPointLookC2SPacket.encode(original, buf);
        WatchPointLookC2SPacket decoded = WatchPointLookC2SPacket.decode(buf);

        assertThat(decoded.yRot).isEqualTo(0.0F);
        assertThat(decoded.xRot).isEqualTo(0.0F);

        WatchPointLookC2SPacket extreme = new WatchPointLookC2SPacket(-360.0F, 360.0F);
        FriendlyByteBuf buf2 = new FriendlyByteBuf(Unpooled.buffer());
        WatchPointLookC2SPacket.encode(extreme, buf2);
        WatchPointLookC2SPacket decodedExtreme = WatchPointLookC2SPacket.decode(buf2);

        assertThat(decodedExtreme.yRot).isEqualTo(-360.0F);
        assertThat(decodedExtreme.xRot).isEqualTo(360.0F);
    }
}
