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

public class PlayerLocatorStructureUpdateS2CPacket implements CustomPacketPayload {
    public static final Type<PlayerLocatorStructureUpdateS2CPacket> TYPE = new Type<>(bodycam.id("player_locator_structure_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerLocatorStructureUpdateS2CPacket> STREAM_CODEC =
            StreamCodec.of(PlayerLocatorStructureUpdateS2CPacket::encode, PlayerLocatorStructureUpdateS2CPacket::decode);

    public final BlockPos structurePos;
    public final String dimension;

    public PlayerLocatorStructureUpdateS2CPacket(BlockPos structurePos, String dimension) {
        this.structurePos = structurePos != null ? structurePos : BlockPos.ZERO;
        this.dimension = dimension != null ? dimension : "";
    }

    public static void encode(RegistryFriendlyByteBuf buf, PlayerLocatorStructureUpdateS2CPacket msg) {
        buf.writeBlockPos(msg.structurePos);
        buf.writeUtf(msg.dimension);
    }

    public static PlayerLocatorStructureUpdateS2CPacket decode(RegistryFriendlyByteBuf buf) {
        return new PlayerLocatorStructureUpdateS2CPacket(buf.readBlockPos(), buf.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerLocatorStructureUpdateS2CPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientPacketHandler.handlePlayerLocatorStructureUpdate(msg)));
    }
}
