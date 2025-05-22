package com.atsuishio.superbwarfare.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.energy.SyncedEntityEnergyStorage;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

@EventBusSubscriber(modid = Mod.MODID)
public class DPSGeneratorEntity extends LivingEntity implements GeoEntity {

    public static final EntityDataAccessor<Integer> DOWN_TIME = SynchedEntityData.defineId(DPSGeneratorEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> ENERGY = SynchedEntityData.defineId(DPSGeneratorEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(DPSGeneratorEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damageDealt = 0;

    public DPSGeneratorEntity(EntityType<DPSGeneratorEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);

        builder.define(DOWN_TIME, 0)
                .define(ENERGY, 0)
                .define(LEVEL, 0);
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
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Level", this.entityData.get(LEVEL));

        var entityCap = this.getCapability(Capabilities.EnergyStorage.ENTITY, null);
        if (entityCap == null) return;

        compound.putInt("Energy", entityCap.getEnergyStored());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(LEVEL, compound.getInt("Level"));

        var entityCap = this.getCapability(Capabilities.EnergyStorage.ENTITY, null);
        if (entityCap == null) return;

        ((SyncedEntityEnergyStorage) entityCap).setEnergy(compound.getInt("Energy"));
        ((SyncedEntityEnergyStorage) entityCap).setCapacity(this.getMaxEnergy());
        ((SyncedEntityEnergyStorage) entityCap).setMaxExtract(this.getMaxTransfer());
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        damageDealt += amount;

        if (this.getHealth() < 0.01) {
            amount = 0;
        }

        if (!this.level().isClientSide()) {
            this.level().playSound(null, BlockPos.containing(this.getX(), this.getY(), this.getZ()), ModSounds.HIT.get(), SoundSource.BLOCKS, 1, 1);
        } else {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.HIT.get(), SoundSource.BLOCKS, 1, 1, false);
        }
        return super.hurt(source, (float) (amount / Math.pow(2, getGeneratorLevel())));
    }

    @SubscribeEvent
    public static void onTargetDown(LivingDeathEvent event) {
        var entity = event.getEntity();
        var sourceEntity = event.getSource().getEntity();

        if (entity instanceof DPSGeneratorEntity generatorEntity) {
            event.setCanceled(true);
            generatorEntity.setHealth(0.00001F);

            if (sourceEntity instanceof Player player) {
                SoundTool.playLocalSound(player, ModSounds.TARGET_DOWN.get(), 1, 1);
                generatorEntity.entityData.set(DOWN_TIME, 40);
            }
        }
    }

    @Override
    public boolean isPickable() {
        return this.entityData.get(DOWN_TIME) == 0;
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
                player.addItem(new ItemStack(ModItems.DPS_GENERATOR_DEPLOYER.get()));
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

        // 每秒恢复生命并充能下方方块
        if (this.tickCount % 20 == 0) {
            var damage = this.getMaxHealth() - this.getHealth();
            var entityCap = this.getCapability(Capabilities.EnergyStorage.ENTITY, null);

            if (entityCap != null) {
                if (damage > 0) {
                    // DPS显示
                    if (getLastDamageSource() != null) {
                        var attacker = getLastDamageSource().getEntity();
                        if (attacker instanceof Player player && !this.level().isClientSide) {
                            player.displayClientMessage(Component.translatable("tips.superbwarfare.dps_generator.dps",
                                    FormatTool.format1DZ(damageDealt)), true);
                        }
                    }

                    // 发电
                    ((SyncedEntityEnergyStorage) entityCap).setMaxReceive(entityCap.getMaxEnergyStored());
                    entityCap.receiveEnergy((int) Math.round(128D * Math.max(getGeneratorLevel(), 1) * Math.pow(2, getGeneratorLevel()) * damage), false);
                    ((SyncedEntityEnergyStorage) entityCap).setMaxReceive(0);
                }

                // 充能底部方块
                this.chargeBlockBelow();

                if (this.getHealth() < 0.01) {
                    this.entityData.set(LEVEL, Math.min(this.entityData.get(LEVEL) + 1, 7));
                    ((SyncedEntityEnergyStorage) entityCap).setCapacity(this.getMaxEnergy());
                    ((SyncedEntityEnergyStorage) entityCap).setMaxExtract(this.getMaxTransfer());


                    if (!this.level().isClientSide()) {
                        this.level().playSound(null, BlockPos.containing(this.getX(), this.getY(), this.getZ()), ModSounds.DPS_GENERATOR_EVOLVE.get(), SoundSource.BLOCKS, 0.5f, 1);
                    } else {
                        this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.DPS_GENERATOR_EVOLVE.get(), SoundSource.BLOCKS, 0.5f, 1, false);
                    }
                }
                this.setHealth(this.getMaxHealth());
                damageDealt = 0;
            }

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
            this.spawnAtLocation(new ItemStack(ModItems.DPS_GENERATOR_DEPLOYER.get()));
            this.remove(RemovalReason.KILLED);
        }
    }

    private PlayState movementPredicate(AnimationState<DPSGeneratorEntity> event) {
        if (this.entityData.get(DOWN_TIME) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.target.down"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.target.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
    }

    protected void chargeBlockBelow() {
        var entityCap = this.getCapability(Capabilities.EnergyStorage.ENTITY, null);
        if (entityCap == null) return;

        if (!entityCap.canExtract() || entityCap.getEnergyStored() <= 0) return;
        var blockPos = this.blockPosition().below();
        var cap = this.level().getCapability(Capabilities.EnergyStorage.BLOCK, blockPos, Direction.UP);
        if (cap == null || !cap.canReceive()) return;

        var extract = entityCap.extractEnergy(entityCap.getEnergyStored(), true);
        var extracted = cap.receiveEnergy(extract, false);
        if (extracted <= 0) return;

        this.level().blockEntityChanged(blockPos);
        entityCap.extractEnergy(extracted, false);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    protected final SyncedEntityEnergyStorage energyStorage = new SyncedEntityEnergyStorage(5120, 0, 2560, this.entityData, ENERGY);

    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public int getGeneratorLevel() {
        return this.entityData.get(LEVEL);
    }

    public int getMaxEnergy() {
        return switch (getGeneratorLevel()) {
            case 1 -> 25600;
            case 2 -> 102400;
            case 3 -> 409600;
            case 4 -> 1638400;
            case 5 -> 6553600;
            case 6 -> 26214400;
            case 7 -> 104857600;
            default -> 5120;
        };
    }

    public int getMaxTransfer() {
        return getMaxEnergy() / 2;
    }

    public void beastCharge() {
        this.entityData.set(LEVEL, 7);
        if (this.getCapability(Capabilities.EnergyStorage.ENTITY, null) instanceof SyncedEntityEnergyStorage storage) {
            storage.setCapacity(this.getMaxEnergy());
            storage.setMaxExtract(this.getMaxTransfer());
            storage.setEnergy(this.getMaxEnergy());
        }
    }
}
