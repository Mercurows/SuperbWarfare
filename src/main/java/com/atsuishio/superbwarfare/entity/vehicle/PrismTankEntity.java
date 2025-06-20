package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ContainerMobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.LandArmorEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.LaserWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;
import static com.atsuishio.superbwarfare.tools.SeekTool.baseFilter;

public class PrismTankEntity extends ContainerMobileVehicleEntity implements GeoEntity, LandArmorEntity, WeaponVehicleEntity, OBBEntity {

    public static final EntityDataAccessor<Integer> CANNON_FIRE_TIME = SynchedEntityData.defineId(PrismTankEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> LASER_LENGTH = SynchedEntityData.defineId(PrismTankEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> LASER_SCALE = SynchedEntityData.defineId(PrismTankEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> LASER_SCALE_O = SynchedEntityData.defineId(PrismTankEntity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obbTurret;

    public PrismTankEntity(EntityType<PrismTankEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(2.5f, 0.8125f, 3.96875f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(2.5f, 0.5f, 0.375f), new Quaternionf(), OBB.Part.BODY);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(0.46875f, 0.78125f, 3.3125f), new Quaternionf(), OBB.Part.BODY);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.46875f, 0.78125f, 3.3125f), new Quaternionf(), OBB.Part.BODY);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(1.375f, 0.28125f, 1.375f), new Quaternionf(), OBB.Part.BODY);
        this.obbTurret = new OBB(this.position().toVector3f(), new Vector3f(0.4375f, 0.90625f, 1.21875f), new Quaternionf(), OBB.Part.BODY);
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        new LaserWeapon()
                                .sound(ModSounds.INTO_MISSILE.get())
                                .sound1p(ModSounds.PRISM_FIRE_1P.get())
                                .sound3p(ModSounds.PRISM_FIRE_3P.get()),
                        new LaserWeapon()
                                .sound(ModSounds.INTO_CANNON.get())
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(4 + ClientMouseHandler.custom3pDistanceLerp, 1, 1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CANNON_FIRE_TIME, 0)
                .define(LASER_LENGTH, 0f)
                .define(LASER_SCALE, 0f)
                .define(LASER_SCALE_O, 0f);
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
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.4f) * damage);
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        this.playSound(ModSounds.WHEEL_STEP.get(), (float) (getDeltaMovement().length() * 0.15), random.nextFloat() * 0.15f + 1.05f);
    }

    @Override
    public double getSubmergedHeight(Entity entity) {
        return super.getSubmergedHeight(entity);
    }

    @Override
    public void baseTick() {
        this.entityData.set(LASER_SCALE_O, this.entityData.get(LASER_SCALE));
        super.baseTick();
        updateOBB();

        if (getLeftTrack() < 0) {
            setLeftTrack(100);
        }

        if (getLeftTrack() > 100) {
            setLeftTrack(0);
        }

        if (getRightTrack() < 0) {
            setRightTrack(100);
        }

        if (getRightTrack() > 100) {
            setRightTrack(0);
        }

        if (this.entityData.get(LASER_SCALE) > 0) {
            this.entityData.set(LASER_SCALE, Math.max(this.entityData.get(LASER_SCALE) - 0.1f, 0));
            this.entityData.set(LASER_SCALE, this.entityData.get(LASER_SCALE) * 0.9f);
        }

        if (this.entityData.get(LASER_SCALE) == 0) {
            this.entityData.set(LASER_LENGTH, 0f);
        }

        if (this.onGround()) {
            float f0 = 0.54f + 0.25f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90;
            this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).normalize().scale(0.05 * getDeltaMovement().dot(getViewVector(1)))));
            this.setDeltaMovement(this.getDeltaMovement().multiply(f0, 0.99, f0));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.99, 0.99, 0.99));
        }

        if (this.isInWater()) {
            float f1 = (float) (0.7f - (0.04f * Math.min(getSubmergedHeight(this), this.getBbHeight())) + 0.08f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90);
            this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).normalize().scale(0.04 * getDeltaMovement().dot(getViewVector(1)))));
            this.setDeltaMovement(this.getDeltaMovement().multiply(f1, 0.85, f1));
        }

        if (this.level() instanceof ServerLevel serverLevel && this.isInWater() && this.getDeltaMovement().length() > 0.1) {
            sendParticle(serverLevel, ParticleTypes.CLOUD, this.getX() + 0.5 * this.getDeltaMovement().x, this.getY() + getSubmergedHeight(this) - 0.2, this.getZ() + 0.5 * this.getDeltaMovement().z, (int) (2 + 4 * this.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
            sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, this.getX() + 0.5 * this.getDeltaMovement().x, this.getY() + getSubmergedHeight(this) - 0.2, this.getZ() + 0.5 * this.getDeltaMovement().z, (int) (2 + 10 * this.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
        }

        turretAngle(10, 12.5f);
        this.terrainCompact(4.6375f, 5.171875f);
        inertiaRotate(1);

        releaseSmokeDecoy(getTurretVector(1));

        if (this.getFirstPassenger() instanceof Player player && fireInputDown && getWeaponIndex(0) == 1 && getEnergy() > VehicleConfig.PRISM_TANK_SHOOT_COST_MODE_2.get() && !cannotFire) {
            vehicleShoot(player, 0);
        }

        lowHealthWarning();
        this.refreshDimensions();
    }

    @Override
    public void terrainCompact(float w, float l) {
        if (onGround()) {
            float x1 = terrainCompactTrackValue(w, l)[0];
            float x2 = terrainCompactTrackValue(w, l - 1)[0];
            float x3 = terrainCompactTrackValue(w, l - 2)[0];
            float x4 = terrainCompactTrackValue(w, l - 3)[0];
            float x5 = terrainCompactTrackValue(w, l - 4)[0];
            float x6 = terrainCompactTrackValue(w, l - 5)[0];

            List<Float> numbersX = Arrays.asList(x1, x2, x3, x4, x5, x6);
            float maxX = Collections.max(numbersX);
            float minX = Collections.min(numbersX);

            float z1 = terrainCompactTrackValue(w, l)[1];
            float z2 = terrainCompactTrackValue(w, l - 1)[1];
            float z3 = terrainCompactTrackValue(w, l - 2)[1];
            float z4 = terrainCompactTrackValue(w, l - 3)[1];
            float z5 = terrainCompactTrackValue(w, l - 4)[1];
            float z6 = terrainCompactTrackValue(w, l - 5)[1];

            List<Float> numbersZ = Arrays.asList(z1, z2, z3, z4, z5, z6);
            float maxZ = Collections.max(numbersZ);
            float minZ = Collections.min(numbersZ);

            float diffX = Math.clamp(-15f, 15f, (minX + maxX) / 2);
            setXRot(Mth.clamp(getXRot() + 0.15f * diffX, -45f, 45f));

            float diffZ = Math.clamp(-15f, 15f, minZ + maxZ);
            setZRot(Mth.clamp(getRoll() + 0.15f * diffZ, -45f, 45f));
        } else if (isInWater()) {
            setXRot(getXRot() * 0.9f);
            setZRot(getRoll() * 0.9f);
        }
    }

    @Override
    public boolean canCollideHardBlock() {
        return getDeltaMovement().horizontalDistance() > 0.07 || Mth.abs(this.entityData.get(POWER)) > 0.12;
    }

    @Override
    public void move(@NotNull MoverType movementType, @NotNull Vec3 movement) {
        super.move(movementType, movement);
        if (this.isInWater() && horizontalCollision) {
            setDeltaMovement(this.getDeltaMovement().add(0, 0.07, 0));
        }
    }

    @Override
    public void vehicleShoot(Player player, int type) {
        Matrix4f transform = getBarrelTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0, 0.5f, 0);
        Vec3 root = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);

        if (getWeaponIndex(0) == 0) {
            if (this.cannotFire) return;

            if (!this.canConsume(VehicleConfig.PRISM_TANK_SHOOT_COST_MODE_1.get())) {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.annihilator.energy_not_enough").withStyle(ChatFormatting.RED), true);
                return;
            }

            Level level = player.level();
            if (level instanceof ServerLevel) {
                if (!player.level().isClientSide) {
                    playShootSound3p(player, 0, 5, 5, 5);
                }

                this.entityData.set(HEAT, entityData.get(HEAT) + 55);
                this.consumeEnergy(VehicleConfig.PRISM_TANK_SHOOT_COST_MODE_1.get());

                ShakeClientMessage.sendToNearbyPlayers(this, 5, 8, 4, 7);
            }

            float dis = laserLengthEntity(root);

            if (dis < laserLength(root)) {
                this.entityData.set(LASER_LENGTH, dis);
            } else {
                this.entityData.set(LASER_LENGTH, laserLength(root));
                hitBlock(root);
            }

            this.entityData.set(LASER_SCALE, 3f);

        } else if (getWeaponIndex(0) == 1) {
            if (this.cannotFire) return;

            if (!this.canConsume(VehicleConfig.PRISM_TANK_SHOOT_COST_MODE_2.get())) {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.annihilator.energy_not_enough").withStyle(ChatFormatting.RED), true);
                return;
            }

            Level level = player.level();

            float pitch = entityData.get(HEAT) <= 60 ? 1.1f : (float) (1.1f - 0.011 * Math.abs(60 - entityData.get(HEAT)));
            SoundTool.playLocalSound(player, ModSounds.PRISM_FIRE_1P_2.get(), 1f, pitch);

            if (level instanceof ServerLevel) {
                if (!player.level().isClientSide) {
                    playShootSound3p(player, 0, 4, 4, 4);
                }

                this.entityData.set(HEAT, entityData.get(HEAT) + 2);
                this.consumeEnergy(VehicleConfig.PRISM_TANK_SHOOT_COST_MODE_2.get());
            }

            float dis = laserLengthEntity(root);

            if (dis < laserLength(root)) {
                this.entityData.set(LASER_LENGTH, dis);
            } else {
                this.entityData.set(LASER_LENGTH, laserLength(root));
                hitBlock(root);
            }

            this.entityData.set(LASER_SCALE, 1f);
        }
    }

    private void hitBlock(Vec3 pos) {
        if (this.level() instanceof ServerLevel) {
            BlockHitResult result = this.level().clip(new ClipContext(pos, pos.add(this.getBarrelVector(1).scale(512)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

            Vec3 hitPos = result.getLocation();
            if (this.getFirstPassenger() != null && level() instanceof ServerLevel serverLevel) {
                if (getWeaponIndex(0) == 0) {
                    findNearEntity(hitPos);
                    sendParticle(serverLevel, ParticleTypes.END_ROD, hitPos.x, hitPos.y, hitPos.z, 24, 0, 0, 0, 0.2, true);
                    sendParticle(serverLevel, ParticleTypes.LAVA, hitPos.x, hitPos.y, hitPos.z, 8, 0, 0, 0, 0.4, true);
                } else {
                    sendParticle(serverLevel, ParticleTypes.END_ROD, hitPos.x, hitPos.y, hitPos.z, 4, 0, 0, 0, 0.05, true);
                    sendParticle(serverLevel, ParticleTypes.LAVA, hitPos.x, hitPos.y, hitPos.z, 2, 0, 0, 0, 0.15, true);
                }
            }
        }
    }

    private float laserLength(Vec3 pos) {
        return (float) pos.distanceTo((Vec3.atLowerCornerOf(level().clip(
                new ClipContext(pos, pos.add(this.getBarrelVector(1).scale(512)),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getBlockPos())));
    }

    private float laserLengthEntity(Vec3 pos) {
        if (this.level() instanceof ServerLevel) {
            double distance = 512 * 512;
            HitResult hitResult = pickNew(pos, 512);
            if (hitResult.getType() != HitResult.Type.MISS) {
                distance = hitResult.getLocation().distanceToSqr(pos);
                double blockReach = 5;
                if (distance > blockReach * blockReach) {
                    Vec3 posB = hitResult.getLocation();
                    hitResult = BlockHitResult.miss(posB, Direction.getNearest(pos.x, pos.y, pos.z), BlockPos.containing(posB));
                }
            }
            Vec3 viewVec = getBarrelVector(1);
            Vec3 toVec = pos.add(viewVec.x * 512, viewVec.y * 512, viewVec.z * 512);
            AABB aabb = getBoundingBox().expandTowards(viewVec.scale(512)).inflate(1.0D, 1.0D, 1.0D);
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(this, pos, toVec, aabb, p -> !p.isSpectator() && p.isAlive() && SeekTool.smokeFilter(p), distance);
            if (entityhitresult != null) {
                Vec3 targetPos = entityhitresult.getLocation();
                double distanceToTarget = pos.distanceToSqr(targetPos);
                if (distanceToTarget > distance || distanceToTarget > 512 * 512) {
                    hitResult = BlockHitResult.miss(targetPos, Direction.getNearest(viewVec.x, viewVec.y, viewVec.z), BlockPos.containing(targetPos));
                } else if (distanceToTarget < distance) {
                    hitResult = entityhitresult;
                }
                if (hitResult.getType() == HitResult.Type.ENTITY) {
                    Entity passenger = this.getFirstPassenger();
                    Entity target = ((EntityHitResult) hitResult).getEntity();

                    if (passenger != null) {
                        if (level() instanceof ServerLevel serverLevel) {
                            target.hurt(ModDamageTypes.causeLaserDamage(this.level().registryAccess(), this, passenger), getWeaponIndex(0) == 0 ? VehicleConfig.PRISM_TANK_DAMAGE_MODE_1.get() : VehicleConfig.PRISM_TANK_DAMAGE_MODE_2.get());
                            Vec3 vec = pos.scale(pos.distanceTo(target.position()));
                            if (getWeaponIndex(0) == 0) {
                                findNearEntity(target.getEyePosition());
                                sendParticle(serverLevel, ParticleTypes.END_ROD, vec.x, vec.y, vec.z, 24, 0, 0, 0, 0.2, true);
                                sendParticle(serverLevel, ParticleTypes.LAVA, vec.x, vec.y, vec.z, 8, 0, 0, 0, 0.4, true);
                            } else {
                                sendParticle(serverLevel, ParticleTypes.END_ROD, vec.x, vec.y, vec.z, 4, 0, 0, 0, 0.05, true);
                                sendParticle(serverLevel, ParticleTypes.LAVA, vec.x, vec.y, vec.z, 2, 0, 0, 0, 0.15, true);
                            }

                            if (getFirstPassenger() != null && !getFirstPassenger().level().isClientSide() && getFirstPassenger() instanceof ServerPlayer player) {
                                var holder = Holder.direct(ModSounds.INDICATION.get());
                                player.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, player.level().random.nextLong()));
                                PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
                            }
                        }
                    }

                    target.invulnerableTime = 1;
                    return (float) pos.distanceTo(target.position());
                }
            }
        }
        return 512;
    }

    public void findNearEntity(Vec3 vec) {
        int aoeDamage = VehicleConfig.PRISM_TANK_AOE_DAMAGE.get();
        int range = VehicleConfig.PRISM_TANK_AOE_RADIUS.get();
        if (level() instanceof ServerLevel serverLevel) {
            List<Entity> entities = seekNearEntities(vec, level(), range);
            for (var e : entities) {
                double dis = vec.distanceTo(e.getEyePosition());
                for (float i = 0; i < dis; i += 0.2f) {
                    Vec3 toVec = vec.vectorTo(e.getEyePosition()).normalize();
                    Vec3 pos = vec.add(toVec.scale(i));
                    sendParticle(serverLevel, ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0, true);
                }

                sendParticle(serverLevel, ParticleTypes.LAVA, e.getX(), e.getEyeY(), e.getZ(), 4, 0, 0, 0, 0.15, true);
                e.hurt(ModDamageTypes.causeLaserDamage(this.level().registryAccess(), this, this.getFirstPassenger()), (float) (aoeDamage - Mth.clamp(dis / range, 0, 0.75) * aoeDamage));

                if (getFirstPassenger() != null && !getFirstPassenger().level().isClientSide() && getFirstPassenger() instanceof ServerPlayer player) {
                    var holder = Holder.direct(ModSounds.INDICATION.get());
                    player.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, player.level().random.nextLong()));
                    PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
                }
            }
        }
    }

    public List<Entity> seekNearEntities(Vec3 vec3, Level level, double seekRange) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> e.position().distanceTo(vec3) <= seekRange
                        && e != this
                        && e.getVehicle() != this
                        && baseFilter(e)
                        && SeekTool.smokeFilter(e)
                        && e.getVehicle() == null
                        && (!e.isAlliedTo(this) || e.getTeam() == null || e.getTeam().getName().equals("TDM"))).toList();
    }

    public HitResult pickNew(Vec3 pos, double pHitDistance) {
        Vec3 vec31 = this.getBarrelVector(1);
        Vec3 vec32 = pos.add(vec31.x * pHitDistance, vec31.y * pHitDistance, vec31.z * pHitDistance);
        return this.level().clip(new ClipContext(pos, vec32, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    }

    @Override
    public void travel() {
        Entity passenger0 = this.getFirstPassenger();

        if (this.getEnergy() <= 0) return;

        if (passenger0 == null) {
            this.leftInputDown = false;
            this.rightInputDown = false;
            this.forwardInputDown = false;
            this.backInputDown = false;
            this.entityData.set(POWER, 0f);
        }

        if (forwardInputDown) {
            this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + (this.entityData.get(POWER) < 0 ? 0.004f : 0.0024f), 0.24f));
        }

        if (backInputDown) {
            this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - (this.entityData.get(POWER) > 0 ? 0.004f : 0.0024f), -0.16f));
            if (rightInputDown) {
                this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 0.1f);
            } else if (this.leftInputDown) {
                this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 0.1f);
            }
        } else {
            if (rightInputDown) {
                this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 0.1f);
            } else if (this.leftInputDown) {
                this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 0.1f);
            }
        }

        if (this.forwardInputDown || this.backInputDown) {
            this.consumeEnergy(VehicleConfig.PRISM_TANK_ENERGY_COST.get());
        }

        this.entityData.set(POWER, this.entityData.get(POWER) * (upInputDown ? 0.5f : (rightInputDown || leftInputDown) ? 0.947f : 0.96f));
        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * (float) Math.max(0.76f - 0.1f * this.getDeltaMovement().horizontalDistance(), 0.3));

        double s0 = getDeltaMovement().dot(this.getViewVector(1));

        this.setLeftWheelRot((float) ((this.getLeftWheelRot() - 1.25 * s0) + Mth.clamp(0.75f * this.entityData.get(DELTA_ROT), -5f, 5f)));
        this.setRightWheelRot((float) ((this.getRightWheelRot() - 1.25 * s0) - Mth.clamp(0.75f * this.entityData.get(DELTA_ROT), -5f, 5f)));

        setLeftTrack((float) ((getLeftTrack() - 1.9 * Math.PI * s0) + Mth.clamp(0.4f * Math.PI * this.entityData.get(DELTA_ROT), -5f, 5f)));
        setRightTrack((float) ((getRightTrack() - 1.9 * Math.PI * s0) - Mth.clamp(0.4f * Math.PI * this.entityData.get(DELTA_ROT), -5f, 5f)));

        this.setYRot((float) (this.getYRot() - (isInWater() && !onGround() ? 2.5 : 6) * entityData.get(DELTA_ROT)));
        if (this.isInWater() || onGround()) {
            float power = this.entityData.get(POWER) * Mth.clamp(1 + (s0 > 0 ? 1 : -1) * getXRot() / 35, 0, 2);
            this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale((!isInWater() && !onGround() ? 0.13f : (isInWater() && !onGround() ? 2 : 2.4f)) * power)));
        }
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.PRISM_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return Math.max(Mth.abs(entityData.get(POWER)), Mth.abs(0.1f * this.entityData.get(DELTA_ROT))) * 2.5f;
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        // From Immersive_Aircraft
        if (!this.hasPassenger(passenger)) {
            return;
        }

        Matrix4f transform = getTurretTransform(1);
        Matrix4f transformV = getVehicleTransform(1);

        int i = this.getSeatIndex(passenger);

        Vector4f worldPosition;
        if (i == 0) {
            worldPosition = transformPosition(transform, 0, -0.6f, 0);
        } else {
            worldPosition = transformPosition(transformV, -0.59375f, 1f, 3.0625f);
        }
        passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z);

        copyEntityData(passenger);
    }

    public void copyEntityData(Entity entity) {
        if (entity == getNthEntity(0)) {
            entity.setYBodyRot(getBarrelYRot(1));
        }
        if (entity == getNthEntity(1)) {
            entity.setYBodyRot(getYRot());
        }
    }

    public int getMaxPassengers() {
        return 2;
    }

    public Vec3 driverPos(float ticks) {
        Matrix4f transform = getBarrelTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0.5f, 1.2f, -0.1f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 driverZoomPos(float ticks) {
        Matrix4f transform = getBarrelTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0, 0.95f, 0f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 getBarrelVector(float pPartialTicks) {
        Matrix4f transform = getBarrelTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }

    public Matrix4f getBarrelTransform(float ticks) {
        Matrix4f transformT = getTurretTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0, 1.484375f, -0.2375f);

        transformT.translate(worldPosition.x, worldPosition.y, worldPosition.z);

        float a = getTurretYaw(ticks);

        float r = (Mth.abs(a) - 90f) / 90f;

        float r2;

        if (Mth.abs(a) <= 90f) {
            r2 = a / 90f;
        } else {
            if (a < 0) {
                r2 = -(180f + a) / 90f;
            } else {
                r2 = (180f - a) / 90f;
            }
        }

        float x = Mth.lerp(ticks, turretXRotO, getTurretXRot());
        float xV = Mth.lerp(ticks, xRotO, getXRot());
        float z = Mth.lerp(ticks, prevRoll, getRoll());

        transformT.rotate(Axis.XP.rotationDegrees(x + r * xV + r2 * z));
        return transformT;
    }

    public Vec3 getTurretVector(float pPartialTicks) {
        Matrix4f transform = getTurretTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }

    public Matrix4f getTurretTransform(float ticks) {
        Matrix4f transformV = getVehicleTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0, 2.14375f, 0.7375f);

        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        transformV.rotate(Axis.YP.rotationDegrees(Mth.lerp(ticks, turretYRotO, getTurretYRot())));
        return transformV;
    }

    @Override
    public void destroy() {
        if (level() instanceof ServerLevel) {
            CustomExplosion explosion = new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), getAttacker(), getAttacker()), 80f,
                    this.getX(), this.getY(), this.getZ(), 5f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1);
            explosion.explode();
            EventHooks.onExplosionStart(this.level(), explosion);
            explosion.finalizeExplosion(false);
            ParticleTool.spawnMediumExplosionParticles(this.level(), this.position());
        }

        explodePassengers();
        super.destroy();
    }

    protected void clampRotation(Entity entity) {
        if (entity == getNthEntity(0)) {
            float a = getTurretYaw(1);
            float r = (Mth.abs(a) - 90f) / 90f;

            float r2;

            if (Mth.abs(a) <= 90f) {
                r2 = a / 90f;
            } else {
                if (a < 0) {
                    r2 = -(180f + a) / 90f;
                } else {
                    r2 = (180f - a) / 90f;
                }
            }

            float min = -32.5f - r * getXRot() - r2 * getRoll();
            float max = 15f - r * getXRot() - r2 * getRoll();

            float f = Mth.wrapDegrees(entity.getXRot());
            float f1 = Mth.clamp(f, min, max);
            entity.xRotO += f1 - f;
            entity.setXRot(entity.getXRot() + f1 - f);

            entity.setYBodyRot(getBarrelYRot(1));
        }

        if (entity == getNthEntity(1)) {
            float f = Mth.wrapDegrees(entity.getXRot());
            float f1 = Mth.clamp(f, -80.0F, 10F);
            entity.xRotO += f1 - f;
            entity.setXRot(entity.getXRot() + f1 - f);

            float f2 = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
            float f3 = Mth.clamp(f2, -100.0F, 100.0F);
            entity.yRotO += f3 - f2;
            entity.setYRot(entity.getYRot() + f3 - f2);
            entity.setYBodyRot(this.getYRot());
        }
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entity) {
        this.clampRotation(entity);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public int mainGunRpm(Player player) {
        if (getWeaponIndex(0) == 0) {
            return 30;
        } else if (getWeaponIndex(0) == 1) {
            return 0;
        }
        return 30;
    }

    @Override
    public boolean canShoot(Player player) {
        if (getWeaponIndex(0) == 0) {
            return getEnergy() > VehicleConfig.PRISM_TANK_SHOOT_COST_MODE_1.get() && !cannotFire;
        }
        return false;
    }

    @Override
    public int getAmmoCount(Player player) {
        var cap = this.getCapability(Capabilities.EnergyStorage.ENTITY, null);
        if (cap == null) return 0;
        return (int) (cap.getEnergyStored() * 100f / this.getMaxEnergy());
    }

    @Override
    public boolean banHand(Player player) {
        return player == getFirstPassenger();
    }

    @Override
    public boolean hidePassenger(Entity entity) {
        return entity == getFirstPassenger();
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
    public boolean hasTracks() {
        return true;
    }

    @Override
    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/vehicle_icon/prism_tank_icon.png");
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderFirstPersonOverlay(GuiGraphics guiGraphics, Font font, Player player, int screenWidth, int screenHeight, float scale) {
        float minWH = (float) Math.min(screenWidth, screenHeight);
        float scaledMinWH = Mth.floor(minWH * scale);
        float centerW = ((screenWidth - scaledMinWH) / 2);
        float centerH = ((screenHeight - scaledMinWH) / 2);

        // 准心
        preciseBlit(guiGraphics, Mod.loc("textures/screens/land/lav_missile_cross.png"), centerW, centerH, 0, 0.0F, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH);

        // 武器名称+过热
        double heat = 1 - this.getEntityData().get(HEAT) / 100.0F;
        guiGraphics.drawString(font, Component.literal("LASER   " + (this.getEntityData().get(HEAT) + 25) + " ℃"), screenWidth / 2 - 33, screenHeight - 65, Mth.hsvToRgb((float) heat / 3.745318352059925F, 1.0F, 1.0F), false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderThirdPersonOverlay(GuiGraphics guiGraphics, Font font, Player player, int screenWidth, int screenHeight, float scale) {
        super.renderThirdPersonOverlay(guiGraphics, font, player, screenWidth, screenHeight, scale);

        double heat = this.getEntityData().get(HEAT) / 100.0F;
        guiGraphics.drawString(font, Component.literal("LASER " + (this.getEntityData().get(HEAT) + 25) + " ℃"), 30, -9, Mth.hsvToRgb(0F, (float) heat, 1.0F), false);
    }

    @Override
    public boolean hasDecoy() {
        return true;
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return zoom ? 0.26 : Minecraft.getInstance().options.getCameraType().isFirstPerson() ? 0.33 : 0.45;
    }

    @Override
    public boolean isEnclosed(int index) {
        return index == 0;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            if (this.getSeatIndex(player) == 0) {
                return new Vec2((float) -getYRotFromVector(this.getBarrelVec(partialTicks)), (float) -getXRotFromVector(this.getBarrelVec(partialTicks)));
            }
        }
        return super.getCameraRotation(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            if (this.getSeatIndex(player) == 0) {
                if (zoom) {
                    return new Vec3(this.driverZoomPos(partialTicks).x, this.driverZoomPos(partialTicks).y, this.driverZoomPos(partialTicks).z);
                } else {
                    return new Vec3(this.driverPos(partialTicks).x, this.driverPos(partialTicks).y, this.driverPos(partialTicks).z);
                }
            }
        }
        return super.getCameraPosition(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean useFixedCameraPos(Entity entity) {
        return this.getSeatIndex(entity) == 0;
    }

    @Override
    public @Nullable ResourceLocation getVehicleItemIcon() {
        return Mod.loc("textures/gui/vehicle/type/land.png");
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obbTurret);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, 0, 1.4375f, -0.21875f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 0, 1.4375f, 4.125f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition3 = transformPosition(transform, 2.09375f, 0.84375f, 0f);
        this.obb3.center().set(new Vector3f(worldPosition3.x, worldPosition3.y, worldPosition3.z));
        this.obb3.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition4 = transformPosition(transform, -2.09375f, 0.84375f, 0f);
        this.obb4.center().set(new Vector3f(worldPosition4.x, worldPosition4.y, worldPosition4.z));
        this.obb4.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition5 = transformPosition(transform, 0, 2.53125f, 0.765625f);
        this.obb5.center().set(new Vector3f(worldPosition5.x, worldPosition5.y, worldPosition5.z));
        this.obb5.setRotation(VectorTool.combineRotations(1, this));

        Matrix4f transformT = getTurretTransform(1);

        Vector4f worldPositionT = transformPosition(transformT, 0, 1.59375f, -0.390625f);
        this.obbTurret.center().set(new Vector3f(worldPositionT.x, worldPositionT.y, worldPositionT.z));
        this.obbTurret.setRotation(VectorTool.combineRotationsTurret(1, this));
    }
}
