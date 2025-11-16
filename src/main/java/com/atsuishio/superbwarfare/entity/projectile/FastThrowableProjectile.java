package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent;
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage;
import com.atsuishio.superbwarfare.tools.ChunkLoadManager;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.atsuishio.superbwarfare.tools.TraceTool.getBlocksAlongRay;

public abstract class FastThrowableProjectile extends ThrowableItemProjectile implements CustomSyncMotionEntity, IEntityAdditionalSpawnData {

    public static Consumer<FastThrowableProjectile> flySound = projectile -> {
    };
    public static Consumer<FastThrowableProjectile> nearFlySound = projectile -> {
    };

    private static final int CHUNK_RADIUS = 1; // 3x3区块

    public float damage = 0;
    public float explosionDamage = 0;
    public float explosionRadius = 0;
    public int durability = 50;
    public boolean firstHit = true;
    public float gravity = 0.05f;

    private boolean isFastMoving = false;
    private final Set<ChunkPos> currentChunks = new HashSet<>();
    private ChunkPos lastChunkPos;

    public FastThrowableProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public FastThrowableProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, double pX, double pY, double pZ, Level pLevel) {
        super(pEntityType, pX, pY, pZ, pLevel);
    }

    public FastThrowableProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, @Nullable Entity pShooter, Level pLevel) {
        super(pEntityType, pLevel);
        this.setOwner(pShooter);
        if (pShooter != null) {
            this.setPos(pShooter.getX(), pShooter.getEyeY() - (double) 0.1F, pShooter.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Damage")) {
            this.damage = compound.getFloat("Damage");
        }
        if (compound.contains("ExplosionDamage")) {
            this.explosionDamage = compound.getFloat("ExplosionDamage");
        }
        if (compound.contains("Radius")) {
            this.explosionRadius = compound.getFloat("Radius");
        }
        if (compound.contains("Durability")) {
            this.durability = compound.getInt("Durability");
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        if (this.damage > 0) {
            compound.putFloat("Damage", this.damage);
        }
        if (this.explosionDamage > 0) {
            compound.putFloat("ExplosionDamage", this.explosionDamage);
        }
        if (this.explosionRadius > 0) {
            compound.putFloat("Radius", this.explosionRadius);
        }
        if (this.durability > 0) {
            compound.putInt("Durability", this.durability);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.isFastMoving && this.isFastMoving() && this.level().isClientSide) {
            flySound.accept(this);
            nearFlySound.accept(this);
        }
        this.isFastMoving = this.isFastMoving();

        Vec3 vec3 = this.getDeltaMovement();
        float friction;
        if (this.isInWater()) {
            friction = 0.8F;
        } else {
            friction = 0.99F;
        }

        // 撤销重力影响
        vec3 = vec3.add(0, this.getGravity(), 0);
        // 重新计算动量
        this.setDeltaMovement(vec3.scale(1 / friction));

        // 重新应用重力
        Vec3 vec31 = this.getDeltaMovement();
        this.setDeltaMovement(vec31.x, vec31.y - (double) this.getGravity(), vec31.z);

        // 同步动量
        this.syncMotion();

        // 更新区块加载位置
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel && forceLoadChunk()) {
            updateChunkLoading(serverLevel);
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult pResult) {
        super.onHitEntity(pResult);
        MinecraftForge.EVENT_BUS.post(
                new ProjectileHitEvent.HitEntity(
                        this.getOwner(),
                        this,
                        pResult.getEntity(),
                        pResult.getLocation()
                )
        );
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult pResult) {
        super.onHitBlock(pResult);
        MinecraftForge.EVENT_BUS.post(
                new ProjectileHitEvent.HitBlock(
                        pResult.getBlockPos(),
                        this.level().getBlockState(pResult.getBlockPos()),
                        pResult.getDirection(),
                        this.getOwner(),
                        this,
                        pResult.getLocation()
                )
        );
    }

    public void destroyBlock() {
        if (ExplosionConfig.EXPLOSION_DESTROY.get()) {
            Vec3 posO = new Vec3(xo, yo, zo);
            List<BlockPos> blockList = getBlocksAlongRay(posO, getDeltaMovement(), getDeltaMovement().length());
            for (BlockPos pos : blockList) {
                BlockState blockState = level().getBlockState(pos);
                if (!blockState.is(Blocks.AIR)) {
                    float hardness = this.level().getBlockState(pos).getBlock().defaultDestroyTime();

                    double resistance = 1 - Mth.clamp(hardness / 100, 0, 0.8);
                    setDeltaMovement(getDeltaMovement().multiply(resistance, resistance, resistance));

                    if (blockState.canOcclude()) {
                        durability -= 10 + (int) (0.5 * hardness);
                    }

                    if (hardness <= durability && hardness != -1 && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                        this.level().destroyBlock(pos, true);
                    }
                    if (hardness == -1 || hardness > durability || durability <= 0) {
                        causeExplode(pos.getCenter());
                        discard();
                        break;
                    }
                }
            }
        }
    }

    public CustomExplosion.Builder buildExplosion(Vec3 vec3) {
        return new CustomExplosion.Builder(this)
                .attacker(this.getOwner())
                .damage(explosionDamage)
                .radius(explosionRadius)
                .position(vec3)
                .withParticleType(explosionParticleType(explosionRadius));
    }

    public void causeExplode(Vec3 vec3) {
        buildExplosion(vec3).explode();

        if (discardAfterExplode()) {
            this.discard();
        }
    }

    public ParticleTool.ParticleType explosionParticleType(float radius) {
        ParticleTool.ParticleType particleType;

        if (radius <= 4) {
            particleType = ParticleTool.ParticleType.SMALL;
        } else if (radius > 4 && radius < 10) {
            particleType = ParticleTool.ParticleType.MEDIUM;
        } else if (radius >= 10 && radius < 20) {
            particleType = ParticleTool.ParticleType.HUGE;
        } else {
            particleType = ParticleTool.ParticleType.GIANT;
        }

        return particleType;
    }

    public boolean discardAfterExplode() {
        return false;
    }

    private void updateChunkLoading(ServerLevel serverLevel) {
        ChunkPos currentPos = new ChunkPos(blockPosition());

        // 检查是否需要更新
        if (lastChunkPos != null && lastChunkPos.equals(currentPos)) {
            return;
        }

        // 计算需要加载的新区块
        Set<ChunkPos> neededChunks = new HashSet<>();
        for (int x = -CHUNK_RADIUS; x <= CHUNK_RADIUS; x++) {
            for (int z = -CHUNK_RADIUS; z <= CHUNK_RADIUS; z++) {
                neededChunks.add(new ChunkPos(currentPos.x + x, currentPos.z + z));
            }
        }

        // 释放不再需要的区块
        Set<ChunkPos> toRelease = new HashSet<>(currentChunks);
        toRelease.removeAll(neededChunks);
        for (ChunkPos pos : toRelease) {
            ChunkLoadManager.releaseChunk(serverLevel, pos);
            currentChunks.remove(pos);
        }

        // 加载新区块
        for (ChunkPos pos : neededChunks) {
            if (!currentChunks.contains(pos)) {
                ChunkLoadManager.forceChunk(serverLevel, pos);
                currentChunks.add(pos);
            }
        }

        lastChunkPos = currentPos;
    }

    @Override
    public void remove(Entity.@NotNull RemovalReason reason) {
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            // 释放所有加载的区块
            for (ChunkPos pos : currentChunks) {
                ChunkLoadManager.releaseChunk(serverLevel, pos);
            }
            currentChunks.clear();
        }
        super.remove(reason);
    }

    @Override
    public void syncMotion() {
        if (this.level().isClientSide) return;
        if (!shouldSyncMotion()) return;

        if (this.tickCount % this.getType().updateInterval() == 0) {
            NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ClientMotionSyncMessage(this));
        }
    }

    public boolean isFastMoving() {
        return this.getDeltaMovement().length() >= 0.5;
    }

    public boolean shouldSyncMotion() {
        return false;
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        var motion = this.getDeltaMovement();
        buffer.writeFloat((float) motion.x);
        buffer.writeFloat((float) motion.y);
        buffer.writeFloat((float) motion.z);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        this.setDeltaMovement(additionalData.readFloat(), additionalData.readFloat(), additionalData.readFloat());
    }

    @NotNull
    public SoundEvent getCloseSound() {
        return SoundEvents.EMPTY;
    }

    @NotNull
    public SoundEvent getSound() {
        return SoundEvents.EMPTY;
    }

    public float getVolume() {
        return 0.5f;
    }

    public boolean forceLoadChunk() {
        return false;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setExplosionDamage(float explosionDamage) {
        this.explosionDamage = explosionDamage;
    }

    public void setExplosionRadius(float radius) {
        this.explosionRadius = radius;
    }

    public float getGravity() {
        return this.gravity;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public void largeTrail() {
        if (level().isClientSide && tickCount > 1) {
            double l = getDeltaMovement().length();
            for (double i = 0; i < l; i += 2) {
                Vec3 startPos = new Vec3(xo, yo, zo);
                Vec3 pos = startPos.add(getDeltaMovement().normalize().scale(-i));
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }
    }

    public void mediumTrail() {
        if (level().isClientSide && tickCount > 1) {
            double l = getDeltaMovement().length();
            for (double i = 0; i < l; i += 2) {
                Vec3 startPos = new Vec3(xo, yo, zo);
                Vec3 pos = startPos.add(getDeltaMovement().normalize().scale(-i));
                float random = this.random.nextFloat();
                level().addParticle(new CustomCloudOption(0.6f, 0.58f, 0.57f, (int) (120 + 40 * random), 1.5f + 0.5f * random, 0, false, false), pos.x + 0.25f * random, pos.y + 0.25f * random, pos.z + 0.25f * random, 0, 0, 0);
            }
        }
    }

    public void smallTrail() {
        if (level().isClientSide && tickCount > 1) {
            double l = getDeltaMovement().length();
            for (double i = 0; i < l; i += 2) {
                Vec3 startPos = new Vec3(xo, yo, zo);
                Vec3 pos = startPos.add(getDeltaMovement().normalize().scale(-i));
                float random = this.random.nextFloat();
                level().addAlwaysVisibleParticle(ParticleTypes.SMOKE, true, pos.x + 0.25f * random, pos.y + 0.25f * random, pos.z + 0.25f * random, 0, 0, 0);
            }
        }
    }
}
