package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.component.ModDataComponents;
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
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FIRE_TIME, 0)
                .define(PITCH, -70f)
                .define(YAW, this.getYRot())

                .define(DEPRESSED, false)
                .define(INTELLIGENT, false)
                .define(TARGET_POS, new Vector3f())
                .define(RADIUS, 0);


    }

    @Override
    public boolean canBeCollidedWith() {
        return super.canBeCollidedWith();
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
        if (!(this.items.getFirst().getItem() instanceof MortarShell)) return;
        if (entityData.get(FIRE_TIME) != 0) return;

        this.shooter = shooter;
        this.entityData.set(FIRE_TIME, 25);

        if (!this.level().isClientSide()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MORTAR_LOAD.get(), SoundSource.PLAYERS, 1f, 1f);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MORTAR_FIRE.get(), SoundSource.PLAYERS, 8f, 1f);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MORTAR_DISTANT.get(), SoundSource.PLAYERS, 32f, 1f);
        }
    }

    @Override
    public boolean canRemoteFire() {
        return this.getItem(0).getItem() instanceof MortarShell && this.getEntityData().get(FIRE_TIME) == 0;
    }

    @Override
    public void remoteFire(@Nullable Player player) {
        this.fire(player);
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack mainHandItem = player.getMainHandItem();

        if (mainHandItem.getItem() instanceof ArtilleryIndicator indicator) {
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

        if (mainHandItem.is(ModTags.Items.CROWBAR) && !player.isShiftKeyDown()) {
            if (this.items.getFirst().getItem() instanceof MortarShell && this.entityData.get(FIRE_TIME) == 0) {
                fire(player);
            }
            return InteractionResult.SUCCESS;
        }

        if (mainHandItem.getItem() instanceof MortarShell && !player.isShiftKeyDown() && this.entityData.get(FIRE_TIME) == 0 && this.items.getFirst().isEmpty()) {
            this.items.set(0, mainHandItem.copyWithCount(1));
            if (!player.isCreative()) {
                mainHandItem.shrink(1);
            }
            fire(player);
        }

        if (player.getMainHandItem().getItem() == ModItems.FIRING_PARAMETERS.get()) {
            if (setTarget(player.getMainHandItem())) {
                player.swing(InteractionHand.MAIN_HAND);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.warn", this.getDisplayName()).withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
        }
        if (player.getOffhandItem().getItem() == ModItems.FIRING_PARAMETERS.get()) {
            if (setTarget(player.getOffhandItem())) {
                player.swing(InteractionHand.OFF_HAND);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.warn", this.getDisplayName()).withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
        }

        if (player.isShiftKeyDown()) {
            if (mainHandItem.is(ModTags.Items.CROWBAR)) {
                this.discard();
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.MORTAR_DEPLOYER.get()));
                if (entityData.get(INTELLIGENT)) {
                    ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.MONITOR.get()));
                }
                return InteractionResult.SUCCESS;
            }
            entityData.set(YAW, player.getYRot());
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean setTarget(ItemStack stack) {
        var parameters = stack.get(ModDataComponents.FIRING_PARAMETERS);
        if (parameters == null) return false;

        var pos = parameters.pos();
        double targetX = pos.getX();
        double targetY = pos.getY();
        double targetZ = pos.getZ();
        var isDepressed = parameters.isDepressed();

        entityData.set(TARGET_POS, new Vector3f((float) targetX, (float) targetY, (float) targetZ));
        entityData.set(DEPRESSED, isDepressed);
        entityData.set(RADIUS, parameters.radius());
        Vec3 randomPos = VectorTool.randomPos(new Vec3(entityData.get(TARGET_POS)), entityData.get(RADIUS));

        try {
            Vec3 launchVector = calculateLaunchVector(getEyePosition(), randomPos, 13, -0.11, entityData.get(DEPRESSED));
            this.look(randomPos);

            if (launchVector == null) {
                return false;
            }
            float angle = (float) -getXRotFromVector(launchVector);
            if (angle < -89 || angle > -20) {
                return false;
            }
            entityData.set(PITCH, angle);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void resetTarget() {
        Vec3 randomPos = VectorTool.randomPos(new Vec3(entityData.get(TARGET_POS)), entityData.get(RADIUS));
        Vec3 launchVector = calculateLaunchVector(getEyePosition(), randomPos, 13, -0.11, entityData.get(DEPRESSED));
        this.look(randomPos);

        if (launchVector == null) {
            return;
        }
        float angle = (float) -getXRotFromVector(launchVector);
        if (angle > -89 && angle < -20) {
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

        if (fireTime == 5 && this.items.getFirst().getItem() instanceof MortarShell) {
            Level level = this.level();
            if (level instanceof ServerLevel server) {
                MortarShellEntity entityToSpawn = MortarShell.createShell(shooter, level, this.items.getFirst());
                entityToSpawn.setPos(this.getX(), this.getEyeY(), this.getZ());
                entityToSpawn.shoot(this.getLookAngle().x, this.getLookAngle().y, this.getLookAngle().z, 13f, (float) 0.1);
                level.addFreshEntity(entityToSpawn);
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, (this.getX() + 3 * this.getLookAngle().x), (this.getY() + 0.1 + 3 * this.getLookAngle().y), (this.getZ() + 3 * this.getLookAngle().z), 8, 0.4, 0.4, 0.4,
                        0.007);
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY(), this.getZ(), 50, 2, 0.02, 2, 0.0005);

                this.clearContent();
                resetTarget();
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
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
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
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        if (!entityData.get(INTELLIGENT)) {
            fire(null);
        }
    }

    @Override
    @Nullable
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.MORTAR_DEPLOYER.get());
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return super.canPlaceItem(slot, stack) && this.entityData.get(FIRE_TIME) == 0 && stack.getItem() instanceof MortarShell;
    }
}
