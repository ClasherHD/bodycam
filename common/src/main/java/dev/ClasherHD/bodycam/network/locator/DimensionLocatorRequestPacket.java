package dev.ClasherHD.bodycam.network.locator;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.item.DimensionLocatorItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DimensionLocatorRequestPacket implements CustomPacketPayload {
    public static final Type<DimensionLocatorRequestPacket> TYPE = new Type<>(bodycam.id("dimension_locator_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DimensionLocatorRequestPacket> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> {}, buf -> new DimensionLocatorRequestPacket());

    private static final ConcurrentHashMap<UUID, Long> LAST_REQUEST_TICK = new ConcurrentHashMap<>();

    public DimensionLocatorRequestPacket() {
    }

    public static void clearCooldown(UUID playerId) {
        if (playerId != null) {
            LAST_REQUEST_TICK.remove(playerId);
        }
    }

    public static void clearCooldowns() {
        LAST_REQUEST_TICK.clear();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DimensionLocatorRequestPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer sender) || sender.server == null) {
                return;
            }
            if ((ModServerConfig.OP_ONLY_MODE.get() && !sender.hasPermissions(2))
                    || !ModServerConfig.ENABLE_DIMENSION_LOCATOR.get()
                    || ModServerConfig.isDimensionBlacklisted(sender.level())) {
                return;
            }
            ItemStack locatorStack = sender.getMainHandItem();
            if (!(locatorStack.getItem() instanceof DimensionLocatorItem) || ItemDurabilityHelper.isEmpty(locatorStack)) {
                locatorStack = sender.getOffhandItem();
            }
            if (!(locatorStack.getItem() instanceof DimensionLocatorItem) || ItemDurabilityHelper.isEmpty(locatorStack)) {
                return;
            }
            long now = sender.level().getGameTime();
            Long last = LAST_REQUEST_TICK.get(sender.getUUID());
            if (last != null && now - last < 20L && now >= last) {
                return;
            }
            LAST_REQUEST_TICK.put(sender.getUUID(), now);
            ItemDurabilityHelper.damagePerUse(locatorStack, sender, 1);
            PacketHandler.sendToPlayer(sender, new DimensionLocatorResponsePacket(DimensionLocatorItem.collectPlayerDimensions(sender)));
        });
    }
}
