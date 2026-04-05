package dev.ClasherHD.bodycam.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "sendSystemMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void onSendSystemMessage(Component component, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (dev.ClasherHD.bodycam.BodycamHelper.getPersistentData(player).getBoolean("bodycam_active")) {
            if (component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable && translatable.getKey().equals("gameMode.changed")) {
                ci.cancel();
            }
        }
    }
}
