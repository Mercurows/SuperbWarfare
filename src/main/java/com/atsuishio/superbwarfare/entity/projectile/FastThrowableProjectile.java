package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage;
import com.atsuishio.superbwarfare.tools.ChunkLoadManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.atsuishio.superbwarfare.tools.TraceTool.getBlocksAlongRay;

public abstract class FastThrowableProjectile extends ThrowableItemProjectile implements CustomSyncMotionEntity, IEntityWithComplexSpawn {

    public static Consumer<FastThrowableProjectile> flySound = projectile -> {
    };
    public static Consumer<FastThrowableProjectile> nearFlySound = projectile -> {
    };

    public int durability = 50;

    public boolean firstHit = true;

    private boolean isFastMoving = false;

    private static final int CHUNK_RADIUS = 1; // 3x3区块
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
        this.applyGravity();

        // 同步动量
        this.syncMotion();

        // 更新区块加载位置
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel && forceLoadChunk()) {
            updateChunkLoading(serverLevel);
        }
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

                    if (hardness <= durability && hardness != -1) {
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

    public void causeExplode(Vec3 vec3) {
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
    public void remove(Entity.RemovalReason reason) {
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
            PacketDistributor.sendToAllPlayers(new ClientMotionSyncMessage(this));
        }
    }

    public boolean isFastMoving() {
        return this.getDeltaMovement().length() >= 0.5;
    }

    public boolean shouldSyncMotion() {
        return false;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        var motion = this.getDeltaMovement();
        buffer.writeFloat((float) motion.x);
        buffer.writeFloat((float) motion.y);
        buffer.writeFloat((float) motion.z);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
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
}
