package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage;
import com.atsuishio.superbwarfare.tools.ChunkLoadTool;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.ProjectileTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Set;

public class CannonShellEntity extends FastThrowableProjectile implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damage = 0;
    private float radius = 0;
    private float explosionDamage = 0;
    private float fireProbability = 0;
    private int fireTime = 0;
    private int durability = 40;
    private boolean firstHit = true;
    public Set<Long> loadedChunks = new HashSet<>();
    private float gravity = 0.1f;

    public CannonShellEntity(EntityType<? extends CannonShellEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public CannonShellEntity(LivingEntity entity, Level world, float damage, float radius, float explosionDamage, float fireProbability, int fireTime, float gravity) {
        super(ModEntities.CANNON_SHELL.get(), entity, world);
        this.damage = damage;
        this.radius = radius;
        this.explosionDamage = explosionDamage;
        this.fireProbability = fireProbability;
        this.fireTime = fireTime;
        this.gravity = gravity;
    }

    public CannonShellEntity durability(int durability) {
        this.durability = durability;
        return this;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);

        pCompound.putFloat("Damage", this.damage);
        pCompound.putFloat("ExplosionDamage", this.explosionDamage);
        pCompound.putFloat("Radius", this.radius);
        pCompound.putFloat("FireProbability", this.fireProbability);
        pCompound.putInt("FireTime", this.fireTime);
        pCompound.putInt("Durability", this.durability);

        ListTag listTag = new ListTag();
        for (long chunkPos : this.loadedChunks) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Pos", chunkPos);
            listTag.add(tag);
        }
        pCompound.put("Chunks", listTag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
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

        if (pCompound.contains("Chunks")) {
            ListTag listTag = pCompound.getList("Chunks", 10);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag tag = listTag.getCompound(i);
                this.loadedChunks.add(tag.getLong("Pos"));
            }
        }
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.HE_5_INCHES.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    public void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        if (this.level() instanceof ServerLevel) {
            Entity entity = entityHitResult.getEntity();
            entity.hurt(ModDamageTypes.causeCannonFireDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            if (this.getOwner() instanceof LivingEntity living) {
                if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                    living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                    PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
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
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        if (this.level() instanceof ServerLevel) {
            double x = blockHitResult.getLocation().x;
            double y = blockHitResult.getLocation().y;
            double z = blockHitResult.getLocation().z;

            if (ExplosionConfig.EXPLOSION_DESTROY.get()) {
                float hardness = this.level().getBlockState(BlockPos.containing(x, y, z)).getBlock().defaultDestroyTime();
                BlockState blockState = this.level().getBlockState(BlockPos.containing(x, y, z));

                if (hardness == -1) {
                    this.discard();
                    causeExplode(blockHitResult.getLocation());
                    return;
                }

                this.durability -= (int) hardness;

                if (hardness <= 50) {
                    BlockPos blockPos = BlockPos.containing(x, y, z);
                    Block.dropResources(this.level().getBlockState(blockPos), this.level(), BlockPos.containing(x, y, z), null);
                    this.level().destroyBlock(blockPos, true);
                }

                if (blockState.is(ModBlocks.SANDBAG.get()) || blockState.is(Blocks.NETHERITE_BLOCK)) {
                    this.durability -= 10;
                }

                if (blockState.is(Blocks.IRON_BLOCK) || blockState.is(Blocks.COPPER_BLOCK)) {
                    this.durability -= 5;
                }

                if (blockState.is(Blocks.GOLD_BLOCK)) {
                    this.durability -= 3;
                }

                if (this.durability <= 0) {
                    causeExplode(blockHitResult.getLocation());
                } else {
                    if (this.firstHit) {
                        ParticleTool.cannonHitParticles(this.level(), this.position(), this);
                        causeExplode(blockHitResult.getLocation());
                        this.firstHit = false;
                    }
                    apExplode(blockHitResult);
                }
            } else {
                if (this.durability > 0) {
                    apExplode(blockHitResult);
                }
                causeExplode(blockHitResult.getLocation());
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            ParticleTool.sendParticle(serverLevel, ParticleTypes.SMOKE, this.xo, this.yo, this.zo,
                    1, 0, 0, 0, 0.001, true);

            // 更新需要加载的区块
            ChunkLoadTool.updateLoadedChunks(serverLevel, this, this.loadedChunks);
        }
        if (this.tickCount > 600 || this.isInWater()) {
            if (this.level() instanceof ServerLevel) {
                ProjectileTool.causeCustomExplode(this,
                        ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(), this, this.getOwner()),
                        this, this.explosionDamage, this.radius, 1.25f);
            }
            this.discard();
        }
    }

    @Override
    public void syncMotion() {
        if (!this.level().isClientSide) {
            PacketDistributor.sendToAllPlayers(new ClientMotionSyncMessage(this));
        }
    }

    private void causeExplode(Vec3 vec) {
        if (Math.random() > fireProbability) {
            fireTime = 0;
        }

        CustomExplosion explosion = new CustomExplosion(this.level(), this,
                ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(),
                        this,
                        this.getOwner()),
                explosionDamage,
                vec.x(),
                vec.y(),
                vec.z(),
                radius,
                ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP).
                setDamageMultiplier(1).setFireTime(fireTime);
        explosion.explode();
        EventHooks.onExplosionStart(this.level(), explosion);
        explosion.finalizeExplosion(false);

        if (radius > 7) {
            ParticleTool.spawnHugeExplosionParticles(this.level(), vec);
        } else {
            ParticleTool.spawnMediumExplosionParticles(this.level(), vec);
        }
        this.discard();
    }

    private void apExplode(HitResult result) {
        if (Math.random() > fireProbability) {
            fireTime = 0;
        }

        CustomExplosion explosion = new CustomExplosion(this.level(), this,
                ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(),
                        this,
                        this.getOwner()),
                explosionDamage,
                result.getLocation().x + 5 * getDeltaMovement().normalize().x,
                result.getLocation().y + 5 * getDeltaMovement().normalize().y,
                result.getLocation().z + 5 * getDeltaMovement().normalize().z,
                radius,
                ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP).
                setDamageMultiplier(1).setFireTime(fireTime);
        explosion.explode();
        EventHooks.onExplosionStart(this.level(), explosion);
        explosion.finalizeExplosion(false);

        if (radius > 7) {
            ParticleTool.spawnHugeExplosionParticles(this.level(), result.getLocation());
        } else {
            ParticleTool.spawnMediumExplosionParticles(this.level(), result.getLocation());
        }
        this.discard();
    }

    private PlayState movementPredicate(AnimationState<CannonShellEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.cannon_shell.idle"));
    }

    @Override
    protected double getDefaultGravity() {
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
    public void onRemovedFromLevel() {
        if (this.level() instanceof ServerLevel serverLevel) {
            ChunkLoadTool.unloadAllChunks(serverLevel, this, this.loadedChunks);
        }
        super.onRemovedFromLevel();
    }
}
