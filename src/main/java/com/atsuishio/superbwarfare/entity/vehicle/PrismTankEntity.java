package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class PrismTankEntity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity, OBBEntity {

    public static final EntityDataAccessor<Float> LASER_LENGTH = SynchedEntityData.defineId(PrismTankEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> LASER_SCALE = SynchedEntityData.defineId(PrismTankEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> LASER_SCALE_O = SynchedEntityData.defineId(PrismTankEntity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obb6;
    public OBB obbTurret;

    public PrismTankEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.PRISM_TANK.get(), world);
    }

    public PrismTankEntity(EntityType<PrismTankEntity> type, Level world) {
        super(type, world);
        this.setMaxUpStep(2.25f);
        this.noCulling = true;

        this.obb = new OBB(this.position().toVector3f(), new Vector3f(2.4f, 0.8125f, 3.71875f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(2.4f, 0.5f, 0.375f), new Quaternionf(), OBB.Part.BODY);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(0.46875f, 0.78125f, 3.3125f), new Quaternionf(), OBB.Part.WHEEL_LEFT);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.46875f, 0.78125f, 3.3125f), new Quaternionf(), OBB.Part.WHEEL_RIGHT);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(1.375f, 0.28125f, 1.375f), new Quaternionf(), OBB.Part.BODY);
        this.obb6 = new OBB(this.position().toVector3f(), new Vector3f(2.0625f, 0.78125f, 0.8125f), new Quaternionf(), OBB.Part.MAIN_ENGINE);
        this.obbTurret = new OBB(this.position().toVector3f(), new Vector3f(0.4375f, 0.90625f, 1.21875f), new Quaternionf(), OBB.Part.TURRET);
    }

    @Override
    public int getContainerSize() {
        return 102;
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(4 + ClientMouseHandler.custom3pDistanceLerp, 1, 1);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LASER_LENGTH, 0f);
        this.entityData.define(LASER_SCALE, 0f);
        this.entityData.define(LASER_SCALE_O, 0f);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.4f) * damage);
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

        this.terrainCompact(4.6375f, 5.171875f);
        inertiaRotate(1);


        lowHealthWarning();
        this.refreshDimensions();
    }

    // 炮弹发射位置
    @Override
    public Vec3 getShootPos(int seatIndex, float ticks) {
        Matrix4f transform = getBarrelTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0, 0.5f, 0);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    // 炮弹发射速度
    @Override
    public float projectileVelocity(Entity entity) {
        return 114514;
    }

    // 炮弹重力
    @Override
    public float projectileGravity(Entity entity) {
        return 0;
    }

    @Override
    public void vehicleShoot(LivingEntity living) {
        Matrix4f transform = getBarrelTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0, 0.5f, 0);
        Vec3 root = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);

        if (getWeaponIndex(0) == 0) {
            if (this.cannotFire) return;

            if (!this.canConsume(VehicleConfig.PRISM_TANK_SHOOT_COST_MODE_1.get()) && living instanceof Player player) {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.annihilator.energy_not_enough").withStyle(ChatFormatting.RED), true);
                return;
            }

            if (level() instanceof ServerLevel) {
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

            if (!this.canConsume(VehicleConfig.PRISM_TANK_SHOOT_COST_MODE_2.get()) && living instanceof Player player) {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.annihilator.energy_not_enough").withStyle(ChatFormatting.RED), true);
                return;
            }

            if (level() instanceof ServerLevel) {
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
            AABB aabb = getBoundingBox().expandTowards(viewVec.scale(512)).inflate(1);
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(this, pos, toVec, aabb,
                    p -> !p.isSpectator() && p.isAlive() && SeekTool.NOT_IN_SMOKE.test(p), distance);

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
                            DamageHandler.doDamage(target, ModDamageTypes.causeLaserDamage(this.level().registryAccess(), this, passenger), getWeaponIndex(0) == 0 ? VehicleConfig.PRISM_TANK_DAMAGE_MODE_1.get() : VehicleConfig.PRISM_TANK_DAMAGE_MODE_2.get());
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
                                NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
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
            List<Entity> entities = new SeekTool.Builder(this)
                    .withinRange(range)
                    .notItsVehicle()
                    .baseFilter()
                    .smokeFilter()
                    .noVehicle()
                    .differentTeam()
                    .build();

            for (var e : entities) {
                double dis = vec.distanceTo(e.getEyePosition());
                for (float i = 0; i < dis; i += 0.2f) {
                    Vec3 toVec = vec.vectorTo(e.getEyePosition()).normalize();
                    Vec3 pos = vec.add(toVec.scale(i));
                    sendParticle(serverLevel, ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0, true);
                }

                sendParticle(serverLevel, ParticleTypes.LAVA, e.getX(), e.getEyeY(), e.getZ(), 4, 0, 0, 0, 0.15, true);
                DamageHandler.doDamage(e, ModDamageTypes.causeLaserDamage(this.level().registryAccess(), this, this.getFirstPassenger()), (float) (aoeDamage - Mth.clamp(dis / range, 0, 0.75) * aoeDamage));

                if (getFirstPassenger() != null && !getFirstPassenger().level().isClientSide() && getFirstPassenger() instanceof ServerPlayer player) {
                    var holder = Holder.direct(ModSounds.INDICATION.get());
                    player.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, player.level().random.nextLong()));
                    NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                }
            }
        }
    }

    public HitResult pickNew(Vec3 pos, double pHitDistance) {
        Vec3 vec31 = this.getBarrelVector(1);
        Vec3 vec32 = pos.add(vec31.x * pHitDistance, vec31.y * pHitDistance, vec31.z * pHitDistance);
        return this.level().clip(new ClipContext(pos, vec32, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    }

    @Override
    public float getEngineSoundVolume() {
        return Math.max(Mth.abs(entityData.get(POWER)), Mth.abs(1.4f * this.entityData.get(DELTA_ROT))) * 0.4f;
    }

    public Vec3 driverPos(float ticks) {
        Matrix4f transform = getBarrelTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0.5f, 1.2f, -0.1f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 getZoomPos(Entity entity, float partialTicks) {
        Matrix4f transform = getBarrelTransform(partialTicks);
        Vector4f worldPosition = transformPosition(transform, 0, 0.95f, 0f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public int getAmmoCount(LivingEntity living) {
        return (int) (this.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::getEnergyStored).orElse(0) * 100f / (float) this.getMaxEnergy());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderFirstPersonOverlay(GuiGraphics guiGraphics, PoseStack poseStack, Font font, Player player, int screenWidth, int screenHeight, float scale, int color) {
        float minWH = (float) Math.min(screenWidth, screenHeight);
        float scaledMinWH = Mth.floor(minWH * scale);
        float centerW = ((screenWidth - scaledMinWH) / 2);
        float centerH = ((screenHeight - scaledMinWH) / 2);

        // 准心
        RenderHelper.blit(poseStack, Mod.loc("textures/screens/land/common_missile.png"), centerW, centerH, 0, 0F, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);

        // 武器名称+过热
        int heat = this.getWeaponHeat(0);
        guiGraphics.drawString(font, Component.literal("LASER   " + (this.getWeaponHeat(0) + 25) + " ℃"), screenWidth / 2 - 33, screenHeight - 65, MathTool.getGradientColor(color, 0xFF0000, heat, 2), false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderThirdPersonOverlay(GuiGraphics guiGraphics, Font font, Player player, int screenWidth, int screenHeight, float scale) {
        super.renderThirdPersonOverlay(guiGraphics, font, player, screenWidth, screenHeight, scale);

        double heat = this.getWeaponHeat(0) / 100F;
        guiGraphics.drawString(font, Component.literal("LASER " + (this.getWeaponHeat(0) + 25) + " ℃"), 30, -9, Mth.hsvToRgb(0F, (float) heat, 1F), false);
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return zoom ? 0.26 : Minecraft.getInstance().options.getCameraType().isFirstPerson() ? 0.33 : 0.45;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            if (this.getSeatIndex(player) == 0) {
                return new Vec2((float) -VehicleVecUtils.getYRotFromVector(this.getBarrelVector(partialTicks)), (float) -VehicleVecUtils.getXRotFromVector(this.getBarrelVector(partialTicks)));
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
                    return new Vec3(this.getZoomPos(player, partialTicks).x, this.getZoomPos(player, partialTicks).y, this.getZoomPos(player, partialTicks).z);
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
    public float getWheelMaxHealth() {
        return 100;
    }

    @Override
    public float getEngineMaxHealth() {
        return 150;
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obb6, this.obbTurret);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, 0, 1.4375f, 0.03125f);
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

        Vector4f worldPosition6 = transformPosition(transform, 0, 1.53125f, -3.125f);
        this.obb6.center().set(new Vector3f(worldPosition6.x, worldPosition6.y, worldPosition6.z));
        this.obb6.setRotation(VectorTool.combineRotations(1, this));

        Matrix4f transformT = getTurretTransform(1);

        Vector4f worldPositionT = transformPosition(transformT, 0, 1.59375f, -0.390625f);
        this.obbTurret.center().set(new Vector3f(worldPositionT.x, worldPositionT.y, worldPositionT.z));
        this.obbTurret.setRotation(VectorTool.combineRotationsTurret(1, this));
    }
}
