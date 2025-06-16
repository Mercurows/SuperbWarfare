package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.ProjectileTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RgoGrenadeEntity extends FastThrowableProjectile implements GeoEntity, ExplosiveProjectile {

    private float explosionDamage = ExplosionConfig.RGO_GRENADE_EXPLOSION_DAMAGE.get();
    private float explosionRadius = ExplosionConfig.RGO_GRENADE_EXPLOSION_RADIUS.get();
    private int fuse = 80;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RgoGrenadeEntity(EntityType<? extends RgoGrenadeEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public RgoGrenadeEntity(EntityType<? extends RgoGrenadeEntity> type, double x, double y, double z, Level world) {
        super(type, x, y, z, world);
        this.noCulling = true;
    }

    public RgoGrenadeEntity(LivingEntity entity, Level level, int fuse) {
        super(ModEntities.RGO_GRENADE.get(), entity, level);
        this.fuse = fuse;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("ExplosionDamage", this.explosionDamage);
        pCompound.putFloat("Radius", this.explosionRadius);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("ExplosionDamage")) {
            this.explosionDamage = pCompound.getFloat("ExplosionDamage");
        }
        if (pCompound.contains("Radius")) {
            this.explosionRadius = pCompound.getFloat("Radius");
        }
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.RGO_GRENADE.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        if (level() instanceof ServerLevel) {
            switch (result.getType()) {
                case BLOCK:
                    BlockHitResult blockResult = (BlockHitResult) result;
                    BlockPos resultPos = blockResult.getBlockPos();
                    BlockState state = this.level().getBlockState(resultPos);
                    if (state.getBlock() instanceof BellBlock bell) {
                        bell.attemptToRing(this.level(), resultPos, blockResult.getDirection());
                    }
                    ProjectileTool.causeCustomExplode(this, this.explosionDamage, this.explosionRadius, 1.2f);

                    break;
                case ENTITY:
                    EntityHitResult entityResult = (EntityHitResult) result;
                    Entity entity = entityResult.getEntity();
                    if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle())
                        return;
                    if (this.getOwner() instanceof LivingEntity living) {
                        if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                            living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                            PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
                        }
                    }
                    if (!(entity instanceof DroneEntity)) {
                        ProjectileTool.causeCustomExplode(this, this.explosionDamage, this.explosionRadius, 1.2f);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        --this.fuse;

        if (this.fuse <= 0) {
            this.discard();
            if (!this.level().isClientSide) {
                ProjectileTool.causeCustomExplode(this, this.explosionDamage, this.explosionRadius, 1.2f);
            }
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            ParticleTool.sendParticle(serverLevel, ParticleTypes.SMOKE, this.xo, this.yo, this.zo,
                    1, 0, 0, 0, 0.01, true);
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.07F;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public void droneShoot(Entity drone) {
        Vec3 vec3 = (new Vec3(0.2 * drone.getDeltaMovement().x, 0.2 * drone.getDeltaMovement().y, 0.2 * drone.getDeltaMovement().z));
        this.setDeltaMovement(vec3);
        double d0 = vec3.horizontalDistance();
        this.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI)));
        this.setXRot((float) (Mth.atan2(vec3.y, d0) * (double) (180F / (float) Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public void setDamage(float damage) {
    }

    @Override
    public void setExplosionDamage(float explosionDamage) {
        this.explosionDamage = explosionDamage;
    }

    @Override
    public void setExplosionRadius(float radius) {
        this.explosionRadius = radius;
    }
}
