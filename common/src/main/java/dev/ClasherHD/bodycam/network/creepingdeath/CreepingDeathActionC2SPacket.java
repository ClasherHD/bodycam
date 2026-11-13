package dev.ClasherHD.bodycam.network.creepingdeath;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import dev.ClasherHD.bodycam.item.CreepingDeathItem;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.registry.ModSounds;
import dev.ClasherHD.bodycam.util.PersistentDataAccess;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class CreepingDeathActionC2SPacket implements CustomPacketPayload {
    public static final Type<CreepingDeathActionC2SPacket> TYPE = new Type<>(bodycam.id("creeping_death_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreepingDeathActionC2SPacket> STREAM_CODEC =
            StreamCodec.of(CreepingDeathActionC2SPacket::encode, CreepingDeathActionC2SPacket::decode);

    public final int mode;
    public final boolean offHand;
    public final UUID chosenUUID;
    public final String chosenName;

    public CreepingDeathActionC2SPacket(int mode, boolean offHand, UUID chosenUUID, String chosenName) {
        this.mode = mode;
        this.offHand = offHand;
        this.chosenUUID = chosenUUID;
        this.chosenName = chosenName != null ? chosenName : "";
    }

    public static void encode(RegistryFriendlyByteBuf buf, CreepingDeathActionC2SPacket msg) {
        buf.writeVarInt(msg.mode);
        buf.writeBoolean(msg.offHand);
        buf.writeBoolean(msg.chosenUUID != null);
        if (msg.chosenUUID != null) {
            buf.writeUUID(msg.chosenUUID);
        }
        buf.writeUtf(msg.chosenName, 64);
    }

    public static CreepingDeathActionC2SPacket decode(RegistryFriendlyByteBuf buf) {
        int mode = buf.readVarInt();
        boolean offHand = buf.readBoolean();
        UUID id = buf.readBoolean() ? buf.readUUID() : null;
        String name = buf.readUtf(64);
        return new CreepingDeathActionC2SPacket(mode, offHand, id, name);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CreepingDeathActionC2SPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer sender) || sender.server == null) {
                return;
            }
            if (ModServerConfig.OP_ONLY_MODE.get() && !sender.hasPermissions(2)) {
                return;
            }
            if (!ModServerConfig.ENABLE_CREEPING_DEATH.get() || ModServerConfig.isDimensionBlacklisted(sender.level())) {
                return;
            }

            InteractionHand hand = msg.offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack stack = sender.getItemInHand(hand);
            if (!(stack.getItem() instanceof CreepingDeathItem)) {
                for (InteractionHand h : InteractionHand.values()) {
                    if (sender.getItemInHand(h).getItem() instanceof CreepingDeathItem) {
                        stack = sender.getItemInHand(h);
                        break;
                    }
                }
            }
            if (!(stack.getItem() instanceof CreepingDeathItem)) {
                return;
            }

            if (msg.mode == OpenCreepingDeathScreenS2CPacket.MODE_SKIN) {
                if (msg.chosenUUID == null || msg.chosenName.isEmpty()) {
                    CreepingDeathItem.clearSkin(stack);
                    sender.displayClientMessage(
                            Component.translatable("message.bodycam.creeping_death.skin_reset").withStyle(ChatFormatting.GRAY),
                            true);
                } else {
                    CreepingDeathItem.setSkin(stack, msg.chosenUUID, msg.chosenName);
                    sender.displayClientMessage(
                            Component.translatable("message.bodycam.creeping_death.skin_selected", msg.chosenName).withStyle(ChatFormatting.GRAY),
                            true);
                }
                return;
            }

            if (msg.mode == OpenCreepingDeathScreenS2CPacket.MODE_TARGET && msg.chosenUUID != null) {
                if (msg.chosenUUID.equals(sender.getUUID())) {
                    return;
                }
                ServerPlayer target = sender.server.getPlayerList().getPlayer(msg.chosenUUID);
                CompoundTag targetData = target != null ? PersistentDataAccess.get(target) : null;
                boolean targetInBodycam = targetData != null && targetData.getBoolean("bodycam_active");
                if (target == null || !target.isAlive()
                        || target.level().dimension() != sender.level().dimension()
                        || target.isCreative() || (target.isSpectator() && !targetInBodycam)) {
                    sender.sendSystemMessage(
                            Component.translatable("message.bodycam.creeping_death.invalid_target").withStyle(ChatFormatting.RED));
                    return;
                }
                if (targetInBodycam || targetData.getBoolean("bodycam_all_seeing_pending")) {
                    BodycamResetCameraPacket.executeReset(target);
                    CreepingDeathEntity.sendHuntNoEscapeTitle(target);
                }
                if (CreepingDeathEntity.isTargetAlreadyHunted(sender.server, target.getUUID())) {
                    sender.sendSystemMessage(
                            Component.translatable("message.bodycam.creeping_death.already_hunted").withStyle(ChatFormatting.RED));
                    return;
                }

                UUID skinUUID = CreepingDeathItem.getSkinUUID(stack);
                String skinName = CreepingDeathItem.getSkinName(stack);
                if (skinUUID == null || skinName.isEmpty()) {
                    skinUUID = target.getUUID();
                    skinName = target.getGameProfile().getName();
                }

                stack.shrink(1);

                CompoundTag senderData = PersistentDataAccess.get(sender);
                senderData.putInt("bodycam_creeping_death_fog_ticks", CreepingDeathItem.USER_PARTICLE_TICKS);
                senderData.putDouble("bodycam_cd_fog_x", sender.getX());
                senderData.putDouble("bodycam_cd_fog_y", sender.getY());
                senderData.putDouble("bodycam_cd_fog_z", sender.getZ());
                senderData.putString("bodycam_cd_fog_dim", sender.level().dimension().location().toString());

                sender.serverLevel().sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        sender.getX(), sender.getY() + 1.0D, sender.getZ(),
                        35, 0.75D, 0.85D, 0.75D, 0.02D
                );
                sender.serverLevel().sendParticles(
                        ParticleTypes.SCULK_SOUL,
                        sender.getX(), sender.getY() + 1.0D, sender.getZ(),
                        25, 0.75D, 0.85D, 0.75D, 0.03D
                );
                sender.level().playSound(
                        null,
                        sender.getX(), sender.getY(), sender.getZ(),
                        ModSounds.CREEPING_DEATH_USE.get(),
                        SoundSource.PLAYERS,
                        1.2F, 0.6F
                );

                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 220, 0, false, false, true));
                target.serverLevel().playSound(
                        null,
                        target.getX(), target.getY(), target.getZ(),
                        ModSounds.CREEPING_DEATH_HEARTBEAT.get(),
                        SoundSource.HOSTILE,
                        2.0F, 0.9F
                );

                CreepingDeathEntity.scheduleSpawn(target.getUUID(), skinUUID, skinName);
            }
        });
    }
}
