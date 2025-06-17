package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class SmokeDecoyEntity extends Entity {

    public SmokeDecoyEntity(EntityType<? extends SmokeDecoyEntity> type, Level world) {
        super(type, world);
    }

    public SmokeDecoyEntity(Level level) {
        super(ModEntities.SMOKE_DECOY.get(), level);
    }

    public int life = 400;
    public int igniteTime = 4;

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        if (compoundTag.contains("IgniteTime")) {
            this.igniteTime = compoundTag.getInt("IgniteTime");
        }
        if (compoundTag.contains("Life")) {
            this.life = compoundTag.getInt("Life");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        compoundTag.putInt("IgniteTime", igniteTime);
        compoundTag.putInt("Life", life);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (tickCount == this.igniteTime) {
            if (this.level() instanceof ServerLevel serverLevel) {
                ParticleTool.sendParticle(serverLevel, ModParticleTypes.CUSTOM_SMOKE.get(), this.xo, this.yo, this.zo,
                        50, 0, 0, 0, 0.07, true);
                ParticleTool.sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, this.xo, this.yo, this.zo, 10, 1, 1, 1, 0.1, true);
                ParticleTool.sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), this.xo, this.yo, this.zo, 30, 0, 0, 0, 0.2, true);
            }
            this.level().playSound(null, this, ModSounds.SMOKE_FIRE.get(), this.getSoundSource(), 2, random.nextFloat() * 0.05f + 1);
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (this.tickCount > this.life) {
            this.discard();
        }
    }

    public void decoyShoot(Entity entity, Vec3 shootVec, float pVelocity, float pInaccuracy) {
        Vec3 vec3 = shootVec.normalize().add(this.random.triangle(0.0, 0.0172275 * (double) pInaccuracy), this.random.triangle(0.0, 0.0172275 * (double) pInaccuracy), this.random.triangle(0.0, 0.0172275 * (double) pInaccuracy)).scale(pVelocity);
        this.setDeltaMovement(entity.getDeltaMovement().scale(0.75).add(vec3));
        double d0 = vec3.horizontalDistance();
        this.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * 57.2957763671875));
        this.setXRot((float) (Mth.atan2(vec3.y, d0) * 57.2957763671875));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }
}
