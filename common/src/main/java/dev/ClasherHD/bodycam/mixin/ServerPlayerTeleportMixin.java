package dev.ClasherHD.bodycam.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerTeleportMixin {
    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z", at = @At("HEAD"), cancellable = true)
    private void onTeleportTo(net.minecraft.server.level.ServerLevel level, double x, double y, double z, java.util.Set<?> relativeMovements, float yaw, float pitch, boolean resetCamera, CallbackInfoReturnable<Boolean> ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (dev.ClasherHD.bodycam.util.BodycamHelper.getPersistentData(player).getBooleanOr("bodycam_active", false)) {
            if (!dev.ClasherHD.bodycam.util.BodycamHelper.getPersistentData(player).getBooleanOr("bodycam_allow_teleport", false)) {
                ci.setReturnValue(false);
            }
        }
    }
}