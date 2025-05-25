package com.atsuishio.superbwarfare.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

@EventBusSubscriber(modid = Mod.MODID)
public class TargetEntity extends LivingEntity implements GeoEntity {

    public static final EntityDataAccessor<Integer> DOWN_TIME = SynchedEntityData.defineId(TargetEntity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TargetEntity(EntityType<TargetEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DOWN_TIME, 0);
    }


    @Override
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return NonNullList.withSize(1, ItemStack.EMPTY);
    }

    @Override
    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot pSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(@NotNull EquipmentSlot pSlot, @NotNull ItemStack pStack) {
    }

    @Override
    public boolean causeFallDamage(float l, float d, @NotNull DamageSource source) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_FIRE)
                || source.getDirectEntity() instanceof ThrownPotion
                || source.getDirectEntity() instanceof AreaEffectCloud
                || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.LIGHTNING_BOLT)
                || source.is(DamageTypes.FALLING_ANVIL)
                || source.is(DamageTypes.DRAGON_BREATH)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.WITHER_SKULL)
                || source.is(DamageTypes.MAGIC)
                || this.entityData.get(DOWN_TIME) > 0) {
            return false;
        }

        if (!this.level().isClientSide()) {
            this.level().playSound(null, BlockPos.containing(this.getX(), this.getY(), this.getZ()), ModSounds.HIT.get(), SoundSource.BLOCKS, 1, 1);
        } else {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.HIT.get(), SoundSource.BLOCKS, 1, 1, false);
        }
        return super.hurt(source, amount);
    }

    @SubscribeEvent
    public static void onTargetDown(LivingDeathEvent event) {
        var entity = event.getEntity();
        var sourceEntity = event.getSource().getEntity();

        if (entity instanceof TargetEntity targetEntity) {
            event.setCanceled(true);
            targetEntity.setHealth(targetEntity.getMaxHealth());

            if (sourceEntity == null) return;

            if (sourceEntity instanceof Player player) {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.target.down",
                        FormatTool.format1D(entity.position().distanceTo(sourceEntity.position()), "m")), true);
                SoundTool.playLocalSound(player, ModSounds.TARGET_DOWN.get(), 1, 1);
                targetEntity.entityData.set(DOWN_TIME, 40);
            }
        }
    }

    @Override
    public boolean isPickable() {
        return this.entityData.get(DOWN_TIME) == 0;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        if (player.getMainHandItem() != ItemStack.EMPTY) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (!this.level().isClientSide()) {
                this.discard();
            }

            if (!player.getAbilities().instabuild) {
                player.addItem(new ItemStack(ModItems.TARGET_DEPLOYER.get()));
            }
        } else {
            this.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3((player.getX()), this.getY(), (player.getZ())));
            this.setXRot(0);
            this.xRotO = this.getXRot();
            this.entityData.set(DOWN_TIME, 0);
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.entityData.get(DOWN_TIME) > 0) {
            this.entityData.set(DOWN_TIME, this.entityData.get(DOWN_TIME) - 1);
        }
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        return new Vec3(0, 0, 0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public @NotNull HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    protected void doPush(@NotNull Entity entityIn) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public void setNoGravity(boolean ignored) {
        super.setNoGravity(true);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.updateSwingTime();
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.ARMOR, 0)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 16)
                .add(Attributes.KNOCKBACK_RESISTANCE, 10)
                .add(Attributes.FLYING_SPEED, 0);
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= 100) {
            this.spawnAtLocation(new ItemStack(ModItems.TARGET_DEPLOYER.get()));
            this.remove(RemovalReason.KILLED);
        }
    }

    private PlayState movementPredicate(AnimationState<TargetEntity> event) {
        if (this.entityData.get(DOWN_TIME) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.target.down"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.target.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

}
