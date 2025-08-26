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
    private float gravity = 0.07f;
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
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("ExplosionDamage", this.explosionDamage);
        pCompound.putFloat("Radius", this.explosionRadius);
        pCompound.putFloat("Fuse", this.fuse);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("ExplosionDamage")) {
            this.explosionDamage = pCompound.getFloat("ExplosionDamage");
        }
        if (pCompound.contains("Radius")) {
            this.explosionRadius = pCompound.getFloat("Radius");
        }
        if (pCompound.contains("Fuse")) {
            this.fuse = pCompound.getInt("Fuse");
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
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
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

    @Override
    public double getDefaultGravity() {
        return this.gravity;
    }

    @Override
    public void setGravity(float gravity) {
        this.gravity = gravity;
    }
}
