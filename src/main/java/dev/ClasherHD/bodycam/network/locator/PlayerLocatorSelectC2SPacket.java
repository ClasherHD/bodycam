package dev.ClasherHD.bodycam.network.locator;

import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.PlayerLocatorCompassItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
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
            if (sender == null) {
                return;
            }

            ItemStack stack = sender.getItemInHand(InteractionHand.MAIN_HAND);
            if (!(stack.getItem() instanceof PlayerLocatorCompassItem)) {
                stack = sender.getItemInHand(InteractionHand.OFF_HAND);
            }

            if (stack.getItem() instanceof PlayerLocatorCompassItem item) {
                CompoundTag tag = stack.getOrCreateTag();
                if (ItemDurabilityHelper.isEmpty(stack) || msg.isDeselect) {
                    item.resetToIdle(tag);
                } else if (msg.targetUUID != null) {
                    item.resetToIdle(tag);
                    tag.putUUID("LocatorTargetUUID", msg.targetUUID);
                    ServerPlayer targetPlayer = sender.server.getPlayerList().getPlayer(msg.targetUUID);
                    if (targetPlayer != null && !dev.ClasherHD.bodycam.enchantment.EnchantmentLevels.isHiddenFromSelection(targetPlayer)) {
                        int reachLevel = dev.ClasherHD.bodycam.enchantment.EnchantmentLevels.reach(stack);
                        item.calculateState(stack, sender, targetPlayer, reachLevel, sender.level().getGameTime());
                    } else {
                        item.resetToIdle(tag);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
