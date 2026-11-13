package dev.ClasherHD.bodycam.event;

import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import dev.ClasherHD.bodycam.entity.ScholarEntity;
import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.ClasherHD.bodycam.registry.ModEntityTypes;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;

public class ModEvents {
    public static void init() {
        EntityAttributeRegistry.register(ModEntityTypes.BODYCAM_DUMMY, BodycamDummyEntity::createAttributes);
        EntityAttributeRegistry.register(ModEntityTypes.HOLOGRAM_DUMMY, BodycamDummyEntity::createAttributes);
        EntityAttributeRegistry.register(ModEntityTypes.WATCH_POINT_DUMMY, WatchPointDummyEntity::createAttributes);
        EntityAttributeRegistry.register(ModEntityTypes.SCHOLAR, ScholarEntity::createAttributes);
        EntityAttributeRegistry.register(ModEntityTypes.CREEPING_DEATH, CreepingDeathEntity::createAttributes);
    }
}
