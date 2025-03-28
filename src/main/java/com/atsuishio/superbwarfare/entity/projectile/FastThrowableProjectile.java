package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.PacketDistributor;

public abstract class FastThrowableProjectile extends ThrowableItemProjectile implements CustomSyncMotionEntity, IEntityWithComplexSpawn {

    public FastThrowableProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public FastThrowableProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, double pX, double pY, double pZ, Level pLevel) {
        super(pEntityType, pX, pY, pZ, pLevel);
    }

    public FastThrowableProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, LivingEntity pShooter, Level pLevel) {
        super(pEntityType, pShooter, pLevel);
    }

    @Override
    public void tick() {
        super.tick();
        this.syncMotion();
    }

    @Override
    public void syncMotion() {
        if (this.level().isClientSide) return;
        if (!shouldSyncMotion()) return;

        if (this.tickCount % this.getType().updateInterval() == 0) {
            PacketDistributor.sendToAllPlayers(new ClientMotionSyncMessage(this));
        }
    }

    public boolean shouldSyncMotion() {
        return false;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        var motion = this.getDeltaMovement();
        buffer.writeFloat((float) motion.x);
        buffer.writeFloat((float) motion.y);
        buffer.writeFloat((float) motion.z);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        this.setDeltaMovement(additionalData.readFloat(), additionalData.readFloat(), additionalData.readFloat());
    }
}
