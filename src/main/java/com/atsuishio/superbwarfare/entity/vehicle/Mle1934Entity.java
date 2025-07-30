package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.RemoteControllableTurret;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.CannonShellWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.ArtilleryIndicator;
import com.atsuishio.superbwarfare.item.common.ammo.CannonShellItem;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import static com.atsuishio.superbwarfare.tools.RangeTool.calculateLaunchVector;

public class Mle1934Entity extends VehicleEntity implements GeoEntity, CannonEntity, RemoteControllableTurret {

    public static final EntityDataAccessor<Integer> COOL_DOWN = SynchedEntityData.defineId(Mle1934Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> RIGHT_BARREL_ANIM = SynchedEntityData.defineId(Mle1934Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(Mle1934Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(Mle1934Entity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Boolean> DEPRESSED = SynchedEntityData.defineId(Mle1934Entity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Vector3f> TARGET_POS = SynchedEntityData.defineId(Mle1934Entity.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Integer> RADIUS = SynchedEntityData.defineId(Mle1934Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> AMMO_COUNT = SynchedEntityData.defineId(Mle1934Entity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Mle1934Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.MLE_1934.get(), world);
    }

    public Mle1934Entity(EntityType<Mle1934Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();

        entityData.set(YAW, getYRot());
        entityData.set(PITCH, getXRot());
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
                                .gravity(projectileGravity())
                                .type(CannonShellEntity.Type.AP)
                                .sound(ModSounds.CANNON_RELOAD.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/ap_shell.png")),
                        new CannonShellWeapon()
                                .hitDamage(VehicleConfig.MLE1934_HE_DAMAGE.get())
                                .explosionDamage(VehicleConfig.MLE1934_HE_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.MLE1934_HE_EXPLOSION_RADIUS.get().floatValue())
                                .durability(1)
                                .fireProbability(0.24F)
                                .fireTime(5)
                                .gravity(projectileGravity())
                                .type(CannonShellEntity.Type.HE)
                                .sound(ModSounds.CANNON_RELOAD.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/he_shell.png")),
                        new CannonShellWeapon()
                                .hitDamage(VehicleConfig.MLE1934_HE_DAMAGE.get())
                                .explosionDamage(VehicleConfig.MLE1934_HE_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.MLE1934_HE_EXPLOSION_RADIUS.get().floatValue())
                                .durability(1)
                                .gravity(projectileGravity())
                                .type(CannonShellEntity.Type.CM)
                                .spreadAmount(50)
                                .sound(ModSounds.CANNON_RELOAD.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/cm_shell.png")),
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(10 + 1.25 * ClientMouseHandler.custom3pDistanceLerp, 1.3, 0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COOL_DOWN, 0);
        this.entityData.define(RIGHT_BARREL_ANIM, 0);
        this.entityData.define(PITCH, 0f);
        this.entityData.define(YAW, 0f);

        this.entityData.define(DEPRESSED, false);
        this.entityData.define(TARGET_POS, new Vector3f());
        this.entityData.define(RADIUS, 0);
        this.entityData.define(AMMO_COUNT, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("CoolDown", this.entityData.get(COOL_DOWN));
        compound.putFloat("Pitch", this.entityData.get(PITCH));
        compound.putFloat("Yaw", this.entityData.get(YAW));

        compound.putBoolean("Depressed", this.entityData.get(DEPRESSED));
        compound.putInt("Radius", this.entityData.get(RADIUS));
        compound.putFloat("TargetX", this.entityData.get(TARGET_POS).x);
        compound.putFloat("TargetY", this.entityData.get(TARGET_POS).y);
        compound.putFloat("TargetZ", this.entityData.get(TARGET_POS).z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(COOL_DOWN, compound.getInt("CoolDown"));
        this.entityData.set(PITCH, compound.getFloat("Pitch"));
        this.entityData.set(YAW, compound.getFloat("Yaw"));

        if (compound.contains("Depressed")) {
            this.entityData.set(DEPRESSED, compound.getBoolean("Depressed"));
        }
        if (compound.contains("Radius")) {
            this.entityData.set(RADIUS, compound.getInt("Radius"));
        }
        if (compound.contains("TargetX") && compound.contains("TargetY") && compound.contains("TargetZ")) {
            this.entityData.set(TARGET_POS, new Vector3f(compound.getFloat("TargetX"), compound.getFloat("TargetX"), compound.getFloat("TargetZ")));
        }
    }

    @Override
    public boolean canRemoteFire() {
        return this.getItem(0).getItem() instanceof CannonShellItem && this.getEntityData().get(COOL_DOWN) == 0;
    }

    @Override
    public void remoteFire(@Nullable Player player) {
        ItemStack stack = this.getItem(0);

        int type = 0;
        if (stack.is(ModItems.HE_5_INCHES.get())) {
            type = 1;
        } else if (stack.is(ModItems.CM_5_INCHES.get())) {
            type = 2;
        }

        this.setWeaponIndex(0, type);
        this.shoot(player, true);
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();

        if (stack.getItem() instanceof ArtilleryIndicator indicator) {
            return indicator.bind(stack, player, this);
        }

        if (stack.is(ModTags.Items.CROWBAR) && !player.isShiftKeyDown()) {
            if (this.items.get(0).getItem() instanceof CannonShellItem) {
                ItemStack item = this.getItem(0);

                int type = 0;
                if (item.is(ModItems.HE_5_INCHES.get())) {
                    type = 1;
                } else if (item.is(ModItems.CM_5_INCHES.get())) {
                    type = 2;
                }
                setWeaponIndex(0, type);
                vehicleShoot(player, 0);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.getItem() instanceof CannonShellItem) {
            if (this.entityData.get(COOL_DOWN) == 0 && (stack.getItem() == this.items.get(0).getItem() || this.items.get(0).isEmpty())) {
                var inStack = this.items.get(0);
                int count = inStack.getCount();

                if (count >= Math.min(this.getMaxStackSize(), inStack.getMaxStackSize())) {
                    return InteractionResult.PASS;
                }

                this.setItem(0, stack.copyWithCount(count + 1));
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.CANNON_RELOAD.get(), 2, 1);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (player.getMainHandItem().getItem() == ModItems.FIRING_PARAMETERS.get()) {
            setTarget(player.getMainHandItem(), player);
        }
        if (player.getOffhandItem().getItem() == ModItems.FIRING_PARAMETERS.get()) {
            setTarget(player.getMainHandItem(), player);
        }
        return super.interact(player, hand);
    }

    //这个炮仰角太低只能用低伸弹道
    @Override
    public void setTarget(ItemStack stack, Entity entity) {
        double targetX = stack.getOrCreateTag().getDouble("TargetX");
        double targetY = stack.getOrCreateTag().getDouble("TargetY");
        double targetZ = stack.getOrCreateTag().getDouble("TargetZ");
        boolean canAim = true;

        entityData.set(TARGET_POS, new Vector3f((float) targetX, (float) targetY, (float) targetZ));
        entityData.set(DEPRESSED, true);
        entityData.set(RADIUS, stack.getOrCreateTag().getInt("Radius"));
        Vec3 randomPos = VectorTool.randomPos(new Vec3(entityData.get(TARGET_POS)), entityData.get(RADIUS));
        Vec3 launchVector = calculateLaunchVector(getEyePosition(), randomPos, shootVelocity(), projectileGravity(), entityData.get(DEPRESSED));

        Component component = Component.literal("");
        Component location = Component.translatable("tips.superbwarfare.mortar.position", this.getDisplayName())
                .append(Component.literal(" X:" + FormatTool.format0D(getX()) + " Y:" + FormatTool.format0D(getY()) + " Z:" + FormatTool.format0D(getZ()) + " "));
        float angle = getXRot();

        if (launchVector == null) {
            canAim = false;
            component = Component.translatable("tips.superbwarfare.mortar.out_of_range");
        } else {
            angle = (float) -getXRotFromVector(launchVector);
            if (angle < -maxPitch() || angle > -minPitch()) {
                canAim = false;
                component = Component.translatable("tips.superbwarfare.mortar.warn", this.getDisplayName());
                if (angle < -maxPitch()) {
                    component = Component.translatable("tips.superbwarfare.ballistics.warn3");
                }
            }
        }

        if (canAim) {
            this.look(randomPos);
            entityData.set(PITCH, angle);
        } else if (entity instanceof Player player) {
            player.displayClientMessage(location.copy().append(component).withStyle(ChatFormatting.RED), false);
        }
    }

    @Override
    public void resetTarget() {
        Vec3 randomPos = VectorTool.randomPos(new Vec3(entityData.get(TARGET_POS)), entityData.get(RADIUS));
        Vec3 launchVector = calculateLaunchVector(getEyePosition(), randomPos, 15, projectileGravity(), entityData.get(DEPRESSED));
        this.look(randomPos);

        if (launchVector == null) {
            return;
        }
        float angle = (float) -getXRotFromVector(launchVector);
        if (angle > -maxPitch() && angle < -minPitch()) {
            entityData.set(PITCH, angle);
        }
    }

    @Override
    public double minPitch() {
        return -2.7;
    }

    @Override
    public double maxPitch() {
        return 30;
    }

    @Override
    public double shootVelocity() {
        return 15;
    }

    @Override
    public float projectileGravity() {
        return 0.1f;
    }

    @Override
    public void look(Vec3 pTarget) {
        Matrix4f transform = getVehicleFlatTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0, 1.4992625f, 1.52065f);
        Vec3 shootPos = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);

        double d0 = pTarget.x - shootPos.x;
        double d2 = pTarget.z - shootPos.z;
        entityData.set(YAW, Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 57.2957763671875) - 90.0F));
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        if (!this.hasPassenger(passenger)) {
            return;
        }

        Matrix4f transform = getVehicleFlatTransform(1);

        float x = 0f;
        float y = 2.0f;
        float z = 0.5f;

        Vector4f worldPosition = transformPosition(transform, x, y, z);
        passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 1f) * damage);
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

        if (this.entityData.get(RIGHT_BARREL_ANIM) > 0) {
            this.entityData.set(RIGHT_BARREL_ANIM, this.entityData.get(RIGHT_BARREL_ANIM) - 1);
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }

        countAmmo();
        lowHealthWarning();
    }

    public void countAmmo () {
        if (level() instanceof ServerLevel) {
            int ammoCount = switch (getWeaponIndex(0)) {
                case 1 -> countItem(ModItems.HE_5_INCHES.get());
                case 2 -> countItem(ModItems.CM_5_INCHES.get());
                default -> countItem(ModItems.AP_5_INCHES.get());
            };

            entityData.set(AMMO_COUNT, ammoCount);
        }
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
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        serverYRot = yaw;
        serverXRot = pitch;
        this.interpolationSteps = 10;
    }

    @Override
    public void destroy() {
        if (level() instanceof ServerLevel) {
            CustomExplosion explosion = new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), getAttacker(), getAttacker()), 160f,
                    this.getX(), this.getY(), this.getZ(), 9f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1);
            explosion.explode();
            ForgeEventFactory.onExplosionStart(this.level(), explosion);
            explosion.finalizeExplosion(false);
            ParticleTool.spawnMediumExplosionParticles(this.level(), this.position());
        }

        explodePassengers();
        super.destroy();
    }

    @Override
    public Vec3 driverZoomPos(float ticks) {
        Matrix4f transform = getVehicleFlatTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0, 1.4992625f + 1.4f, 1.52065f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public void vehicleShoot(Player player, int type) {
        shoot(player, false);
    }

    public void shoot(Player player, boolean reset) {
        if (this.entityData.get(COOL_DOWN) > 0) return;
        if (getFirstPassenger() != null && getFirstPassenger() != player) return;

        if (player.level() instanceof ServerLevel serverLevel) {
            if (getAmmoCount(player) == 0 && !InventoryTool.hasCreativeAmmoBox(getFirstPassenger())) return;
            Matrix4f transform = getVehicleFlatTransform(1);

            // 左炮管

            if (!(getAmmoCount(player) == 0 && !InventoryTool.hasCreativeAmmoBox(getFirstPassenger()))) {
                Vector4f worldPositionL = transformPosition(transform, 0.486775f, 1.4992625f, 1.52065f);
                summonShell(new Vec3(worldPositionL.x, worldPositionL.y, worldPositionL.z), player);
            }

            // 右炮管
            Mod.queueServerWork(3, () -> {
                if (getAmmoCount(player) == 0 && !InventoryTool.hasCreativeAmmoBox(getFirstPassenger())) return;
                Vector4f worldPositionR = transformPosition(transform, -0.486775f, 1.4992625f, 1.52065f);
                summonShell(new Vec3(worldPositionR.x, worldPositionR.y, worldPositionR.z), player);
                this.entityData.set(RIGHT_BARREL_ANIM, 20);
            });


            if (player instanceof ServerPlayer serverPlayer) {
                if (player == getFirstPassenger()) {
                    Mod.queueServerWork(44, () -> SoundTool.playLocalSound(serverPlayer, ModSounds.CANNON_RELOAD.get(), 2, 1));
                }
            }

            this.entityData.set(COOL_DOWN, 74);

            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    this.getX() + 5 * this.getLookAngle().x,
                    this.getY(),
                    this.getZ() + 5 * this.getLookAngle().z,
                    100, 7, 0.02, 7, 0.005);

            ShakeClientMessage.sendToNearbyPlayers(this, 20, 15, 15, 45);

            if (reset) {
                resetTarget();
            }
        }
    }

    public void summonShell(Vec3 pos, Player player) {
        if (player.level() instanceof ServerLevel level) {
            var entityToSpawnLeft = ((CannonShellWeapon) getWeapon(0)).create(player);

            entityToSpawnLeft.setPos(pos.x, pos.y, pos.z);
            entityToSpawnLeft.shoot(this.getLookAngle().x, this.getLookAngle().y, this.getLookAngle().z, 15, 0.05f);
            level.addFreshEntity(entityToSpawnLeft);

            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    this.getX() + 5 * this.getLookAngle().x,
                    this.getY(),
                    this.getZ() + 5 * this.getLookAngle().z,
                    100, 7, 0.02, 7, 0.005);

            double x = pos.x + 9 * this.getLookAngle().x;
            double y = pos.y + 9 * this.getLookAngle().y;
            double z = pos.z + 9 * this.getLookAngle().z;

            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 10, 0.4, 0.4, 0.4, 0.0075);
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 10, 0.4, 0.4, 0.4, 0.0075);

            int count = 6;

            for (float i = 9.5f; i < 16; i += .5f) {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        pos.x + i * this.getLookAngle().x,
                        pos.y + i * this.getLookAngle().y,
                        pos.z + i * this.getLookAngle().z,
                        Mth.clamp(count--, 1, 5), 0.15, 0.15, 0.15, 0.0025);
            }

            if (player instanceof ServerPlayer serverPlayer) {
                if (player == getFirstPassenger()) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.MK_42_FIRE_1P.get(), 2, 1);
                }
            }

