package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import dev.ClasherHD.bodycam.entity.HologramDummyEntity;
import dev.ClasherHD.bodycam.entity.ScholarEntity;
import dev.ClasherHD.bodycam.entity.WatchPointDummyEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

@SuppressWarnings("null")
public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(bodycam.MODID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<BodycamDummyEntity>> BODYCAM_DUMMY =
            ENTITY_TYPES.register("bodycam_dummy",
                    () -> EntityType.Builder.<BodycamDummyEntity>of(BodycamDummyEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(key("bodycam_dummy")));

    public static final RegistrySupplier<EntityType<HologramDummyEntity>> HOLOGRAM_DUMMY =
            ENTITY_TYPES.register("hologram_dummy",
                    () -> EntityType.Builder.<HologramDummyEntity>of(HologramDummyEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(key("hologram_dummy")));

    public static final RegistrySupplier<EntityType<WatchPointDummyEntity>> WATCH_POINT_DUMMY =
            ENTITY_TYPES.register("watch_point_dummy",
                    () -> EntityType.Builder.<WatchPointDummyEntity>of(WatchPointDummyEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(key("watch_point_dummy")));

    public static final RegistrySupplier<EntityType<CreepingDeathEntity>> CREEPING_DEATH =
            ENTITY_TYPES.register("creeping_death",
                    () -> EntityType.Builder.<CreepingDeathEntity>of(CreepingDeathEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build(key("creeping_death")));

    public static final RegistrySupplier<EntityType<ScholarEntity>> SCHOLAR =
            ENTITY_TYPES.register("scholar",
                    () -> EntityType.Builder.<ScholarEntity>of(ScholarEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(key("scholar")));

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, bodycam.id(name));
    }

    public static void register() {
        ENTITY_TYPES.register();
    }
}
