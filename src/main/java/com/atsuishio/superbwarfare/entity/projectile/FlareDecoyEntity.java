package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;

public class FlareDecoyEntity extends Entity {

    public FlareDecoyEntity(EntityType<? extends FlareDecoyEntity> type, Level world) {
        super(type, world);
    }

    public FlareDecoyEntity(Level level) {
        super(ModEntities.FLARE_DECOY.get(), level);
    }

    public FlareDecoyEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.FLARE_DECOY.get(), level);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(this.getDeltaMovement().add(0, -0.02, 0));
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (level().isClientSide()) {
            level().addAlwaysVisibleParticle(ParticleTypes.END_ROD, true, this.xo, this.yo, this.zo, 0, 0, 0);
            level().addAlwaysVisibleParticle(ParticleTypes.CLOUD, true, this.xo, this.yo, this.zo, 0, 0, 0);
        }
        if (this.tickCount > 200 || this.isInWater() || this.onGround()) {
            this.discard();
        }
    }

    public void decoyShoot(Entity entity, Vec3 shootVec, float pVelocity, float pInaccuracy) {
        Vec3 vec3 = shootVec.normalize().add(this.random.triangle(0, 0.0172275 * (double) pInaccuracy), this.random.triangle(0, 0.0172275 * (double) pInaccuracy), this.random.triangle(0, 0.0172275 * (double) pInaccuracy)).scale((double) pVelocity);
        this.setDeltaMovement(entity.getDeltaMovement().scale(0.75).add(vec3));
        double d0 = vec3.horizontalDistance();
        this.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * 57.2957763671875));
        this.setXRot((float) (Mth.atan2(vec3.y, d0) * 57.2957763671875));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }
}
