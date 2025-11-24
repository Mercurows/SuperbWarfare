package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.ArtilleryIndicator;
import com.atsuishio.superbwarfare.item.Monitor;
import com.atsuishio.superbwarfare.item.common.ammo.MortarShell;
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
import org.joml.Vector3f;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.ArrayList;
import java.util.List;

import static com.atsuishio.superbwarfare.tools.RangeTool.calculateLaunchVector;

public class MortarEntity extends ArtilleryEntity {
    public static final EntityDataAccessor<Float> TARGET_PITCH = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TARGET_YAW = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> INTELLIGENT = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.BOOLEAN);

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
        this.entityData.set(TARGET_YAW, yRot);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(INTELLIGENT, false);
        this.entityData.define(TARGET_PITCH, -70f);
        this.entityData.define(TARGET_YAW, this.getYRot());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("TargetPitch", this.entityData.get(TARGET_PITCH));
        compound.putFloat("TargetYaw", this.entityData.get(TARGET_YAW));
        compound.putBoolean("Intelligent", this.entityData.get(INTELLIGENT));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
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
    public void vehicleShoot(@Nullable LivingEntity living, String weaponName) {
        if (!(this.items.get(0).getItem() instanceof MortarShell)) return;
        var gunData = getGunData(weaponName);
        if (gunData == null) return;
        if (entityData.get(FIRE_TIME) != 0) return;
        var soundInfo = gunData.compute().soundInfo;

        this.shooter = living;
        this.entityData.set(FIRE_TIME, 25);

        if (!this.level().isClientSide()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), soundInfo.vehicleReload, SoundSource.PLAYERS, 1f, 1f);
        }

        if (level() instanceof ServerLevel serverLevel) {
            SoundTool.playDistantSound(serverLevel, soundInfo.fire3P, position(), (float) (0.25f * gunData.compute().soundRadius), random.nextFloat() * 0.1f + 1, null);
            SoundTool.playDistantSound(serverLevel, soundInfo.fire3PFar, position(), (float) gunData.compute().soundRadius, random.nextFloat() * 0.1f + 1, null);
        }
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
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
                vehicleShoot(player, "Main");
            }
            return InteractionResult.SUCCESS;
        }

        if (mainHandItem.getItem() instanceof MortarShell && !player.isShiftKeyDown() && this.entityData.get(FIRE_TIME) == 0 && this.items.get(0).isEmpty()) {
            this.items.set(0, mainHandItem.copyWithCount(1));
            if (!player.isCreative()) {
                mainHandItem.shrink(1);
            }
            vehicleShoot(player, "Main");
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

        return list;
    }

    @Override
    protected float getEyeHeight(@NotNull Pose pPose, @NotNull EntityDimensions pSize) {
        return 0.2F;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (entityData.get(FIRE_TIME) == 5 && this.items.get(0).getItem() instanceof MortarShell) {
            Level level = this.level();
            var gunData = getGunData("Main");
            if (level instanceof ServerLevel server && gunData != null) {
                MortarShellEntity entityToSpawn = MortarShell.createShell(shooter, level, this.items.get(0), getProjectileGravity("Main"), (float) gunData.compute().damage, (float) gunData.compute().explosionDamage, (float) gunData.compute().explosionRadius);
                entityToSpawn.setPos(this.getX(), this.getEyeY(), this.getZ());
                entityToSpawn.shoot(this.getLookAngle().x, this.getLookAngle().y, this.getLookAngle().z, getProjectileVelocity("Main"), getProjectileSpread("Main"));
                level.addFreshEntity(entityToSpawn);

                ParticleTool.spawnMediumCannonMuzzleParticles(getLookAngle(), new Vec3(this.getX(), this.getEyeY(), this.getZ()).add(getLookAngle().scale(1.5)), server, this);

                this.clearContent();

                if (this.entityData.get(INTELLIGENT)) {
                    this.resetTarget("Main");
                }

                gunData.shakePlayers(this);
            }
        }
    }

    @Override
    public void setTarget(ItemStack stack, Entity entity, String weaponName) {
        double targetX = stack.getOrCreateTag().getDouble("TargetX");
        double targetY = stack.getOrCreateTag().getDouble("TargetY") - 1;
        double targetZ = stack.getOrCreateTag().getDouble("TargetZ");
        boolean canAim = true;

        entityData.set(TARGET_POS, new Vector3f((float) targetX, (float) targetY, (float) targetZ));
        entityData.set(DEPRESSED, stack.getOrCreateTag().getBoolean("IsDepressed"));
        entityData.set(RADIUS, stack.getOrCreateTag().getInt("Radius"));
        Vec3 randomPos = VectorTool.randomPos(new Vec3(entityData.get(TARGET_POS)), entityData.get(RADIUS));
        Vec3 launchVector = calculateLaunchVector(getEyePosition(), randomPos, getProjectileVelocity(weaponName), getProjectileGravity(weaponName), entityData.get(DEPRESSED));
        Vec3 launchVector2 = calculateLaunchVector(getEyePosition(), randomPos, getProjectileVelocity(weaponName), getProjectileGravity(weaponName), !entityData.get(DEPRESSED));

        Component component = Component.literal("");
        Component location = Component.translatable("tips.superbwarfare.mortar.position", this.getDisplayName())
                .append(Component.literal(" X:" + FormatTool.format0D(getX()) + " Y:" + FormatTool.format0D(getY()) + " Z:" + FormatTool.format0D(getZ()) + " "));
        float angle = getXRot();

        if (launchVector == null || launchVector2 == null) {
            canAim = false;
            component = Component.translatable("tips.superbwarfare.mortar.out_of_range");
        } else {
            angle = (float) -VehicleVecUtils.getXRotFromVector(launchVector);
            float angle2 = (float) -VehicleVecUtils.getXRotFromVector(launchVector2);
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
    public void resetTarget(String weaponName) {
        Vec3 randomPos = VectorTool.randomPos(new Vec3(entityData.get(TARGET_POS)), entityData.get(RADIUS));
        Vec3 launchVector = calculateLaunchVector(getEyePosition(), randomPos, getProjectileVelocity(weaponName), getProjectileGravity(weaponName), entityData.get(DEPRESSED));
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
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
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
        return super.canPlaceItem(slot, stack) && this.entityData.get(FIRE_TIME) == 0 && stack.getItem() instanceof MortarShell;
    }
}
