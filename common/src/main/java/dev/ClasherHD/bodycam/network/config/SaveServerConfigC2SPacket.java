package dev.ClasherHD.bodycam.network.config;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ServerConfigSnapshot;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class SaveServerConfigC2SPacket implements CustomPacketPayload {
    public static final Type<SaveServerConfigC2SPacket> TYPE = new Type<>(bodycam.id("save_server_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveServerConfigC2SPacket> STREAM_CODEC =
            StreamCodec.of(SaveServerConfigC2SPacket::encode, SaveServerConfigC2SPacket::decode);

    public final ServerConfigSnapshot snapshot;

    public SaveServerConfigC2SPacket(ServerConfigSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public static void encode(RegistryFriendlyByteBuf buf, SaveServerConfigC2SPacket msg) {
        msg.snapshot.encode(buf);
    }

    public static SaveServerConfigC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new SaveServerConfigC2SPacket(ServerConfigSnapshot.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveServerConfigC2SPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player && player.hasPermissions(2)) {
                msg.snapshot.applyToConfigAndSave();
                player.sendSystemMessage(Component.translatable("message.bodycam.config_saved").withStyle(ChatFormatting.GREEN));
            }
        });
    }
}
