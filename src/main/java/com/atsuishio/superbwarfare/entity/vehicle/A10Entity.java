package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.projectile.GunGrenadeEntity;
import com.atsuishio.superbwarfare.entity.projectile.MelonBombEntity;
import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class A10Entity extends MobileVehicleEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float yRotSync;


    public A10Entity(EntityType<A10Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public float maxUpStep() {
        return 0.5f;
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(17, 3, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        this.playSound(ModSounds.WHEEL_STEP.get(), (float) (getDeltaMovement().length() * 0.5), random.nextFloat() * 0.1f + 1f);
    }

    @Override
    public boolean sendFireStarParticleOnHurt() {
        return false;
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .multiply(0.4f)
                .multiply(1.5f, DamageTypes.ARROW)
                .multiply(1.5f, DamageTypes.TRIDENT)
                .multiply(2.5f, DamageTypes.MOB_ATTACK)
                .multiply(2f, DamageTypes.MOB_ATTACK_NO_AGGRO)
                .multiply(1.5f, DamageTypes.MOB_PROJECTILE)
                .multiply(12.5f, DamageTypes.LAVA)
                .multiply(6f, DamageTypes.EXPLOSION)
                .multiply(6f, DamageTypes.PLAYER_EXPLOSION)
                .multiply(2.4f, ModDamageTypes.CUSTOM_EXPLOSION)
                .multiply(2f, ModDamageTypes.PROJECTILE_BOOM)
                .multiply(0.75f, ModDamageTypes.MINE)
                .multiply(1.5f, ModDamageTypes.CANNON_FIRE)
                .multiply(0.25f, ModTags.DamageTypes.PROJECTILE)
                .multiply(0.85f, ModTags.DamageTypes.PROJECTILE_ABSOLUTE)
                .multiply(15f, ModDamageTypes.VEHICLE_STRIKE)
                .custom((source, damage) -> getSourceAngle(source, 0.25f) * damage)
                .custom((source, damage) -> {
                    if (source.getDirectEntity() instanceof MelonBombEntity) {
                        return 3f * damage;
                    }
                    if (source.getDirectEntity() instanceof MortarShellEntity) {
                        return 1.25f * damage;
                    }
                    if (source.getDirectEntity() instanceof GunGrenadeEntity) {
                        return 1.5f * damage;
                    }
                    return damage;
                })
                .reduce(7);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        float f;

        f = (float) Mth.clamp(Math.max((onGround() ? 0.785f : 0.79f) - 0.01 * getDeltaMovement().length(), 0.5) + 0.031f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90, 0.01, 0.99);

        boolean forward = Mth.abs((float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) < 90;

        this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).scale((forward ? 0.23 : -0.23) * this.getDeltaMovement().length())));
        this.setDeltaMovement(this.getDeltaMovement().multiply(f, f, f));

        if (this.isInWater() && this.tickCount % 4 == 0) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.6, 0.6));
            if (lastTickSpeed > 0.4) {
                this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, this.getFirstPassenger() == null ? this : this.getFirstPassenger()), (float) (20 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))));
            }
        }

        this.terrainCompact(5f, 5f);
        this.refreshDimensions();
    }

    @Override
    public void travel() {
        Entity passenger = this.getFirstPassenger();

//        if (this.getEnergy() <= 0) return;

        float diffX;
        float diffY;

        if (passenger == null || isInWater()) {
            this.leftInputDown = false;
            this.rightInputDown = false;
            this.forwardInputDown = false;
            this.backInputDown = false;
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.95f);
            if (onGround()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.94, 1, 0.94));
            } else {
                this.setXRot(Mth.clamp(this.getXRot() + 0.1f, -89, 89));
            }
        } else if (passenger instanceof Player player) {
            if (level().isClientSide && this.getEnergy() > 0) {
                level().playLocalSound(this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(), this.getEngineSound(), this.getSoundSource(), Math.min((this.forwardInputDown ? 7.5f : 5f) * 2 * Mth.abs(this.entityData.get(POWER)), 0.25f), (random.nextFloat() * 0.1f + 1.2f), false);
            }

            if (forwardInputDown && getEnergy() > 0) {
                this.consumeEnergy(VehicleConfig.TOM_6_ENERGY_COST.get());
                this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.002f, 1f));
            }

            if (backInputDown) {
                this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.002f, onGround() ? -0.2f : 0.01f));
            }

            if (!onGround()) {
                if (rightInputDown) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 0.2f);
                } else if (this.leftInputDown) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 0.2f);
                }
            } else {
                // 刹车
                if (upInputDown) {
                    this.entityData.set(POWER, this.entityData.get(POWER) * 0.8f);
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.9, 1, 0.9));
                }
                if (!(backInputDown || forwardInputDown)) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.97, 1, 0.97));
                }
            }

            diffY = Mth.clamp(Mth.wrapDegrees(passenger.getYHeadRot() - this.getYRot()), -90f, 90f);
            diffX = Mth.clamp(Mth.wrapDegrees(passenger.getXRot() - this.getXRot()), -90f, 90f);

            float roll = Mth.abs(Mth.clamp(getRoll() / 60, -1.5f, 1.5f));

            float addY = Mth.clamp(Math.max((this.onGround() ? 0.1f : 0.2f) * (float) Math.max(getDeltaMovement().dot(getViewVector(1)), 0.05), 0f) * diffY - 0.5f * this.entityData.get(DELTA_ROT), -1.5f * (roll + 1), 1.5f * (roll + 1));
            float addX = Mth.clamp(Math.min((float) Math.max(getDeltaMovement().dot(getViewVector(1)) - 0.2, 0.02), 0.5f) * diffX, -1.3f, 1.3f);
            float addZ = this.entityData.get(DELTA_ROT) - (this.onGround() ? 0 : 0.01f) * diffY * (float) getDeltaMovement().dot(getViewVector(1));

            float i = getXRot() / 90;
            yRotSync = addY * (1 - Mth.abs(i)) + addZ * i;

            this.setYRot(this.getYRot() + yRotSync);
            this.setXRot(Mth.clamp(this.getXRot() + addX, onGround() ? 0 : -120, onGround() ? 0 : 120));
            this.setZRot(this.getRoll() - addZ * (1 - Mth.abs(i)));

            setFlap1LRot(Mth.clamp(-Mth.clamp(diffX, -22.5f, 22.5f) - 8 * addZ * (1 - Mth.abs(i)), -22.5f, 22.5f));
            setFlap1RRot(Mth.clamp(-Mth.clamp(diffX, -22.5f, 22.5f) + 8 * addZ * (1 - Mth.abs(i)), -22.5f, 22.5f));

            setFlap2LRot(Mth.clamp(Mth.clamp(diffX, -22.5f, 22.5f) - 8 * addZ * (1 - Mth.abs(i)), -22.5f, 22.5f));
            setFlap2RRot(Mth.clamp(Mth.clamp(diffX, -22.5f, 22.5f) + 8 * addZ * (1 - Mth.abs(i)), -22.5f, 22.5f));

            setFlap3Rot(diffY * 0.7f);

            player.displayClientMessage(Component.literal("speed: " + FormatTool.format2D(getDeltaMovement().dot(getViewVector(1)) * 72)), true);
        }

        this.entityData.set(POWER, this.entityData.get(POWER) * 0.99f);
        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * 0.95f);

        this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale(Math.max((90 + this.getXRot()) / 90, 0.3) * 0.4 * this.entityData.get(POWER))));

        double flapAngle = (getFlap1LRot() + getFlap1RRot()) / 2;

        setDeltaMovement(getDeltaMovement().add(0.0f, Mth.clamp(Math.sin((onGround() ? 25 + flapAngle : -(getXRot() - 25) + flapAngle) * Mth.DEG_TO_RAD) * Math.sin((90 - this.getXRot()) * Mth.DEG_TO_RAD) * getDeltaMovement().dot(getViewVector(1)) * 0.06, -0.04, 0.065), 0.0f));
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.WHEEL_CHAIR_ENGINE.get();
    }

    protected void clampRotation(Entity entity) {
        float f = Mth.wrapDegrees(entity.getXRot() - this.getXRot());
        float f1 = Mth.clamp(f, -85.0F, 60F);
        entity.xRotO += f1 - f;
        entity.setXRot(entity.getXRot() + f1 - f);

        entity.setYBodyRot(this.getYRot());
        float f2 = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
        float f3 = Mth.clamp(f2, -90.0F, 90.0F);
        entity.yRotO += f3 - f2;
        entity.setYRot(entity.getYRot() + f3 - f2);
        entity.setYBodyRot(this.getYRot());
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entity) {
        this.clampRotation(entity);
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        // From Immersive_Aircraft
        if (!this.hasPassenger(passenger)) {
            return;
        }

        Matrix4f transform = getVehicleTransform(1);

        float x = 0f;
        float y = 0.1f;
        float z = 3.8494875f;

        // TODO 正确调整乘客位置
        y += (float) passenger.getVehicleAttachmentPoint(this).y;

        int i = this.getSeatIndex(passenger);

        if (i == 0) {
            Vector4f worldPosition = transformPosition(transform, x, y, z);
            passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z);
        }

        if (passenger != this.getFirstPassenger()) {
            passenger.setXRot(passenger.getXRot() + (getXRot() - xRotO));
        }

        copyEntityData(passenger);
    }

    public void copyEntityData(Entity entity) {
        float i = getXRot() / 90;

        float f = Mth.wrapDegrees(entity.getYRot() - getYRot());
        float g = Mth.clamp(f, -105.0f, 105.0f);
        entity.yRotO += g - f;
        entity.setYRot(entity.getYRot() + g - f + yRotSync * Mth.abs(i));
        entity.setYHeadRot(entity.getYRot());
        entity.setYBodyRot(getYRot());
    }

    @Override
    public Matrix4f getVehicleTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo + 2.375f, getY() + 2.375f), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        transform.rotate(Axis.XP.rotationDegrees(Mth.lerp(ticks, xRotO, getXRot())));
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(ticks, prevRoll, getRoll())));
        return transform;
    }

    @Override
    public void destroy() {
        if (this.crash) {
            crashPassengers();
        } else {
            explodePassengers();
        }

        if (level() instanceof ServerLevel) {
            CustomExplosion explosion = new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), this, getAttacker()), 300.0f,
                    this.getX(), this.getY(), this.getZ(), 8f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1);
            explosion.explode();
            EventHooks.onExplosionStart(this.level(), explosion);
            explosion.finalizeExplosion(false);
            ParticleTool.spawnHugeExplosionParticles(this.level(), this.position());
        }
        this.discard();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public float getMaxHealth() {
        return 300;
    }

    @Override
    public int getMaxEnergy() {
        return 5000000;
    }

    @Override
    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/vehicle_icon/tom_6_icon.png");
    }

    @Override
    public boolean allowFreeCam() {
        return true;
    }
}
