package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
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

    public int durability = 50;

    public boolean firstHit = true;

    private boolean isFastMoving = false;

    private static final int CHUNK_RADIUS = 1; // 加载3x3区块区域
    private final Set<ChunkPos> forcedChunks = new HashSet<>();
    private ChunkPos lastChunkPos;

    public FastThrowableProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public FastThrowableProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, double pX, double pY, double pZ, Level pLevel) {
        super(pEntityType, pX, pY, pZ, pLevel);
    }

    public FastThrowableProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, @Nullable LivingEntity pShooter, Level pLevel) {
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
        Vec3 vec31 = this.getDeltaMovement();
        this.setDeltaMovement(vec31.x, vec31.y - (double) this.getGravity(), vec31.z);

        // 同步动量
        this.syncMotion();

        // 更新区块加载位置
        if (!level().isClientSide && forceLoadChunk()) {
            updateChunkLoading();
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
                        discard();
                    }
                }
            }
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

    @Override
    public void syncMotion() {
        if (this.level().isClientSide) return;
        if (!shouldSyncMotion()) return;

        if (this.tickCount % this.getType().updateInterval() == 0) {
            Mod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new ClientMotionSyncMessage(this));
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
