package com.atsuishio.superbwarfare.entity;

import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

@Mod.EventBusSubscriber
public class TargetEntity extends LivingEntity implements GeoEntity {

    public static final EntityDataAccessor<Integer> DOWN_TIME = SynchedEntityData.defineId(TargetEntity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TargetEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.TARGET.get(), world);
    }

    public TargetEntity(EntityType<TargetEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DOWN_TIME, 0);
    }

    @Override
    protected float getStandingEyeHeight(@NotNull Pose pPose, @NotNull EntityDimensions pSize) {
        return 1.57F;
    }

    @Override
    public @NotNull MobType getMobType() {
        return super.getMobType();
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

    private static final DamageModifier DAMAGE_MODIFIER = DamageModifier.createDefaultModifier()
            .immuneTo(DamageTypes.LIGHTNING_BOLT)
            .immuneTo(DamageTypes.FALLING_ANVIL)
            .immuneTo(DamageTypes.MAGIC);

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // 不处理/kill伤害
        if (source.is(DamageTypes.GENERIC_KILL)) {
            this.remove(RemovalReason.KILLED);
            return super.hurt(source, amount);
        }

        amount = DAMAGE_MODIFIER.compute(source, amount);
        if (amount <= 0 || this.entityData.get(DOWN_TIME) > 0) {
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
        // 不处理/kill伤害
        if (event.getSource().is(DamageTypes.GENERIC_KILL)) return;
        var sourceEntity = event.getSource().getEntity();

        if (entity == null) return;

        if (entity instanceof TargetEntity targetEntity) {
            event.setCanceled(true);
            targetEntity.setHealth(targetEntity.getMaxHealth());

            if (sourceEntity == null) return;

            if (sourceEntity instanceof Player player) {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.target.down",
                        FormatTool.format1D((entity.position()).distanceTo((sourceEntity.position())), "m")), true);
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
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        if (!player.getMainHandItem().isEmpty() && !player.getMainHandItem().is(ModTags.Items.CROWBAR)) {
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
            this.remove(TargetEntity.RemovalReason.KILLED);
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

    @Override
    @Nullable
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.TARGET_DEPLOYER.get());
    }
}
