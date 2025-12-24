package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.ProjectileTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RgoGrenadeEntity extends FastThrowableProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    public RgoGrenadeEntity(EntityType<? extends RgoGrenadeEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.explosionDamage = ExplosionConfig.RGO_GRENADE_EXPLOSION_DAMAGE.get();
        this.explosionRadius = ExplosionConfig.RGO_GRENADE_EXPLOSION_RADIUS.get();
    }

    public RgoGrenadeEntity(EntityType<? extends RgoGrenadeEntity> type, double x, double y, double z, Level level) {
        super(type, x, y, z, level);
        this.noCulling = true;
        this.explosionDamage = ExplosionConfig.RGO_GRENADE_EXPLOSION_DAMAGE.get();
        this.explosionRadius = ExplosionConfig.RGO_GRENADE_EXPLOSION_RADIUS.get();
    }

    public RgoGrenadeEntity(LivingEntity entity, Level level) {
        super(ModEntities.RGO_GRENADE.get(), entity, level);
        this.noCulling = true;
        this.explosionDamage = ExplosionConfig.RGO_GRENADE_EXPLOSION_DAMAGE.get();
        this.explosionRadius = ExplosionConfig.RGO_GRENADE_EXPLOSION_RADIUS.get();
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.RGO_GRENADE.get();
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
}
