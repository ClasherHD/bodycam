package dev.ClasherHD.bodycam.item;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorSyncS2CPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorStructureUpdateS2CPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorTargetUpdateS2CPacket;
import dev.ClasherHD.bodycam.registry.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"null", "deprecation"})
public class PlayerLocatorCompassItem extends Item {

    public PlayerLocatorCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer) {
            ServerPlayer sPlayer = (ServerPlayer) player;

            if (ModServerConfig.OP_ONLY_MODE.get() && !sPlayer.hasPermissions(2)) {
                sPlayer.sendSystemMessage(Component.translatable("message.bodycam.op_only").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
            if (!ModServerConfig.ENABLE_PLAYER_LOCATOR.get()) {
                sPlayer.sendSystemMessage(Component.translatable("message.bodycam.player_locator_disabled").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }

            level.playSound(null, sPlayer.getX(), sPlayer.getY(), sPlayer.getZ(),
                    dev.ClasherHD.bodycam.registry.ModSounds.PLAYER_LOCATOR_USED.get(), SoundSource.PLAYERS, 0.25F, 1.0F);

            Map<UUID, Integer> jammers = new HashMap<>();
            Map<UUID, String> dimensions = new HashMap<>();
            Map<UUID, BlockPos> positions = new HashMap<>();

            for (ServerPlayer p : sPlayer.server.getPlayerList().getPlayers()) {
                if (!p.getUUID().equals(sPlayer.getUUID())) {
                    jammers.put(p.getUUID(), getJammerMode(p));
                    dimensions.put(p.getUUID(), getTargetDimension(p));
                    positions.put(p.getUUID(), getTargetPosition(p));
                }
            }

            UUID currentTarget = stack.hasTag() && stack.getOrCreateTag().contains("LocatorTargetUUID")
                    ? stack.getOrCreateTag().getUUID("LocatorTargetUUID")
                    : null;

            boolean hasReach = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.REACH_ENCHANTMENT.get(), stack) > 0;

            PacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sPlayer),
                    new PlayerLocatorSyncS2CPacket(jammers, dimensions, positions, currentTarget, hasReach)
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer)) {
            return;
        }

        ServerPlayer owner = (ServerPlayer) entity;
        CompoundTag tag = stack.getOrCreateTag();

        if (!tag.contains("LocatorTargetUUID")) {
            if (tag.getInt("LocatorState") != 0) {
                tag.putInt("LocatorState", 0);
            }
            return;
        }

        UUID targetUUID = tag.getUUID("LocatorTargetUUID");
        ServerPlayer targetPlayer = owner.server.getPlayerList().getPlayer(targetUUID);

        if (targetPlayer == null) {
            resetToIdle(tag);
            return;
        }

        long currentTick = owner.level().getGameTime();
        int currentState = tag.getInt("LocatorState");
        if (currentState == 2 || currentState == 4) {
            long timestamp = tag.getLong("LocatorStateTimestamp");
            if (currentTick - timestamp >= 200) {
                resetToIdle(tag);
                return;
            }
        }

        if (currentState == 1) {
            if (currentTick % 20 == 0) {
                PacketHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> owner),
                        new PlayerLocatorTargetUpdateS2CPacket(targetUUID, getTargetPosition(targetPlayer), getTargetDimension(targetPlayer))
                );
            }
        }

        boolean hasReach = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.REACH_ENCHANTMENT.get(), stack) > 0;
        long lastCheck = tag.getLong("LocatorLastCheckTick");
        String lastTargetDim = tag.getString("LocatorLastTargetDim");
        String myLastDim = tag.getString("LocatorLastMyDim");

        String currentTargetDim = getTargetDimension(targetPlayer);
        String currentMyDim = owner.level().dimension().location().toString();

        boolean dimChanged = !currentTargetDim.equals(lastTargetDim) || !currentMyDim.equals(myLastDim);
        boolean periodic = (currentTick - lastCheck >= 100);

        if (dimChanged || periodic || !tag.contains("LocatorState")) {
            calculateState(stack, owner, targetPlayer, hasReach, currentTick);
        }
    }

    public void calculateState(ItemStack stack, ServerPlayer owner, ServerPlayer targetPlayer, boolean hasReach, long currentTick) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong("LocatorLastCheckTick", currentTick);

        String currentTargetDim = getTargetDimension(targetPlayer);
        String currentMyDim = owner.level().dimension().location().toString();
        
        boolean dimChanged = !currentTargetDim.equals(tag.getString("LocatorLastTargetDim")) 
                || !currentMyDim.equals(tag.getString("LocatorLastMyDim"));

        tag.putString("LocatorLastTargetDim", currentTargetDim);
        tag.putString("LocatorLastMyDim", currentMyDim);

        int jammerMode = getJammerMode(targetPlayer);
        double distSq = owner.blockPosition().distSqr(getTargetPosition(targetPlayer));
        double maxDist = ModServerConfig.MAX_MONITOR_DISTANCE.get();
        double maxDistSq = maxDist * maxDist;
        boolean sameDim = currentMyDim.equals(currentTargetDim);

        boolean isJammed = (jammerMode == 1) || (jammerMode == 2 && (!sameDim || distSq > maxDistSq));

        int newState;
        if (isJammed) {
            newState = 4;
        } else if (sameDim) {
            if (distSq <= maxDistSq) {
                newState = 1;
            } else {
                newState = 2;
            }
        } else {
            if (hasReach) {
                boolean myVanilla = isVanillaDim(owner.level().dimension());
                boolean targetVanilla = isVanillaDim(targetPlayer.level().dimension());
                if (myVanilla && targetVanilla) {
                    newState = 3;
                } else {
                    newState = 2;
                }
            } else {
                newState = 2;
            }
        }

        int previousState = tag.contains("LocatorState") ? tag.getInt("LocatorState") : -1;

        if (newState != previousState || dimChanged) {
            tag.putInt("LocatorState", newState);
            tag.putLong("LocatorStateTimestamp", currentTick);

            if (newState == 3) {
                BlockPos structPos = findRoutingStructure(owner, targetPlayer.level().dimension());
                if (structPos != null) {
                    tag.putInt("LocatorCachedStructX", structPos.getX());
                    tag.putInt("LocatorCachedStructZ", structPos.getZ());

                    PacketHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> owner),
                            new PlayerLocatorStructureUpdateS2CPacket(structPos, currentMyDim)
                    );
                } else {
                    tag.putInt("LocatorState", 2);
                }
            } else {
                tag.remove("LocatorCachedStructX");
                tag.remove("LocatorCachedStructZ");
            }
        }
    }

    public void resetToIdle(CompoundTag tag) {
        tag.remove("LocatorTargetUUID");
        tag.remove("LocatorTargetUUIDMost");
        tag.remove("LocatorTargetUUIDLeast");
        tag.putInt("LocatorState", 0);
        tag.remove("LocatorStateTimestamp");
        tag.remove("LocatorCachedStructX");
        tag.remove("LocatorCachedStructZ");
        tag.remove("LocatorLastTargetDim");
        tag.remove("LocatorLastMyDim");
        tag.remove("LocatorLastCheckTick");
    }

    private int getJammerMode(ServerPlayer player) {
        if (player.getPersistentData().hasUUID("bodycam_active_jammer_id")) {
            long lastHeartbeat = player.getPersistentData().getLong("bodycam_jammer_heartbeat");
            boolean isCreative = player.isCreative();
            if (isCreative || (player.level().getGameTime() - lastHeartbeat <= 10)) {
                return player.getPersistentData().getInt("bodycam_jammer_mode");
            }
        }
        return 0;
    }

    private boolean isVanillaDim(ResourceKey<Level> dim) {
        return dim == Level.OVERWORLD || dim == Level.NETHER || dim == Level.END;
    }

    private BlockPos findRoutingStructure(ServerPlayer owner, ResourceKey<Level> targetDim) {
        ServerLevel level = owner.serverLevel();
        ResourceKey<Level> myDim = level.dimension();
        BlockPos playerPos = owner.blockPosition();

        if (myDim == Level.END) {
            return new BlockPos(0, 64, 0);
        } else if (myDim == Level.NETHER) {
            return level.findNearestMapStructure(StructureTags.RUINED_PORTAL, playerPos, 100, false);
        } else if (myDim == Level.OVERWORLD) {
            if (targetDim == Level.NETHER) {
                return level.findNearestMapStructure(StructureTags.RUINED_PORTAL, playerPos, 100, false);
            } else if (targetDim == Level.END) {
                return level.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, playerPos, 100, false);
            }
        }
        return null;
    }

    private BlockPos getTargetPosition(ServerPlayer p) {
        if (p.getPersistentData().getBoolean("bodycam_active")) {
            net.minecraft.world.phys.Vec3 dummyVec = dev.ClasherHD.bodycam.entity.BodycamDummyEntity.DUMMY_POS.get(p.getUUID());
            if (dummyVec != null) {
                return new BlockPos((int) dummyVec.x, (int) dummyVec.y, (int) dummyVec.z);
            }
            net.minecraft.world.phys.Vec3 origVec = dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket.ORIGINAL_POS.get(p.getUUID());
            if (origVec != null) {
                return new BlockPos((int) origVec.x, (int) origVec.y, (int) origVec.z);
            }
            if (p.getPersistentData().contains("bodycam_orig_x")) {
                return new BlockPos(
                        (int) p.getPersistentData().getDouble("bodycam_orig_x"),
                        (int) p.getPersistentData().getDouble("bodycam_orig_y"),
                        (int) p.getPersistentData().getDouble("bodycam_orig_z")
                );
            }
        }
        return p.blockPosition();
    }

    private String getTargetDimension(ServerPlayer p) {
        if (p.getPersistentData().getBoolean("bodycam_active")) {
            String origDim = dev.ClasherHD.bodycam.network.bodycam.BodycamSetCameraPacket.ORIGINAL_DIM.get(p.getUUID());
            if (origDim != null) {
                return origDim;
            }
            if (p.getPersistentData().contains("bodycam_orig_dim")) {
                return p.getPersistentData().getString("bodycam_orig_dim");
            }
        }
        return p.level().dimension().location().toString();
    }
}
