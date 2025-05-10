package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.projectile.GunGrenadeEntity;
import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.*;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.*;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class A10Entity extends ContainerMobileVehicleEntity implements GeoEntity, WeaponVehicleEntity, AircraftEntity {
    public static final EntityDataAccessor<Integer> LOADED_ROCKET = SynchedEntityData.defineId(A10Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> LOADED_BOMB = SynchedEntityData.defineId(A10Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> LOADED_MISSILE = SynchedEntityData.defineId(A10Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> FIRE_TIME = SynchedEntityData.defineId(A10Entity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(A10Entity.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float yRotSync;
    private boolean fly;
    private int flyTime;
    public int fireIndex;
    public int reloadCoolDownBomb;
    public int reloadCoolDownMissile;
    public String lockingTargetO = "none";
    public String lockingTarget = "none";

    public String test;
    public int lockTime;
    public boolean locking;
    public boolean locked;

    public A10Entity(EntityType<A10Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public float maxUpStep() {
        return 0.5f;
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        new SmallCannonShellWeapon()
                                .damage(VehicleConfig.A_10_CANNON_DAMAGE.get())
                                .explosionDamage(VehicleConfig.A_10_CANNON_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.A_10_CANNON_EXPLOSION_RADIUS.get().floatValue())
                                .sound(ModSounds.INTO_CANNON.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/cannon_30mm.png")),
                        new HeliRocketWeapon()
                                .damage(VehicleConfig.A_10_ROCKET_DAMAGE.get())
                                .explosionDamage(VehicleConfig.A_10_ROCKET_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.A_10_ROCKET_EXPLOSION_RADIUS.get().floatValue())
                                .sound(ModSounds.INTO_MISSILE.get()),
                        new Mk82Weapon()
                                .sound(ModSounds.INTO_MISSILE.get()),
                        new Agm65Weapon()
                                .sound(ModSounds.INTO_MISSILE.get()),
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(17, 3, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LOADED_ROCKET, 0)
                .define(LOADED_BOMB, 0)
                .define(LOADED_MISSILE, 0)
                .define(FIRE_TIME, 0)
                .define(TARGET_UUID, "none");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("LoadedRocket", this.entityData.get(LOADED_ROCKET));
        compound.putInt("LoadedBomb", this.entityData.get(LOADED_BOMB));
        compound.putInt("LoadedMissile", this.entityData.get(LOADED_MISSILE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(LOADED_ROCKET, compound.getInt("LoadedRocket"));
        this.entityData.set(LOADED_BOMB, compound.getInt("LoadedBomb"));
        this.entityData.set(LOADED_MISSILE, compound.getInt("LoadedMissile"));
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        this.playSound(ModSounds.WHEEL_STEP.get(), (float) (getDeltaMovement().length() * 0.2), random.nextFloat() * 0.1f + 1f);
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

        lockingTargetO = getTargetUuid();

        super.baseTick();
        float f;

        f = (float) Mth.clamp(Math.max((onGround() ? 0.88f : 0.885f) - 0.01 * getDeltaMovement().length(), 0.5) + 0.001f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90, 0.01, 0.99);

        boolean forward = Mth.abs((float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) < 90;

        this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).scale((forward ? 0.13 : -0.02) * this.getDeltaMovement().length())));
        this.setDeltaMovement(this.getDeltaMovement().multiply(f, f, f));

        if (this.isInWater() && this.tickCount % 4 == 0) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.6, 0.6));
            if (lastTickSpeed > 0.4) {
                this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, this.getFirstPassenger() == null ? this : this.getFirstPassenger()), (float) (20 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))));
            }
        }

        if (this.level() instanceof ServerLevel) {
            if (reloadCoolDown > 0) {
                reloadCoolDown--;
            }
            if (reloadCoolDownBomb > 0) {
                reloadCoolDownBomb--;
            }
            if (reloadCoolDownMissile > 0) {
                reloadCoolDownMissile--;
            }
            handleAmmo();
        }

        if (this.getFirstPassenger() instanceof Player player && fireInputDown) {
            if (this.getWeaponIndex(0) == 0) {
                if ((this.entityData.get(AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(player)) && !cannotFire) {
                    vehicleShoot(player, 0);
                }
            } else if (this.getWeaponIndex(0) == 1) {
                if (this.entityData.get(AMMO) > 0) {
                    vehicleShoot(player, 0);
                }
            }
        }
        if (onGround()) {
            this.terrainCompact(4f, 4f);
        } else {
            this.setZRot(this.roll * 0.99f);
        }

        if (entityData.get(FIRE_TIME) > 0) {
            entityData.set(FIRE_TIME, entityData.get(FIRE_TIME) - 1);
        }

        if (this.getWeaponIndex(0) == 3) {
            seekTarget();
        }

        releaseDecoy();
        this.refreshDimensions();
    }

    private void handleAmmo() {
        boolean hasCreativeAmmoBox = this.getFirstPassenger() instanceof Player player && InventoryTool.hasCreativeAmmoBox(player);

        int ammoCount = countItem(ModItems.SMALL_SHELL.get());

        if ((hasItem(ModItems.ROCKET_70.get()) || hasCreativeAmmoBox) && reloadCoolDown == 0 && this.getEntityData().get(LOADED_ROCKET) < 28) {
            this.entityData.set(LOADED_ROCKET, this.getEntityData().get(LOADED_ROCKET) + 1);
            reloadCoolDown = 15;
            if (!hasCreativeAmmoBox) {
                this.getItemStacks().stream().filter(stack -> stack.is(ModItems.ROCKET_70.get())).findFirst().ifPresent(stack -> stack.shrink(1));
            }
            this.level().playSound(null, this, ModSounds.MISSILE_RELOAD.get(), this.getSoundSource(), 2, 1);
        }

        if ((hasItem(ModItems.MEDIUM_AERIAL_BOMB.get()) || hasCreativeAmmoBox) && reloadCoolDownBomb == 0 && this.getEntityData().get(LOADED_BOMB) < 3) {
            this.entityData.set(LOADED_BOMB, this.getEntityData().get(LOADED_BOMB) + 1);
            reloadCoolDownBomb = 300;
            if (!hasCreativeAmmoBox) {
                this.getItemStacks().stream().filter(stack -> stack.is(ModItems.MEDIUM_AERIAL_BOMB.get())).findFirst().ifPresent(stack -> stack.shrink(1));
            }
            this.level().playSound(null, this, ModSounds.BOMB_RELOAD.get(), this.getSoundSource(), 2, 1);
        }

        if ((hasItem(ModItems.AGM.get()) || hasCreativeAmmoBox) && reloadCoolDownMissile == 0 && this.getEntityData().get(LOADED_MISSILE) < 4) {
            this.entityData.set(LOADED_MISSILE, this.getEntityData().get(LOADED_MISSILE) + 1);
            reloadCoolDownMissile = 400;
            if (!hasCreativeAmmoBox) {
                this.getItemStacks().stream().filter(stack -> stack.is(ModItems.AGM.get())).findFirst().ifPresent(stack -> stack.shrink(1));
            }
            this.level().playSound(null, this, ModSounds.BOMB_RELOAD.get(), this.getSoundSource(), 2, 1);
        }

        if (this.getWeaponIndex(0) == 0) {
            this.entityData.set(AMMO, ammoCount);
        } else if (this.getWeaponIndex(0) == 1) {
            this.entityData.set(AMMO, this.getEntityData().get(LOADED_ROCKET));
        } else if (this.getWeaponIndex(0) == 2) {
            this.entityData.set(AMMO, this.getEntityData().get(LOADED_BOMB));
        } else if (this.getWeaponIndex(0) == 3) {
            this.entityData.set(AMMO, this.getEntityData().get(LOADED_MISSILE));
        }
    }

    public void seekTarget() {
        if (!(this.getFirstPassenger() instanceof Player player)) return;

        if (getTargetUuid().equals(lockingTargetO) && !getTargetUuid().equals("none")) {
            lockTime++;
        } else {
            resetSeek(player);
        }

        Entity entity = SeekTool.seekCustomSizeEntitiy(this, this.level(), 384, 20, 0.9, true);
        if (entity != null) {
            if (lockTime == 0) {
                setTargetUuid(String.valueOf(entity.getUUID()));
            }
            if (!String.valueOf(entity.getUUID()).equals(getTargetUuid())) {
                resetSeek(player);
                setTargetUuid(String.valueOf(entity.getUUID()));
            }
        } else {
            setTargetUuid("none");
        }

        if (lockTime == 1) {
            if (player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, ModSounds.JET_LOCK.get(), 2, 1);
            }
        }

        if (lockTime > 20) {
            if (player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, ModSounds.JET_LOCKON.get(), 2, 1);
            }
            locked = true;
        }
    }

    public void resetSeek(Player player) {
        lockTime = 0;
        locked = false;
        if (player instanceof ServerPlayer serverPlayer) {
            var clientboundstopsoundpacket = new ClientboundStopSoundPacket(Mod.loc("jet_lock"), SoundSource.PLAYERS);
            serverPlayer.connection.send(clientboundstopsoundpacket);
        }
    }

    public void setTargetUuid(String uuid) {
        this.lockingTarget = uuid;
    }

    public String getTargetUuid() {
        return this.lockingTarget;
    }

    @Override
    public void travel() {
        Entity passenger = this.getFirstPassenger();
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

            if (getEnergy() > 0) {
                if (forwardInputDown) {
                    this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.002f, 1f));
                }

                if (backInputDown) {
                    this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.002f, onGround() ? -0.04f : 0.05f));
                }
            }

            if (!onGround()) {
                if (rightInputDown) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 0.4f);
                } else if (this.leftInputDown) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 0.4f);
                }
            } else {
                // 刹车
                if (upInputDown) {
                    this.entityData.set(POWER, this.entityData.get(POWER) * 0.8f);
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.98, 1, 0.98));
                }
            }

            diffY = Mth.clamp(Mth.wrapDegrees(passenger.getYHeadRot() - this.getYRot()), -90f, 90f);
            diffX = Mth.clamp(Mth.wrapDegrees(passenger.getXRot() - this.getXRot()), -90f, 90f);

            float roll = Mth.abs(Mth.clamp(getRoll() / 60, -1.5f, 1.5f));

            float addY = Mth.clamp(Math.max((this.onGround() ? 0.1f : 0.2f) * (float) Math.max(getDeltaMovement().dot(getViewVector(1)), 0), 0f) * diffY - 0.5f * this.entityData.get(DELTA_ROT), -1.5f * (roll + 1), 1.5f * (roll + 1));
            float addX = Mth.clamp(Math.min((float) Math.max(getDeltaMovement().dot(getViewVector(1)) - 0.17, 0.01), 0.7f) * diffX, -2.4f, 2.4f);
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

            this.setPropellerRot(this.getPropellerRot() + 30 * this.entityData.get(POWER));

            // 起落架
            if (!onGround() && getDeltaMovement().dot(getViewVector(1)) * 72 > 140) {
                flyTime = Math.min(flyTime + 1, 20);
            }

            if (getDeltaMovement().dot(getViewVector(1)) * 72 < 140 && fly) {
                flyTime = Math.max(flyTime - 1, 0);
            }

            if (!fly && flyTime == 20) {
                fly = true;
            }

            if (fly && flyTime == 0) {
                fly = false;
            }

            if (fly) {
                entityData.set(GEAR_ROT, Math.min(entityData.get(GEAR_ROT) + 5, 85));
            } else {
                entityData.set(GEAR_ROT, Math.max(entityData.get(GEAR_ROT) - 5, 0));
            }
        }

        this.entityData.set(POWER, this.entityData.get(POWER) * 0.99f);
        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * 0.95f);

        if (getEnergy() > 0) {
            this.consumeEnergy((int) Mth.abs(this.entityData.get(POWER)) * VehicleConfig.A_10_MAX_ENERGY_COST.get());
        }

        this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale(Math.max((90 + this.getXRot()) / 90, 0.8) * 0.4 * this.entityData.get(POWER))));
        double flapAngle = (getFlap1LRot() + getFlap1RRot()) / 2;
        setDeltaMovement(getDeltaMovement().add(0.0f, Mth.clamp(Math.sin((onGround() ? 23 + flapAngle : -(getXRot() - 23) + flapAngle) * Mth.DEG_TO_RAD) * Math.sin((90 - this.getXRot()) * Mth.DEG_TO_RAD) * getDeltaMovement().dot(getViewVector(1)) * 0.063, -0.04, 0.065), 0.0f));
    }

    @Override
    public void move(@NotNull MoverType movementType, @NotNull Vec3 movement) {
        if (!this.level().isClientSide()) {
            MobileVehicleEntity.IGNORE_ENTITY_GROUND_CHECK_STEPPING = true;
        }
        if (level() instanceof ServerLevel && canCollideBlockBeastly()) {
            collideBlockBeastly();
        }

        super.move(movementType, movement);
        if (level() instanceof ServerLevel) {
            if (this.horizontalCollision) {
                collideBlock();
                if (canCollideHardBlock()) {
                    collideHardBlock();
                }
            }

            if (lastTickSpeed < 0.3 || collisionCoolDown > 0) return;
            Entity driver = EntityFindUtil.findEntity(this.level(), this.entityData.get(LAST_DRIVER_UUID));

            if ((verticalCollision)) {
                if (entityData.get(GEAR_ROT) > 10 || (Mth.abs(getRoll()) > 20)) {
                    this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) ((8 + Mth.abs(getRoll() * 0.2f)) * (lastTickSpeed - 0.3) * (lastTickSpeed - 0.3)));
                    if (!this.level().isClientSide) {
                        this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
                    }
                    this.bounceVertical(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
                } else {
                    if (Mth.abs((float) lastTickVerticalSpeed) > 0.4) {
                        this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) (96 * ((Mth.abs((float) lastTickVerticalSpeed) - 0.4) * (lastTickSpeed - 0.3) * (lastTickSpeed - 0.3))));
                        if (!this.level().isClientSide) {
                            this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
                        }
                        this.bounceVertical(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
                    }
                }

            }

            if (this.horizontalCollision) {
                this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) (126 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))));
                this.bounceHorizontal(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
                if (!this.level().isClientSide) {
                    this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
                }
                collisionCoolDown = 4;
                crash = true;
                this.entityData.set(POWER, 0.8f * entityData.get(POWER));
            }
        }
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.A_10_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return entityData.get(POWER) * 1.5f;
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
        float z = 3.95f;

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

    public Vec3 driverPos(float ticks) {
        Matrix4f transform = getVehicleTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0, 1.35f, 4f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 driverZoomPos(float ticks) {
        Matrix4f transform = getVehicleTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0, 1.35f, 4.15f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
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
        return VehicleConfig.A_10_HP.get();
    }

    @Override
    public int getMaxEnergy() {
        return VehicleConfig.A_10_MAX_ENERGY.get();
    }

    @Override
    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/vehicle_icon/a10_icon.png");
    }

    @Override
    public boolean allowFreeCam() {
        return true;
    }

    @Override
    public Vec3 shootPos(float tickDelta) {
        Matrix4f transform = getVehicleTransform(tickDelta);
        Vector4f worldPosition;
        if (getWeaponIndex(0) == 0) {
            worldPosition = transformPosition(transform, 0.1321625f, -0.56446875f, 7.85210625f);
        } else if (getWeaponIndex(0) == 1) {
            worldPosition = transformPosition(transform, 0f, -1.76f, 1.87f);
        } else {
            worldPosition = transformPosition(transform, 0f, -1.203125f, 0.0625f);
        }
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 shootVec(float tickDelta) {
        return new Vec3(getViewVector(tickDelta).x, getViewVector(tickDelta).y - 0.08, getViewVector(tickDelta).z);
    }

    @Override
    public void vehicleShoot(Player player, int type) {
        Matrix4f transform = getVehicleTransform(1);

        if (getWeaponIndex(0) == 0) {
            if (this.cannotFire) return;

            boolean hasCreativeAmmo = getFirstPassenger() instanceof Player pPlayer && InventoryTool.hasCreativeAmmoBox(pPlayer);

            Vector4f worldPosition = transformPosition(transform, 0.1321625f, -0.56446875f, 7.85210625f);

            if (this.entityData.get(AMMO) > 0 || hasCreativeAmmo) {

                entityData.set(FIRE_TIME, Math.min(entityData.get(FIRE_TIME) + 6, 6));

                var entityToSpawn = ((SmallCannonShellWeapon) getWeapon(0)).create(player);

                entityToSpawn.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
                entityToSpawn.shoot(getLookAngle().x, getLookAngle().y - 0.07, getLookAngle().z, 30, 0.5f);
                level().addFreshEntity(entityToSpawn);

                sendParticle((ServerLevel) this.level(), ParticleTypes.LARGE_SMOKE, worldPosition.x, worldPosition.y, worldPosition.z, 1, 0, 0, 0, 0, false);

                if (!hasCreativeAmmo) {
                    this.getItemStacks().stream().filter(stack -> stack.is(ModItems.SMALL_SHELL.get())).findFirst().ifPresent(stack -> stack.shrink(1));
                }

            }

            Level level = player.level();
            final Vec3 center = new Vec3(this.getX(), this.getEyeY(), this.getZ());

            for (Entity target : level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(5), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).toList()) {
                if (target instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new ShakeClientMessage(6, 5, 12, this.getX(), this.getEyeY(), this.getZ()));
                }
            }

            this.entityData.set(HEAT, this.entityData.get(HEAT) + 2);


        } else if (getWeaponIndex(0) == 1 && this.getEntityData().get(LOADED_ROCKET) > 0) {
            var heliRocketEntity = ((HeliRocketWeapon) getWeapon(0)).create(player);

            Vector4f worldPosition;
            Vector4f worldPosition2;
            Vec3 shootAngle;

            if (fireIndex == 0) {
                worldPosition = transformPosition(transform, -6.63f, -0.55f, 1.83f);
                worldPosition2 = transformPosition(transform, -6.61f, -0.55f, 2.83f);
                fireIndex = 1;
            } else if (fireIndex == 1) {
                worldPosition = transformPosition(transform, -5.28f, -1.76f, 1.87f);
                worldPosition2 = transformPosition(transform, -5.27f, -1.76f, 2.87f);
                fireIndex = 2;
            } else if (fireIndex == 2) {
                worldPosition = transformPosition(transform, 5.28f, -1.76f, 1.87f);
                worldPosition2 = transformPosition(transform, 5.27f, -1.76f, 2.87f);
                fireIndex = 3;
            } else {
                worldPosition = transformPosition(transform, 6.63f, -0.55f, 1.83f);
                worldPosition2 = transformPosition(transform, 6.61f, -0.55f, 2.83f);
                fireIndex = 0;
            }

            shootAngle = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z).vectorTo(new Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z)).normalize();

            heliRocketEntity.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            heliRocketEntity.shoot(shootAngle.x, shootAngle.y - 0.075, shootAngle.z, 8, 0.5f);
            player.level().addFreshEntity(heliRocketEntity);

            BlockPos pos = BlockPos.containing(new Vec3(worldPosition.x, worldPosition.y, worldPosition.z));

            this.level().playSound(null, pos, ModSounds.HELICOPTER_ROCKET_FIRE_3P.get(), SoundSource.PLAYERS, 4, 1);

            this.entityData.set(LOADED_ROCKET, this.getEntityData().get(LOADED_ROCKET) - 1);

            Level level = player.level();
            final Vec3 center = new Vec3(this.getX(), this.getEyeY(), this.getZ());

            for (Entity target : level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(5), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).toList()) {
                if (target instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new ShakeClientMessage(6, 5, 12, this.getX(), this.getEyeY(), this.getZ()));
                }
            }

            reloadCoolDown = 15;

        } else if (getWeaponIndex(0) == 2 && this.getEntityData().get(LOADED_BOMB) > 0) {
            var Mk82Entity = ((Mk82Weapon) getWeapon(0)).create(player);

            Vector4f worldPosition;

            if (this.getEntityData().get(LOADED_BOMB) == 3) {
                worldPosition = transformPosition(transform, 0.55625f, -1.203125f, 0.0625f);
            } else if (this.getEntityData().get(LOADED_BOMB) == 2) {
                worldPosition = transformPosition(transform, 0f, -1.203125f, 0.0625f);
            } else {
                worldPosition = transformPosition(transform, -0.55625f, -1.203125f, 0.0625f);
            }

            Mk82Entity.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            Mk82Entity.shoot(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z, (float) getDeltaMovement().length(), 10);
            player.level().addFreshEntity(Mk82Entity);

            BlockPos pos = BlockPos.containing(new Vec3(worldPosition.x, worldPosition.y, worldPosition.z));

            this.level().playSound(null, pos, ModSounds.BOMB_RELEASE.get(), SoundSource.PLAYERS, 3, 1);

            if (this.getEntityData().get(LOADED_BOMB) == 3) {
                reloadCoolDownBomb = 300;
            }
            this.entityData.set(LOADED_BOMB, this.getEntityData().get(LOADED_BOMB) - 1);

        } else if (getWeaponIndex(0) == 3 && this.getEntityData().get(LOADED_MISSILE) > 0) {
            var Agm65Entity = ((Agm65Weapon) getWeapon(0)).create(player);

            Vector4f worldPosition;

            if (this.getEntityData().get(LOADED_MISSILE) == 4) {
                worldPosition = transformPosition(transform, 1.56875f, -0.943f - 0.5f, 0.1272f);
            } else if (this.getEntityData().get(LOADED_MISSILE) == 3) {
                worldPosition = transformPosition(transform, -1.56875f, -0.943f - 0.5f, 0.1272f);
            } else if (this.getEntityData().get(LOADED_MISSILE) == 2) {
                worldPosition = transformPosition(transform, 3.9321875f, -0.88680625f - 0.5f, 0.12965f);
            } else {
                worldPosition = transformPosition(transform, -3.9321875f, -0.88680625f - 0.5f, 0.12965f);
            }

            if (locked) {
                Agm65Entity.setTargetUuid(getTargetUuid());
            }
            Agm65Entity.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            Agm65Entity.shoot(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z, (float) getDeltaMovement().length() + 1, 1);
            player.level().addFreshEntity(Agm65Entity);

            BlockPos pos = BlockPos.containing(new Vec3(worldPosition.x, worldPosition.y, worldPosition.z));

            this.level().playSound(null, pos, ModSounds.BOMB_RELEASE.get(), SoundSource.PLAYERS, 3, 1);

            if (this.getEntityData().get(LOADED_MISSILE) == 3) {
                reloadCoolDownMissile = 400;
            }

            this.entityData.set(LOADED_MISSILE, this.getEntityData().get(LOADED_MISSILE) - 1);
        }
    }

    public float shootingVolume() {
        return entityData.get(FIRE_TIME) * 0.3f;
    }

    public float shootingPitch() {
        return 0.7f + entityData.get(FIRE_TIME) * 0.05f;
    }

    @Override
    public int mainGunRpm(Player player) {
        if (getWeaponIndex(0) == 2) {
            return 600;
        }
        if (getWeaponIndex(0) == 3) {
            return 120;
        }
        return 0;
    }

    @Override
    public boolean canShoot(Player player) {
        if (getWeaponIndex(0) == 2 || getWeaponIndex(0) == 3) {
            return this.entityData.get(AMMO) > 0;
        }
        return false;
    }

    @Override
    public int getAmmoCount(Player player) {
        return this.entityData.get(AMMO);
    }

    @Override
    public boolean banHand(Player player) {
        return true;
    }

    @Override
    public boolean hidePassenger(Entity entity) {
        return false;
    }

    @Override
    public int zoomFov() {
        return 3;
    }

    @Override
    public int getWeaponHeat(Player player) {
        return entityData.get(HEAT);
    }

    @Override
    public float getRotX(float tickDelta) {
        return this.getPitch(tickDelta);
    }

    @Override
    public float getRotY(float tickDelta) {
        return this.getYaw(tickDelta);
    }

    @Override
    public float getRotZ(float tickDelta) {
        return this.getRoll(tickDelta);
    }

    @Override
    public float getPower() {
        return this.entityData.get(POWER);
    }

    @Override
    public int getDecoy() {
        return this.entityData.get(DECOY_COUNT);
    }
}
