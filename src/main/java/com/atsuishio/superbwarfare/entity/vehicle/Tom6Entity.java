package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.vehicle.VehicleProp;
import com.atsuishio.superbwarfare.data.vehicle.subdata.DestroyInfo;
import com.atsuishio.superbwarfare.entity.projectile.MelonBombEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.CameraTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isFreeCam;
import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraPitch;
import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraYaw;

public class Tom6Entity extends VehicleEntity implements GeoEntity {

    public static final EntityDataAccessor<Boolean> MELON = SynchedEntityData.defineId(Tom6Entity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public float deltaXo;
    public float deltaYo;
    public float deltaX;
    public float deltaY;

    public Tom6Entity(EntityType<Tom6Entity> type, Level world) {
        super(type, world);

        appendModification(VehicleProp.DESTROY_INFO, (data, v) -> {
            if (data.vehicle.getEntityData().get(MELON)) {
                return new DestroyInfo(
                        v.crashPassengers,
                        v.explodePassengers,
                        v.explodeBlocks,
                        VehicleConfig.TOM_6_BOMB_EXPLOSION_DAMAGE.get(),
                        VehicleConfig.TOM_6_BOMB_EXPLOSION_RADIUS.get().floatValue(),
                        ParticleTool.ParticleType.HUGE
                );
            } else {
                return v;
            }
        });
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(4, 1, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MELON, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Melon", this.entityData.get(MELON));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(MELON, compound.getBoolean("Melon"));
    }

    @Override
    public boolean shouldSendHitParticles() {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(ModSounds.WHEEL_STEP.get(), (float) (getDeltaMovement().length() * 0.3), random.nextFloat() * 0.1f + 1f);
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        if (player.getMainHandItem().is(Items.MELON) && !entityData.get(MELON)) {
            entityData.set(MELON, true);
            player.getMainHandItem().shrink(1);
            player.level().playSound(player, this.getOnPos(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1, 1);
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    @Override
    public void baseTick() {
        deltaXo = deltaX;
        deltaYo = deltaY;
        super.baseTick();

        deltaX = entityData.get(MOUSE_SPEED_Y);
        deltaY = entityData.get(MOUSE_SPEED_X);

        float f = (float) Mth.clamp(0.69f + 0.101f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90, 0.01, 0.99);

        boolean forward = Mth.abs((float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) < 90;

        this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).scale((forward ? 0.24 : -0.24) * this.getDeltaMovement().length())));
        this.setDeltaMovement(this.getDeltaMovement().multiply(f, f, f));

        if (this.isInWater() && this.tickCount % 4 == 0) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.6, 0.6));
            if (lastTickSpeed > 0.4) {
                this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, this.getFirstPassenger() == null ? this : this.getFirstPassenger()), (float) (20 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))));
            }
        }

        this.terrainCompact(1f, 1.2f);
        this.refreshDimensions();
    }

    @Override
    public void travel() {
        Entity passenger = this.getFirstPassenger();

        if (passenger == null || isInWater()) {
            setLeftInputDown(false);
            setRightInputDown(false);
            setForwardInputDown(false);
            setBackInputDown(false);
            setUpInputDown(false);
            setDownInputDown(false);
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.95f);
            if (onGround()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.96, 1, 0.96));
            } else {
                this.setXRot(Mth.clamp(this.getXRot() + 0.1f, -89, 89));
            }
        } else if (passenger instanceof Player player) {
            if (forwardInputDown() && getEnergy() > 0) {
                this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.1f, 1f));
            }

            if (this.level() instanceof ServerLevel) {
                this.consumeEnergy((int) (Mth.abs(this.entityData.get(POWER)) * VehicleConfig.TOM_6_ENERGY_COST.get()));
            }

            if (backInputDown() || downInputDown()) {
                this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - (this.entityData.get(POWER) > 0 ? 0.1f : 0.01f), onGround() ? -0.2f : 0.2f));
            }

            if (!onGround()) {
                if (rightInputDown()) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 0.4f);
                } else if (this.leftInputDown()) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 0.4f);
                }
            }

            float diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(passenger.getYHeadRot() - this.getYRot()));
            float diffX = Math.clamp(-60f, 60f, Mth.wrapDegrees(passenger.getXRot() - this.getXRot()));

            float roll = Mth.abs(Mth.clamp(getRoll() / 60, -1.5f, 1.5f));

            float addY = Mth.clamp(Math.min((this.onGround() ? 1.5f : 0.9f) * (float) Math.max(getDeltaMovement().length() - 0.06, 0.1), 0.9f) * diffY - 0.5f * this.entityData.get(DELTA_ROT), -3 * (roll + 1), 3 * (roll + 1));
            float addX = Mth.clamp(Math.min((float) Math.max(getDeltaMovement().length() - 0.1, 0.01), 0.9f) * diffX, -4, 4);
            float addZ = this.entityData.get(DELTA_ROT) - (this.onGround() ? 0 : 0.01f) * diffY * (float) getDeltaMovement().length();

            float i = getXRot() / 90;

            float yRotSync = addY * (1 - Mth.abs(i)) + addZ * i;

            this.setYRot(this.getYRot() + yRotSync);
            this.setXRot(Mth.clamp(this.getXRot() + addX, onGround() ? -12 : -120, onGround() ? 3 : 120));
            this.setZRot(this.getRoll() - addZ * (1 - Mth.abs(i)));

            // 空格投掷西瓜炸弹
            if (upInputDown() && !onGround() && entityData.get(MELON)) {
                entityData.set(MELON, false);

                Matrix4f transform = getVehicleTransform(1);
                Vector4f worldPosition;
                worldPosition = transformPosition(transform, 0, 0.3f, 0);

                MelonBombEntity melonBomb = new MelonBombEntity(player, player.level());
                melonBomb.setExplosionDamage(VehicleConfig.TOM_6_BOMB_EXPLOSION_DAMAGE.get());
                melonBomb.setExplosionRadius(VehicleConfig.TOM_6_BOMB_EXPLOSION_RADIUS.get().floatValue());
                melonBomb.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
                melonBomb.shoot(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z, (float) getDeltaMovement().length(), 0);
                passenger.level().addFreshEntity(melonBomb);

                this.level().playSound(null, getOnPos(), SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1, 1);
                setUpInputDown(false);
            }
        }

        this.entityData.set(POWER, this.entityData.get(POWER) * 0.995f);
        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * 0.95f);

        this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale(0.04 * this.entityData.get(POWER))));

        setDeltaMovement(getDeltaMovement().add(0.0f, Mth.clamp(Math.sin((onGround() ? 45 : -(getXRot() - 20)) * Mth.DEG_TO_RAD) * Math.sin((90 - this.getXRot()) * Mth.DEG_TO_RAD) * getDeltaMovement().dot(getViewVector(1)) * 0.04, -0.04, 0.09), 0.0f));

