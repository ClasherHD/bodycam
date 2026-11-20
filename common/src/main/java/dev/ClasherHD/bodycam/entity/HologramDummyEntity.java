package dev.ClasherHD.bodycam.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public class HologramDummyEntity extends BodycamDummyEntity {
    public HologramDummyEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean isLinkedToOwnerHealth() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        super.hurtServer(level, source, amount);
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        super.isInvulnerableTo(level, source);
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
