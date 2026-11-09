package dev.ClasherHD.bodycam.network.logbook;

import dev.ClasherHD.bodycam.item.LogBookItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class LogBookActionC2SPacket {
    public static final int ACTION_UNLOCK = 0;
    public static final int ACTION_SET_PIN = 1;

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

    public static void encode(LogBookActionC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
        buf.writeBoolean(msg.offHand);
        buf.writeUtf(msg.pin, 8);
        buf.writeUtf(msg.newPin, 8);
    }

    public static LogBookActionC2SPacket decode(FriendlyByteBuf buf) {
        return new LogBookActionC2SPacket(buf.readVarInt(), buf.readBoolean(), buf.readUtf(8), buf.readUtf(8));
    }

    public static void handle(LogBookActionC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) {
                return;
            }
            ItemStack stack = msg.offHand ? sender.getOffhandItem() : sender.getMainHandItem();
            if (!(stack.getItem() instanceof LogBookItem)) {
                return;
            }
            if (msg.action == ACTION_UNLOCK) {
                LogBookItem.handleUnlock(sender, stack, msg.pin);
            } else if (msg.action == ACTION_SET_PIN) {
                LogBookItem.handleSetPin(sender, stack, msg.pin, msg.newPin);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
