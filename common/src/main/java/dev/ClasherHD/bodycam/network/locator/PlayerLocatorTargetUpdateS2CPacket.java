package dev.ClasherHD.bodycam.network.locator;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.network.ClientPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public class PlayerLocatorTargetUpdateS2CPacket implements CustomPacketPayload {
    public static final Type<PlayerLocatorTargetUpdateS2CPacket> TYPE = new Type<>(bodycam.id("player_locator_target_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerLocatorTargetUpdateS2CPacket> STREAM_CODEC =
            StreamCodec.of(PlayerLocatorTargetUpdateS2CPacket::encode, PlayerLocatorTargetUpdateS2CPacket::decode);

    public final UUID targetUUID;
    public final BlockPos pos;
    public final String dimension;

    public PlayerLocatorTargetUpdateS2CPacket(UUID targetUUID, BlockPos pos, String dimension) {
        this.targetUUID = targetUUID;
        this.pos = pos != null ? pos : BlockPos.ZERO;
        this.dimension = dimension != null ? dimension : "";
    }

    public static void encode(RegistryFriendlyByteBuf buf, PlayerLocatorTargetUpdateS2CPacket msg) {
        buf.writeUUID(msg.targetUUID);
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.dimension);
    }

    public static PlayerLocatorTargetUpdateS2CPacket decode(RegistryFriendlyByteBuf buf) {
        return new PlayerLocatorTargetUpdateS2CPacket(buf.readUUID(), buf.readBlockPos(), buf.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerLocatorTargetUpdateS2CPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientPacketHandler.handlePlayerLocatorTargetUpdate(msg)));
    }
}
