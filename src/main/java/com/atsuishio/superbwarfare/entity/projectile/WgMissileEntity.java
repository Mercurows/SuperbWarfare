package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class WgMissileEntity extends MissileProjectile implements GeoEntity, ExplosiveProjectile {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public UUID launcherVehicle;

    public WgMissileEntity(EntityType<? extends WgMissileEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public WgMissileEntity(LivingEntity entity, Level level, float damage, float explosionDamage, float explosionRadius) {
        super(ModEntities.WG_MISSILE.get(), entity, level);
        this.noCulling = true;
        this.damage = damage;
        this.explosionDamage = explosionDamage;
        this.explosionRadius = explosionRadius;
        this.durability = 50;
        this.gravity = 0;
    }

    public WgMissileEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.WG_MISSILE.get(), level);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.WIRE_GUIDE_MISSILE.get();
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (this.level() instanceof ServerLevel) {
            BlockPos resultPos = blockHitResult.getBlockPos();
            float hardness = this.level().getBlockState(resultPos).getBlock().defaultDestroyTime();
            if (hardness != -1) {
                if (ExplosionConfig.EXPLOSION_DESTROY.get()) {
                    if (firstHit) {
                        causeExplode(blockHitResult.getLocation());
                        firstHit = false;
                        Mod.queueServerWork(3, this::discard);
                    }
                    if (ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                        this.level().destroyBlock(resultPos, true);
                    }
                }
            } else {
                causeExplode(blockHitResult.getLocation());
                this.discard();
            }
            if (!ExplosionConfig.EXPLOSION_DESTROY.get()) {
                causeExplode(blockHitResult.getLocation());
                this.discard();
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle())
            return;
        if (this.level() instanceof ServerLevel) {
            if (entity == this.getOwner() || (this.getOwner() != null && entity == this.getOwner().getVehicle()))
                return;
            if (this.getOwner() instanceof LivingEntity living) {
                if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                    living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                    NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                }
            }

            DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            causeExplode(result.getLocation());
            this.discard();
        }
    }

    @Override
    public ParticleTool.ParticleType explosionParticleType() {
        return ParticleTool.ParticleType.HUGE;
    }

    @Override
    public void tick() {
        super.tick();
        largeTrail();

        if (tickCount > 0 && this.getOwner() != null && getOwner().getVehicle() instanceof VehicleEntity vehicle) {
            this.setDeltaMovement(this.getDeltaMovement().add(getLookAngle()));
            Entity shooter = this.getOwner();

            Vec3 lookVec = vehicle.getBarrelVector(1).normalize();
            Vec3 vec3 = TraceTool.vehicleFindLookingPos(shooter,this, vehicle, vehicle.getZoomPos(shooter, 1), 512);
            Vec3 toVec = getDeltaMovement();

            if (launcherVehicle == vehicle.getUUID()) {
                if (vec3 != null) {
                    toVec = this.position().vectorTo(vec3).normalize();
                } else {
                    BlockHitResult result = level().clip(new ClipContext(vehicle.getZoomPos(shooter, 1), vehicle.getZoomPos(shooter, 1).add(lookVec.scale(512)),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
                    Vec3 hitPos = result.getLocation();

                    toVec = this.position().vectorTo(hitPos).normalize();
                }
            }

            turn(toVec, 5);
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 0.8, 0.8));
        }

        if (this.tickCount > 400 || this.isInWater() || this.entityData.get(HEALTH) <= 0) {
            if (this.level() instanceof ServerLevel) {
                causeExplode(position());
            }
            this.discard();
        }
        destroyBlock();
    }

    private PlayState movementPredicate(AnimationState<WgMissileEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.jvm.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public @NotNull SoundEvent getCloseSound() {
        return ModSounds.ROCKET_ENGINE.get();
    }

    @Override
    public @NotNull SoundEvent getSound() {
        return ModSounds.ROCKET_FLY.get();
    }

    @Override
    public float getVolume() {
        return 0.4f;
    }

    public void setLauncherVehicle(UUID uuid) {
        this.launcherVehicle = uuid;
    }
}
