package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;

public class CannonShellEntity extends FastThrowableProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float fireProbability = 0;
    private int fireTime = 0;

    public enum Type {
        AP, HE, CM, GRAPE, WP
    }

    private Type type = Type.AP;
    private int spreadAmount = 50;
    private int spreadAngle = 15;

    public CannonShellEntity(EntityType<? extends CannonShellEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public CannonShellEntity durability(int durability) {
        this.durability = durability;
        return this;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean isColliding(BlockPos pPos, BlockState pState) {
        return true;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);

        pCompound.putFloat("FireProbability", this.fireProbability);
        pCompound.putInt("FireTime", this.fireTime);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        if (pCompound.contains("FireProbability")) {
            this.fireProbability = pCompound.getFloat("FireProbability");
        }

        if (pCompound.contains("FireTime")) {
            this.fireTime = pCompound.getInt("FireTime");
        }
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.LARGE_SHELL_HE.get();
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (this.level() instanceof ServerLevel) {
            if (type == Type.WP) {
                findNearEntity(blockHitResult.getLocation(), getOwner());
                causeExplode(blockHitResult.getLocation());
                this.discard();
            }
            if (type != Type.AP) {
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
    public void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (this.level() instanceof ServerLevel) {
            Entity entity = entityHitResult.getEntity();
            if (this.getOwner() != null && entity == this.getOwner().getVehicle())
                return;

            if (type == Type.GRAPE) {
                DamageHandler.doDamage(entity, ModDamageTypes.causeGrapeShotHitDamage(this.level().registryAccess(), this, this.getOwner()), 0.5f * this.damage);
            } else {
                DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);
            }

            if (type == Type.WP) {
                findNearEntity(entityHitResult.getLocation(), getOwner());
            }

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            ParticleTool.cannonHitParticles(this.level(), this.position(), this);
            causeExplode(entityHitResult.getLocation());
            if (entity instanceof VehicleEntity) {
                this.discard();
            }

        }
    }

    public void findNearEntity(Vec3 pos, Entity shooter) {
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }

        var entities = new SeekTool.Builder(shooter)
                .withinRange(pos, explosionRadius)
                .notItsVehicle()
                .baseFilter()
                .noVehicle()
                .build();

        for (Entity e : entities) {
            var dis = pos.distanceTo(e.position());

            if (e instanceof LivingEntity living && checkNoClip(e, pos)) {
                if (living instanceof Player player && player.isCreative()) {
                    return;
                }
                if (!living.level().isClientSide()) {
                    living.addEffect(new MobEffectInstance(ModMobEffects.PHOSPHORUS_FIRE.get(), (int) (300 - 30 * dis), (int) Math.max(explosionRadius - dis, 0)), this.getOwner());
                }
            }
        }
    }

    @Override
    public void tick() {
        if (type == Type.GRAPE) {
            releaseGrapeShot(getOwner());
        }
        super.tick();

        mediumTrail();
        destroyBlock();

        if ((type == Type.CM || type == Type.WP) && tickCount > 3) {
            // 使用Minecraft内置的光线追踪进行碰撞检测
            int spreadTime = 8;
            BlockHitResult hitResult = level().clip(new ClipContext(
                    position(),
                    position().add(getDeltaMovement().scale(spreadTime)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.ANY,
                    this
            ));

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                if (type == Type.CM) {
                    releaseClusterMunitions(getOwner());
                } else {
                    releaseWp(getOwner());
                }
            }

            Entity target = TraceTool.findLookingEntity(this, getDeltaMovement().scale(spreadTime).length());

            if (target != null && target != this) {
                if (type == Type.CM) {
                    releaseClusterMunitions(getOwner());
                } else {
                    releaseWp(getOwner());
                }
            }
        }
    }

    public void releaseClusterMunitions(Entity shooter) {
        if (level() instanceof ServerLevel serverLevel) {
            ParticleTool.spawnMediumExplosionParticles(serverLevel, position());
            for (int index0 = 0; index0 < spreadAmount; index0++) {
                GunGrenadeEntity gunGrenadeEntity = new GunGrenadeEntity(shooter, serverLevel,
                        6 * damage / spreadAmount,
                        5 * explosionDamage / spreadAmount,
                        explosionRadius / 2
                );

                gunGrenadeEntity.setPos(position().x, position().y, position().z);
                gunGrenadeEntity.shoot(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z, (float) (random.nextFloat() * 0.2f + 0.4f * getDeltaMovement().length()),
                        spreadAngle);
                serverLevel.addFreshEntity(gunGrenadeEntity);
            }
            discard();
        }
    }

    public void releaseGrapeShot(Entity shooter) {
        if (level() instanceof ServerLevel serverLevel) {
            for (int index0 = 0; index0 < spreadAmount; index0++) {
                GrapeshotEntity grapeProjectileEntity = new GrapeshotEntity(shooter, serverLevel, damage / spreadAmount);
                grapeProjectileEntity.setPos(this.xo, this.yo, this.zo);
                grapeProjectileEntity.shoot(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z, (float) (random.nextFloat() * 0.2f + 0.9f * getDeltaMovement().length()),
                        spreadAngle);
                serverLevel.addFreshEntity(grapeProjectileEntity);
            }
            discard();
        }
    }

    private void releaseWp(Entity shooter) {
        if (level() instanceof ServerLevel serverLevel) {
            ParticleTool.spawnMediumExplosionParticles(serverLevel, position());
            for (int index0 = 0; index0 < spreadAmount; index0++) {
                WhitePhosphorusProjectileEntity whitePhosphorusProjectileEntity = new WhitePhosphorusProjectileEntity(shooter, serverLevel);

                whitePhosphorusProjectileEntity.setPos(position().x, position().y, position().z);
                whitePhosphorusProjectileEntity.shoot(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z, (float) (random.nextFloat() * 0.02f + 0.3f * getDeltaMovement().length()),
                        spreadAngle);
                serverLevel.addFreshEntity(whitePhosphorusProjectileEntity);
            }
            discard();
        }
    }

    @Override
    public void syncMotion() {
        if (!this.level().isClientSide) {
            NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ClientMotionSyncMessage(this));
        }
    }

    @Override
    public boolean discardAfterExplode() {
        return true;
    }

    private PlayState movementPredicate(AnimationState<CannonShellEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.cannon_shell.idle"));
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
    public @NotNull SoundEvent getSound() {
        return ModSounds.SHELL_FLY.get();
    }

    @Override
    public float getVolume() {
        return 0.07f;
    }

    @Override
    public boolean forceLoadChunk() {
        return true;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setSpreadAmount(int spreadAmount) {
        this.spreadAmount = spreadAmount;
    }

    public void setSpreadAngle(int spreadAngle) {
        this.spreadAngle = spreadAngle;
    }

    @Override
    public int getLife() {
        return 800;
    }
}
