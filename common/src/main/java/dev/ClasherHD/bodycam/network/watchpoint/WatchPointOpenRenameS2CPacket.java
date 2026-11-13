package dev.ClasherHD.bodycam.network.watchpoint;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.network.ClientPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@SuppressWarnings("null")
public class WatchPointOpenRenameS2CPacket implements CustomPacketPayload {
    public static final Type<WatchPointOpenRenameS2CPacket> TYPE = new Type<>(bodycam.id("watch_point_open_rename"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WatchPointOpenRenameS2CPacket> STREAM_CODEC =
            StreamCodec.of(WatchPointOpenRenameS2CPacket::encode, WatchPointOpenRenameS2CPacket::decode);

    public final BlockPos pos;
    public final String currentName;

    public WatchPointOpenRenameS2CPacket(BlockPos pos, String currentName) {
        this.pos = pos;
        this.currentName = currentName == null ? "" : currentName;
    }

    public static void encode(RegistryFriendlyByteBuf buf, WatchPointOpenRenameS2CPacket msg) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.currentName, 32);
    }

    public static WatchPointOpenRenameS2CPacket decode(RegistryFriendlyByteBuf buf) {
        return new WatchPointOpenRenameS2CPacket(buf.readBlockPos(), buf.readUtf(32));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WatchPointOpenRenameS2CPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientPacketHandler.handleOpenWatchPointRename(msg)));
    }
}
