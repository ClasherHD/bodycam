package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;

public class SyncBodycamRequestC2SPacket {
    public final boolean hasReach;
    public final boolean isOnHologram;

    public SyncBodycamRequestC2SPacket(boolean hasReach, boolean isOnHologram) {
        this.hasReach = hasReach;
        this.isOnHologram = isOnHologram;
    }

    public static void encode(SyncBodycamRequestC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.hasReach);
        buf.writeBoolean(msg.isOnHologram);
    }

    public static SyncBodycamRequestC2SPacket decode(FriendlyByteBuf buf) {
        return new SyncBodycamRequestC2SPacket(buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(SyncBodycamRequestC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !sender.hasPermissions(2)) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("This feature is restricted to Server Operators.").withStyle(net.minecraft.ChatFormatting.RED));
                return;
            }
            if (msg.isOnHologram && !dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_HOLOGRAM_BLOCK.get()) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("Hologram cross-dimension projection is disabled on this server.").withStyle(net.minecraft.ChatFormatting.RED));
                return;
            }

            Map<UUID, Integer> jammers = new HashMap<>();
            Map<UUID, UUID> targets = new HashMap<>();
            Map<UUID, String> dimensions = new HashMap<>();
            Map<UUID, BlockPos> positions = new HashMap<>();
            Map<UUID, Boolean> anonymizers = new HashMap<>();

            for (ServerPlayer p : sender.server.getPlayerList().getPlayers()) {
                int mode = 0;
                boolean hasAnonymizer = false;
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                    ItemStack stack = p.getInventory().getItem(i);
                    if (stack.getItem() instanceof dev.ClasherHD.bodycam.item.JammerItem && stack.hasTag() && stack.getTag().contains("JammerMode")) {
                        mode = Math.max(mode, stack.getTag().getInt("JammerMode"));
                    }
                    if (stack.getItem() instanceof dev.ClasherHD.bodycam.item.AnonymizerItem && stack.hasTag() && stack.getTag().contains("AnonymizerActive")) {
                        if (stack.getTag().getBoolean("AnonymizerActive")) {
                            hasAnonymizer = true;
                        }
                    }
                }
                jammers.put(p.getUUID(), mode);
                anonymizers.put(p.getUUID(), hasAnonymizer);

                if (p.getPersistentData().getBoolean("bodycam_active") && p.getPersistentData().contains("bodycam_target_uuid")) {
                    targets.put(p.getUUID(), p.getPersistentData().getUUID("bodycam_target_uuid"));
                }

                if (p.getPersistentData().getBoolean("bodycam_active")) {
                    net.minecraft.world.phys.Vec3 dummyVec = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.get(p.getUUID());
                    if (dummyVec != null) {
                        positions.put(p.getUUID(), new BlockPos((int) dummyVec.x, (int) dummyVec.y, (int) dummyVec.z));
                    } else {
                        net.minecraft.world.phys.Vec3 origVec = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS.get(p.getUUID());
                        if (origVec != null) {
                            positions.put(p.getUUID(), new BlockPos((int) origVec.x, (int) origVec.y, (int) origVec.z));
                        } else {
                            positions.put(p.getUUID(), p.blockPosition());
                        }
                    }
                    String origDim = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_DIM.get(p.getUUID());
                    dimensions.put(p.getUUID(), origDim != null ? origDim : p.level().dimension().location().toString());
                } else {
                    dimensions.put(p.getUUID(), p.level().dimension().location().toString());
                    positions.put(p.getUUID(), p.blockPosition());
                }
            }

            PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> sender),
                new SyncBodycamStatesS2CPacket(jammers, targets, dimensions, positions, anonymizers, msg.hasReach, msg.isOnHologram)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
