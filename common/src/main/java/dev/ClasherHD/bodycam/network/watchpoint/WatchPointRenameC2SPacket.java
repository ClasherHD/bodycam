package dev.ClasherHD.bodycam.network.watchpoint;

import dev.ClasherHD.bodycam.block.entity.WatchPointBlockEntity;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

@SuppressWarnings("null")
public class WatchPointRenameC2SPacket implements CustomPacketPayload {
    public static final Type<WatchPointRenameC2SPacket> TYPE = new Type<>(bodycam.id("watch_point_rename"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WatchPointRenameC2SPacket> STREAM_CODEC =
            StreamCodec.of(WatchPointRenameC2SPacket::encode, WatchPointRenameC2SPacket::decode);

    public final BlockPos pos;
    public final String name;

    public WatchPointRenameC2SPacket(BlockPos pos, String name) {
        this.pos = pos;
        this.name = name == null ? "" : name;
    }

    public static void encode(RegistryFriendlyByteBuf buf, WatchPointRenameC2SPacket msg) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.name, 32);
    }

    public static WatchPointRenameC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new WatchPointRenameC2SPacket(buf.readBlockPos(), buf.readUtf(32));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WatchPointRenameC2SPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            if (ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                return;
            }
            if (!ModServerConfig.ENABLE_WATCH_POINT.get()) {
                return;
            }
            if (player.blockPosition().distSqr(msg.pos) > 64.0D) {
                return;
            }
            if (!player.level().isLoaded(msg.pos)) {
                return;
            }
            if (player.level().getBlockEntity(msg.pos) instanceof WatchPointBlockEntity wp) {
                if (player.getUUID().equals(wp.getOwnerUUID())) {
                    String safe = msg.name.trim();
                    if (safe.length() > 32) {
                        safe = safe.substring(0, 32);
                    }
                    wp.setCustomName(safe);
                }
            }
        });
    }
}
