package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Code based on @BobMowzie's MowziesMobs, @EEEAB's EEEABsMobs and @Mercurows's DreamaticVoyage
 */
public class LaserEntity extends AbstractLaserEntity {

    public static final double RADIUS = 512D;

    public LaserEntity(EntityType<? extends LaserEntity> type, Level level) {
        super(type, level, 1);
    }

    public LaserEntity(Level level, LivingEntity caster, double x, double y, double z, float yaw, float pitch, int duration) {
        super(ModEntities.LASER.get(), level, 1);
        this.caster = caster;
        this.setYaw(yaw);
        this.setPitch(pitch);
        this.setDuration(duration);
        this.setPos(x, y, z);
        this.calculateEndPos(RADIUS);
        if (!level().isClientSide) {
            setCasterId(caster.getId());
        }
    }

    @Override
    public void beamTick() {
        if (!this.level().isClientSide) {
            if (this.caster instanceof Player) {
                this.updateWithPlayer();
            } else if (this.caster != null) {
                this.updateWithEntity();
            }
        }

        if (caster != null) {
            this.yaw = (float) Math.toRadians(caster.yHeadRot + 90);
            this.pitch = (float) -Math.toRadians(caster.getXRot());
        }

        if (this.tickCount >= this.getCountDown()) {
            this.calculateEndPos(RADIUS);

            raytraceEntities(this.level(), new Vec3(getX(), getY(), getZ()), new Vec3(endPosX, endPosY, endPosZ));

            if (this.blockSide != null) {
                this.spawnExplosionParticles();
            }
        }
    }

    @Override
    public CustomHitResult raytraceEntities(Level world, Vec3 from, Vec3 to) {
        CustomHitResult result = new CustomHitResult();
        result.setBlockHit(this.level().clip(new ClipContext(new Vec3(getX(), getY(), getZ()), new Vec3(endPosX, endPosY, endPosZ),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)));
        if (result.getBlockHit() != null) {
            Vec3 hitVec = result.getBlockHit().getLocation();
            collidePosX = hitVec.x;
            collidePosY = hitVec.y;
            collidePosZ = hitVec.z;
            blockSide = result.getBlockHit().getDirection();
        } else {
            List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, new AABB(Math.min(getX(), collidePosX), Math.min(getY(), collidePosY), Math.min(getZ(), collidePosZ), Math.max(getX(), collidePosX), Math.max(getY(), collidePosY), Math.max(getZ(), collidePosZ)).inflate(1, 1, 1));
            for (LivingEntity entity : entities) {
                if (entity == this.caster) {
                    continue;
                }
                float pad = entity.getPickRadius() + getBaseScale();
                AABB aabb = entity.getBoundingBox().inflate(pad, pad, pad);
                Optional<Vec3> hit = aabb.clip(from, to);
                if (aabb.contains(from)) {
                    result.addEntityHit(entity);
                } else if (hit.isPresent()) {
                    result.addEntityHit(entity);
                }
            }

            var target = result.getEntities().stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(this.caster)));
            if (target.isPresent()) {
                collidePosX = target.get().getX();
                collidePosY = target.get().getY();
                collidePosZ = target.get().getZ();
            } else {
                collidePosX = endPosX;
                collidePosY = endPosY;
                collidePosZ = endPosZ;
            }
            blockSide = null;
        }

        return result;
    }

    public void spawnExplosionParticles() {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        if (this.caster == null) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    private void updateWithPlayer() {
        this.setYaw((float) Math.toRadians(caster.yHeadRot + 90));
        this.setPitch((float) Math.toRadians(-caster.getXRot()));
        Vec3 vecOffset = caster.getLookAngle().normalize().scale(1.25);
        this.setPos(caster.getX() + vecOffset.x(), caster.getY() + caster.getBbHeight() * 0.5F + vecOffset.y(), caster.getZ() + vecOffset.z());
    }

    private void updateWithEntity() {
        double radians = Math.toRadians(this.caster.yHeadRot + 90);
        this.setYaw((float) radians);
        this.setPitch((float) ((double) (-this.caster.getXRot()) * Math.PI / 180.0));
        double offsetX = Math.cos(radians) * (float) 0.0;
        double offsetZ = Math.sin(radians) * (float) 0.0;
        this.setPos(this.caster.getX() + offsetX, this.caster.getY((float) 0.75), this.caster.getZ() + offsetZ);
    }

}
