package dev.ClasherHD.bodycam.network.logbook;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.item.LogBookItem;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LogBookActionC2SPacket implements CustomPacketPayload {
    public static final Type<LogBookActionC2SPacket> TYPE = new Type<>(bodycam.id("logbook_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LogBookActionC2SPacket> STREAM_CODEC =
            StreamCodec.of(LogBookActionC2SPacket::encode, LogBookActionC2SPacket::decode);

    public static final int ACTION_UNLOCK = 0;
    public static final int ACTION_SET_PIN = 1;

    private static final ConcurrentHashMap<UUID, Long> LAST_ACTION_TICK = new ConcurrentHashMap<>();

    public final int action;
    public final boolean offHand;
    public final String pin;
    public final String newPin;

    public LogBookActionC2SPacket(int action, boolean offHand, String pin, String newPin) {
        this.action = action;
        this.offHand = offHand;
        this.pin = pin == null ? "" : pin;
        this.newPin = newPin == null ? "" : newPin;
    }

    public static void clearCooldown(UUID playerId) {
        if (playerId != null) {
            LAST_ACTION_TICK.remove(playerId);
        }
    }

    public static void clearCooldowns() {
        LAST_ACTION_TICK.clear();
    }

    public static void encode(RegistryFriendlyByteBuf buf, LogBookActionC2SPacket msg) {
        buf.writeVarInt(msg.action);
        buf.writeBoolean(msg.offHand);
        buf.writeUtf(msg.pin, 8);
        buf.writeUtf(msg.newPin, 8);
    }

    public static LogBookActionC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new LogBookActionC2SPacket(buf.readVarInt(), buf.readBoolean(), buf.readUtf(8), buf.readUtf(8));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LogBookActionC2SPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            long now = player.level().getGameTime();
            Long last = LAST_ACTION_TICK.get(player.getUUID());
            if (last != null && now - last < 5L && now >= last) {
                return;
            }
            LAST_ACTION_TICK.put(player.getUUID(), now);
            ItemStack stack = msg.offHand ? player.getOffhandItem() : player.getMainHandItem();
            if (!(stack.getItem() instanceof LogBookItem)) {
                return;
            }
            if (msg.action == ACTION_UNLOCK) {
                LogBookItem.handleUnlock(player, stack, msg.pin);
            } else if (msg.action == ACTION_SET_PIN) {
                LogBookItem.handleSetPin(player, stack, msg.pin, msg.newPin);
            }
        });
    }
}
