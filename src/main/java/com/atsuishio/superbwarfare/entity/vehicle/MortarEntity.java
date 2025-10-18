package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.RemoteControllableTurret;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.ArtilleryIndicator;
import com.atsuishio.superbwarfare.item.Monitor;
import com.atsuishio.superbwarfare.item.common.ammo.MortarShell;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

import static com.atsuishio.superbwarfare.tools.RangeTool.calculateLaunchVector;

public class MortarEntity extends VehicleEntity implements GeoEntity, RemoteControllableTurret {

    public static final EntityDataAccessor<Integer> FIRE_TIME = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> INTELLIGENT = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> DEPRESSED = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Vector3f> TARGET_POS = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Integer> RADIUS = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private LivingEntity shooter = null;

    public MortarEntity(PlayMessages.SpawnEntity packet, Level level) {
        this(ModEntities.MORTAR.get(), level);
    }

    public MortarEntity(EntityType<MortarEntity> type, Level level) {
        super(type, level);
    }

    public MortarEntity(Level level, float yRot) {
        super(ModEntities.MORTAR.get(), level);
        this.setYRot(yRot);
        this.entityData.set(YAW, yRot);
    }

    @Override
    public boolean shouldSendHitParticles() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FIRE_TIME, 0);
        this.entityData.define(PITCH, -70f);
        this.entityData.define(YAW, this.getYRot());

        this.entityData.define(DEPRESSED, false);
        this.entityData.define(INTELLIGENT, false);
        this.entityData.define(TARGET_POS, new Vector3f());
        this.entityData.define(RADIUS, 0);
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected float getEyeHeight(@NotNull Pose pPose, @NotNull EntityDimensions pSize) {
        return 0.2F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Pitch", this.entityData.get(PITCH));
        compound.putFloat("Yaw", this.entityData.get(YAW));
        compound.putBoolean("Intelligent", this.entityData.get(INTELLIGENT));

        compound.putBoolean("Depressed", this.entityData.get(DEPRESSED));
        compound.putInt("Radius", this.entityData.get(RADIUS));
        compound.putFloat("TargetX", this.entityData.get(TARGET_POS).x);
        compound.putFloat("TargetY", this.entityData.get(TARGET_POS).y);
        compound.putFloat("TargetZ", this.entityData.get(TARGET_POS).z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Pitch")) {
            this.entityData.set(PITCH, compound.getFloat("Pitch"));
        }
        if (compound.contains("Yaw")) {
            this.entityData.set(YAW, compound.getFloat("Yaw"));
        }
        if (compound.contains("Intelligent")) {
            this.entityData.set(INTELLIGENT, compound.getBoolean("Intelligent"));
        }

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

    public void fire(@Nullable LivingEntity shooter) {
        if (!(this.items.get(0).getItem() instanceof MortarShell)) return;
        if (entityData.get(FIRE_TIME) != 0) return;

        this.shooter = shooter;
        this.entityData.set(FIRE_TIME, 25);

        if (!this.level().isClientSide()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MORTAR_LOAD.get(), SoundSource.PLAYERS, 1f, 1f);
        }

        if (level() instanceof ServerLevel serverLevel) {
            SoundTool.playDistantSound(serverLevel, ModSounds.MORTAR_FIRE.get(), position(), 8, random.nextFloat() * 0.1f + 1, null);
            SoundTool.playDistantSound(serverLevel, ModSounds.MORTAR_DISTANT.get(), position(), 32, random.nextFloat() * 0.1f + 1, null);
        }
    }

    @Override
    public boolean canRemoteFire() {
//        return this.getItem(0).getItem() instanceof MortarShell && this.getEntityData().get(FIRE_TIME) == 0;
        return false;
    }

    @Override
    public void remoteFire(@Nullable Player player) {
        this.fire(player);
    }

    @Override
    public double minPitch() {
        return 20;
    }

    @Override
    public double maxPitch() {
        return 89;
    }

    @Override
    public double shootVelocity() {
        return 10;
    }

    @Override
    public float projectileGravity() {
        return 0.13f;
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        var result = super.interact(player, hand);
        if (result != InteractionResult.PASS) return result;

        ItemStack mainHandItem = player.getMainHandItem();

        if (mainHandItem.getItem() instanceof ArtilleryIndicator indicator && this.entityData.get(INTELLIGENT)) {
            return indicator.bind(mainHandItem, player, this);
        }

        if (mainHandItem.getItem() instanceof Monitor && player.isShiftKeyDown() && !this.entityData.get(INTELLIGENT)) {
            entityData.set(INTELLIGENT, true);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5F, 1);
            }
            if (!player.isCreative()) {
                mainHandItem.shrink(1);
            }
        }

        if (mainHandItem.is(ModTags.Items.TOOLS_CROWBAR)) {
            if (this.items.get(0).getItem() instanceof MortarShell && this.entityData.get(FIRE_TIME) == 0 && level() instanceof ServerLevel) {
                fire(player);
            }
            return InteractionResult.SUCCESS;
        }

        if (mainHandItem.getItem() instanceof MortarShell && !player.isShiftKeyDown() && this.entityData.get(FIRE_TIME) == 0 && this.items.get(0).isEmpty()) {
            this.items.set(0, mainHandItem.copyWithCount(1));
            if (!player.isCreative()) {
                mainHandItem.shrink(1);
            }
            fire(player);
        }

        if (player.getMainHandItem().getItem() == ModItems.FIRING_PARAMETERS.get()) {
            setTarget(player.getMainHandItem(), player);
        }
        if (player.getOffhandItem().getItem() == ModItems.FIRING_PARAMETERS.get()) {
            setTarget(player.getOffhandItem(), player);
        }

        if (player.isShiftKeyDown()) {
            entityData.set(YAW, player.getYRot());
        }

        return InteractionResult.FAIL;
    }

    @Override
    public @NotNull List<ItemStack> getRetrieveItems() {
        var list = new ArrayList<ItemStack>();

        list.add(new ItemStack(ModItems.MORTAR_DEPLOYER.get()));
        if (entityData.get(INTELLIGENT)) {
            list.add(new ItemStack(ModItems.MONITOR.get()));
        }

        return list;
    }

    @Override
    public void setTarget(ItemStack stack, Entity entity) {
        double targetX = stack.getOrCreateTag().getDouble("TargetX");
        double targetY = stack.getOrCreateTag().getDouble("TargetY") - 1;
        double targetZ = stack.getOrCreateTag().getDouble("TargetZ");
        boolean canAim = true;

        entityData.set(TARGET_POS, new Vector3f((float) targetX, (float) targetY, (float) targetZ));
        entityData.set(DEPRESSED, stack.getOrCreateTag().getBoolean("IsDepressed"));
        entityData.set(RADIUS, stack.getOrCreateTag().getInt("Radius"));
        Vec3 randomPos = VectorTool.randomPos(new Vec3(entityData.get(TARGET_POS)), entityData.get(RADIUS));
        Vec3 launchVector = calculateLaunchVector(getEyePosition(), randomPos, shootVelocity(), projectileGravity(), entityData.get(DEPRESSED));
        Vec3 launchVector2 = calculateLaunchVector(getEyePosition(), randomPos, shootVelocity(), projectileGravity(), !entityData.get(DEPRESSED));

        Component component = Component.literal("");
        Component location = Component.translatable("tips.superbwarfare.mortar.position", this.getDisplayName())
                .append(Component.literal(" X:" + FormatTool.format0D(getX()) + " Y:" + FormatTool.format0D(getY()) + " Z:" + FormatTool.format0D(getZ()) + " "));
        float angle = getXRot();

        if (launchVector == null || launchVector2 == null) {
            canAim = false;
            component = Component.translatable("tips.superbwarfare.mortar.out_of_range");
        } else {
            angle = (float) -getXRotFromVector(launchVector);
            float angle2 = (float) -getXRotFromVector(launchVector2);
            if (angle < -maxPitch() || angle > -minPitch()) {
                if (angle2 > -maxPitch() && angle2 < -minPitch()) {
                    component = Component.translatable("tips.superbwarfare.ballistics.warn2");
                    canAim = false;
                } else {
                    component = Component.translatable("tips.superbwarfare.mortar.warn", this.getDisplayName());
                    if (entity instanceof Player player) {
                        player.displayClientMessage(location.copy().append(component).withStyle(ChatFormatting.RED), false);
                    }
                    return;
                }
            }

            if (angle < -maxPitch()) {
                component = Component.translatable("tips.superbwarfare.ballistics.warn");
                canAim = false;
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
        Vec3 launchVector = calculateLaunchVector(getEyePosition(), randomPos, shootVelocity(), projectileGravity(), entityData.get(DEPRESSED));
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
    public void look(Vec3 pTarget) {
        Vec3 vec3 = EntityAnchorArgument.Anchor.EYES.apply(this);
        double d0 = (pTarget.x - vec3.x) * 0.2;
        double d2 = (pTarget.z - vec3.z) * 0.2;
        entityData.set(YAW, Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 57.2957763671875) - 90.0F));
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        return new Vec3(0, Math.min(super.getDeltaMovement().y, 0), 0);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        int fireTime = this.entityData.get(FIRE_TIME);
        if (fireTime > 0) {
            this.entityData.set(FIRE_TIME, fireTime - 1);
        }

        if (fireTime == 5 && this.items.get(0).getItem() instanceof MortarShell) {
            Level level = this.level();
            if (level instanceof ServerLevel server) {
                MortarShellEntity entityToSpawn = MortarShell.createShell(shooter, level, this.items.get(0));
                entityToSpawn.setPos(this.getX(), this.getEyeY(), this.getZ());
                entityToSpawn.shoot(this.getLookAngle().x, this.getLookAngle().y, this.getLookAngle().z, (float) shootVelocity(), (float) 0.1);
                level.addFreshEntity(entityToSpawn);

                ParticleTool.spawnMediumCannonMuzzleParticles(getLookAngle(), new Vec3(this.getX(), this.getEyeY(), this.getZ()).add(getLookAngle().scale(1.5)), server, this);

//                this.clearContent();

                if (this.entityData.get(INTELLIGENT)) {
                    this.resetTarget();
                }
                ShakeClientMessage.sendToNearbyPlayers(this, 6, 6, 8, 14);
            }
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
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
    public void travel() {
        float diffY = Mth.wrapDegrees(entityData.get(YAW) - this.getYRot());
        float diffX = Mth.wrapDegrees(entityData.get(PITCH) - this.getXRot());

        this.setYRot(this.getYRot() + Mth.clamp(0.5f * diffY, -20f, 20f));
        this.setXRot(Mth.clamp(this.getXRot() + Mth.clamp(0.5f * diffX, -20f, 20f), -89, -20));
    }

    private PlayState movementPredicate(AnimationState<MortarEntity> event) {
        if (this.entityData.get(FIRE_TIME) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mortar.fire"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mortar.idle"));
    }

    @Override
    public void destroy() {
        if (this.level() instanceof ServerLevel level) {
            var x = this.getX();
            var y = this.getY();
            var z = this.getZ();
            level.explode(null, x, y, z, 0, Level.ExplosionInteraction.NONE);
            ItemEntity mortar = new ItemEntity(level, x, (y + 1), z, new ItemStack(ModItems.MORTAR_DEPLOYER.get()));
            mortar.setPickUpDelay(10);
            level.addFreshEntity(mortar);
            if (entityData.get(INTELLIGENT)) {
                ItemEntity monitor = new ItemEntity(level, x, (y + 1), z, new ItemStack(ModItems.MONITOR.get()));
                monitor.setPickUpDelay(10);
                level.addFreshEntity(monitor);
            }
        }
        super.destroy();
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
    public int getMaxStackSize() {
        return 1;
    }

//    @Override
//    public void setChanged() {
//        if (!entityData.get(INTELLIGENT)) {
//            fire(null);
//        }
//    }

    @Override
    @Nullable
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.MORTAR_DEPLOYER.get());
    }

//    @Override
//    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
//        return super.canPlaceItem(slot, stack) && this.entityData.get(FIRE_TIME) == 0 && stack.getItem() instanceof MortarShell;
//    }

    @Override
    public boolean hasEnergyStorage() {
        return false;
    }
}
