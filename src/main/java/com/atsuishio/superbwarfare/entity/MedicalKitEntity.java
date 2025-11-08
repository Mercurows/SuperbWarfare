package com.atsuishio.superbwarfare.entity;

import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import static com.atsuishio.superbwarfare.item.common.MedicalKitItem.treat;

public class MedicalKitEntity extends Entity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MedicalKitEntity(EntityType<MedicalKitEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!this.level().isClientSide()) {
                this.discard();
            }

            if (!player.getAbilities().instabuild) {
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.MEDICAL_KIT.get()));
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public void tick() {
        super.tick();

        this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.05, 0.0));

        if (!this.level().noCollision(this.getBoundingBox())) {
            this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        float f = 0.98F;
        if (this.onGround()) {
            this.setXRot(-90);
            BlockPos pos = this.getBlockPosBelowThatAffectsMyMovement();
            f = this.level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98F;
        } else {
            this.updateRotation();
        }

        this.setDeltaMovement(this.getDeltaMovement().multiply(f, 0.98, f));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, -0.9, 1.0));
        }

        if (this.tickCount >= 10) {
            touchEntity();
        }

        this.refreshDimensions();
    }

    public void touchEntity() {
        if (level() instanceof ServerLevel serverLevel) {
            var frontBox = getBoundingBox().inflate(0.3);

            var entities = level().getEntities(EntityTypeTest.forClass(LivingEntity.class), frontBox, living -> living.getHealth() < living.getMaxHealth()).stream().toList();

            for (var entity : entities) {
                treat(entity);
                serverLevel.playSound(null, position().x, position().y, position().z, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1);
                this.discard();
                break;
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
    }

    protected void updateRotation() {
        if (getDeltaMovement().length() > 0.05) {
            Vec3 vec3 = this.getDeltaMovement();
            double d0 = vec3.horizontalDistance();
            this.setXRot(lerpRotation(this.xRotO, (float) (Mth.atan2(vec3.y, d0) * (double) (180F / (float) Math.PI))));
            this.setYRot(lerpRotation(this.yRotO, (float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI))));
        }
    }

    protected static float lerpRotation(float pCurrentRotation, float pTargetRotation) {
        while (pTargetRotation - pCurrentRotation < -180F) {
            pCurrentRotation -= 360F;
        }

        while (pTargetRotation - pCurrentRotation >= 180F) {
            pCurrentRotation += 360F;
        }

        return Mth.lerp(0.2F, pCurrentRotation, pTargetRotation);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}