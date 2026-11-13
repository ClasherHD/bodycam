package dev.ClasherHD.bodycam.network.locator;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.item.DimensionLocatorItem;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class DimensionLocatorSelectC2SPacket implements CustomPacketPayload {
    public static final Type<DimensionLocatorSelectC2SPacket> TYPE = new Type<>(bodycam.id("dimension_locator_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DimensionLocatorSelectC2SPacket> STREAM_CODEC =
            StreamCodec.of(DimensionLocatorSelectC2SPacket::encode, DimensionLocatorSelectC2SPacket::decode);

    public final String dimension;

    public DimensionLocatorSelectC2SPacket(String dimension) {
        this.dimension = dimension == null ? "" : dimension;
    }

    public static void encode(RegistryFriendlyByteBuf buf, DimensionLocatorSelectC2SPacket msg) {
        buf.writeUtf(msg.dimension, 256);
    }

    public static DimensionLocatorSelectC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new DimensionLocatorSelectC2SPacket(buf.readUtf(256));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DimensionLocatorSelectC2SPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer sender) || sender.server == null) {
                return;
            }
            if ((ModServerConfig.OP_ONLY_MODE.get() && !sender.hasPermissions(2))
                    || !ModServerConfig.ENABLE_DIMENSION_LOCATOR.get()
                    || ModServerConfig.isDimensionBlacklisted(sender.level())) {
                return;
            }
            DimensionLocatorItem.handleSelectDimension(sender, msg.dimension);
        });
    }
}
