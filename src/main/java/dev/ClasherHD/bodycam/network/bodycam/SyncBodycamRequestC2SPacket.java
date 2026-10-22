package dev.ClasherHD.bodycam.network.bodycam;

import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.compat.curios.CuriosCompat;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class SyncBodycamRequestC2SPacket {
    public final boolean isOnHologram;

    public SyncBodycamRequestC2SPacket(boolean isOnHologram) {
        this.isOnHologram = isOnHologram;
    }

    public static void encode(SyncBodycamRequestC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isOnHologram);
    }

    public static SyncBodycamRequestC2SPacket decode(FriendlyByteBuf buf) {
        return new SyncBodycamRequestC2SPacket(buf.readBoolean());
    }

    public static void handle(SyncBodycamRequestC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) {
                return;
            }

            if (dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.get() && !sender.hasPermissions(2)) {
                sender.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(ChatFormatting.RED));
                return;
            }
            if (msg.isOnHologram && !dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_HOLOGRAM_BLOCK.get()) {
                sender.sendSystemMessage(Component.translatable("message.bodycam.hologram_disabled").withStyle(ChatFormatting.RED));
                return;
            }

            int reachLevel;
            if (!msg.isOnHologram) {
                ItemStack spyglassStack = EnchantmentLevels.heldBodycam(sender);
                if (spyglassStack.isEmpty() || ItemDurabilityHelper.isEmpty(spyglassStack)) {
                    return;
                }
                reachLevel = EnchantmentLevels.reach(spyglassStack);
            } else {
                reachLevel = EnchantmentLevels.sessionReach(sender, true);
            }

            Map<UUID, Integer> jammers = new HashMap<>();
            Map<UUID, UUID> targets = new HashMap<>();
            Map<UUID, String> dimensions = new HashMap<>();
            Map<UUID, BlockPos> positions = new HashMap<>();
            Map<UUID, Boolean> anonymizers = new HashMap<>();
            Set<UUID> hidden = new HashSet<>();
            Map<UUID, String> watchPoints = new LinkedHashMap<>();

            for (ServerPlayer p : sender.server.getPlayerList().getPlayers()) {
                if (EnchantmentLevels.isHiddenFromSelection(p)) {
                    hidden.add(p.getUUID());
                    continue;
                }

                int mode = 0;
                boolean hasAnonymizer = false;
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                    ItemStack stack = p.getInventory().getItem(i);
                    if (stack.getItem() instanceof JammerItem
                            && !ItemDurabilityHelper.isEmpty(stack)
                            && stack.hasTag() && stack.getTag().contains("JammerMode")) {
                        mode = PodestBlockEntity.combineJammerModes(mode, stack.getTag().getInt("JammerMode"));
                    }
                    if (stack.getItem() instanceof AnonymizerItem
                            && !ItemDurabilityHelper.isEmpty(stack)
                            && stack.hasTag() && stack.getTag().contains("AnonymizerActive")) {
                        if (stack.getTag().getBoolean("AnonymizerActive")) {
                            hasAnonymizer = true;
                        }
                    }
                }
                for (ItemStack curioStack : CuriosCompat.getEquippedItems(p, ModItems.JAMMER.get())) {
                    if (!ItemDurabilityHelper.isEmpty(curioStack)
                            && curioStack.hasTag() && curioStack.getTag().contains("JammerMode")) {
                        mode = PodestBlockEntity.combineJammerModes(mode, curioStack.getTag().getInt("JammerMode"));
                    }
                }
                for (ItemStack curioStack : CuriosCompat.getEquippedItems(p, ModItems.ANONYMIZER.get())) {
                    if (!ItemDurabilityHelper.isEmpty(curioStack)
                            && curioStack.hasTag() && curioStack.getTag().getBoolean("AnonymizerActive")) {
                        hasAnonymizer = true;
                    }
                }

                mode = PodestBlockEntity.combineJammerModes(mode, PodestBlockEntity.getPodestJammerMode(p));
                hasAnonymizer = hasAnonymizer || PodestBlockEntity.hasPodestAnonymizer(p);

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
                        net.minecraft.world.phys.Vec3 origVec = BodycamSetCameraPacket.ORIGINAL_POS.get(p.getUUID());
                        if (origVec != null) {
                            positions.put(p.getUUID(), new BlockPos((int) origVec.x, (int) origVec.y, (int) origVec.z));
                        } else {
                            positions.put(p.getUUID(), p.blockPosition());
                        }
                    }
                    String origDim = BodycamSetCameraPacket.ORIGINAL_DIM.get(p.getUUID());
                    dimensions.put(p.getUUID(), origDim != null ? origDim : p.level().dimension().location().toString());
                } else {
                    dimensions.put(p.getUUID(), p.level().dimension().location().toString());
                    positions.put(p.getUUID(), p.blockPosition());
                }
            }

            if (dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_WATCH_POINT.get()) {
                for (WatchPointDummyEntity wp : WatchPointDummyEntity.getActiveForOwner(sender.server, sender.getUUID())) {
                    UUID wpId = wp.getUUID();
                    watchPoints.put(wpId, wp.getSelectionDisplayName());
                    dimensions.put(wpId, wp.level().dimension().location().toString());
                    positions.put(wpId, wp.blockPosition());
                }
            }

            PacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sender),
                    new SyncBodycamStatesS2CPacket(jammers, targets, dimensions, positions, anonymizers, hidden, watchPoints, reachLevel, msg.isOnHologram)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
