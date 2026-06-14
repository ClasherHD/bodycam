package dev.ClasherHD.bodycam.event;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.network.PacketHandler;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = bodycam.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
@SuppressWarnings("null")
public class ModEvents {

    @SubscribeEvent
    public static void commonSetup(final FMLCommonSetupEvent event) {
        PacketHandler.init();
        event.enqueueWork(() -> {
            net.minecraftforge.common.world.ForgeChunkManager.setForcedChunkLoadingCallback(bodycam.MODID,
                    (level, ticketHelper) -> {
                        ticketHelper.getEntityTickets().forEach((uuid, chunks) -> {
                            net.minecraft.world.entity.Entity entity = level.getEntity(uuid);
                            if (entity instanceof BodycamDummyEntity dummy) {
                                java.util.UUID ownerUUID = dummy.getOwnerUUID();
                                if (ownerUUID != null) {
                                    net.minecraft.server.level.ServerPlayer owner = level
                                            .getServer().getPlayerList()
                                            .getPlayer(ownerUUID);
                                    if (owner != null && owner.getPersistentData()
                                            .getBoolean("bodycam_active")) {
                                        return;
                                    }
                                }
                            }
                            ticketHelper.removeAllTickets(uuid);
                        });
                    });
        });
    }

    @SubscribeEvent
    public static void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.COMPASS_DUMMY.get(), BodycamDummyEntity.createAttributes().build());
        event.put(ModEntityTypes.HOLOGRAM_DUMMY.get(), BodycamDummyEntity.createAttributes().build());
    }
}
