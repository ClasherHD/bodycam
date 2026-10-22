package dev.ClasherHD.bodycam.network.watchpoint;

import dev.ClasherHD.bodycam.block.entity.WatchPointBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class RenameWatchPointC2SPacket {
    public final BlockPos pos;
    public final String newName;

    public RenameWatchPointC2SPacket(BlockPos pos, String newName) {
        this.pos = pos;
        this.newName = newName == null ? "" : newName;
    }

    public static void encode(RenameWatchPointC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.newName, 32);
    }

    public static RenameWatchPointC2SPacket decode(FriendlyByteBuf buf) {
        return new RenameWatchPointC2SPacket(buf.readBlockPos(), buf.readUtf(32));
    }

    public static void handle(RenameWatchPointC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (player.blockPosition().distSqr(msg.pos) > 64.0D) {
                return;
            }
            if (player.level().getBlockEntity(msg.pos) instanceof WatchPointBlockEntity wp) {
                if (wp.getOwnerUUID() == null || wp.getOwnerUUID().equals(player.getUUID())) {
                    wp.setCustomName(msg.newName);
                    player.displayClientMessage(Component.translatable("message.bodycam.watch_point.renamed", wp.getCustomName().isEmpty() ? "Watch Point" : wp.getCustomName())
                            .withStyle(ChatFormatting.GREEN), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
