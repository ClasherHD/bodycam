package dev.ClasherHD.bodycam.network;

import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import dev.ClasherHD.bodycam.item.CreepingDeathItem;
import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.network.creepingdeath.CreepingDeathActionC2SPacket;
import dev.ClasherHD.bodycam.network.creepingdeath.OpenCreepingDeathScreenS2CPacket;
import dev.ClasherHD.bodycam.util.BodycamPersistentDataHolder;
import dev.architectury.networking.NetworkManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@SuppressWarnings("null")
class CreepingDeathActionC2SPacketTest {

    @Test
    void handleModeTarget_whenAlreadyHunted_returnsBeforeCameraResetTitleAndShrink() {
        UUID senderId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList playerList = mock(PlayerList.class);
        ServerLevel level = mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(server.getPlayerList()).thenReturn(playerList);

        ServerPlayer sender = mock(ServerPlayer.class, withSettings().extraInterfaces(BodycamPersistentDataHolder.class));
        try {
            var serverField = ServerPlayer.class.getField("server");
            serverField.setAccessible(true);
            serverField.set(sender, server);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        when(sender.getUUID()).thenReturn(senderId);
        when(sender.level()).thenReturn(level);

        ItemStack heldStack = mock(ItemStack.class);
        CreepingDeathItem cdItem = mock(CreepingDeathItem.class);
        when(heldStack.getItem()).thenReturn(cdItem);
        when(sender.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(heldStack);

        ServerPlayer target = mock(ServerPlayer.class, withSettings().extraInterfaces(BodycamPersistentDataHolder.class));
        CompoundTag targetTag = new CompoundTag();
        targetTag.putBoolean("bodycam_active", true);
        targetTag.putBoolean("bodycam_all_seeing_pending", true);
        when(((BodycamPersistentDataHolder) target).bodycam$getPersistentData()).thenReturn(targetTag);
        when(target.getUUID()).thenReturn(targetId);
        when(target.isAlive()).thenReturn(true);
        when(target.level()).thenReturn(level);
        when(target.isCreative()).thenReturn(false);
        when(target.isSpectator()).thenReturn(true);
        when(playerList.getPlayer(targetId)).thenReturn(target);

        NetworkManager.PacketContext context = mock(NetworkManager.PacketContext.class);
        when(context.getPlayer()).thenReturn(sender);
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(context).queue(any(Runnable.class));

        try (MockedStatic<ModServerConfig> configMock = Mockito.mockStatic(ModServerConfig.class);
             MockedStatic<CreepingDeathEntity> cdMock = Mockito.mockStatic(CreepingDeathEntity.class);
             MockedStatic<BodycamResetCameraPacket> resetMock = Mockito.mockStatic(BodycamResetCameraPacket.class)) {

            configMock.when(() -> ModServerConfig.isDimensionBlacklisted(level)).thenReturn(false);
            cdMock.when(() -> CreepingDeathEntity.isTargetAlreadyHunted(server, targetId)).thenReturn(true);

            CreepingDeathActionC2SPacket packet = new CreepingDeathActionC2SPacket(
                    OpenCreepingDeathScreenS2CPacket.MODE_TARGET,
                    false,
                    targetId,
                    "Target"
            );
            CreepingDeathActionC2SPacket.handle(packet, context);

            resetMock.verify(() -> BodycamResetCameraPacket.executeReset(any()), never());
            cdMock.verify(() -> CreepingDeathEntity.sendHuntNoEscapeTitle(any()), never());
            verify(heldStack, never()).shrink(1);
            verify(sender).sendSystemMessage(any(Component.class));
        }
    }
}
