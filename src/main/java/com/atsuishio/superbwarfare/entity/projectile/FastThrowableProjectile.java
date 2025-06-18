package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
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
        this.applyGravity();

        // 同步动量
        this.syncMotion();
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

                    durability -= 10 + (int) (0.5 * hardness);

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


    @Override
    public void syncMotion() {
        if (this.level().isClientSide) return;
        if (!shouldSyncMotion()) return;

        if (this.tickCount % this.getType().updateInterval() == 0) {
            PacketDistributor.sendToAllPlayers(new ClientMotionSyncMessage(this));
        }
    }

    public boolean isFastMoving() {
        return this.getDeltaMovement().lengthSqr() >= 6.25;
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
}
