package dev.ClasherHD.bodycam.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerNetworking {
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(BodycamPacketIDs.RESET_CAMERA_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                dev.ClasherHD.bodycam.network.BodycamResetCameraPacket.executeReset(player);
                ServerPlayNetworking.send(player, BodycamPacketIDs.RESET_CAMERA_PACKET_ID, PacketByteBufs.empty());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(BodycamPacketIDs.REQUEST_CAMERA_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            int type = buf.readInt();
            if (type == 0) {
                boolean hasReach = buf.readBoolean();
                boolean isOnHologram = buf.readBoolean();
                server.execute(() -> {
                    if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !player.hasPermissions(2)) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("This feature is restricted to Server Operators.").withStyle(net.minecraft.ChatFormatting.RED));
                        return;
                    }
                    if (isOnHologram && !dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_HOLOGRAM_BLOCK.get()) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Hologram cross-dimension projection is disabled on this server.").withStyle(net.minecraft.ChatFormatting.RED));
                        return;
                    }

                    Map<UUID, Integer> jammers = new HashMap<>();
                    Map<UUID, UUID> targets = new HashMap<>();
                    Map<UUID, String> dimensions = new HashMap<>();
                    Map<UUID, BlockPos> positions = new HashMap<>();
                    Map<UUID, Boolean> anonymizers = new HashMap<>();

                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                        int mode = 0;
                        boolean hasAnonymizer = false;
                        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                            net.minecraft.world.item.ItemStack stack = p.getInventory().getItem(i);
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

                        if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(p).getBoolean("bodycam_active") && dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(p).contains("bodycam_target_uuid")) {
                            targets.put(p.getUUID(), dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(p).getUUID("bodycam_target_uuid"));
                        }

                        if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(p).getBoolean("bodycam_active")) {
                            Vec3 dummyVec = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.get(p.getUUID());
                            if (dummyVec != null) {
                                positions.put(p.getUUID(), new BlockPos((int) dummyVec.x, (int) dummyVec.y, (int) dummyVec.z));
                            } else {
                                Vec3 origVec = dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.ORIGINAL_POS.get(p.getUUID());
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

                    FriendlyByteBuf obuf = PacketByteBufs.create();
                    dev.ClasherHD.bodycam.network.SyncBodycamStatesS2CPacket.encode(new dev.ClasherHD.bodycam.network.SyncBodycamStatesS2CPacket(jammers, targets, dimensions, positions, anonymizers, hasReach, isOnHologram), obuf);
                    ServerPlayNetworking.send(player, BodycamPacketIDs.SYNC_STATES_PACKET_ID, obuf);
                });
            } else if (type == 1) {
                UUID targetId = buf.readUUID();
                boolean hasReachClient = buf.readBoolean();
                boolean isOnHologram = buf.readBoolean();
                server.execute(() -> {
                    boolean hasReachActual = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(dev.ClasherHD.bodycam.BodycamFabric.REACH_ENCHANTMENT, player.getMainHandItem()) > 0;
                    dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putBoolean("bodycam_has_reach", hasReachActual);
                    dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).putBoolean("bodycam_is_hologram", isOnHologram);
                    dev.ClasherHD.bodycam.network.BodycamSetCameraPacket.executeSet(player, targetId, hasReachClient, isOnHologram);
                });
            }
        });
    }
}
