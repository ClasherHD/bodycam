package dev.ClasherHD.bodycam.network.watchpoint;

import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
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

    @Test
    @DisplayName("WatchPointLookC2SPacket.handle should reject NaN/Infinity and apply valid finite rotations")
    void watchPointLookC2SPacket_handle_rejectsNonFiniteAndAppliesFinite() {
        MinecraftTestEnvironment.ensureBootstrapped();
        UUID ownerId = UUID.randomUUID();
        UUID dummyId = UUID.randomUUID();

        ServerPlayer player = mock(ServerPlayer.class);
        MinecraftServer server = mock(MinecraftServer.class);
        MinecraftTestEnvironment.setField(player, "server", server);
        CompoundTag persistent = new CompoundTag();
        persistent.putBoolean("bodycam_active", true);
        persistent.putBoolean("bodycam_watching_watchpoint", true);
        persistent.putUUID("bodycam_target_uuid", dummyId);
        when(player.getPersistentData()).thenReturn(persistent);
        when(player.getUUID()).thenReturn(ownerId);

        WatchPointDummyEntity dummy = mock(WatchPointDummyEntity.class);
        when(dummy.getUUID()).thenReturn(dummyId);
        when(dummy.getOwnerUUID()).thenReturn(ownerId);
        when(dummy.isAlive()).thenReturn(true);
        when(dummy.isRemoved()).thenReturn(false);

        WatchPointDummyEntity.ACTIVE_WATCH_POINTS.add(dummy);
        try {
            NetworkEvent.Context ctx = mock(NetworkEvent.Context.class);
            when(ctx.getSender()).thenReturn(player);
            doAnswer(inv -> {
                ((Runnable) inv.getArgument(0)).run();
                return null;
            }).when(ctx).enqueueWork(any(Runnable.class));

            WatchPointLookC2SPacket.handle(new WatchPointLookC2SPacket(Float.NaN, 10.0F), () -> ctx);
            WatchPointLookC2SPacket.handle(new WatchPointLookC2SPacket(10.0F, Float.POSITIVE_INFINITY), () -> ctx);
            WatchPointLookC2SPacket.handle(new WatchPointLookC2SPacket(Float.NEGATIVE_INFINITY, Float.NaN), () -> ctx);

            verify(dummy, never()).setYRot(anyFloat());
            verify(dummy, never()).setXRot(anyFloat());
            verify(player, never()).setYRot(anyFloat());
            verify(player, never()).setXRot(anyFloat());

            WatchPointLookC2SPacket.handle(new WatchPointLookC2SPacket(45.0F, -120.0F), () -> ctx);

            verify(dummy).setYRot(45.0F);
            verify(dummy).setXRot(-90.0F);
            verify(player).setYRot(45.0F);
            verify(player).setXRot(-90.0F);
        } finally {
            WatchPointDummyEntity.ACTIVE_WATCH_POINTS.remove(dummy);
        }
    }
}
