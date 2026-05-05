package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.misc.ArtilleryIndicatorItem;
import com.atsuishio.superbwarfare.item.misc.FiringParametersItemKt;
import com.atsuishio.superbwarfare.item.misc.MonitorItem;
import com.atsuishio.superbwarfare.item.projectile.MortarShellItem;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import com.atsuishio.superbwarfare.tools.VectorToolKt;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.*;

import java.util.ArrayList;
import java.util.List;

import static com.atsuishio.superbwarfare.tools.TrajectoryCalculator.calculateLaunchVector;

public class MortarEntity extends ArtilleryEntity {
    public static final EntityDataAccessor<Integer> FIRE_TIME = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> TARGET_PITCH = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TARGET_YAW = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> INTELLIGENT = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> NEED_RESET_TARGET = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.BOOLEAN);

    private LivingEntity shooter = null;

    public MortarEntity(EntityType<MortarEntity> type, Level level) {
        super(type, level);
    }

    public MortarEntity(Level level, float yRot) {
        super(ModEntities.MORTAR.get(), level);
        this.setYRot(yRot);
        this.entityData.set(TARGET_YAW, yRot);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(INTELLIGENT, false)
                .define(TARGET_PITCH, -70f)
                .define(TARGET_YAW, this.getYRot())
                .define(FIRE_TIME, 0)
                .define(NEED_RESET_TARGET, true);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("TargetPitch", this.entityData.get(TARGET_PITCH));
        compound.putFloat("TargetYaw", this.entityData.get(TARGET_YAW));
        compound.putBoolean("Intelligent", this.entityData.get(INTELLIGENT));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("TargetPitch")) {
            this.entityData.set(TARGET_PITCH, compound.getFloat("TargetPitch"));
        }
        if (compound.contains("TargetYaw")) {
            this.entityData.set(TARGET_YAW, compound.getFloat("TargetYaw"));
        }
        if (compound.contains("Intelligent")) {
            this.entityData.set(INTELLIGENT, compound.getBoolean("Intelligent"));
        }
    }

    @Override
    public void vehicleShoot(LivingEntity living, @NotNull String weaponName) {
        if (!(this.getItems().getFirst().getItem() instanceof MortarShellItem)) return;
        var gunData = getGunData(weaponName);
        if (gunData == null) return;
        if (entityData.get(FIRE_TIME) != 0) return;
        var soundInfo = gunData.get(GunProp.SOUND_INFO);

        this.shooter = living;
        this.entityData.set(FIRE_TIME, 25);

        if (!this.level().isClientSide()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), soundInfo.vehicleReload, SoundSource.PLAYERS, 1f, 1f);
        }

        if (level() instanceof ServerLevel serverLevel) {
            SoundTool.playDistantSound(serverLevel, soundInfo.fire3P, position(), (float) (0.25f * gunData.get(GunProp.SOUND_RADIUS)), random.nextFloat() * 0.1f + 1, null);
            SoundTool.playDistantSound(serverLevel, soundInfo.fire3PFar, position(), gunData.get(GunProp.SOUND_RADIUS).floatValue(), random.nextFloat() * 0.1f + 1, null);
        }
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        var result = super.interact(player, hand);
        if (result != InteractionResult.PASS) return result;

        ItemStack mainHandItem = player.getMainHandItem();

        if (mainHandItem.getItem() instanceof ArtilleryIndicatorItem indicator && this.entityData.get(INTELLIGENT)) {
            return indicator.bind(mainHandItem, player, this);
        }

        if (mainHandItem.getItem() instanceof MonitorItem && !this.entityData.get(INTELLIGENT)) {
            entityData.set(INTELLIGENT, true);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5F, 1);
            }
            if (!player.isCreative()) {
                mainHandItem.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        if (mainHandItem.is(ModTags.Items.TOOLS_CROWBAR)) {
            if (this.getItems().getFirst().getItem() instanceof MortarShellItem && this.entityData.get(FIRE_TIME) == 0 && level() instanceof ServerLevel) {
                vehicleShoot(player, "Main");
            }
            return InteractionResult.SUCCESS;
        }

        if (mainHandItem.getItem() instanceof MortarShellItem && !player.isShiftKeyDown() && this.entityData.get(FIRE_TIME) == 0 && this.getItems().getFirst().isEmpty()) {
            this.getItems().set(0, mainHandItem.copyWithCount(1));
            if (!player.isCreative()) {
                mainHandItem.shrink(1);
            }
            vehicleShoot(player, "Main");
            entityData.set(NEED_RESET_TARGET, false);
            return InteractionResult.SUCCESS;
        }

        if (player.getMainHandItem().getItem() == ModItems.FIRING_PARAMETERS.get()) {
            setTarget(player.getMainHandItem(), player, "Main");
        }
        if (player.getOffhandItem().getItem() == ModItems.FIRING_PARAMETERS.get()) {
            setTarget(player.getOffhandItem(), player, "Main");
        }

        if (player.isShiftKeyDown()) {
            entityData.set(TARGET_YAW, player.getYRot());
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

        if (getItems().getFirst() != ItemStack.EMPTY) {
            list.add(getItems().getFirst());
        }

        return list;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (entityData.get(FIRE_TIME) > 0) {
            entityData.set(FIRE_TIME, entityData.get(FIRE_TIME) - 1);
        }

        if (entityData.get(FIRE_TIME) == 5 && this.getItems().getFirst().getItem() instanceof MortarShellItem) {
            Level level = this.level();
            var gunData = getGunData("Main");
            if (level instanceof ServerLevel server && gunData != null) {
                MortarShellEntity entityToSpawn = MortarShellItem.createShell(shooter, level, this.getItems().getFirst(), getProjectileGravity("Main"), gunData.get(GunProp.DAMAGE).floatValue(), gunData.get(GunProp.EXPLOSION_DAMAGE).floatValue(), gunData.get(GunProp.EXPLOSION_RADIUS).floatValue());
                entityToSpawn.setPos(this.getX(), this.getEyeY(), this.getZ());
                entityToSpawn.shoot(this.getLookAngle().x, this.getLookAngle().y, this.getLookAngle().z, getProjectileVelocity("Main"), getProjectileSpread("Main"));
                entityToSpawn.setLife(gunData.get(GunProp.PROJECTILE_LIFE));
                level.addFreshEntity(entityToSpawn);

                ParticleTool.spawnMediumCannonMuzzleParticles(getLookAngle(), new Vec3(this.getX(), this.getEyeY(), this.getZ()).add(getLookAngle().scale(1.5)), server, this);

                this.clearContent();

                if (this.entityData.get(INTELLIGENT) && entityData.get(NEED_RESET_TARGET)) {
                    this.resetTarget("Main");
                }

                entityData.set(NEED_RESET_TARGET, true);

                gunData.shakePlayers(this);
            }
        }
    }

    @Override
    public void setTarget(@NotNull ItemStack stack, Entity entity, @NotNull String weaponName) {
        var parameters = FiringParametersItemKt.getFiringParameters(stack);
        boolean canAim = true;

        setTargetPos(parameters.pos());
        setDepressed(!parameters.isDepressed());
        setRadius(parameters.radius());
        Vec3 randomPos = VectorToolKt.randomPos(getTargetPos().getCenter(), getRadius()).add(0, -1, 0);
        Vec3 flatTrajectory = calculateLaunchVector(getEyePosition(), randomPos, getProjectileVelocity(weaponName), getProjectileGravity(weaponName), getDepressed());
        Vec3 highTrajectory = calculateLaunchVector(getEyePosition(), randomPos, getProjectileVelocity(weaponName), getProjectileGravity(weaponName), !getDepressed());

        Component component = Component.literal("");
        Component location = Component.translatable("tips.superbwarfare.mortar.position", this.getDisplayName())
                .append(Component.literal(" X:" + FormatTool.format0D(getX()) + " Y:" + FormatTool.format0D(getY()) + " Z:" + FormatTool.format0D(getZ()) + " "));
        float angle = getXRot();

        if (flatTrajectory == null || highTrajectory == null) {
            canAim = false;
            component = Component.translatable("tips.superbwarfare.mortar.out_of_range");
        } else {
            angle = (float) -VehicleVecUtils.getXRotFromVector(flatTrajectory);
            float angle2 = (float) -VehicleVecUtils.getXRotFromVector(highTrajectory);
            if (angle < -getTurretMaxPitch() || angle > -getTurretMinPitch()) {
                if (angle2 > -getTurretMaxPitch() && angle2 < -getTurretMinPitch()) {
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

            if (angle < -getTurretMaxPitch()) {
                component = Component.translatable("tips.superbwarfare.ballistics.warn");
                canAim = false;
            }
        }

        if (canAim) {
            this.look(randomPos);
            entityData.set(TARGET_PITCH, angle);
        } else if (entity instanceof Player player) {
            player.displayClientMessage(location.copy().append(component).withStyle(ChatFormatting.RED), false);
        }
    }

    @Override
    public void resetTarget(@NotNull String weaponName) {
        Vec3 randomPos = VectorToolKt.randomPos(getTargetPos().getCenter(), getRadius()).add(0, -1, 0);
        Vec3 launchVector = calculateLaunchVector(getEyePosition(), randomPos, getProjectileVelocity(weaponName), getProjectileGravity(weaponName), getDepressed());
        this.look(randomPos);

        if (launchVector == null) {
            return;
        }
        float angle = (float) -VehicleVecUtils.getXRotFromVector(launchVector);
        if (angle > -getTurretMaxPitch() && angle < -getTurretMinPitch()) {
            entityData.set(TARGET_PITCH, angle);
        }
    }

    public void look(Vec3 pTarget) {
        Vec3 vec3 = EntityAnchorArgument.Anchor.EYES.apply(this);
        double d0 = (pTarget.x - vec3.x) * 0.2;
        double d2 = (pTarget.z - vec3.z) * 0.2;
        entityData.set(TARGET_YAW, Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 57.2957763671875) - 90F));
    }

    @Override
    public void travel() {
        float diffY = Mth.wrapDegrees(entityData.get(TARGET_YAW) - this.getYRot());
        float diffX = Mth.wrapDegrees(entityData.get(TARGET_PITCH) - this.getXRot());

        this.setYRot(this.getYRot() + Mth.clamp(0.5f * diffY, -20f, 20f));
        this.setXRot(Mth.clamp(this.getXRot() + Mth.clamp(0.5f * diffX, -20f, 20f), -getTurretMaxPitch(), -getTurretMinPitch()));
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
        discard();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!entityData.get(INTELLIGENT)) {
            vehicleShoot(null, "Main");
        }
    }

    @Override
    @Nullable
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.MORTAR_DEPLOYER.get());
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return super.canPlaceItem(slot, stack) && this.entityData.get(FIRE_TIME) == 0 && stack.getItem() instanceof MortarShellItem;
    }

    @Override
    public boolean canBind() {
        return this.entityData.get(INTELLIGENT);
    }
}