            if (!this.level().isClientSide()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MK_42_FIRE_3P.get(), SoundSource.PLAYERS, 24f, 1f);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MK_42_FAR.get(), SoundSource.PLAYERS, 48f, 1f);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MK_42_VERYFAR.get(), SoundSource.PLAYERS, 96f, 1f);
            }

            consumeAmmo(player);
        }
    }

    public void consumeAmmo(Player player) {
        if (player == getFirstPassenger()) {
            if (InventoryTool.hasCreativeAmmoBox(player)) return;

            if (entityData.get(AMMO_COUNT) > 0) {
                this.items.get(0).shrink(1);
            } else {
                Item ammo = switch (getWeaponIndex(0))
                {
                    case 1 -> ModItems.HE_5_INCHES.get();
                    case 2 -> ModItems.CM_5_INCHES.get();
                    default -> ModItems.AP_5_INCHES.get();
                };
                var ammoCount = InventoryTool.countItem(player.getInventory().items, ammo);

                if (ammoCount <= 0) return;
                InventoryTool.consumeItem(player.getInventory().items, ammo, 1);
            }
        } else {
            this.items.get(0).shrink(1);
        }
    }

    @Override
    public void travel() {
        Entity passenger = this.getFirstPassenger();
        if (passenger != null) {
            entityData.set(YAW, passenger.getYHeadRot());
            entityData.set(PITCH, passenger.getXRot() - 2f);
        }

        float diffY = Mth.wrapDegrees(entityData.get(YAW) - this.getYRot());
        float diffX = Mth.wrapDegrees(entityData.get(PITCH) - this.getXRot());

        turretTurnSound(diffX, diffY, 0.95f);

        this.setYRot(this.getYRot() + Mth.clamp(0.5f * diffY, -1.25f, 1.25f));
        this.setXRot(Mth.clamp(this.getXRot() + Mth.clamp(0.5f * diffX, -2f, 2f), -30, 5f));
    }

    protected void clampRotation(Entity entity) {
        float f = Mth.wrapDegrees(entity.getXRot());
        float f1 = Mth.clamp(f, -30.0F, 7.0F);
        entity.xRotO += f1 - f;
        entity.setXRot(entity.getXRot() + f1 - f);
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entity) {
        this.clampRotation(entity);
    }

    private PlayState fireLeftPredicate(AnimationState<Mle1934Entity> event) {
        if (this.entityData.get(COOL_DOWN) > 54) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mle1934.fire_left"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mle1934.idle"));
    }

    private PlayState fireRightPredicate(AnimationState<Mle1934Entity> event) {
        if (this.entityData.get(RIGHT_BARREL_ANIM) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mle1934.fire_right"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mle1934.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "fireLeft", 0, this::fireLeftPredicate));
        data.add(new AnimationController<>(this, "fireRight", 0, this::fireRightPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
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
        int playerAmmo = 0;
        if (player == getFirstPassenger()) {
            Item ammo = switch (getWeaponIndex(0))
            {
                case 1 -> ModItems.HE_5_INCHES.get();
                case 2 -> ModItems.CM_5_INCHES.get();
                default -> ModItems.AP_5_INCHES.get();
            };
            playerAmmo = InventoryTool.countItem(player.getInventory().items, ammo);
        }

        return playerAmmo + entityData.get(AMMO_COUNT);
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
    public int getWeaponHeat(Player player) {
        return 0;
    }

    @Override
    public Vec3 getBarrelVector(float pPartialTicks) {
        if (getFirstPassenger() != null) {
            return getFirstPassenger().getViewVector(pPartialTicks);
        }
        return super.getBarrelVector(pPartialTicks);
    }

    @Override
    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/vehicle_icon/mle1934_icon.png");
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return zoom ? 0.15 : 0.3;
    }

    @Override
    public boolean isEnclosed(int index) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            return new Vec2(Mth.lerp(partialTicks, player.yRotO, player.getYRot()), Mth.lerp(partialTicks, player.xRotO, player.getXRot()));
        }
        return super.getCameraRotation(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            if (zoom) {
                return new Vec3(this.driverZoomPos(partialTicks).x, this.driverZoomPos(partialTicks).y, this.driverZoomPos(partialTicks).z);
            } else {
                return new Vec3(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
            }
        }
        return super.getCameraPosition(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean useFixedCameraPos(Entity entity) {
        return true;
    }

    @Override
    public @Nullable ResourceLocation getVehicleItemIcon() {
        return Mod.loc("textures/gui/vehicle/type/defense.png");
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize() {
        return 2;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return removeItem(0, 2);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return super.canPlaceItem(slot, stack) && this.entityData.get(COOL_DOWN) == 0 && stack.getItem() instanceof CannonShellItem;
    }
}