//        Vector3f direction = getRightDirection().mul(-Math.sin(this.getRoll() * Mth.DEG_TO_RAD) * this.entityData.get(POWER));
//        setDeltaMovement(getDeltaMovement().add(new Vec3(direction.x, direction.y, direction.z).scale(3)));
//
//        this.setDeltaMovement(this.getDeltaMovement().add(
//                Mth.sin(-this.getYRot() * 0.017453292F) * 0.19 * this.entityData.get(POWER),
//                Mth.clamp(Math.sin((onGround() ? 45 : -(getXRot() - 30)) * Mth.DEG_TO_RAD) * getDeltaMovement().dot(getViewVector(1)) * 0.067, -0.04, 0.09),
//                Mth.cos(this.getYRot() * 0.017453292F) * 0.19 * this.entityData.get(POWER)
//        ));
    }

    @Override
    public boolean engineRunning() {
        return (getFirstPassenger() != null && Math.abs(getDeltaMovement().length()) > 0);
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.FLY_LOOP.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return (float) getDeltaMovement().length();
    }

    protected void clampRotation(Entity entity) {
        passengerYaw(entity, -45, 45, 0);
        passengerPitch(entity, -60, 85, 0);
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

        passengerPos(passenger, callback, 0, 0.6f, -0.4f, getVehicleTransform(1));
    }

    @Override
    public void copyEntityData(Entity entity) {
        entity.setYBodyRot(getYRot());
    }

    @Override
    public int passengerSeatLocation(Entity entity) {
        return 2;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return 0.3;
    }

    @Override
    public float rotateYOffset() {
        return 0.5f;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (isFreeCam(player) && this.getSeatIndex(player) == 0 && Mth.abs((float) (freeCameraYaw * freeCameraPitch)) > 0.01) {
            return new Vec2((float) (getYaw(partialTicks) - 0.5f * Mth.lerp(partialTicks, deltaYo, deltaY) - freeCameraYaw), (float) (getPitch(partialTicks) - 0.5f * Mth.lerp(partialTicks, deltaXo, deltaX) + freeCameraPitch));
        }

        return super.getCameraRotation(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (isFreeCam(player) && this.getSeatIndex(player) == 0 && Mth.abs((float) (freeCameraYaw * freeCameraPitch)) > 0.01) {
            Matrix4f transform = getClientVehicleTransform(partialTicks);

            Vector4f maxCameraPosition = transformPosition(transform, 0, 3f, -6 - (float) ClientMouseHandler.custom3pDistanceLerp);
            Vec3 finalPos = CameraTool.getMaxZoom(transform, maxCameraPosition);

            if (isFirstPerson) {
                return new Vec3(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
            } else {
                return finalPos;
            }
        }
        return super.getCameraPosition(partialTicks, player, false, false);
    }
}
