package dev.ClasherHD.bodycam.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class EntityRegistryFabric {

    public static final EntityType<CompassDummyEntity> COMPASS_DUMMY = FabricEntityTypeBuilder.<CompassDummyEntity>create(MobCategory.MISC, CompassDummyEntity::new)
            .dimensions(EntityDimensions.fixed(0.6F, 1.8F))
            .trackRangeChunks(256)
            .trackedUpdateRate(1)
            .build();

    public static final EntityType<HologramDummyEntity> HOLOGRAM_DUMMY = FabricEntityTypeBuilder.<HologramDummyEntity>create(MobCategory.MISC, HologramDummyEntity::new)
            .dimensions(EntityDimensions.fixed(0.6F, 1.8F))
            .trackRangeChunks(256)
            .trackedUpdateRate(1)
            .build();

    public static void registerEntities() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation("bodycam", "compass_dummy"), COMPASS_DUMMY);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation("bodycam", "hologram_dummy"), HOLOGRAM_DUMMY);
    }
}
