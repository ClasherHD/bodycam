package dev.ClasherHD.bodycam.network.watchpoint;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import dev.architectury.networking.NetworkManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.UUID;

@SuppressWarnings("null")
public class WatchPointLookC2SPacket implements CustomPacketPayload {
    public static final Type<WatchPointLookC2SPacket> TYPE = new Type<>(bodycam.id("watch_point_look"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WatchPointLookC2SPacket> STREAM_CODEC =
            StreamCodec.of(WatchPointLookC2SPacket::encode, WatchPointLookC2SPacket::decode);

    public final float yRot;
    public final float xRot;

    public WatchPointLookC2SPacket(float yRot, float xRot) {
        this.yRot = yRot;
        this.xRot = xRot;
    }

    public static void encode(RegistryFriendlyByteBuf buf, WatchPointLookC2SPacket msg) {
        buf.writeFloat(msg.yRot);
        buf.writeFloat(msg.xRot);
    }

    public static WatchPointLookC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new WatchPointLookC2SPacket(buf.readFloat(), buf.readFloat());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WatchPointLookC2SPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer sender) || sender.server == null) {
                return;
            }
            if (!Float.isFinite(msg.yRot) || !Float.isFinite(msg.xRot)) {
                return;
            }
            CompoundTag data = PersistentDataAccess.get(sender);
            if (!data.getBoolean("bodycam_active")
                    || !data.getBoolean("bodycam_watching_watchpoint")
                    || !data.contains("bodycam_target_uuid")) {
                return;
            }
            UUID targetId = data.getUUID("bodycam_target_uuid");
            WatchPointDummyEntity dummy = WatchPointDummyEntity.findByUUID(sender.server, targetId);
            if (dummy == null || !dummy.isAlive() || dummy.isRemoved()) {
                return;
            }
            if (!sender.getUUID().equals(dummy.getOwnerUuidOrNull())
                    && !data.getBoolean("bodycam_all_seeing_active")) {
                return;
            }
            float clampedPitch = Mth.clamp(msg.xRot, -90.0F, 90.0F);
            float clampedYaw = Mth.wrapDegrees(msg.yRot);
            dummy.setYRot(clampedYaw);
            dummy.setXRot(clampedPitch);
            dummy.setYHeadRot(clampedYaw);
            dummy.setYBodyRot(clampedYaw);
            sender.setYRot(clampedYaw);
            sender.setXRot(clampedPitch);
            sender.setYHeadRot(clampedYaw);
        });
    }
}
