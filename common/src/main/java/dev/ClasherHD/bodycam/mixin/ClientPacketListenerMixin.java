package dev.ClasherHD.bodycam.mixin;

import dev.ClasherHD.bodycam.client.camera.CameraSnapState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Shadow
    private ClientLevel level;

    @Inject(method = "handleTeleportEntity", at = @At("TAIL"))
    private void bodycam$onHandleTeleportEntityTail(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
        if (this.level == null) {
            return;
        }
        Entity entity = this.level.getEntity(packet.getId());
        if (entity != null && !entity.isControlledByLocalInstance()) {
            CameraSnapState.handleRemoteVanillaPositionPacket(entity, true);
        }
    }

    @Inject(method = "handleMoveEntity", at = @At("TAIL"))
    private void bodycam$onHandleMoveEntityTail(ClientboundMoveEntityPacket packet, CallbackInfo ci) {
        if (this.level == null) {
            return;
        }
        Entity entity = packet.getEntity(this.level);
        if (entity != null && !entity.isControlledByLocalInstance()) {
            CameraSnapState.handleRemoteVanillaPositionPacket(entity, packet.hasPosition());
        }
    }

    @Inject(method = "handleRotateMob", at = @At("TAIL"))
    private void bodycam$onHandleRotateMobTail(ClientboundRotateHeadPacket packet, CallbackInfo ci) {
        if (this.level == null) {
            return;
        }
        Entity entity = packet.getEntity(this.level);
        if (entity != null) {
            CameraSnapState.handleRemoteVanillaHeadPacket(entity, (float) (packet.getYHeadRot() * 360) / 256.0F);
        }
    }
}
