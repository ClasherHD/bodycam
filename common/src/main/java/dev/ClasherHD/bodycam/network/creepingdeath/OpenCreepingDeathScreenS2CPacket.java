package dev.ClasherHD.bodycam.network.creepingdeath;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.network.ClientPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OpenCreepingDeathScreenS2CPacket implements CustomPacketPayload {
    public static final Type<OpenCreepingDeathScreenS2CPacket> TYPE = new Type<>(bodycam.id("open_creeping_death_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCreepingDeathScreenS2CPacket> STREAM_CODEC =
            StreamCodec.of(OpenCreepingDeathScreenS2CPacket::encode, OpenCreepingDeathScreenS2CPacket::decode);

    public static final int MODE_TARGET = 0;
    public static final int MODE_SKIN = 1;

    public record PlayerEntryData(UUID uuid, String name, boolean disabled) {
    }

    public final int mode;
    public final boolean offHand;
    public final List<PlayerEntryData> entries;
    public final UUID selectedSkinUUID;

    public OpenCreepingDeathScreenS2CPacket(int mode, boolean offHand, List<PlayerEntryData> entries, UUID selectedSkinUUID) {
        this.mode = mode;
        this.offHand = offHand;
        this.entries = entries;
        this.selectedSkinUUID = selectedSkinUUID;
    }

    public static void encode(RegistryFriendlyByteBuf buf, OpenCreepingDeathScreenS2CPacket msg) {
        buf.writeVarInt(msg.mode);
        buf.writeBoolean(msg.offHand);
        buf.writeVarInt(msg.entries.size());
        for (PlayerEntryData entry : msg.entries) {
            buf.writeUUID(entry.uuid());
            buf.writeUtf(entry.name(), 64);
            buf.writeBoolean(entry.disabled());
        }
        buf.writeBoolean(msg.selectedSkinUUID != null);
        if (msg.selectedSkinUUID != null) {
            buf.writeUUID(msg.selectedSkinUUID);
        }
    }

    public static OpenCreepingDeathScreenS2CPacket decode(RegistryFriendlyByteBuf buf) {
        int mode = buf.readVarInt();
        boolean offHand = buf.readBoolean();
        int size = Math.max(0, Math.min(buf.readVarInt(), 2048));
        List<PlayerEntryData> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new PlayerEntryData(buf.readUUID(), buf.readUtf(64), buf.readBoolean()));
        }
        UUID selectedSkin = buf.readBoolean() ? buf.readUUID() : null;
        return new OpenCreepingDeathScreenS2CPacket(mode, offHand, entries, selectedSkin);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenCreepingDeathScreenS2CPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientPacketHandler.handleOpenCreepingDeathScreen(msg)));
    }
}
