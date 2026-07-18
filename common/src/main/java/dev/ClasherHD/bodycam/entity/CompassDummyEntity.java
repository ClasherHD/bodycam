package dev.ClasherHD.bodycam.entity;


import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CompassDummyEntity extends BodycamDummyEntity {
    public CompassDummyEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        if (this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) != null) {
            this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0D);
        }
    }
}
