package dev.ClasherHD.bodycam.network.bodycam;

import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.platform.BodycamPlatform;
import dev.ClasherHD.bodycam.registry.ModBlocks;
import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("null")
public class SyncBodycamRequestC2SPacket implements CustomPacketPayload {
    public static final Type<SyncBodycamRequestC2SPacket> TYPE = new Type<>(bodycam.id("sync_bodycam_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBodycamRequestC2SPacket> STREAM_CODEC =
            StreamCodec.of(SyncBodycamRequestC2SPacket::encode, SyncBodycamRequestC2SPacket::decode);

    public final boolean isOnHologram;

    public SyncBodycamRequestC2SPacket(boolean isOnHologram) {
        this.isOnHologram = isOnHologram;
    }

    public static void encode(RegistryFriendlyByteBuf buf, SyncBodycamRequestC2SPacket msg) {
        buf.writeBoolean(msg.isOnHologram);
    }

    public static SyncBodycamRequestC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new SyncBodycamRequestC2SPacket(buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncBodycamRequestC2SPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer sender) || sender.server == null) {
                return;
            }
            CompoundTag senderData = PersistentDataAccess.get(sender);
            long now = sender.level().getGameTime();
            long lastCd = senderData.getLong("bodycam_sync_req_cd");
            if (now - lastCd < 5L && now >= lastCd) {
                return;
            }
            senderData.putLong("bodycam_sync_req_cd", now);

            if (ModServerConfig.OP_ONLY_MODE.get() && !sender.hasPermissions(2)) {
                sender.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
                return;
            }
            if (ModServerConfig.isDimensionBlacklisted(sender.level())) {
                sender.sendSystemMessage(Component.translatable("message.bodycam.dimension_blacklisted").withStyle(ChatFormatting.RED));
                return;
            }
            if (CreepingDeathEntity.blockObservationIfHunted(sender)) {
                return;
            }

            boolean isOnHologram = msg.isOnHologram;
            if (isOnHologram) {
                if (!ModServerConfig.ENABLE_HOLOGRAM_BLOCK.get()) {
                    sender.sendSystemMessage(Component.translatable("message.bodycam.hologram_disabled").withStyle(ChatFormatting.RED));
                    return;
                }
                boolean onHologramBlock = sender.level().getBlockState(sender.blockPosition().below()).is(ModBlocks.HOLOGRAM_BLOCK.get())
                        || sender.level().getBlockState(sender.blockPosition()).is(ModBlocks.HOLOGRAM_BLOCK.get());
                if (!onHologramBlock) {
                    isOnHologram = false;
                }
            }

            int reachLevel;
            if (!isOnHologram) {
                ItemStack spyglassStack = EnchantmentLevels.heldBodycam(sender);
                if (spyglassStack.isEmpty() || ItemDurabilityHelper.isEmpty(spyglassStack)) {
                    return;
                }
                reachLevel = EnchantmentLevels.reach(spyglassStack);
            } else {
                reachLevel = EnchantmentLevels.sessionReach(sender, true);
            }

            syncForPlayer(sender, reachLevel, isOnHologram, false);
        });
    }

    public static void sendAllSeeingEyeState(ServerPlayer sender) {
        if (sender == null || sender.server == null) {
            return;
        }
        syncForPlayer(sender, 2, false, true);
    }

    private static void syncForPlayer(ServerPlayer sender, int reachLevel, boolean isOnHologram, boolean isAllSeeingEye) {
        Map<UUID, Integer> jammers = new HashMap<>();
        Map<UUID, UUID> targets = new HashMap<>();
        Map<UUID, String> dimensions = new HashMap<>();
        Map<UUID, BlockPos> positions = new HashMap<>();
        Map<UUID, Boolean> anonymizers = new HashMap<>();
        Set<UUID> hidden = new HashSet<>();
        Map<UUID, String> watchPoints = new LinkedHashMap<>();

        String senderDim = sender.level().dimension().location().toString();
        double maxDist = ModServerConfig.MAX_MONITOR_DISTANCE.get();
        double maxDistSq = maxDist * maxDist;

        for (ServerPlayer p : sender.server.getPlayerList().getPlayers()) {
            if (!isAllSeeingEye && EnchantmentLevels.isHiddenFromSelection(p)) {
                hidden.add(p.getUUID());
                continue;
            }

            int mode = 0;
            boolean hasAnonymizer = false;
            if (!isAllSeeingEye) {
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                    ItemStack stack = p.getInventory().getItem(i);
                    CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
                    if (stack.getItem() instanceof JammerItem
                            && !ItemDurabilityHelper.isEmpty(stack)
                            && tag != null && tag.contains("JammerMode")) {
                        mode = PodestBlockEntity.combineJammerModes(mode, tag.getInt("JammerMode"));
                    }
                    if (stack.getItem() instanceof AnonymizerItem
                            && !ItemDurabilityHelper.isEmpty(stack)
                            && tag != null && tag.getBoolean("AnonymizerActive")) {
                        hasAnonymizer = true;
                    }
                }
                for (ItemStack accStack : BodycamPlatform.getEquippedAccessoryItems(p, ModItems.JAMMER.get())) {
                    CompoundTag tag = ItemNbtAccess.getReadOnlyTag(accStack);
                    if (!ItemDurabilityHelper.isEmpty(accStack) && tag != null && tag.contains("JammerMode")) {
                        mode = PodestBlockEntity.combineJammerModes(mode, tag.getInt("JammerMode"));
                    }
                }
                for (ItemStack accStack : BodycamPlatform.getEquippedAccessoryItems(p, ModItems.ANONYMIZER.get())) {
                    CompoundTag tag = ItemNbtAccess.getReadOnlyTag(accStack);
                    if (!ItemDurabilityHelper.isEmpty(accStack) && tag != null && tag.getBoolean("AnonymizerActive")) {
                        hasAnonymizer = true;
                    }
                }

                mode = PodestBlockEntity.combineJammerModes(mode, PodestBlockEntity.getPodestJammerMode(p));
                hasAnonymizer = hasAnonymizer || PodestBlockEntity.hasPodestAnonymizer(p);
            }

            jammers.put(p.getUUID(), mode);
            anonymizers.put(p.getUUID(), hasAnonymizer);

            CompoundTag pData = PersistentDataAccess.get(p);
            if (pData.getBoolean("bodycam_active") && pData.contains("bodycam_target_uuid")) {
                targets.put(p.getUUID(), pData.getUUID("bodycam_target_uuid"));
            }

            BlockPos targetPos;
            String targetDim;
            if (pData.getBoolean("bodycam_active")) {
                Vec3 dummyVec = BodycamDummyEntity.DUMMY_POS.get(p.getUUID());
                if (dummyVec != null) {
                    targetPos = BlockPos.containing(dummyVec);
                } else {
                    Vec3 origVec = BodycamSetCameraPacket.ORIGINAL_POS.get(p.getUUID());
                    if (origVec != null) {
                        targetPos = BlockPos.containing(origVec);
                    } else if (pData.contains("bodycam_orig_x")) {
                        targetPos = BlockPos.containing(
                                pData.getDouble("bodycam_orig_x"),
                                pData.getDouble("bodycam_orig_y"),
                                pData.getDouble("bodycam_orig_z")
                        );
                    } else {
                        targetPos = p.blockPosition();
                    }
                }
                String origDim = BodycamSetCameraPacket.ORIGINAL_DIM.get(p.getUUID());
                if (origDim == null && pData.contains("bodycam_orig_dim")) {
                    origDim = pData.getString("bodycam_orig_dim");
                }
                targetDim = origDim != null ? origDim : p.level().dimension().location().toString();
            } else {
                targetDim = p.level().dimension().location().toString();
                targetPos = p.blockPosition();
            }

            if (isAllSeeingEye) {
                dimensions.put(p.getUUID(), targetDim);
                positions.put(p.getUUID(), targetPos);
                continue;
            }

            boolean sameDim = senderDim.equals(targetDim);
            double distSq = sender.blockPosition().distSqr(targetPos);
            boolean withinBaseDist = sameDim && (distSq <= maxDistSq);
            boolean isJammed = (mode == 1) || (mode == 2 && !withinBaseDist);

            boolean inReach;
            if (reachLevel >= 2) {
                inReach = true;
            } else if (reachLevel >= 1) {
                inReach = sameDim;
            } else {
                inReach = withinBaseDist;
            }

            if (isJammed || EnchantmentLevels.isDimensionScrambled(p)) {
                dimensions.put(p.getUUID(), senderDim);
            } else {
                dimensions.put(p.getUUID(), targetDim);
            }

            if (!isJammed && inReach) {
                positions.put(p.getUUID(), withinBaseDist ? sender.blockPosition() : new BlockPos(30_000_000, 0, 30_000_000));
            }
        }

        if (ModServerConfig.ENABLE_WATCH_POINT.get()) {
            if (isAllSeeingEye) {
                for (WatchPointDummyEntity wp : WatchPointDummyEntity.getAllActive(sender.server)) {
                    UUID wpId = wp.getUUID();
                    String ownerName = wp.getOwnerName();
                    String wpName = wp.getSelectionDisplayName();
                    String display = (ownerName != null && !ownerName.isEmpty() && !sender.getUUID().equals(wp.getOwnerUuidOrNull()))
                            ? ownerName + ": " + wpName
                            : wpName;
                    watchPoints.put(wpId, display);
                    dimensions.put(wpId, wp.level().dimension().location().toString());
                    positions.put(wpId, wp.blockPosition());
                }
            } else {
                for (WatchPointDummyEntity wp : WatchPointDummyEntity.getActiveForOwner(sender.server, sender.getUUID())) {
                    UUID wpId = wp.getUUID();
                    watchPoints.put(wpId, wp.getSelectionDisplayName());
                    dimensions.put(wpId, wp.level().dimension().location().toString());
                    positions.put(wpId, wp.blockPosition());
                }
            }
        }

        PacketHandler.sendToPlayer(sender, new SyncBodycamStatesS2CPacket(
                jammers, targets, dimensions, positions, anonymizers, hidden, watchPoints, reachLevel, isOnHologram, isAllSeeingEye
        ));
    }
}
