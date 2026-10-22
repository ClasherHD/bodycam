package dev.ClasherHD.bodycam.network.watchpoint;

import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class WatchPointLookC2SPacket {
    public final float yRot;
    public final float xRot;

    public WatchPointLookC2SPacket(float yRot, float xRot) {
        this.yRot = yRot;
        this.xRot = xRot;
    }

    public static void encode(WatchPointLookC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.yRot);
        buf.writeFloat(msg.xRot);
    }

    public static WatchPointLookC2SPacket decode(FriendlyByteBuf buf) {
        return new WatchPointLookC2SPacket(buf.readFloat(), buf.readFloat());
    }

    public static void handle(WatchPointLookC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !player.getPersistentData().getBoolean("bodycam_active")
                    || !player.getPersistentData().getBoolean("bodycam_watching_watchpoint")) {
                return;
            }
            if (!player.getPersistentData().contains("bodycam_target_uuid")) {
                return;
            }
            UUID targetId = player.getPersistentData().getUUID("bodycam_target_uuid");
            WatchPointDummyEntity dummy = WatchPointDummyEntity.findByUUID(player.server, targetId);
            if (dummy != null && player.getUUID().equals(dummy.getOwnerUUID())) {
                float clampedPitch = Mth.clamp(msg.xRot, -90.0F, 90.0F);
                float wrappedYaw = Mth.wrapDegrees(msg.yRot);
                dummy.setYRot(wrappedYaw);
                dummy.setXRot(clampedPitch);
                dummy.setYHeadRot(wrappedYaw);
                dummy.setYBodyRot(wrappedYaw);
                player.setYRot(wrappedYaw);
                player.setXRot(clampedPitch);
                player.setYHeadRot(wrappedYaw);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
