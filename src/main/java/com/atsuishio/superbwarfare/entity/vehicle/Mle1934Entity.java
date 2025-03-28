package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.CannonShellWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.common.ammo.CannonShellItem;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;

public class Mle1934Entity extends VehicleEntity implements GeoEntity, CannonEntity {

    public static final EntityDataAccessor<Integer> COOL_DOWN = SynchedEntityData.defineId(Mle1934Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(Mle1934Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(Mle1934Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(Mle1934Entity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Mle1934Entity(EntityType<Mle1934Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        new CannonShellWeapon()
                                .hitDamage(VehicleConfig.MLE1934_AP_DAMAGE.get())
                                .explosionDamage(VehicleConfig.MLE1934_AP_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.MLE1934_AP_EXPLOSION_RADIUS.get().floatValue())
                                .durability(70)
                                .sound(ModSounds.CANNON_RELOAD.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/ap_shell.png")),
                        new CannonShellWeapon()
                                .hitDamage(VehicleConfig.MLE1934_HE_DAMAGE.get())
                                .explosionDamage(VehicleConfig.MLE1934_HE_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.MLE1934_HE_EXPLOSION_RADIUS.get().floatValue())
                                .durability(1)
                                .fireProbability(0.24F)
                                .fireTime(5)
                                .sound(ModSounds.CANNON_RELOAD.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/he_shell.png")),
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(10, 1.3, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COOL_DOWN, 0)
                .define(TYPE, 0)
                .define(PITCH, 0f)
                .define(YAW, 0f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("CoolDown", this.entityData.get(COOL_DOWN));
        compound.putInt("Type", this.entityData.get(TYPE));
        compound.putFloat("Pitch", this.entityData.get(PITCH));
        compound.putFloat("Yaw", this.entityData.get(YAW));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(COOL_DOWN, compound.getInt("CoolDown"));
        this.entityData.set(TYPE, compound.getInt("Type"));
        this.entityData.set(PITCH, compound.getFloat("Pitch"));
        this.entityData.set(YAW, compound.getFloat("Yaw"));
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();

        if (player.getMainHandItem().getItem() == ModItems.FIRING_PARAMETERS.get() && player.isCrouching()) {
            setTarget(player.getMainHandItem());
            return InteractionResult.SUCCESS;
        }
        if (player.getOffhandItem().getItem() == ModItems.FIRING_PARAMETERS.get() && player.isCrouching()) {
            setTarget(player.getOffhandItem());
            return InteractionResult.SUCCESS;
        }

        if (stack.getItem() instanceof CannonShellItem) {
            if (this.entityData.get(COOL_DOWN) == 0) {
                var weaponType = stack.is(ModItems.AP_5_INCHES.get()) ? 0 : 1;
                setWeaponIndex(0, weaponType);
                vehicleShoot(player, 0);
            }
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    public void setTarget(ItemStack stack) {
        var tag = NBTTool.getTag(stack);
        int targetX = tag.getInt("TargetX");
        int targetY = tag.getInt("TargetY");
        int targetZ = tag.getInt("TargetZ");
        this.look(new Vec3(targetX, targetY, targetZ));
    }

    private void look(Vec3 pTarget) {
        Vec3 vec3 = this.getEyePosition();
        double d0 = pTarget.x - vec3.x;
        double d1 = pTarget.y - vec3.y;
        double d2 = pTarget.z - vec3.z;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        double distance = pTarget.distanceTo(vec3);
        entityData.set(YAW, Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 57.2957763671875) - 90.0F));
        entityData.set(PITCH, Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * 57.2957763671875))) - (float) (distance * 0.008f));
    }

    @Override
    public double getEyeY() {
        return 2.16F;
    }

    // TODO add entity packet
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        return super.getAddEntityPacket(entity);
    }

    //    @Override
//    public Packet<ClientGamePacketListener> getAddEntityPacket() {
//        return NetworkHooks.getEntitySpawningPacket(this);
//    }

    @Override
    @ParametersAreNonnullByDefault
    protected void positionRider(Entity pPassenger, MoveFunction pCallback) {
        if (this.hasPassenger(pPassenger)) {
            float f1 = (float) ((this.isRemoved() ? 0.009999999776482582 : this.getPassengerRidingPosition(this).y) + pPassenger.getVehicleAttachmentPoint(this).y);
            Vec3 vec3 = (new Vec3(1, 0.0, 0.0)).yRot(-this.getYRot() * 0.017453292F - 1.5707964F);
            pCallback.accept(pPassenger, this.getX() + vec3.x, this.getY() + (double) f1, this.getZ() + vec3.z);
        }
    }

    @Override
    public @NotNull Vec3 getPassengerRidingPosition(@NotNull Entity entity) {
        return super.getPassengerRidingPosition(entity).add(0, -0.075, 0);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .multiply(0.2f)
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
                .multiply(10f, ModDamageTypes.VEHICLE_STRIKE)
                .custom((source, damage) -> getSourceAngle(source, 1f) * damage)
                .reduce(8);
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        return new Vec3(0, Math.min(super.getDeltaMovement().y, 0), 0);
    }

    @Override
    public void baseTick() {
        super.baseTick();

        if (this.entityData.get(COOL_DOWN) > 0) {
            this.entityData.set(COOL_DOWN, this.entityData.get(COOL_DOWN) - 1);
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }

        lowHealthWarning();
    }

    @Override
    public void handleClientSync() {
        if (isControlledByLocalInstance()) {
            interpolationSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (interpolationSteps <= 0) {
            return;
        }

        double interpolatedYaw = Mth.wrapDegrees(serverYRot - (double) getYRot());
        setYRot(getYRot() + (float) interpolatedYaw / (float) interpolationSteps);
        setXRot(getXRot() + (float) (serverXRot - (double) getXRot()) / (float) interpolationSteps);
        setRot(getYRot(), getXRot());

    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        serverYRot = yaw;
        serverXRot = pitch;
        this.interpolationSteps = 10;
    }

    @Override
    public void destroy() {
        if (level() instanceof ServerLevel) {
            CustomExplosion explosion = new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), getAttacker(), getAttacker()), 120f,
                    this.getX(), this.getY(), this.getZ(), 6f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP).setDamageMultiplier(1);
            explosion.explode();
            EventHooks.onExplosionStart(this.level(), explosion);
            explosion.finalizeExplosion(false);
            ParticleTool.spawnMediumExplosionParticles(this.level(), this.position());
        }

        explodePassengers();
        this.discard();
    }

    @Override
    public void vehicleShoot(Player player, int type) {
        if (this.entityData.get(COOL_DOWN) > 0) return;

        Level level = player.level();
        if (level instanceof ServerLevel server) {
            int consumed;
            if (InventoryTool.hasCreativeAmmoBox(player)) {
                consumed = 2;
            } else {
                var ammo = getWeaponIndex(0) == 0 ? ModItems.AP_5_INCHES.get() : ModItems.HE_5_INCHES.get();
                var ammoCount = InventoryTool.countItem(player.getInventory().items, ammo);

                // 尝试消耗两发弹药
                if (ammoCount <= 0) return;
                consumed = InventoryTool.consumeItem(player.getInventory().items, ammo, 2);
            }

            boolean salvoShoot = consumed == 2;

            float yRot = this.getYRot();
            if (yRot < 0) {
                yRot += 360;
            }
            yRot = yRot + 90 % 360;

            var leftPos = new Vector3d(0, 0, -0.45);
            leftPos.rotateZ(-this.getXRot() * Mth.DEG_TO_RAD);
            leftPos.rotateY(-yRot * Mth.DEG_TO_RAD);

            // 左炮管
            var entityToSpawnLeft = ((CannonShellWeapon) getWeapon(0)).create(player);

            entityToSpawnLeft.setPos(this.getX() + leftPos.x,
                    this.getEyeY() - 0.2 + leftPos.y,
                    this.getZ() + leftPos.z);
            entityToSpawnLeft.shoot(this.getLookAngle().x, this.getLookAngle().y, this.getLookAngle().z, 15, 0.05f);
            level.addFreshEntity(entityToSpawnLeft);

            var leftPosP1 = new Vector3d(8, 0, -0.45);
            leftPosP1.rotateZ(-this.getXRot() * Mth.DEG_TO_RAD);
            leftPosP1.rotateY(-yRot * Mth.DEG_TO_RAD);

            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    this.getX() + leftPosP1.x,
                    this.getEyeY() - 0.2 + leftPosP1.y,
                    this.getZ() + leftPosP1.z,
                    10, 0.4, 0.4, 0.4, 0.0075);

            server.sendParticles(ParticleTypes.CLOUD,
                    this.getX() + leftPosP1.x,
                    this.getEyeY() - 0.2 + leftPosP1.y,
                    this.getZ() + leftPosP1.z,
                    10, 0.4, 0.4, 0.4, 0.0075);

            int count = 5;

            for (float i = 9.5f; i < 14; i += .5f) {
                var leftPosP = new Vector3d(i, 0, -0.45);
                leftPosP.rotateZ(-this.getXRot() * Mth.DEG_TO_RAD);
                leftPosP.rotateY(-yRot * Mth.DEG_TO_RAD);

                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        this.getX() + leftPosP.x,
                        this.getEyeY() - 0.2 + leftPosP.y,
                        this.getZ() + leftPosP.z,
                        Mth.clamp(count--, 1, 5), 0.1, 0.1, 0.1, 0.002);
            }

            // 右炮管
            if (salvoShoot) {
                var rightPos = new Vector3d(0, 0, 0.45);
                rightPos.rotateZ(-this.getXRot() * Mth.DEG_TO_RAD);
                rightPos.rotateY(-yRot * Mth.DEG_TO_RAD);

                var entityToSpawnRight = ((CannonShellWeapon) getWeapon(0)).create(player);

                entityToSpawnRight.setPos(this.getX() + rightPos.x,
                        this.getEyeY() - 0.2 + rightPos.y,
                        this.getZ() + rightPos.z);
                entityToSpawnRight.shoot(this.getLookAngle().x, this.getLookAngle().y, this.getLookAngle().z, 15, 0.05f);
                level.addFreshEntity(entityToSpawnRight);

                var rightPosP1 = new Vector3d(8, 0, 0.45);
                rightPosP1.rotateZ(-this.getXRot() * Mth.DEG_TO_RAD);
                rightPosP1.rotateY(-yRot * Mth.DEG_TO_RAD);

                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        this.getX() + rightPosP1.x,
                        this.getEyeY() - 0.2 + rightPosP1.y,
                        this.getZ() + rightPosP1.z,
                        10, 0.4, 0.4, 0.4, 0.0075);

                server.sendParticles(ParticleTypes.CLOUD,
                        this.getX() + rightPosP1.x,
                        this.getEyeY() - 0.2 + rightPosP1.y,
                        this.getZ() + rightPosP1.z,
                        10, 0.4, 0.4, 0.4, 0.0075);

                int countR = 5;

                for (float i = 9.5f; i < 14; i += .5f) {
                    var rightPosP = new Vector3d(i, 0, 0.45);
                    rightPosP.rotateZ(-this.getXRot() * Mth.DEG_TO_RAD);
                    rightPosP.rotateY(-yRot * Mth.DEG_TO_RAD);

                    server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            this.getX() + rightPosP.x,
                            this.getEyeY() - 0.2 + rightPosP.y,
                            this.getZ() + rightPosP.z,
                            Mth.clamp(countR--, 1, 5), 0.1, 0.1, 0.1, 0.002);
                }

