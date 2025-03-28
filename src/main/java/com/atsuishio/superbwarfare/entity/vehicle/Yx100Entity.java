package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.projectile.C4Entity;
import com.atsuishio.superbwarfare.entity.projectile.MelonBombEntity;
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketEntity;
import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ContainerMobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.LandArmorEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.CannonShellWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.ProjectileWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class Yx100Entity extends ContainerMobileVehicleEntity implements GeoEntity, LandArmorEntity, WeaponVehicleEntity {
    public static final EntityDataAccessor<Integer> MG_AMMO = SynchedEntityData.defineId(Yx100Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> LOADED_AMMO = SynchedEntityData.defineId(Yx100Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> GUN_FIRE_TIME = SynchedEntityData.defineId(Yx100Entity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Yx100Entity(EntityType<Yx100Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public float maxUpStep() {
        return 2.25F;
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        // AP
                        new CannonShellWeapon()
                                .hitDamage(VehicleConfig.YX_100_AP_CANNON_DAMAGE.get())
                                .explosionRadius(VehicleConfig.YX_100_AP_CANNON_EXPLOSION_RADIUS.get().floatValue())
                                .explosionDamage(VehicleConfig.YX_100_AP_CANNON_EXPLOSION_DAMAGE.get())
                                .fireProbability(0)
                                .fireTime(0)
                                .durability(60)
                                .velocity(40)
                                .sound(ModSounds.INTO_MISSILE.get())
                                .ammo(ModItems.AP_5_INCHES.get())
                                .icon(ModUtils.loc("textures/screens/vehicle_weapon/ap_shell.png")),
                        // HE
                        new CannonShellWeapon()
                                .hitDamage(VehicleConfig.YX_100_HE_CANNON_DAMAGE.get())
                                .explosionRadius(VehicleConfig.YX_100_HE_CANNON_EXPLOSION_RADIUS.get().floatValue())
                                .explosionDamage(VehicleConfig.YX_100_HE_CANNON_EXPLOSION_DAMAGE.get())
                                .fireProbability(0.18F)
                                .fireTime(2)
                                .durability(1)
                                .velocity(25)
                                .sound(ModSounds.INTO_CANNON.get())
                                .ammo(ModItems.HE_5_INCHES.get())
                                .icon(ModUtils.loc("textures/screens/vehicle_weapon/he_shell.png")),
                },
                new VehicleWeapon[]{
                        // 机枪
                        new ProjectileWeapon()
                                .damage(VehicleConfig.HEAVY_MACHINE_GUN_DAMAGE.get())
                                .headShot(2)
                                .zoom(false)
                                .bypassArmorRate(0.4f)
                                .ammo(ModItems.HEAVY_AMMO.get())
                                .icon(ModUtils.loc("textures/screens/vehicle_weapon/gun_12_7mm.png")),
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return switch (index) {
            case 0 -> new ThirdPersonCameraPosition(5, 1.5, -0.8669625);
            case 1 -> new ThirdPersonCameraPosition(-1, 0.5, 0);
            default -> null;
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MG_AMMO, 0)
                .define(LOADED_AMMO, 0)
                .define(GUN_FIRE_TIME, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("LoadedAmmo", this.entityData.get(LOADED_AMMO));
        compound.putInt("WeaponType", getWeaponIndex(0));
        compound.putInt("PassengerWeaponType", getWeaponIndex(1));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(LOADED_AMMO, compound.getInt("LoadedAmmo"));
        setWeaponIndex(0, compound.getInt("WeaponType"));
        setWeaponIndex(1, compound.getInt("PassengerWeaponType"));
    }

    // TODO AEP
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        return super.getAddEntityPacket(entity);
    }

    //    @Override
//    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
//        return NetworkHooks.getEntitySpawningPacket(this);
//    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .immuneTo(DamageTypes.ARROW)
                .immuneTo(DamageTypes.TRIDENT)
                .immuneTo(DamageTypes.MOB_ATTACK)
                .immuneTo(DamageTypes.MOB_ATTACK_NO_AGGRO)
                .immuneTo(DamageTypes.MOB_PROJECTILE)
                .immuneTo(DamageTypes.PLAYER_ATTACK)
                .immuneTo(ModTags.DamageTypes.PROJECTILE)
                .immuneTo(ModDamageTypes.VEHICLE_STRIKE)
                .multiply(0.2f)
                .multiply(2f, DamageTypes.EXPLOSION)
                .multiply(0.75f, ModDamageTypes.CUSTOM_EXPLOSION)
                .multiply(0.75f, ModDamageTypes.PROJECTILE_BOOM)
                .multiply(0.5f, ModDamageTypes.MINE)
                .multiply(0.5f, ModDamageTypes.LUNGE_MINE)
                .multiply(1.5f, ModDamageTypes.CANNON_FIRE)
                .multiply(0.15f, ModTags.DamageTypes.PROJECTILE_ABSOLUTE)
                .custom((source, damage) -> getSourceAngle(source, 1f) * damage)
                .custom((source, damage) -> {
                    if (source.getDirectEntity() instanceof MelonBombEntity) {
                        return 3f * damage;
                    }
                    if (source.getDirectEntity() instanceof SmallCannonShellEntity) {
                        return 0.375f * damage;
                    }
                    if (source.getDirectEntity() instanceof C4Entity) {
                        return 4f * damage;
                    }
                    if (source.getDirectEntity() instanceof RpgRocketEntity) {
                        return 1.5f * damage;
                    }
                    return damage;
                })
                .reduce(9);
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        this.playSound(ModSounds.BMP_STEP.get(), Mth.abs(this.entityData.get(POWER)) * 8, random.nextFloat() * 0.15f + 1f);
    }

    @Override
    public void baseTick() {
        super.baseTick();

        if (getLeftTrack() < 0) {
            setLeftTrack(80);
        }

        if (getLeftTrack() > 80) {
            setLeftTrack(0);
        }

        if (getRightTrack() < 0) {
            setRightTrack(80);
        }

        if (getRightTrack() > 80) {
            setRightTrack(0);
        }

        if (this.entityData.get(GUN_FIRE_TIME) > 0) {
            this.entityData.set(GUN_FIRE_TIME, this.entityData.get(GUN_FIRE_TIME) - 1);
        }

        if (reloadCoolDown == 70 && this.getFirstPassenger() instanceof Player player) {
            SoundTool.playLocalSound(player, ModSounds.YX_100_RELOAD.get());
        }

        if (this.level() instanceof ServerLevel) {
            if (reloadCoolDown > 0 && this.entityData.get(AMMO) > 0) {
                reloadCoolDown--;
            }
            this.handleAmmo();
        }

        if (this.onGround()) {
            float f0 = 0.54f + 0.25f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90;
            this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).normalize().scale(0.05 * this.getDeltaMovement().horizontalDistance())));
            this.setDeltaMovement(this.getDeltaMovement().multiply(f0, 0.85, f0));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.98, 0.95, 0.98));
        }

        if (this.isInWater()) {
            float f1 = (float) (0.7f - (0.04f * Math.min(getSubmergedHeight(this), this.getBbHeight())) + 0.08f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90);
            this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).normalize().scale(0.04 * this.getDeltaMovement().horizontalDistance())));
            this.setDeltaMovement(this.getDeltaMovement().multiply(f1, 0.85, f1));
        }

        if (this.level() instanceof ServerLevel serverLevel && this.isInWater() && this.getDeltaMovement().length() > 0.1) {
            sendParticle(serverLevel, ParticleTypes.CLOUD, this.getX() + 0.5 * this.getDeltaMovement().x, this.getY() + getSubmergedHeight(this) - 0.2, this.getZ() + 0.5 * this.getDeltaMovement().z, (int) (2 + 4 * this.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
            sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, this.getX() + 0.5 * this.getDeltaMovement().x, this.getY() + getSubmergedHeight(this) - 0.2, this.getZ() + 0.5 * this.getDeltaMovement().z, (int) (2 + 10 * this.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
        }

        collideBlock();
        if (this.getDeltaMovement().length() > 0.075) {
            collideHardBlock();
        }

        turretAngle(5, 5);
        gunnerAngle(15, 15);
        lowHealthWarning();
        this.terrainCompat(4.6f, 6.7f);
        inertiaRotate(1.2f);

        this.refreshDimensions();
    }


    private void handleAmmo() {
        boolean hasCreativeAmmo = false;

        if (this.getFirstPassenger() instanceof Player player) {
            hasCreativeAmmo = InventoryTool.hasCreativeAmmoBox(player);
        }

        if (hasCreativeAmmo) {
            this.entityData.set(AMMO, 9999);
            this.entityData.set(MG_AMMO, 9999);
        } else {
            this.entityData.set(AMMO, countItem(getWeapon(0).ammo));
            this.entityData.set(MG_AMMO, countItem(getWeapon(1).ammo));
        }

        if (this.getEntityData().get(LOADED_AMMO) == 0
                && reloadCoolDown <= 0
                && (hasCreativeAmmo || hasItem(getWeapon(0).ammo))
        ) {
            this.entityData.set(LOADED_AMMO, 1);
            if (!hasCreativeAmmo) {
                consumeItem(getWeapon(0).ammo, 1);
            }
        }
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
        boolean hasCreativeAmmo = false;
        for (int i = 0; i < getMaxPassengers() - 1; i++) {
            if (getNthEntity(i) instanceof Player pPlayer && InventoryTool.hasCreativeAmmoBox(pPlayer)) {
                hasCreativeAmmo = true;
            }
        }
        if (reloadCoolDown == 0 && type == 0) {

            if (!this.canConsume(VehicleConfig.YX_100_SHOOT_COST.get())) {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.annihilator.energy_not_enough").withStyle(ChatFormatting.RED), true);
                return;
            }

            Matrix4f transform = getBarrelTransform(1);
            Vector4f worldPosition = transformPosition(transform, 0, 0, 0);

            var cannonShell = (CannonShellWeapon) getWeapon(0);
            var entityToSpawn = cannonShell.create(player);

            entityToSpawn.setPos(worldPosition.x - 1.1 * this.getDeltaMovement().x, worldPosition.y, worldPosition.z - 1.1 * this.getDeltaMovement().z);
            entityToSpawn.shoot(getBarrelVector(1).x, getBarrelVector(1).y + 0.005f, getBarrelVector(1).z, cannonShell.velocity, 0.02f);
            level().addFreshEntity(entityToSpawn);

            if (!player.level().isClientSide) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.playSound(ModSounds.YX_100_FIRE_3P.get(), 8, 1);
                    serverPlayer.playSound(ModSounds.YX_100_FAR.get(), 16, 1);
                    serverPlayer.playSound(ModSounds.YX_100_VERYFAR.get(), 32, 1);
                }
            }

            this.entityData.set(CANNON_RECOIL_TIME, 40);
            this.entityData.set(LOADED_AMMO, 0);
            this.consumeEnergy(10000);
            this.entityData.set(YAW, getTurretYRot());

            reloadCoolDown = 80;

            if (this.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        this.getX() + 5 * getBarrelVector(1).x,
                        this.getY() + 0.1,
                        this.getZ() + 5 * getBarrelVector(1).z,
                        300, 6, 0.02, 6, 0.005);

                double x = worldPosition.x + 9 * getBarrelVector(1).x;
                double y = worldPosition.y + 9 * getBarrelVector(1).y;
                double z = worldPosition.z + 9 * getBarrelVector(1).z;

                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 10, 0.4, 0.4, 0.4, 0.0075);
                server.sendParticles(ParticleTypes.CLOUD, x, y, z, 10, 0.4, 0.4, 0.4, 0.0075);

                int count = 6;

                for (float i = 9.5f; i < 23; i += .5f) {
                    server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            worldPosition.x + i * getBarrelVector(1).x,
                            worldPosition.y + i * getBarrelVector(1).y,
                            worldPosition.z + i * getBarrelVector(1).z,
                            Mth.clamp(count--, 1, 5), 0.15, 0.15, 0.15, 0.0025);
                }

                Vector4f worldPositionL = transformPosition(transform, -0.35f, 0, 0);
                Vector4f worldPositionR = transformPosition(transform, 0.35f, 0, 0);

                for (float i = 3f; i < 6; i += .5f) {
                    server.sendParticles(ParticleTypes.CLOUD,
                            worldPositionL.x + i * getBarrelVector(1).x,
                            worldPositionL.y + i * getBarrelVector(1).y,
                            worldPositionL.z + i * getBarrelVector(1).z,
                            1, 0.025, 0.025, 0.025, 0.0015);

                    server.sendParticles(ParticleTypes.CLOUD,
                            worldPositionR.x + i * getBarrelVector(1).x,
                            worldPositionR.y + i * getBarrelVector(1).y,
                            worldPositionR.z + i * getBarrelVector(1).z,
                            1, 0.025, 0.025, 0.025, 0.0015);
                }
            }


            Level level = player.level();
            final Vec3 center = new Vec3(this.getX(), this.getEyeY(), this.getZ());

            for (Entity target : level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(8), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).toList()) {
                if (target instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new ShakeClientMessage(10, 8, 40, this.getX(), this.getEyeY(), this.getZ()));
                }
            }
        }

        if (type == 1) {
            if (this.cannotFire) return;
            Matrix4f transform = getGunTransform(1);
            Vector4f worldPosition = transformPosition(transform, 0, -0.25f, 0);

            var projectile = (ProjectileWeapon) getWeapon(1);
            var projectileEntity = projectile.create(player);

            projectileEntity.setPos(worldPosition.x - 1.1 * this.getDeltaMovement().x, worldPosition.y, worldPosition.z - 1.1 * this.getDeltaMovement().z);
            projectileEntity.shoot(getGunnerVector(1).x, getGunnerVector(1).y, getGunnerVector(1).z, 20, 0.3f);

            this.level().addFreshEntity(projectileEntity);

            float pitch = this.entityData.get(HEAT) <= 60 ? 1 : (float) (1 - 0.011 * Math.abs(60 - this.entityData.get(HEAT)));

            if (!player.level().isClientSide) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.playSound(ModSounds.M_2_FIRE_3P.get(), 4, pitch);
                    serverPlayer.playSound(ModSounds.M_2_FAR.get(), 12, pitch);
                    serverPlayer.playSound(ModSounds.M_2_VERYFAR.get(), 24, pitch);
                }
            }

            this.entityData.set(GUN_FIRE_TIME, 2);
            this.entityData.set(HEAT, this.entityData.get(HEAT) + 4);

            Level level = player.level();
            final Vec3 center = new Vec3(this.getX(), this.getEyeY(), this.getZ());

            for (Entity target : level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(4), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).toList()) {
                if (target instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new ShakeClientMessage(6, 4, 8, this.getX(), this.getEyeY(), this.getZ()));
                }
            }

            if (hasCreativeAmmo) return;

            ItemStack ammoBox = this.getItemStacks().stream().filter(stack -> {
                if (stack.is(ModItems.AMMO_BOX.get())) {
                    return AmmoType.HEAVY.get(stack) > 0;
                }
                return false;
            }).findFirst().orElse(ItemStack.EMPTY);

            if (!ammoBox.isEmpty()) {
                AmmoType.HEAVY.add(ammoBox, -1);
            } else {
                consumeItem(getWeapon(1).ammo, 1);
            }
        }
    }

    @Override
    public void travel() {
        Entity passenger0 = this.getFirstPassenger();

        if (this.getEnergy() <= 0) return;

        if (!(passenger0 instanceof Player)) {
            this.leftInputDown = false;
            this.rightInputDown = false;
            this.forwardInputDown = false;
            this.backInputDown = false;
            this.entityData.set(POWER, 0f);
        }

        if (forwardInputDown) {
            this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + (this.entityData.get(POWER) < 0 ? 0.004f : 0.0024f), 0.21f));
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
            this.consumeEnergy(VehicleConfig.YX_100_ENERGY_COST.get());
        }

        this.entityData.set(POWER, this.entityData.get(POWER) * (upInputDown ? 0.5f : (rightInputDown || leftInputDown) ? 0.947f : 0.96f));
        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * (float) Math.max(0.76f - 0.1f * this.getDeltaMovement().horizontalDistance(), 0.3));

        float angle = (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1));
        double s0;

        if (Mth.abs(angle) < 90) {
            s0 = this.getDeltaMovement().horizontalDistance();
        } else {
            s0 = -this.getDeltaMovement().horizontalDistance();
        }

        this.setLeftWheelRot((float) ((this.getLeftWheelRot() - 1.25 * s0) + Mth.clamp(0.75f * this.entityData.get(DELTA_ROT), -5f, 5f)));
        this.setRightWheelRot((float) ((this.getRightWheelRot() - 1.25 * s0) - Mth.clamp(0.75f * this.entityData.get(DELTA_ROT), -5f, 5f)));

        setLeftTrack((float) ((getLeftTrack() - 1.5 * Math.PI * s0) + Mth.clamp(0.4f * Math.PI * this.entityData.get(DELTA_ROT), -5f, 5f)));
        setRightTrack((float) ((getRightTrack() - 1.5 * Math.PI * s0) - Mth.clamp(0.4f * Math.PI * this.entityData.get(DELTA_ROT), -5f, 5f)));

        if (this.isInWater() || onGround()) {
            this.setYRot((float) (this.getYRot() - (isInWater() && !onGround() ? 2.5 : 6) * entityData.get(DELTA_ROT)));
            this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale((!isInWater() && !onGround() ? 0.13f : (isInWater() && !onGround() ? 2 : 2.4f)) * this.entityData.get(POWER))));
        }
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.BMP_ENGINE.get();
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        // From Immersive_Aircraft
        if (!this.hasPassenger(passenger)) {
            return;
        }

        Matrix4f transform = getTurretTransform(1);

        int i = this.getOrderedPassengers().indexOf(passenger);

        var worldPosition = switch (i) {
            case 0 -> transformPosition(transform, 0.8669625f, 0.2f, 0.6076875f);
            case 1 -> transformPosition(transform, -0.87890625f, 0.5f, -0.6640625f);
            case 2 -> transformPosition(transform, 1f, 0.15f, -0.6640625f);
            default -> throw new IllegalStateException("Unexpected value: " + i);
        };

        passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z);

        copyEntityData(passenger);
    }

    public void copyEntityData(Entity entity) {
        if (entity == getNthEntity(0)) {
            entity.setYBodyRot(getBarrelYRot(1));
        }
    }

    public Vec3 driverZoomPos(float ticks) {
        Matrix4f transform = getTurretTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0, 0, 0.6076875f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    public int getMaxPassengers() {
        return 3;
    }

    @Override
    public Vec3 getBarrelVector(float pPartialTicks) {
        Matrix4f transform = getBarrelTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }

    public Vec3 getGunnerVector(float pPartialTicks) {
        Matrix4f transform = getGunnerBarrelTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }

    public Matrix4f getBarrelTransform(float ticks) {
        Matrix4f transformT = getTurretTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0f, 0.653275f, 0.750975f);

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

    public Matrix4f getTurretTransform(float ticks) {
        Matrix4f transformV = getVehicleTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0, 2.1484375f, 0);

        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        transformV.rotate(Axis.YP.rotationDegrees(Mth.lerp(ticks, turretYRotO, getTurretYRot())));
        return transformV;
    }

    public Matrix4f getGunTransform(float ticks) {
        Matrix4f transformT = getTurretTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, -0.87890625f, 1.31171875F, -0.6640625f);

        transformT.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        transformT.rotate(Axis.YP.rotationDegrees(Mth.lerp(ticks, gunYRotO, getGunYRot()) - Mth.lerp(ticks, turretYRotO, getTurretYRot())));
        return transformT;
    }

    public Matrix4f getGunnerBarrelTransform(float ticks) {
        Matrix4f transformG = getGunTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0f, 0.4325125f, 0.0632125f);

        transformG.translate(worldPosition.x, worldPosition.y, worldPosition.z);

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

        float x = Mth.lerp(ticks, gunXRotO, getGunXRot());
        float xV = Mth.lerp(ticks, xRotO, getXRot());
        float z = Mth.lerp(ticks, prevRoll, getRoll());

        transformG.rotate(Axis.XP.rotationDegrees(x + r * xV + r2 * z));
        return transformG;
    }


    @Override
    public void destroy() {
        if (level() instanceof ServerLevel) {
            CustomExplosion explosion = new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), getAttacker(), getAttacker()), 80f,
                    this.getX(), this.getY(), this.getZ(), 5f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP).setDamageMultiplier(1);
            explosion.explode();
            EventHooks.onExplosionStart(this.level(), explosion);
            explosion.finalizeExplosion(false);
            ParticleTool.spawnMediumExplosionParticles(this.level(), this.position());
        }

        explodePassengers();
        this.discard();
    }

    protected void clampRotation(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (entity.level().isClientSide && entity == getFirstPassenger()) {
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

            float min = -30f - r * getXRot() - r2 * getRoll();
            float max = 10f - r * getXRot() - r2 * getRoll();

            float f = Mth.wrapDegrees(entity.getXRot());
            float f1 = Mth.clamp(f, min, max);
            entity.xRotO += f1 - f;
            entity.setXRot(entity.getXRot() + f1 - f);

            if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                float f2 = Mth.wrapDegrees(entity.getYRot() - this.getBarrelYRot(1));
                float f3 = Mth.clamp(f2, -20.0F, 20.0F);
                entity.yRotO += f3 - f2;
                entity.setYRot(entity.getYRot() + f3 - f2);
                entity.setYBodyRot(getBarrelYRot(1));
            }
        } else if (entity == getNthEntity(1)) {
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

            float min = -60f - r * getXRot() - r2 * getRoll();
            float max = 10f - r * getXRot() - r2 * getRoll();

            float f = Mth.wrapDegrees(entity.getXRot());
            float f1 = Mth.clamp(f, min, max);
            entity.xRotO += f1 - f;
            entity.setXRot(entity.getXRot() + f1 - f);

            if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                float f2 = Mth.wrapDegrees(entity.getYRot() - this.getGunYRot(1));
                float f3 = Mth.clamp(f2, -150.0F, 150.0F);
                entity.yRotO += f3 - f2;
                entity.setYRot(entity.getYRot() + f3 - f2);
                entity.setYBodyRot(entity.getYRot());
            }
        } else if (entity == getNthEntity(2)) {
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

            float min = -90f - r * getXRot() - r2 * getRoll();
            float max = 22.5f - r * getXRot() - r2 * getRoll();

            float f = Mth.wrapDegrees(entity.getXRot());
            float f1 = Mth.clamp(f, min, max);
            entity.xRotO += f1 - f;
            entity.setXRot(entity.getXRot() + f1 - f);
        }
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entity) {
        this.clampRotation(entity);
    }

    private PlayState cannonShootPredicate(AnimationState<Yx100Entity> event) {
        if (this.entityData.get(CANNON_RECOIL_TIME) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.yx100.fire"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.yx100.idle"));
    }

    private PlayState gunShootPredicate(AnimationState<Yx100Entity> event) {
        if (this.entityData.get(GUN_FIRE_TIME) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.yx100.fire2"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.yx100.idle2"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "cannon", 0, this::cannonShootPredicate));
        data.add(new AnimationController<>(this, "gun", 0, this::gunShootPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public int getMaxEnergy() {
        return VehicleConfig.YX_100_MAX_ENERGY.get();
    }

    @Override
    public float getMaxHealth() {
        return VehicleConfig.YX_100_HP.get();
    }

    @Override
    public int mainGunRpm(Player player) {
        if (player == getNthEntity(0)) {
            return 15;
        }

        if (player == getNthEntity(1)) {
            return 500;
        }
        return 15;
    }

    @Override
    public boolean canShoot(Player player) {
        return switch (getSeatIndex(player)) {
            case 0 -> this.entityData.get(LOADED_AMMO) > 0 && getEnergy() > VehicleConfig.YX_100_SHOOT_COST.get();
            case 1 -> (this.entityData.get(MG_AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(player)) && !cannotFire;
            default -> false;
        };
    }

    @Override
    public int getAmmoCount(Player player) {
        if (player == getNthEntity(0)) {
            return this.entityData.get(LOADED_AMMO);
        }
        if (player == getNthEntity(1)) {
            return this.entityData.get(MG_AMMO);
        }
        return 0;
    }

    @Override
    public boolean banHand(Player player) {
        return hidePassenger(player);
    }

    @Override
    public boolean hidePassenger(Entity entity) {
        return entity == getNthEntity(0) || entity == getNthEntity(1);
    }

    @Override
    public int zoomFov() {
        return 3;
    }

    @Override
    public void changeWeapon(int index, int value, boolean isScroll) {
        if (index != 0) return;

        if (entityData.get(LOADED_AMMO) > 0) {
            if (this.getFirstPassenger() instanceof Player player && !InventoryTool.hasCreativeAmmoBox(player)) {
                this.insertItem(getWeapon(0).ammo, 1);
            }
            entityData.set(LOADED_AMMO, 0);
        }

        this.reloadCoolDown = 80;

        if (this.getFirstPassenger() instanceof ServerPlayer player) {
            var clientboundstopsoundpacket = new ClientboundStopSoundPacket(ModSounds.YX_100_RELOAD.get().getLocation(), SoundSource.PLAYERS);
            player.connection.send(clientboundstopsoundpacket);
        }

        WeaponVehicleEntity.super.changeWeapon(index, value, isScroll);
    }

    public Vec3 getGunVec(float ticks) {
        return getGunnerVector(ticks);
    }

    @Override
    public ResourceLocation getVehicleIcon() {
        return ModUtils.loc("textures/vehicle_icon/yx_100_icon.png");
    }
}
