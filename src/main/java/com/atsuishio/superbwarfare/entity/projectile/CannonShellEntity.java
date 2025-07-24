package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PlayMessages;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Set;

public class CannonShellEntity extends FastThrowableProjectile implements GeoEntity, ExplosiveProjectile {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float damage = 0;
    private float radius = 0;
    private float explosionDamage = 0;
    private float fireProbability = 0;
    private int fireTime = 0;
    private static final int CHUNK_RADIUS = 1; // 加载3x3区块区域
    private final Set<ChunkPos> forcedChunks = new HashSet<>();
    private ChunkPos lastChunkPos;
    private float gravity = 0.1f;

    public enum Type {
        AP, HE, CM
    }
    private Type type = Type.AP;
    private int sparedAmount = 50;

    public CannonShellEntity(EntityType<? extends CannonShellEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public CannonShellEntity(LivingEntity entity, Level world, float damage, float radius, float explosionDamage, float fireProbability, int fireTime, float gravity, Type type, int sparedAmount) {
        super(ModEntities.CANNON_SHELL.get(), entity, world);
        this.noCulling = true;
        this.damage = damage;
        this.radius = radius;
        this.explosionDamage = explosionDamage;
        this.fireProbability = fireProbability;
        this.fireTime = fireTime;
        this.gravity = gravity;
        this.type = type;
        this.sparedAmount = sparedAmount;
    }

    public CannonShellEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.CANNON_SHELL.get(), level);
    }

    public CannonShellEntity durability(int durability) {
        this.durability = durability;
        return this;
    }

    @Override
    public boolean isColliding(BlockPos pPos, BlockState pState) {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);

        pCompound.putFloat("Damage", this.damage);
        pCompound.putFloat("ExplosionDamage", this.explosionDamage);
        pCompound.putFloat("Radius", this.radius);
        pCompound.putFloat("FireProbability", this.fireProbability);
        pCompound.putInt("FireTime", this.fireTime);
        pCompound.putInt("Durability", this.durability);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        if (pCompound.contains("Damage")) {
            this.damage = pCompound.getFloat("Damage");
        }

        if (pCompound.contains("ExplosionDamage")) {
            this.explosionDamage = pCompound.getFloat("ExplosionDamage");
        }

        if (pCompound.contains("Radius")) {
            this.radius = pCompound.getFloat("Radius");
        }

        if (pCompound.contains("FireProbability")) {
            this.fireProbability = pCompound.getFloat("FireProbability");
        }

        if (pCompound.contains("FireTime")) {
            this.fireTime = pCompound.getInt("FireTime");
        }

        if (pCompound.contains("Durability")) {
            this.durability = pCompound.getInt("Durability");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.HE_5_INCHES.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    public void onHitBlock(BlockHitResult blockHitResult) {
        if (this.level() instanceof ServerLevel) {
            if (type == Type.HE || type == Type.CM) {
                causeExplode(blockHitResult.getLocation());
                this.discard();
                return;
            }
            BlockPos resultPos = blockHitResult.getBlockPos();
            float hardness = this.level().getBlockState(resultPos).getBlock().defaultDestroyTime();
            if (hardness != -1) {
                if (ExplosionConfig.EXPLOSION_DESTROY.get()) {
                    if (firstHit) {
                        causeExplode(blockHitResult.getLocation());
                        firstHit = false;
                        Mod.queueServerWork(3, this::discard);
                    }
                    this.level().destroyBlock(resultPos, true);
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
    public void onHitEntity(EntityHitResult entityHitResult) {
        if (this.level() instanceof ServerLevel) {
            Entity entity = entityHitResult.getEntity();
            if (this.getOwner() != null && entity == this.getOwner().getVehicle())
                return;
            DamageHandler.doDamage(entity, ModDamageTypes.causeCannonFireDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            if (this.getOwner() instanceof LivingEntity living) {
                if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                    living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                    Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                }
            }

            ParticleTool.cannonHitParticles(this.level(), this.position(), this);
            causeExplode(entityHitResult.getLocation());
            if (entity instanceof VehicleEntity) {
                this.discard();
            }

        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel && tickCount > 1) {
            double l = getDeltaMovement().length();
            for (double i = 0; i < l; i ++) {
                Vec3 startPos = new Vec3(this.xo, this.yo, this.zo);
                Vec3 pos = startPos.add(getDeltaMovement().normalize().scale(-i));
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0.001, true);
            }
        }

        destroyBlock();

        if (this.tickCount > 600 || this.isInWater()) {
            if (this.level() instanceof ServerLevel) {
                causeExplode(position());
            }
            this.discard();
        }

        if (type == Type.CM) {
            // 使用Minecraft内置的光线追踪进行碰撞检测
            BlockHitResult hitResult = level().clip(new ClipContext(
                    position(),
                    position().add(getDeltaMovement().scale(12)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.ANY,
                    this
            ));

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                releaseClusterMunitions((LivingEntity) getOwner());
            }
        }

        if (!level().isClientSide) {
            // 更新区块加载位置
            updateChunkLoading();
        }

    }

    private void updateChunkLoading() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        ChunkPos currentPos = new ChunkPos(blockPosition());

        // 检查是否需要更新
        if (lastChunkPos != null && lastChunkPos.equals(currentPos)) {
            return;
        }

        // 计算需要加载的新区块
        Set<ChunkPos> newChunks = new HashSet<>();
        for (int x = -CHUNK_RADIUS; x <= CHUNK_RADIUS; x++) {
            for (int z = -CHUNK_RADIUS; z <= CHUNK_RADIUS; z++) {
                newChunks.add(new ChunkPos(currentPos.x + x, currentPos.z + z));
            }
        }

        // 卸载不再需要的区块
        Set<ChunkPos> toUnload = new HashSet<>(forcedChunks);
        toUnload.removeAll(newChunks);

        for (ChunkPos pos : toUnload) {
            serverLevel.setChunkForced(pos.x, pos.z, false);
            forcedChunks.remove(pos);
        }

        // 加载新区块
        for (ChunkPos pos : newChunks) {
            if (!forcedChunks.contains(pos)) {
                serverLevel.setChunkForced(pos.x, pos.z, true);
                forcedChunks.add(pos);
            }
        }

        lastChunkPos = currentPos;
    }

    private void releaseClusterMunitions(LivingEntity shooter) {
        if (level() instanceof ServerLevel serverLevel) {
            ParticleTool.spawnMediumExplosionParticles(serverLevel, position());
            for (int index0 = 0; index0 < sparedAmount; index0++) {
                GunGrenadeEntity gunGrenadeEntity = new GunGrenadeEntity(shooter, serverLevel,
                        6 * damage / sparedAmount,
                        5 * explosionDamage / sparedAmount,
                        radius / 2
                );

                gunGrenadeEntity.setPos(position().x, position().y, position().z);
                gunGrenadeEntity.shoot(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z, (float) (random.nextFloat() * 0.2f + 0.4f * getDeltaMovement().length()),
                        25);
                serverLevel.addFreshEntity(gunGrenadeEntity);
            }
            discard();
        }
    }

    @Override
    public void syncMotion() {
        if (!this.level().isClientSide) {
            Mod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new ClientMotionSyncMessage(this));
        }
    }

    private void causeExplode(Vec3 vec3) {
        CustomExplosion explosion = new CustomExplosion(this.level(), this,
                ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(),
                        this,
                        this.getOwner()),
                explosionDamage,
                vec3.x,
                vec3.y,
                vec3.z,
                radius,
                ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).
                setDamageMultiplier(1);
        explosion.explode();
        ForgeEventFactory.onExplosionStart(this.level(), explosion);
        explosion.finalizeExplosion(false);
        if (radius > 9) {
            ParticleTool.spawnHugeExplosionParticles(this.level(), vec3);
        } else {
            ParticleTool.spawnMediumExplosionParticles(this.level(), vec3);
        }
        discard();
    }

    private PlayState movementPredicate(AnimationState<CannonShellEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.cannon_shell.idle"));
    }

    @Override
    protected float getGravity() {
        return gravity;
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
    public void remove(Entity.RemovalReason reason) {
        // 释放所有加载的区块
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            for (ChunkPos pos : forcedChunks) {
                serverLevel.setChunkForced(pos.x, pos.z, false);
            }
            forcedChunks.clear();
        }
        super.remove(reason);
    }

    @Override
    public SoundEvent getSound() {
        return ModSounds.SHELL_FLY.get();
    }

    @Override
    public float getVolume() {
        return 0.07f;
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
        this.radius = radius;
    }
}
