package dev.ClasherHD.bodycam.registry;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.entity.CompassDummyEntity;
import dev.ClasherHD.bodycam.entity.HologramDummyEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, bodycam.MODID);

    public static final RegistryObject<EntityType<CompassDummyEntity>> COMPASS_DUMMY = ENTITY_TYPES
            .register(
                    "compass_dummy",
                    () -> EntityType.Builder.<CompassDummyEntity>of(
                                    CompassDummyEntity::new,
                                    MobCategory.MISC)
                            .sized(0.6F, 1.8F).clientTrackingRange(10)
                            .build("compass_dummy"));

    public static final RegistryObject<EntityType<HologramDummyEntity>> HOLOGRAM_DUMMY = ENTITY_TYPES
            .register(
                    "hologram_dummy",
                    () -> EntityType.Builder.<HologramDummyEntity>of(
                                    HologramDummyEntity::new,
                                    MobCategory.MISC)
                            .sized(0.6F, 1.8F).clientTrackingRange(10)
                            .build("hologram_dummy"));
}
