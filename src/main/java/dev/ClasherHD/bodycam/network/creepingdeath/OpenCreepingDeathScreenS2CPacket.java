package dev.ClasherHD.bodycam.network.creepingdeath;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class OpenCreepingDeathScreenS2CPacket {

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

    public static void encode(OpenCreepingDeathScreenS2CPacket msg, FriendlyByteBuf buf) {
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

    public static OpenCreepingDeathScreenS2CPacket decode(FriendlyByteBuf buf) {
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

    public static void handle(OpenCreepingDeathScreenS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> dev.ClasherHD.bodycam.client.network.ClientPacketHandler.handleOpenCreepingDeathScreen(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}
