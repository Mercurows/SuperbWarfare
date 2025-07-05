package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.common.ammo.MortarShell;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import static com.atsuishio.superbwarfare.tools.RangeTool.calculateLaunchVector;

public class MortarEntity extends VehicleEntity implements GeoEntity, Container {

    public static final EntityDataAccessor<Integer> FIRE_TIME = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

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
    }

    private LivingEntity shooter = null;

    private void fire(@Nullable LivingEntity shooter) {
        if (!(this.stack.getItem() instanceof MortarShell)) return;

        this.shooter = shooter;
        this.entityData.set(FIRE_TIME, 25);

        if (!this.level().isClientSide()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MORTAR_LOAD.get(), SoundSource.PLAYERS, 1f, 1f);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MORTAR_FIRE.get(), SoundSource.PLAYERS, 8f, 1f);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MORTAR_DISTANT.get(), SoundSource.PLAYERS, 32f, 1f);
        }
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack mainHandItem = player.getMainHandItem();

        if (mainHandItem.getItem() instanceof MortarShell && !player.isShiftKeyDown() && this.entityData.get(FIRE_TIME) == 0) {
            this.stack = mainHandItem.copyWithCount(1);
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
                player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.warn").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
        }
        if (player.getOffhandItem().getItem() == ModItems.FIRING_PARAMETERS.get()) {
            if (setTarget(player.getOffhandItem())) {
                player.swing(InteractionHand.OFF_HAND);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.warn").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
        }

        if (player.isShiftKeyDown()) {
            if (mainHandItem.getItem() == ModItems.CROWBAR.get()) {
                this.discard();
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.MORTAR_DEPLOYER.get()));
                return InteractionResult.SUCCESS;
            }
            entityData.set(YAW, player.getYRot());
        }

        return InteractionResult.SUCCESS;
    }

    public boolean setTarget(ItemStack stack) {
        double targetX = stack.getOrCreateTag().getDouble("TargetX");
        double targetY = stack.getOrCreateTag().getDouble("TargetY");
        double targetZ = stack.getOrCreateTag().getDouble("TargetZ");
        boolean isDepressed = stack.getOrCreateTag().getBoolean("IsDepressed");

        try {
            Vec3 launchVector = calculateLaunchVector(getEyePosition(), new Vec3(targetX, targetY, targetZ), 11.4, -0.146, isDepressed);
            this.look(new Vec3(targetX, targetY, targetZ));
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

    private void look(Vec3 pTarget) {
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

        if (fireTime == 5 && this.stack.getItem() instanceof MortarShell) {
            Level level = this.level();
            if (level instanceof ServerLevel server) {
                MortarShellEntity entityToSpawn = MortarShell.createShell(shooter, level, this.stack);
                entityToSpawn.setPos(this.getX(), this.getEyeY(), this.getZ());
                entityToSpawn.shoot(this.getLookAngle().x, this.getLookAngle().y, this.getLookAngle().z, 11.4f, (float) 0.5);
                level.addFreshEntity(entityToSpawn);
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, (this.getX() + 3 * this.getLookAngle().x), (this.getY() + 0.1 + 3 * this.getLookAngle().y), (this.getZ() + 3 * this.getLookAngle().z), 8, 0.4, 0.4, 0.4,
                        0.007);
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY(), this.getZ(), 50, 2, 0.02, 2, 0.0005);

                this.stack = ItemStack.EMPTY;
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
        }
        super.destroy();
    }

    public String getSyncedAnimation() {
        return null;
    }

    public void setAnimation(String animation) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private ItemStack stack = ItemStack.EMPTY;

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return stack == ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return slot == 0 ? stack : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0 || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            stack = ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return removeItem(0, 1);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot != 0) return;
        this.stack = stack;
    }

    @Override
    public void setChanged() {
        fire(null);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        this.stack = ItemStack.EMPTY;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot != 0 || this.entityData.get(FIRE_TIME) != 0) return false;
        return stack.getItem() instanceof MortarShell;
    }

    private LazyOptional<?> itemHandler = LazyOptional.of(() -> new InvWrapper(this));

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing) {
        if (this.isAlive() && capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        return this.getCapability(cap, null);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandler = LazyOptional.of(() -> new InvWrapper(this));
    }
}
