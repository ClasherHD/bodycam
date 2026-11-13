package dev.ClasherHD.bodycam.network.locator;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.item.PlayerLocatorCompassItem;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerLocatorSelectC2SPacket implements CustomPacketPayload {
    public static final Type<PlayerLocatorSelectC2SPacket> TYPE = new Type<>(bodycam.id("player_locator_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerLocatorSelectC2SPacket> STREAM_CODEC =
            StreamCodec.of(PlayerLocatorSelectC2SPacket::encode, PlayerLocatorSelectC2SPacket::decode);

    private static final ConcurrentHashMap<UUID, Long> LAST_SELECT_TICK = new ConcurrentHashMap<>();

    public final UUID targetUUID;
    public final boolean isDeselect;

    public PlayerLocatorSelectC2SPacket(UUID targetUUID, boolean isDeselect) {
        this.targetUUID = targetUUID;
        this.isDeselect = isDeselect;
    }

    public static void clearCooldown(UUID playerId) {
        if (playerId != null) {
            LAST_SELECT_TICK.remove(playerId);
        }
    }

    public static void clearCooldowns() {
        LAST_SELECT_TICK.clear();
    }

    public static void encode(RegistryFriendlyByteBuf buf, PlayerLocatorSelectC2SPacket msg) {
        buf.writeBoolean(msg.targetUUID != null);
        if (msg.targetUUID != null) {
            buf.writeUUID(msg.targetUUID);
        }
        buf.writeBoolean(msg.isDeselect);
    }

    public static PlayerLocatorSelectC2SPacket decode(RegistryFriendlyByteBuf buf) {
        UUID id = buf.readBoolean() ? buf.readUUID() : null;
        boolean deselect = buf.readBoolean();
        return new PlayerLocatorSelectC2SPacket(id, deselect);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerLocatorSelectC2SPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer sender) || sender.server == null) {
                return;
            }
            if ((ModServerConfig.OP_ONLY_MODE.get() && !sender.hasPermissions(2))
                    || !ModServerConfig.ENABLE_PLAYER_LOCATOR.get()
                    || ModServerConfig.isDimensionBlacklisted(sender.level())) {
                return;
            }
            long now = sender.level().getGameTime();
            Long last = LAST_SELECT_TICK.get(sender.getUUID());
            if (last != null && now - last < 20L && now >= last) {
                return;
            }
            LAST_SELECT_TICK.put(sender.getUUID(), now);
            PlayerLocatorCompassItem.handleSelection(sender, msg.targetUUID, msg.isDeselect);
        });
    }
}
