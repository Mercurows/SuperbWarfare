package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.RangeTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PtkmProjectileEntity extends FastThrowableProjectile implements ExplosiveProjectile, GeoEntity, MineEntity {
    private float damage = 750;
    private float explosionDamage = 200;
    private float explosionRadius = 10;
    private float gravity = 0.05f;
    private int shootTime = 3;
    private Entity target = null;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PtkmProjectileEntity(EntityType<? extends PtkmProjectileEntity> type, Level world) {
        super(type, world);
    }

    public PtkmProjectileEntity(LivingEntity entity, Level level) {
        super(ModEntities.PTKM_PROJECTILE.get(), entity, level);
    }

    public PtkmProjectileEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.PTKM_PROJECTILE.get(), level);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.PTKM_1R.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }



    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);

    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
    }

    @Override
    public void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        if (this.level() instanceof ServerLevel) {
            Entity entity = entityHitResult.getEntity();
            if (this.getOwner() != null && entity == this.getOwner().getVehicle())
                return;

            if (target != null && tickCount > shootTime) {
                DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), damage);
            } else {
                DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), 20);
            }

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            if (this.getOwner() instanceof LivingEntity living) {
                if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                    living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                    Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                }
            }

            explode(entityHitResult.getLocation());
            this.discard();
        }
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        if (this.level() instanceof ServerLevel) {
            explode(blockHitResult.getLocation());
            this.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel && tickCount > 0) {
            double l = getDeltaMovement().length();
            for (double i = 0; i < l; i++) {
                Vec3 pos = position().add(getDeltaMovement().normalize().scale(-i));
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0.001, true);
            }
        }

        if (target != null) {
            if (tickCount == shootTime) {
                Vec3 targetVel = target.getDeltaMovement();
                Vec3 targetVec = RangeTool.calculateFiringSolution(position(), target.getBoundingBox().getCenter(), targetVel, 15, 0.05);
                this.setDeltaMovement(targetVec.scale(15));
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, BlockPos.containing(position()), ModSounds.EXPLOSION_AIR.get(), SoundSource.BLOCKS, 8, 1);
                    ParticleTool.spawnSmallExplosionParticles(serverLevel, position());
                    ParticleTool.sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, position().x, position().y, position().z,
                            40, 0.5, 0.25, 0.5, 0.01, true);
                    ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, position().x, position().y, position().z,
                            30, 0.5, 0.25, 0.5, 0.005, true);
                    spawnDirectionalParticles(this, 55, 3.25, serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE);
                    spawnDirectionalParticles(this, 50, 3, serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE);
                    spawnDirectionalParticles(this, 45, 2.75, serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE);
                    spawnDirectionalParticles(this, 40, 2.5, serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE);
                    int count = 8;

                    for (float i = 0; i < this.distanceTo(target); i += .5f) {
                        ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD,
                                position().x + i * getDeltaMovement().normalize().x,
                                position().y + i * getDeltaMovement().normalize().y,
                                position().z + i * getDeltaMovement().normalize().z,
                                Mth.clamp(count--, 2, 8), 0.25, 0.25, 0.25, 0.0025, true);
                        ParticleTool.sendParticle(serverLevel, ParticleTypes.FLAME,
                                position().x + i * getDeltaMovement().normalize().x,
                                position().y + i * getDeltaMovement().normalize().y,
                                position().z + i * getDeltaMovement().normalize().z,
                                Mth.clamp(count--, 2, 8), 0.25, 0.25, 0.25, 0.0025, true);
                    }

                    for (float i = 0; i < 16; i += .5f) {
                        ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                position().x + i * getDeltaMovement().scale(-1).normalize().x,
                                position().y + i * getDeltaMovement().scale(-1).normalize().y,
                                position().z + i * getDeltaMovement().scale(-1).normalize().z,
                                Mth.clamp(count--, 2, 8), 0.25, 0.25, 0.25, 0.0025, true);
                    }
                }
            }
        } else {
            if (tickCount > 100) {
                explode(position());
            }
        }

    }

    public void explode(Vec3 pos) {
        new CustomExplosion.Builder(this)
                .damageSource(ModDamageTypes.causeCustomExplosionDamage(level().registryAccess(), this, this.getOwner()))
                .damage(explosionDamage)
                .radius(explosionRadius)
                .position(pos)
                .withParticleType(ParticleTool.ParticleType.MEDIUM)
                .particlePosition(pos)
                .explode();
    }

    @Override
    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public void setExplosionDamage(float damage) {
        this.explosionDamage = damage;
    }

    @Override
    public void setExplosionRadius(float radius) {
        this.explosionRadius = radius;
    }

    public void setShootTime(int time) {
        this.shootTime = time;
    }

    public void setTarget(Entity entity) {
        this.target = entity;
    }

    @Override
    public float getGravity() {
        return this.gravity;
    }

    @Override
    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }



    public static void spawnDirectionalParticles(Entity projectile, int count, double radius, ServerLevel level, SimpleParticleType particle) {
        Vec3 deltaMovement = projectile.getDeltaMovement();

        Vec3 direction = deltaMovement.normalize();
        Vec3 position = projectile.position();

        // 构建垂直正交基
        Vec3 randomPerp = getRandomPerpendicular(direction);
        Vec3 u = randomPerp.normalize();
        Vec3 v = direction.cross(u).normalize();

        spawnCircularParticles(level, position, u, v, count, radius, particle);
    }

    private static Vec3 getRandomPerpendicular(Vec3 dir) {
        Vec3 candidate1 = new Vec3(dir.y, -dir.x, 0); // 在XY平面垂直
        if (candidate1.lengthSqr() > 1e-4) return candidate1;
        return new Vec3(0, dir.z, -dir.y); // 备用垂直向量
    }

    private static void spawnCircularParticles(ServerLevel level, Vec3 center, Vec3 u, Vec3 v, int count, double radius, SimpleParticleType particle) {
        for (int i = 0; i < count; i++) {
            double theta = 2 * Math.PI * i / count;
            double xOffset = radius * (Math.cos(theta) * u.x + Math.sin(theta) * v.x);
            double yOffset = radius * (Math.cos(theta) * u.y + Math.sin(theta) * v.y);
            double zOffset = radius * (Math.cos(theta) * u.z + Math.sin(theta) * v.z);

            Vec3 pos = center.add(xOffset, yOffset, zOffset);
            spawnParticle(level, pos, particle);
        }
    }

    private static void spawnParticle(ServerLevel level, Vec3 pos, SimpleParticleType particle) {
        ParticleTool.sendParticle(level, particle, pos.x, pos.y, pos.z,
                1, 0.02, 0.02, 0.02, 0.0001, true);
    }
}