                this.entityData.set(TYPE, 1);
            } else {
                this.entityData.set(TYPE, -1);
            }

            if (player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, ModSounds.MK_42_FIRE_1P.get(), 2, 1);
                Mod.queueServerWork(44, () -> SoundTool.playLocalSound(serverPlayer, ModSounds.CANNON_RELOAD.get(), 2, 1));
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.MK_42_FIRE_3P.get(), SoundSource.PLAYERS, 6, 1);
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.MK_42_FAR.get(), SoundSource.PLAYERS, 16, 1);
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.MK_42_VERYFAR.get(), SoundSource.PLAYERS, 32, 1);
            }

            this.entityData.set(COOL_DOWN, 74);

            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    this.getX() + 5 * this.getLookAngle().x,
                    this.getY(),
                    this.getZ() + 5 * this.getLookAngle().z,
                    100, 7, 0.02, 7, 0.005);

            final Vec3 center = new Vec3(this.getX(), this.getEyeY(), this.getZ());

            for (Entity target : level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(20), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).toList()) {
                if (target instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new ShakeClientMessage(15, 15, 45, this.getX(), this.getEyeY(), this.getZ()));
                }
            }
        }
    }

    @Override
    public void travel() {
        Entity passenger = this.getFirstPassenger();
        if (passenger != null) {
            entityData.set(YAW, passenger.getYHeadRot());
            entityData.set(PITCH, passenger.getXRot() - 1.2f);
        }

        float diffY = Mth.wrapDegrees(entityData.get(YAW) - this.getYRot());
        float diffX = Mth.wrapDegrees(entityData.get(PITCH) - this.getXRot());

        turretTurnSound(diffX, diffY, 0.95f);

        this.setYRot(this.getYRot() + Mth.clamp(0.5f * diffY, -1.25f, 1.25f));
        this.setXRot(Mth.clamp(this.getXRot() + Mth.clamp(0.5f * diffX, -2f, 2f), -30, 4f));
    }

    protected void clampRotation(Entity entity) {
        float f = Mth.wrapDegrees(entity.getXRot());
        float f1 = Mth.clamp(f, -30.0F, 4.0F);
        entity.xRotO += f1 - f;
        entity.setXRot(entity.getXRot() + f1 - f);
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entity) {
        this.clampRotation(entity);
    }

    private PlayState movementPredicate(AnimationState<Mle1934Entity> event) {
        if (this.entityData.get(COOL_DOWN) > 64) {
            if (this.entityData.get(TYPE) == 1) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mle1934.salvo_fire"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mle1934.fire"));
            }
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mle1934.idle"));
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
    public float getMaxHealth() {
        return VehicleConfig.MLE1934_HP.get();
    }

    @Override
    public int mainGunRpm(Player player) {
        return 0;
    }

    @Override
    public boolean canShoot(Player player) {
        return true;
    }

    @Override
    public int getAmmoCount(Player player) {
        var ammo = getWeaponIndex(0) == 0 ? ModItems.AP_5_INCHES.get() : ModItems.HE_5_INCHES.get();
        return InventoryTool.countItem(player.getInventory().items, ammo);
    }

    @Override
    public boolean banHand(Player player) {
        return true;
    }

    @Override
    public boolean hidePassenger(Entity entity) {
        return true;
    }

    @Override
    public int zoomFov() {
        return 5;
    }

    @Override
    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/vehicle_icon/mle1934_icon.png");
    }
}
