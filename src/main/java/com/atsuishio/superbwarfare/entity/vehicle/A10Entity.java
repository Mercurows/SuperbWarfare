package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.*;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.*;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.Pair;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraPitch;
import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraYaw;
import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class A10Entity extends ContainerMobileVehicleEntity implements GeoEntity, WeaponVehicleEntity, AircraftEntity, OBBEntity {

    public static Consumer<MobileVehicleEntity> fireSound = vehicle -> {
    };

    public static final EntityDataAccessor<Integer> LOADED_ROCKET = SynchedEntityData.defineId(A10Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> LOADED_BOMB = SynchedEntityData.defineId(A10Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> LOADED_MISSILE = SynchedEntityData.defineId(A10Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> FIRE_TIME = SynchedEntityData.defineId(A10Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(A10Entity.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public int fireIndex;
    public int reloadCoolDownBomb;
    public int reloadCoolDownMissile;
    public String lockingTargetO = "none";
    public String lockingTarget = "none";
    public float destroyRot;
    public int lockTime;
    public boolean locked;
    private boolean wasFiring = false;
    public float delta_x;
    public float delta_y;
    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obb6;
    public OBB obb7;
    public OBB obb8;
    public OBB obb9;
    public OBB obb10;
    public OBB obb11;

    public A10Entity(EntityType<A10Entity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(0.6875f, 1.09375f, 3.65625f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(8.8125f, 0.3125f, 1.40625f), new Quaternionf(), OBB.Part.BODY);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(3.1875f, 0.125f, 0.96875f), new Quaternionf(), OBB.Part.BODY);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.0625f, 1.09375f, 0.84375f), new Quaternionf(), OBB.Part.BODY);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(0.0625f, 1.09375f, 0.84375f), new Quaternionf(), OBB.Part.BODY);
        this.obb6 = new OBB(this.position().toVector3f(), new Vector3f(0.625f, 0.78125f, 1.09375f), new Quaternionf(), OBB.Part.BODY);
        this.obb7 = new OBB(this.position().toVector3f(), new Vector3f(0.6875f, 0.75f, 2.9375f), new Quaternionf(), OBB.Part.BODY);
        this.obb8 = new OBB(this.position().toVector3f(), new Vector3f(0.75f, 0.75f, 1.5625f), new Quaternionf(), OBB.Part.BODY);
        this.obb9 = new OBB(this.position().toVector3f(), new Vector3f(0.75f, 0.75f, 1.5625f), new Quaternionf(), OBB.Part.BODY);
        this.obb10 = new OBB(this.position().toVector3f(), new Vector3f(0.34375f, 0.359375f, 1.78125f), new Quaternionf(), OBB.Part.BODY);
        this.obb11 = new OBB(this.position().toVector3f(), new Vector3f(0.34375f, 0.359375f, 1.78125f), new Quaternionf(), OBB.Part.BODY);
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
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.25f) * damage * (getHealth() > 0.1f ? 0.4f : 0.05f));
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() == ModItems.ROCKET_70.get() && this.entityData.get(LOADED_ROCKET) < 28) {
            // 装载火箭
            this.entityData.set(LOADED_ROCKET, this.entityData.get(LOADED_ROCKET) + 1);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            this.level().playSound(null, this, ModSounds.MISSILE_RELOAD.get(), this.getSoundSource(), 2, 1);
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        if (stack.getItem() == ModItems.MEDIUM_AERIAL_BOMB.get() && this.entityData.get(LOADED_BOMB) < 3) {
            // 装载航弹
            this.entityData.set(LOADED_BOMB, this.entityData.get(LOADED_BOMB) + 1);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            this.level().playSound(null, this, ModSounds.BOMB_RELOAD.get(), this.getSoundSource(), 2, 1);
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        if (stack.getItem() == ModItems.AGM.get() && this.entityData.get(LOADED_MISSILE) < 4) {
            // 装载导弹
            this.entityData.set(LOADED_MISSILE, this.entityData.get(LOADED_MISSILE) + 1);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            this.level().playSound(null, this, ModSounds.BOMB_RELOAD.get(), this.getSoundSource(), 2, 1);
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.interact(player, hand);
    }

    @Override
    public void baseTick() {
        if (!this.wasFiring && this.isFiring() && this.level().isClientSide()) {
            fireSound.accept(this);
        }
        this.wasFiring = this.isFiring();

        this.lockingTargetO = getTargetUuid();

        super.baseTick();
        this.updateOBB();
        float f = (float) Mth.clamp(Math.max((onGround() ? 0.819f : 0.82f) - 0.0035 * getDeltaMovement().length(), 0.5) + 0.001f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90, 0.01, 0.99);

        boolean forward = getDeltaMovement().dot(getViewVector(1)) > 0;
        this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).scale((forward ? 0.227 : 0.1) * getDeltaMovement().dot(getViewVector(1)))));
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
            terrainCompactA10();
        }

        if (entityData.get(FIRE_TIME) > 0) {
            entityData.set(FIRE_TIME, entityData.get(FIRE_TIME) - 1);
        }

        if (this.getWeaponIndex(0) == 3) {
            seekTarget();
        }

        lowHealthWarning();

        releaseDecoy();
        this.refreshDimensions();
    }

    @Override
    public void lowHealthWarning() {
        Matrix4f transform = this.getVehicleTransform(1);
        if (this.getHealth() <= 0.4 * this.getMaxHealth()) {
            List<Entity> entities = getPlayer(level());
            for (var e : entities) {
                if (e instanceof ServerPlayer player) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        Vector4f position = transformPosition(transform, -1.603125f, 0.875f, -5.0625f);
                        sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, position.x, position.y, position.z, 5, 0.25, 0.25, 0.25, 0, true);
                    }
                }
            }
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.getHealth() <= 0.25 * this.getMaxHealth()) {
                playLowHealthParticle(serverLevel);
            }
            if (this.getHealth() <= 0.15 * this.getMaxHealth()) {
                playLowHealthParticle(serverLevel);
            }
        }

        if (this.getHealth() <= 0.1 * this.getMaxHealth()) {
            List<Entity> entities = getPlayer(level());
            for (var e : entities) {
                if (e instanceof ServerPlayer player) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        Vector4f position = transformPosition(transform, -1.603125f, 0.875f, -5.0625f);
                        sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, position.x, position.y, position.z, 5, 0.25, 0.25, 0.25, 0, true);
                        sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, position.x, position.y, position.z, 5, 0.25, 0.25, 0.25, 0, true);
                        sendParticle(serverLevel, ParticleTypes.FLAME, position.x, position.y, position.z, 5, 0.25, 0.25, 0.25, 0, true);
                        sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), position.x, position.y, position.z, 5, 0.25, 0.25, 0.25, 0, true);
                        Vector4f position2 = transformPosition(transform, 1.603125f, 0.875f, -5.0625f);
                        sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, position2.x, position2.y, position2.z, 5, 0.25, 0.25, 0.25, 0, true);
                        sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, position2.x, position2.y, position2.z, 5, 0.25, 0.25, 0.25, 0, true);
                        sendParticle(serverLevel, ParticleTypes.FLAME, position2.x, position2.y, position2.z, 5, 0.25, 0.25, 0.25, 0, true);
                        sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), position2.x, position2.y, position2.z, 5, 0.25, 0.25, 0.25, 0, true);
                    }
                }
            }
            if (this.level() instanceof ServerLevel serverLevel) {
                sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ(), 2, 0.35 * this.getBbWidth(), 0.15 * this.getBbHeight(), 0.35 * this.getBbWidth(), 0.01, true);
                sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ(), 2, 0.35 * this.getBbWidth(), 0.15 * this.getBbHeight(), 0.35 * this.getBbWidth(), 0.01, true);
                sendParticle(serverLevel, ParticleTypes.FLAME, this.getX(), this.getY() + 0.85f * getBbHeight(), this.getZ(), 4, 0.35 * this.getBbWidth(), 0.12 * this.getBbHeight(), 0.35 * this.getBbWidth(), 0.05, true);
                sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), this.getX(), this.getY() + 0.85f * getBbHeight(), this.getZ(), 4, 0.1 * this.getBbWidth(), 0.05 * this.getBbHeight(), 0.1 * this.getBbWidth(), 0.4, true);
            }
            if (this.tickCount % 15 == 0) {
                this.level().playSound(null, this.getOnPos(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1, 1);
            }
        }

        if (this.getHealth() < 0.1f * this.getMaxHealth() && tickCount % 13 == 0) {
            this.level().playSound(null, this.getOnPos(), ModSounds.NO_HEALTH.get(), SoundSource.PLAYERS, 1, 1);
        } else if (this.getHealth() >= 0.1f && this.getHealth() < 0.4f * this.getMaxHealth() && tickCount % 10 == 0) {
            this.level().playSound(null, this.getOnPos(), ModSounds.LOW_HEALTH.get(), SoundSource.PLAYERS, 1, 1);
        }
    }

    public void terrainCompactA10() {
        if (onGround()) {
            Matrix4f transform = this.getWheelsTransform(1);

            // 前
            Vector4f positionF = transformPosition(transform, 0.141675f, 0, 4.6315125f);
            // 左后
            Vector4f positionLB = transformPosition(transform, 2.5752f, 0, -0.7516125f);
            // 右后
            Vector4f positionRB = transformPosition(transform, -2.5752f, 0, -0.7516125f);

            Vec3 p1 = new Vec3(positionF.x, positionF.y, positionF.z);
            Vec3 p2 = new Vec3(positionLB.x, positionLB.y, positionLB.z);
            Vec3 p3 = new Vec3(positionRB.x, positionRB.y, positionRB.z);

            // 确定点位是否在墙里来调整点位高度
            float p1y = (float) this.traceBlockY(p1, 3);
            float p2y = (float) this.traceBlockY(p2, 3);
            float p3y = (float) this.traceBlockY(p3, 3);

            p1 = new Vec3(positionF.x, p1y, positionF.z);
            p2 = new Vec3(positionLB.x, p2y, positionLB.z);
            p3 = new Vec3(positionRB.x, p3y, positionRB.z);
            Vec3 p4 = p2.add(p3).scale(0.5);

            // 通过点位位置获取角度

            // 左后-右后
            Vec3 v1 = p2.vectorTo(p3);
            // 后-前
            Vec3 v2 = p4.vectorTo(p1);

            double x = getXRotFromVector(v2);
            double z = getXRotFromVector(v1);

            float diffX = Math.clamp(-5f, 5f, Mth.wrapDegrees((float) (-2 * x) - getXRot()));
            setXRot(Mth.clamp(getXRot() + 0.05f * diffX, -45f, 45f));

            float diffZ = Math.clamp(-5f, 5f, Mth.wrapDegrees((float) (-2 * z) - getRoll()));
            setZRot(Mth.clamp(getRoll() + 0.05f * diffZ, -45f, 45f));
        } else if (isInWater()) {
            setXRot(getXRot() * 0.9f);
            setZRot(getRoll() * 0.9f);
        }
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

        Entity entity = SeekTool.seekCustomSizeEntity(this, this.level(), 384, 18, 0.9, true);
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

        if (lockTime > 10) {
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

        if (getHealth() > 0.1f * getMaxHealth()) {
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
            } else if (passenger instanceof Player) {
                if (getEnergy() > 0) {
                    if (forwardInputDown) {
                        this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.004f, sprintInputDown ? 1f : 0.0575f));
                    }

                    if (backInputDown) {
                        this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.002f, -0.2f));
                    }
                }

                if (!onGround()) {
                    if (rightInputDown) {
                        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 1.2f);
                    } else if (this.leftInputDown) {
                        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 1.2f);
                    }
                }

                // 刹车
                if (downInputDown) {
                    if (onGround()) {
                        this.entityData.set(POWER, this.entityData.get(POWER) * 0.8f);
                        this.setDeltaMovement(this.getDeltaMovement().multiply(0.97, 1, 0.97));
                    } else {
                        this.entityData.set(POWER, this.entityData.get(POWER) * 0.97f);
                        this.setDeltaMovement(this.getDeltaMovement().multiply(0.994, 1, 0.994));
                    }
                    this.entityData.set(PLANE_BREAK, Math.min(this.entityData.get(PLANE_BREAK) + 10, 60f));
                }
            }

            if (getEnergy() > 0 && !this.level().isClientSide) {
                this.consumeEnergy((int) (Mth.abs(this.entityData.get(POWER)) * VehicleConfig.A_10_MAX_ENERGY_COST.get()));
            }

            float rotSpeed = 1.5f + 2 * Mth.abs(VectorTool.calculateY(getRoll()));

            float addY = Mth.clamp(Math.max((this.onGround() ? 0.1f : 0.2f) * (float) getDeltaMovement().length(), 0f) * entityData.get(MOUSE_SPEED_X), -rotSpeed, rotSpeed);
            float addX = Mth.clamp(Math.min((float) Math.max(getDeltaMovement().dot(getViewVector(1)) - 0.24, 0.03), 0.4f) * entityData.get(MOUSE_SPEED_Y), -3.5f, 3.5f);
            float addZ = this.entityData.get(DELTA_ROT) - (this.onGround() ? 0 : 0.004f) * entityData.get(MOUSE_SPEED_X) * (float) getDeltaMovement().dot(getViewVector(1));

            float i = getXRot() / 80;

            delta_x = addX;
            delta_y = addY - VectorTool.calculateY(getXRot()) * addZ;

            this.setYRot(this.getYRot() + delta_y);
            if (!onGround()) {
                this.setXRot(this.getXRot() + delta_x);
                this.setZRot(this.getRoll() - addZ * (1 - Mth.abs(i)));
            }

            if (!onGround()) {
                float speed = Mth.clamp(Mth.abs(roll) / 90, 0, 1);
                if (this.roll > 0) {
                    setZRot(roll - Math.min(speed, roll));
                } else if (this.roll < 0) {
                    setZRot(roll + Math.min(speed, -roll));
                }
            }

            this.setPropellerRot(this.getPropellerRot() + 30 * this.entityData.get(POWER));

            // 起落架
            if (upInputDown) {
                upInputDown = false;
                if (entityData.get(GEAR_ROT) == 0 && !onGround()) {
                    entityData.set(GEAR_UP, true);
                } else if (entityData.get(GEAR_ROT) == 85) {
                    entityData.set(GEAR_UP, false);
                }
            }

            if (onGround()) {
                entityData.set(GEAR_UP, false);
            }

            if (entityData.get(GEAR_UP)) {
                entityData.set(GEAR_ROT, Math.min(entityData.get(GEAR_ROT) + 5, 85));
            } else {
                entityData.set(GEAR_ROT, Math.max(entityData.get(GEAR_ROT) - 5, 0));
            }

            float flapX = (1 - (Mth.abs(getRoll())) / 90) * Mth.clamp(entityData.get(MOUSE_SPEED_Y), -22.5f, 22.5f) - VectorTool.calculateY(getRoll()) * Mth.clamp(entityData.get(MOUSE_SPEED_X), -22.5f, 22.5f);

            setFlap1LRot(Mth.clamp(-flapX - 4 * addZ - this.entityData.get(PLANE_BREAK), -22.5f, 22.5f));
            setFlap1RRot(Mth.clamp(-flapX + 4 * addZ - this.entityData.get(PLANE_BREAK), -22.5f, 22.5f));
            setFlap1L2Rot(Mth.clamp(-flapX - 4 * addZ + this.entityData.get(PLANE_BREAK), -22.5f, 22.5f));
            setFlap1R2Rot(Mth.clamp(-flapX + 4 * addZ + this.entityData.get(PLANE_BREAK), -22.5f, 22.5f));

            setFlap2LRot(Mth.clamp(flapX - 4 * addZ, -22.5f, 22.5f));
            setFlap2RRot(Mth.clamp(flapX + 4 * addZ, -22.5f, 22.5f));

            float flapY = (1 - (Mth.abs(getRoll())) / 90) * Mth.clamp(entityData.get(MOUSE_SPEED_X), -22.5f, 22.5f) + VectorTool.calculateY(getRoll()) * Mth.clamp(entityData.get(MOUSE_SPEED_Y), -22.5f, 22.5f);

            setFlap3Rot(flapY * 5);
        } else if (!onGround()) {
            float diffX;
            this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.0003f, 0.02f));
            destroyRot += 0.1f;
            diffX = 90 - this.getXRot();
            this.setXRot(this.getXRot() + diffX * 0.001f * destroyRot);
            this.setZRot(this.getRoll() - destroyRot);
            setDeltaMovement(getDeltaMovement().add(0, -0.03, 0));
            setDeltaMovement(getDeltaMovement().add(0, -destroyRot * 0.005, 0));
        }

        this.entityData.set(POWER, this.entityData.get(POWER) * 0.99f);
        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * 0.85f);
        this.entityData.set(PLANE_BREAK, this.entityData.get(PLANE_BREAK) * 0.8f);

        Matrix4f transform = getVehicleTransform(1);
        double flapAngle = (getFlap1LRot() + getFlap1RRot() + getFlap1L2Rot() + getFlap1R2Rot()) / 4;

        Vector4f force0 = transformPosition(transform, 0, 0, 0);
        Vector4f force1 = transformPosition(transform, 0, 1, 0);

        Vec3 force = new Vec3(force0.x, force0.y, force0.z).vectorTo(new Vec3(force1.x, force1.y, force1.z));

        setDeltaMovement(getDeltaMovement().add(force.scale(getDeltaMovement().dot(getViewVector(1)) * 0.022 * (1 + Math.sin((onGround() ? 25 : flapAngle + 25) * Mth.DEG_TO_RAD)))));

        this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale(0.2 * this.entityData.get(POWER))));
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
            }
        }
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.A_10_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return entityData.get(POWER) * (sprintInputDown ? 5.5f : 3f);
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        // From Immersive_Aircraft
        if (!this.hasPassenger(passenger)) {
            return;
        }

        Matrix4f transform = getVehicleTransform(1);

        float x = 0f;
        float y = 0.1f + (float) passenger.getVehicleAttachmentPoint(this).y;
        float z = 3.95f;

        Vector4f worldPosition = transformPosition(transform, x, y, z);
        passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z);

        copyEntityData(passenger);
    }

    @Override
    public Vec3 driverZoomPos(float ticks) {
        Matrix4f transform = getVehicleTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0, 1.35f, 4.15f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    public void copyEntityData(Entity entity) {
        entity.setYHeadRot(entity.getYHeadRot() + delta_y);
        entity.setYRot(entity.getYRot() + delta_y);
        entity.setYBodyRot(this.getYRot());
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
        super.destroy();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/vehicle_icon/a10_icon.png");
    }

    @Override
    public Vec3 shootPos(float tickDelta) {
        Matrix4f transform = getVehicleTransform(tickDelta);
        Vector4f worldPosition;
        if (getWeaponIndex(0) == 0) {
            worldPosition = transformPosition(transform, 0.1321625f, -0.56446875f, 7.85210625f);
        } else if (getWeaponIndex(0) == 1) {
            worldPosition = transformPosition(transform, 0f, -1.443f, 0.13f);
        } else {
            worldPosition = transformPosition(transform, 0f, -1.203125f, 0.0625f);
        }
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 shootVec(float tickDelta) {
        Matrix4f transform = getVehicleTransform(tickDelta);
        Vector4f worldPosition;
        Vector4f worldPosition2;
        if (getWeaponIndex(0) == 3) {
            worldPosition = transformPosition(transform, 0, 0, 0);
            worldPosition2 = transformPosition(transform, 0, 0f, 1);
        } else {
            worldPosition = transformPosition(transform, 0, 0, 0);
            worldPosition2 = transformPosition(transform, 0, -0.03f, 1);
        }
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z).vectorTo(new Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z)).normalize();
    }

    @Override
    public Float gearRot(float tickDelta) {
        return Mth.lerp(tickDelta, gearRotO, entityData.get(GEAR_ROT));
    }

    @Override
    public void vehicleShoot(Player player, int type) {
        Matrix4f transform = getVehicleTransform(1);

        if (getWeaponIndex(0) == 0) {
            if (this.cannotFire) return;

            boolean hasCreativeAmmo = getFirstPassenger() instanceof Player pPlayer && InventoryTool.hasCreativeAmmoBox(pPlayer);

            Vector4f worldPosition = transformPosition(transform, 0.1321625f, -0.56446875f, 7.85210625f);
            Vector4f worldPosition2 = transformPosition(transform, 0.1321625f + 0.01f, -0.56446875f - 0.03f, 8.85210625f);

            Vec3 shootVec = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z).vectorTo(new Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z)).normalize();


            if (this.entityData.get(AMMO) > 0 || hasCreativeAmmo) {
                entityData.set(FIRE_TIME, Math.min(entityData.get(FIRE_TIME) + 6, 6));

                var entityToSpawn = ((SmallCannonShellWeapon) getWeapon(0)).create(player);

                entityToSpawn.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
                entityToSpawn.shoot(shootVec.x, shootVec.y, shootVec.z, 30, 0.5f);
                level().addFreshEntity(entityToSpawn);

                sendParticle((ServerLevel) this.level(), ParticleTypes.LARGE_SMOKE, worldPosition.x, worldPosition.y, worldPosition.z, 1, 0.2, 0.2, 0.2, 0.001, true);
                sendParticle((ServerLevel) this.level(), ParticleTypes.CLOUD, worldPosition.x, worldPosition.y, worldPosition.z, 2, 0.5, 0.5, 0.5, 0.005, true);

                if (!hasCreativeAmmo) {
                    this.getItemStacks().stream().filter(stack -> stack.is(ModItems.SMALL_SHELL.get())).findFirst().ifPresent(stack -> stack.shrink(1));
                }

            }

            ShakeClientMessage.sendToNearbyPlayers(this, 5, 6, 5, 12);

            this.entityData.set(HEAT, this.entityData.get(HEAT) + 2);
        } else if (getWeaponIndex(0) == 1 && this.getEntityData().get(LOADED_ROCKET) > 0) {
            var heliRocketEntity = ((HeliRocketWeapon) getWeapon(0)).create(player);

            Vector4f worldPosition;
            Vector4f worldPosition2;

            if (fireIndex == 0) {
                worldPosition = transformPosition(transform, -3.9321875f, -1.38680625f, 0.12965f);
                worldPosition2 = transformPosition(transform, -3.9321875f + 0.01f + 0.005f, -1.38680625f - 0.03f, 1.12965f);
                fireIndex = 1;
            } else if (fireIndex == 1) {
                worldPosition = transformPosition(transform, -1.56875f, -1.443f, 0.1272f);
                worldPosition2 = transformPosition(transform, -1.56875f + 0.01f + 0.005f, -1.443f - 0.03f, 1.1272f);
                fireIndex = 2;
            } else if (fireIndex == 2) {
                worldPosition = transformPosition(transform, 1.56875f, -1.443f, 0.1272f);
                worldPosition2 = transformPosition(transform, 1.56875f + 0.01f - 0.002f, -1.443f - 0.03f, 1.1272f);
                fireIndex = 3;
            } else {
                worldPosition = transformPosition(transform, 3.9321875f, -1.38680625f, 0.12965f);
                worldPosition2 = transformPosition(transform, 3.9321875f + 0.01f - 0.002f, -1.38680625f - 0.03f, 1.12965f);
                fireIndex = 0;
            }

            Vec3 shootVec = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z).vectorTo(new Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z)).normalize();

            heliRocketEntity.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            heliRocketEntity.shoot(shootVec.x, shootVec.y, shootVec.z, 8, 0.5f);
            player.level().addFreshEntity(heliRocketEntity);

            BlockPos pos = BlockPos.containing(new Vec3(worldPosition.x, worldPosition.y, worldPosition.z));

            this.level().playSound(null, pos, ModSounds.HELICOPTER_ROCKET_FIRE_3P.get(), SoundSource.PLAYERS, 4, 1);

            this.entityData.set(LOADED_ROCKET, this.getEntityData().get(LOADED_ROCKET) - 1);

            ShakeClientMessage.sendToNearbyPlayers(this, 5, 6, 5, 12);

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
                worldPosition = transformPosition(transform, 5.28f, -1.76f, 1.87f);
            } else if (this.getEntityData().get(LOADED_MISSILE) == 3) {
                worldPosition = transformPosition(transform, -5.28f, -1.76f, 1.87f);
            } else if (this.getEntityData().get(LOADED_MISSILE) == 2) {
                worldPosition = transformPosition(transform, 6.63f, -1.55f, 1.83f);
            } else {
                worldPosition = transformPosition(transform, -6.63f, -1.55f, 1.83f);
            }

            if (locked) {
                Agm65Entity.setTargetUuid(getTargetUuid());
            }
            Agm65Entity.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            Agm65Entity.shoot(shootVec(1).x, shootVec(1).y, shootVec(1).z, (float) getDeltaMovement().length() + 1, 1);
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

    public boolean isFiring() {
        return this.entityData.get(FIRE_TIME) > 0;
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

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return 0;
    }

    @Override
    public double getMouseSensitivity() {
        return ClientEventHandler.zoomVehicle ? 0.03 : 0.07;
    }

    @Override
    public double getMouseSpeedX() {
        return 0.3;
    }

    @Override
    public double getMouseSpeedY() {
        return 0.3;
    }

    @Override
    public boolean isEnclosed(int index) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public Pair<Quaternionf, Quaternionf> getPassengerRotation(Entity entity, float tickDelta) {
        return Pair.of(Axis.XP.rotationDegrees(-this.getViewXRot(tickDelta)), Axis.ZP.rotationDegrees(-this.getRoll(tickDelta)));
    }

    public Matrix4f getClientVehicleTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo + 2.375f, getY() + 2.375f), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees((float) (-Mth.lerp(ticks, yRotO, getYRot()) + freeCameraYaw)));
        transform.rotate(Axis.XP.rotationDegrees((float) (Mth.lerp(ticks, xRotO, getXRot()) + freeCameraPitch)));
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(ticks, prevRoll, getRoll())));
        return transform;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (this.getSeatIndex(player) == 0) {
            return new Vec2((float) (getRotY(partialTicks) - freeCameraYaw), (float) (getRotX(partialTicks) + freeCameraPitch));
        }

        return super.getCameraRotation(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (this.getSeatIndex(player) == 0) {
            Matrix4f transform = getClientVehicleTransform(partialTicks);

            Vector4f maxCameraPosition = transformPosition(transform, 0, 4, -14 - (float) ClientMouseHandler.custom3pDistanceLerp);
            Vec3 finalPos = CameraTool.getMaxZoom(transform, maxCameraPosition);

            if (isFirstPerson) {
                return new Vec3(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
            } else {
                return finalPos;
            }
        }
        return super.getCameraPosition(partialTicks, player, false, false);
    }

    @Override
    public @Nullable ResourceLocation getVehicleItemIcon() {
        return Mod.loc("textures/gui/vehicle/type/aircraft.png");
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obb6, this.obb7, this.obb8, this.obb9, this.obb10, this.obb11);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, 0, 2.65625f - 2.375f, 1.71875f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 0, 0, -0.46875f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition3 = transformPosition(transform, 0, 2.4375f - 2.375f, -6.71875f);
        this.obb3.center().set(new Vector3f(worldPosition3.x, worldPosition3.y, worldPosition3.z));
        this.obb3.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition4 = transformPosition(transform, -3.125f, 3.65625f - 2.375f, -6.71875f);
        this.obb4.center().set(new Vector3f(worldPosition4.x, worldPosition4.y, worldPosition4.z));
        this.obb4.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition5 = transformPosition(transform, 3.125f, 3.65625f - 2.375f, -6.71875f);
        this.obb5.center().set(new Vector3f(worldPosition5.x, worldPosition5.y, worldPosition5.z));
        this.obb5.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition6 = transformPosition(transform, 0f, 2.34375f - 2.375f, 6.46875f);
        this.obb6.center().set(new Vector3f(worldPosition6.x, worldPosition6.y, worldPosition6.z));
        this.obb6.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition7 = transformPosition(transform, 0f, 2.5625f - 2.375f, -4.875f);
        this.obb7.center().set(new Vector3f(worldPosition7.x, worldPosition7.y, worldPosition7.z));
        this.obb7.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition8 = transformPosition(transform, -1.625f, 3.375f - 2.375f, -3.5f);
        this.obb8.center().set(new Vector3f(worldPosition8.x, worldPosition8.y, worldPosition8.z));
        this.obb8.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition9 = transformPosition(transform, 1.625f, 3.375f - 2.375f, -3.5f);
        this.obb9.center().set(new Vector3f(worldPosition9.x, worldPosition9.y, worldPosition9.z));
        this.obb9.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition10 = transformPosition(transform, -2.703125f, 1.921875f - 2.375f, 0.03125f);
        this.obb10.center().set(new Vector3f(worldPosition10.x, worldPosition10.y, worldPosition10.z));
        this.obb10.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition11 = transformPosition(transform, 2.703125f, 1.921875f - 2.375f, 0.03125f);
        this.obb11.center().set(new Vector3f(worldPosition11.x, worldPosition11.y, worldPosition11.z));
        this.obb11.setRotation(VectorTool.combineRotations(1, this));
    }
}
