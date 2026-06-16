package dev.ClasherHD.bodycam.network;

import dev.ClasherHD.bodycam.item.PlayerLocatorCompassItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings({"null", "deprecation"})
public class PlayerLocatorSelectC2SPacket {
    private final UUID targetUUID;
    private final boolean isDeselect;

    public PlayerLocatorSelectC2SPacket(UUID targetUUID, boolean isDeselect) {
        this.targetUUID = targetUUID;
        this.isDeselect = isDeselect;
    }

    public static void encode(PlayerLocatorSelectC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.targetUUID != null);
        if (msg.targetUUID != null) {
            buf.writeUUID(msg.targetUUID);
        }
        buf.writeBoolean(msg.isDeselect);
    }

    public static PlayerLocatorSelectC2SPacket decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readBoolean() ? buf.readUUID() : null;
        boolean deselect = buf.readBoolean();
        return new PlayerLocatorSelectC2SPacket(uuid, deselect);
    }

    public static void handle(PlayerLocatorSelectC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            ItemStack stack = sender.getItemInHand(InteractionHand.MAIN_HAND);
            if (!(stack.getItem() instanceof PlayerLocatorCompassItem)) {
                stack = sender.getItemInHand(InteractionHand.OFF_HAND);
            }

            if (stack.getItem() instanceof PlayerLocatorCompassItem) {
                PlayerLocatorCompassItem item = (PlayerLocatorCompassItem) stack.getItem();
                CompoundTag tag = stack.getOrCreateTag();
                if (msg.isDeselect) {
                    item.resetToIdle(tag);
                } else if (msg.targetUUID != null) {
                    tag.putUUID("LocatorTargetUUID", msg.targetUUID);
                    ServerPlayer targetPlayer = sender.server.getPlayerList().getPlayer(msg.targetUUID);
                    if (targetPlayer != null) {
                        boolean hasReach = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                                dev.ClasherHD.bodycam.registry.ModEnchantments.REACH_ENCHANTMENT.get(), stack) > 0;
                        item.calculateState(stack, sender, targetPlayer, hasReach, sender.level().getGameTime());
                    } else {
                        item.resetToIdle(tag);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
