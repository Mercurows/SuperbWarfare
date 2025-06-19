package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.projectile.*;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.Monitor;
import com.atsuishio.superbwarfare.item.common.ammo.MortarShell;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class DroneEntity extends MobileVehicleEntity implements GeoEntity {

    public static final EntityDataAccessor<Boolean> LINKED = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> CONTROLLER = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> KAMIKAZE_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> DELTA_X_ROT = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public boolean fire;
    public int collisionCoolDown;
    public double lastTickSpeed;
    public double lastTickVerticalSpeed;
    public ItemStack currentItem = ItemStack.EMPTY;

    public float pitch;
    public float pitchO;

    public int holdTickX;
    public int holdTickY;
    public int holdTickZ;

    public DroneEntity(EntityType<DroneEntity> type, Level world) {
        super(type, world);
    }

    public float getBodyPitch() {
        return pitch;
    }

    public void setBodyXRot(float rot) {
        pitch = rot;
    }

    public float getBodyPitch(float tickDelta) {
        return Mth.lerp(0.6f * tickDelta, pitchO, getBodyPitch());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DELTA_X_ROT, 0f)
                .define(CONTROLLER, "undefined")
                .define(LINKED, false)
                .define(KAMIKAZE_MODE, 0);
    }

    @Override
    public boolean causeFallDamage(float l, float d, @NotNull DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Linked", this.entityData.get(LINKED));
        compound.putString("Controller", this.entityData.get(CONTROLLER));
        compound.putInt("Ammo", this.entityData.get(AMMO));
        compound.putInt("KamikazeMode", this.entityData.get(KAMIKAZE_MODE));

        CompoundTag item = new CompoundTag();
        if (!item.isEmpty()) {
            this.currentItem.save(level().registryAccess(), item);
        }
        compound.put("Item", item);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Linked"))
            this.entityData.set(LINKED, compound.getBoolean("Linked"));
        if (compound.contains("Controller"))
            this.entityData.set(CONTROLLER, compound.getString("Controller"));
        if (compound.contains("Ammo"))
            this.entityData.set(AMMO, compound.getInt("Ammo"));
        if (compound.contains("KamikazeMode"))
            this.entityData.set(KAMIKAZE_MODE, compound.getInt("KamikazeMode"));
        if (compound.contains("Item"))
            this.currentItem = ItemStack.parse(level().registryAccess(), compound.getCompound("Item"))
                    .orElse(ItemStack.EMPTY);
    }

    @Override
    public int maxRepairCoolDown() {
        return -1;
    }

    @Override
    public void baseTick() {
        pitchO = this.getBodyPitch();
        setBodyXRot(pitch * 0.9f);

        super.baseTick();

        setZRot(getRoll() * 0.9f);

        lastTickSpeed = this.getDeltaMovement().length();
        lastTickVerticalSpeed = this.getDeltaMovement().y;

        if (collisionCoolDown > 0) {
            collisionCoolDown--;
        }

        Player controller = EntityFindUtil.findPlayer(this.level(), this.entityData.get(CONTROLLER));


        if (!this.onGround()) {
            if (controller != null) {
                ItemStack stack = controller.getMainHandItem();
                var tag = NBTTool.getTag(stack);
                if (!stack.is(ModItems.MONITOR.get()) || !tag.getBoolean("Using")) {
                    upInputDown = false;
                    downInputDown = false;
                    forwardInputDown = false;
                    backInputDown = false;
                    leftInputDown = false;
                    rightInputDown = false;
                }

                if (tickCount % 5 == 0) {
                    controller.getInventory().items.stream().filter(pStack -> pStack.getItem() == ModItems.MONITOR.get())
                            .forEach(pStack -> {
                                if (tag.getString(Monitor.LINKED_DRONE).equals(this.getStringUUID())) {
                                    Monitor.getDronePos(pStack, this.position());
                                }
                            });
                }
            }
        }

        if (this.isInWater()) {
            this.hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.EXPLOSION), controller), 0.25f + (float) (2 * lastTickSpeed));
        }

        if (this.fire) {
            if (this.entityData.get(AMMO) > 0) {
                this.entityData.set(AMMO, this.entityData.get(AMMO) - 1);
                if (controller != null) {
                    droneDrop(controller);
                }
            }
            if (this.entityData.get(KAMIKAZE_MODE) != 0) {
                if (controller != null) {
                    var stack = controller.getMainHandItem();
                    if (stack.is(ModItems.MONITOR.get())) {
                        var tag = NBTTool.getTag(stack);
                        Monitor.disLink(tag, controller);
                        NBTTool.saveTag(stack, tag);
                    }
                    this.hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.EXPLOSION), controller), 10000);
                }

            }
            this.fire = false;
        }

        this.refreshDimensions();
    }

    private void droneDrop(@Nullable Player player) {
        if (!this.level().isClientSide()) {
            RgoGrenadeEntity rgoGrenadeEntity = new RgoGrenadeEntity(player, this.level(), 160);
            rgoGrenadeEntity.setPos(this.getX(), this.getEyeY() - 0.09, this.getZ());
            rgoGrenadeEntity.droneShoot(this);
            this.level().addFreshEntity(rgoGrenadeEntity);
        }
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() == ModItems.MONITOR.get()) {
            var tag = NBTTool.getTag(stack);
            if (!player.isCrouching()) {
                if (!this.entityData.get(LINKED)) {
                    if (tag.getBoolean("Linked")) {
                        player.displayClientMessage(Component.translatable("tips.superbwarfare.monitor.already_linked").withStyle(ChatFormatting.RED), true);
                        return InteractionResult.sidedSuccess(this.level().isClientSide());
                    }

                    this.entityData.set(LINKED, true);
                    this.entityData.set(CONTROLLER, player.getStringUUID());

                    Monitor.link(tag, this.getStringUUID());
                    NBTTool.saveTag(stack, tag);
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.monitor.linked").withStyle(ChatFormatting.GREEN), true);

                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5F, 1);
                    }
                } else {
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.drone.already_linked").withStyle(ChatFormatting.RED), true);
                }
            } else {
                if (this.entityData.get(LINKED)) {
                    if (!tag.getBoolean("Linked")) {
                        player.displayClientMessage(Component.translatable("tips.superbwarfare.drone.already_linked").withStyle(ChatFormatting.RED), true);
                        return InteractionResult.sidedSuccess(this.level().isClientSide());
                    }

                    this.entityData.set(CONTROLLER, "none");
                    this.entityData.set(LINKED, false);

                    Monitor.disLink(tag, player);
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.monitor.unlinked").withStyle(ChatFormatting.RED), true);

                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5F, 1);
                    }
                }
            }
        } else if (stack.is(ModItems.CROWBAR.get()) && player.isCrouching()) {
            // 返还物品
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.DRONE.get()));

            // 返还普通弹药
            for (int index0 = 0; index0 < this.entityData.get(AMMO); index0++) {
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.RGO_GRENADE.get()));
            }

            // 返还神风弹药
            if (this.entityData.get(KAMIKAZE_MODE) != 0) {
                ItemHandlerHelper.giveItemToPlayer(player, this.currentItem);
            }

            player.getInventory().items.stream().filter(stack_ -> stack_.getItem() == ModItems.MONITOR.get())
                    .forEach(itemStack -> {
                        var tag = NBTTool.getTag(itemStack);
                        if (tag.getString(Monitor.LINKED_DRONE).equals(this.getStringUUID())) {
                            Monitor.disLink(tag, player);
                        }
                    });

            if (!this.level().isClientSide()) {
                this.discard();
            }
        } else if (stack.getItem() == ModItems.RGO_GRENADE.get() && this.entityData.get(KAMIKAZE_MODE) == 0) {
            // 装载普通弹药
            if (this.entityData.get(AMMO) < 6) {
                this.entityData.set(AMMO, this.entityData.get(AMMO) + 1);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.BULLET_SUPPLY.get(), SoundSource.PLAYERS, 0.5F, 1);
                }
            }
        } else if (stack.getItem() instanceof MortarShell && this.entityData.get(AMMO) == 0 && this.entityData.get(KAMIKAZE_MODE) == 0) {
            // 迫击炮神风
            var copy = stack.copy();
            copy.setCount(1);
            this.currentItem = copy;

            if (!player.isCreative()) {
                stack.shrink(1);
            }
            this.entityData.set(KAMIKAZE_MODE, 1);

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.BULLET_SUPPLY.get(), SoundSource.PLAYERS, 0.5F, 1);
            }
        } else if (stack.getItem() == ModItems.C4_BOMB.get() && this.entityData.get(AMMO) == 0 && this.entityData.get(KAMIKAZE_MODE) == 0) {
            // C4神风
            this.currentItem = new ItemStack(stack.getItem(), 1);

            if (!player.isCreative()) {
                stack.shrink(1);
            }
            this.entityData.set(KAMIKAZE_MODE, 2);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.BULLET_SUPPLY.get(), SoundSource.PLAYERS, 0.5F, 1);
            }
        } else if (stack.getItem() == ModItems.ROCKET.get() && this.entityData.get(AMMO) == 0 && this.entityData.get(KAMIKAZE_MODE) == 0) {
            // RPG神风
            this.currentItem = new ItemStack(stack.getItem(), 1);

            if (!player.isCreative()) {
                stack.shrink(1);
            }
            this.entityData.set(KAMIKAZE_MODE, 3);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.BULLET_SUPPLY.get(), SoundSource.PLAYERS, 0.5F, 1);
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public void travel() {
        if (!this.onGround()) {
            // left and right
            if (rightInputDown) {
                holdTickX++;
                this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 0.3f * Math.min(holdTickX, 5));
            } else if (this.leftInputDown) {
                holdTickX++;
                this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 0.3f * Math.min(holdTickX, 5));
            } else {
                holdTickX = 0;
            }

            // forward and backward
            if (forwardInputDown) {
                holdTickZ++;
                this.entityData.set(DELTA_X_ROT, this.entityData.get(DELTA_X_ROT) - 0.3f * Math.min(holdTickZ, 5));
            } else if (backInputDown) {
                holdTickZ++;
                this.entityData.set(DELTA_X_ROT, this.entityData.get(DELTA_X_ROT) + 0.3f * Math.min(holdTickZ, 5));
            } else {
                holdTickZ = 0;
            }

            this.setDeltaMovement(this.getDeltaMovement().multiply(0.965, 0.935, 0.965));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 1, 0.8));
            this.setZRot(this.roll * 0.7f);
            this.setXRot(this.getXRot() * 0.7f);
            this.setBodyXRot(this.getBodyPitch() * 0.7f);
        }

        if (this.isInWater() && this.tickCount % 4 == 0) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.6, 0.6));
            this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, this.getFirstPassenger() == null ? this : this.getFirstPassenger()), 26 + (float) (60 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))));
        }

        boolean up = this.upInputDown;
        boolean down = this.downInputDown;

        if (up) {
            holdTickY++;
            this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.01f * Math.min(holdTickY, 5), 0.2f));
        } else if (down) {
            holdTickY++;
            this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.02f * Math.min(holdTickY, 5), this.onGround() ? 0 : 0.01f));
        } else {
            holdTickY = 0;
        }

        if (!(up || down)) {
            if (this.getDeltaMovement().y() < 0) {
                this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.01f, 0.4f));
            } else {
                this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.01f, this.onGround() ? 0 : 0.01f));
            }
        }

        this.entityData.set(POWER, this.entityData.get(POWER) * 0.99f);
        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * 0.7f);
        this.entityData.set(DELTA_X_ROT, this.entityData.get(DELTA_X_ROT) * 0.7f);

        this.setZRot(Mth.clamp(this.getRoll() - this.entityData.get(DELTA_ROT), -30, 30));
        this.setBodyXRot(Mth.clamp(this.getBodyPitch() - this.entityData.get(DELTA_X_ROT), -30, 30));

        setDeltaMovement(getDeltaMovement().add(0.0f, this.entityData.get(POWER) * 0.6f, 0.0f));

        Vector3f direction = getRightDirection().mul(this.entityData.get(DELTA_ROT));
        setDeltaMovement(getDeltaMovement().add(new Vec3(direction.x, direction.y, direction.z).scale(0.03)));

        Vector3f directionZ = getForwardDirection().mul(-this.entityData.get(DELTA_X_ROT));
        setDeltaMovement(getDeltaMovement().add(new Vec3(directionZ.x, directionZ.y, directionZ.z).scale(0.03)));

        Player controller = EntityFindUtil.findPlayer(this.level(), this.entityData.get(CONTROLLER));
        if (controller != null) {
            ItemStack stack = controller.getMainHandItem();
            var tag = NBTTool.getTag(stack);
            if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using")) {
                this.setYRot(this.getYRot() + 0.5f * entityData.get(MOUSE_SPEED_X));
                this.setXRot(Mth.clamp(this.getXRot() + 0.5f * entityData.get(MOUSE_SPEED_Y), -10, 90));
            }
        }

        float f = 0.7f;
        AABB aabb = AABB.ofSize(this.getEyePosition(), f, 0.3, f);
        var level = this.level();
        for (var target : level.getEntitiesOfClass(Entity.class, aabb, e -> true)) {
            if (this != target && target != null
                    && !(target instanceof ItemEntity || target instanceof Projectile || target instanceof ProjectileEntity || target instanceof LaserEntity
                    || target.getType().is(ModTags.EntityTypes.DECOY) || target instanceof AreaEffectCloud || target instanceof C4Entity)) {
                hitEntityCrash(controller, target);
            }
        }
    }

    public void hitEntityCrash(Player player, Entity target) {
        if (lastTickSpeed > 0.12) {
            if (this.entityData.get(KAMIKAZE_MODE) != 0 && 20 * lastTickSpeed > this.getHealth()) {
                if (this.entityData.get(KAMIKAZE_MODE) == 1) {
                    var mortarShell = new MortarShellEntity(player, this.level());
                    target.hurt(ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(), mortarShell, player), ExplosionConfig.DRONE_KAMIKAZE_HIT_DAMAGE.get());
                    target.invulnerableTime = 0;
                } else if (this.entityData.get(KAMIKAZE_MODE) == 2) {
                    var c4 = new C4Entity(player, this.level());
                    target.hurt(ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(), c4, player), ExplosionConfig.DRONE_KAMIKAZE_HIT_DAMAGE_C4.get());
                    target.invulnerableTime = 0;
                } else if (this.entityData.get(KAMIKAZE_MODE) == 3) {
                    var rpg = new RpgRocketEntity(player, this.level());
                    target.hurt(ModDamageTypes.causeCannonFireDamage(this.level().registryAccess(), rpg, player), ExplosionConfig.DRONE_KAMIKAZE_HIT_DAMAGE_RPG.get());
                    target.invulnerableTime = 0;
                }

                if (player != null && player.getMainHandItem().is(ModItems.MONITOR.get())) {
                    var stack = player.getMainHandItem();
                    var tag = NBTTool.getTag(stack);
                    Monitor.disLink(tag, player);
                    NBTTool.saveTag(stack, tag);
                }
            }
            target.hurt(ModDamageTypes.causeDroneHitDamage(this.level().registryAccess(), this, player), (float) (5 * lastTickSpeed));

            this.hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.EXPLOSION), Objects.requireNonNullElse(player, this)), (float) (((this.entityData.get(KAMIKAZE_MODE) != 0) ? 20 : 4) * lastTickSpeed));
        }
    }

    @Override
    public boolean engineRunning() {
        return !onGround();
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.DRONE_SOUND.get();
    }

    @Override
    public float getEngineSoundVolume() {
        Player player = EntityFindUtil.findPlayer(this.level(), this.entityData.get(CONTROLLER));

        if (player == null) return entityData.get(POWER);
        ItemStack stack = player.getMainHandItem();

        if (stack.is(ModItems.MONITOR.get()) && NBTTool.getTag(stack).getBoolean("Using") && NBTTool.getTag(stack).getBoolean("Linked")) {
            return entityData.get(POWER) * 0.25f;
        }
        return entityData.get(POWER) * 2f;
    }

    @Override
    public void move(@NotNull MoverType movementType, @NotNull Vec3 movement) {
        super.move(movementType, movement);
        Player controller = EntityFindUtil.findPlayer(this.level(), this.entityData.get(CONTROLLER));

        if (lastTickSpeed < 0.2 || collisionCoolDown > 0) return;

        if ((verticalCollision) && Mth.abs((float) lastTickVerticalSpeed) > 1) {
            this.hurt(ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), this, controller == null ? this : controller), (float) (20 * ((Mth.abs((float) lastTickVerticalSpeed) - 1) * (lastTickSpeed - 0.2) * (lastTickSpeed - 0.2))));
            collisionCoolDown = 4;
        }

        if (this.horizontalCollision) {
            this.hurt(ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), this, controller == null ? this : controller), (float) (10 * ((lastTickSpeed - 0.2) * (lastTickSpeed - 0.2))));
            collisionCoolDown = 4;
        }
    }

    @Override
    public void destroy() {
        Player controller = EntityFindUtil.findPlayer(this.level(), this.entityData.get(CONTROLLER));
        if (controller != null) {
            if (controller.getMainHandItem().is(ModItems.MONITOR.get())) {
                var item = controller.getMainHandItem();
                var tag = NBTTool.getTag(item);
                Monitor.disLink(tag, controller);
                NBTTool.saveTag(item, tag);
            }
        }

        // 无人机爆炸
        if (level() instanceof ServerLevel) {
            level().explode(null, this.getX(), this.getY(), this.getZ(), 0, Level.ExplosionInteraction.NONE);
        }

        // 神风自爆
        if (this.entityData.get(KAMIKAZE_MODE) != 0) {
            kamikazeExplosion(this.entityData.get(KAMIKAZE_MODE));
        }

        // RGO投弹
        if (this.level() instanceof ServerLevel) {
            int count = this.entityData.get(AMMO);
            for (int i = 0; i < count; i++) {
                droneDrop(controller);
            }
        }

        String id = this.entityData.get(CONTROLLER);
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException ignored) {
            this.discard();
            return;
        }

        Player player = this.level().getPlayerByUUID(uuid);
        if (player != null) {
            player.getInventory().items.stream().filter(stack -> stack.getItem() == ModItems.MONITOR.get())
                    .forEach(stack -> {
                        var tag = NBTTool.getTag(stack);
                        if (tag.getString(Monitor.LINKED_DRONE).equals(this.getStringUUID())) {
                            Monitor.disLink(tag, player);
                            NBTTool.saveTag(stack, tag);
                        }
                    });
        }

        super.destroy();
    }

    private void kamikazeExplosion(int mode) {
        Entity attacker = EntityFindUtil.findEntity(this.level(), this.entityData.get(LAST_ATTACKER_UUID));
        Player controller = EntityFindUtil.findPlayer(this.level(), this.entityData.get(CONTROLLER));

        assert controller != null;
        Entity mortarShell = new MortarShellEntity(controller, level());
        Entity c4 = new C4Entity(controller, level());
        Entity rpg = new RpgRocketEntity(controller, level());

        CustomExplosion explosion = switch (mode) {
            case 1 -> new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(), mortarShell, attacker), ExplosionConfig.DRONE_KAMIKAZE_EXPLOSION_DAMAGE.get(),
                    this.getX(), this.getY(), this.getZ(), ExplosionConfig.DRONE_KAMIKAZE_EXPLOSION_RADIUS.get(), ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1);

            case 2 -> new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(), c4, attacker), ExplosionConfig.C4_EXPLOSION_DAMAGE.get(),
                    this.getX(), this.getY(), this.getZ(), ExplosionConfig.C4_EXPLOSION_RADIUS.get(), ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1);

            case 3 -> new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(), rpg, attacker), ExplosionConfig.RPG_EXPLOSION_DAMAGE.get(),
                    this.getX(), this.getY(), this.getZ(), ExplosionConfig.RPG_EXPLOSION_RADIUS.get(), ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1);

            default -> null;
        };

        if (explosion == null) return;

        explosion.explode();
        EventHooks.onExplosionStart(this.level(), explosion);
        explosion.finalizeExplosion(false);
        if (mode == 1) {
            ParticleTool.spawnMediumExplosionParticles(this.level(), this.position());

            if (this.currentItem.getItem() instanceof MortarShell) {
                this.createAreaCloud(this.currentItem.get(DataComponents.POTION_CONTENTS), this.level(), ExplosionConfig.DRONE_KAMIKAZE_EXPLOSION_DAMAGE.get(), ExplosionConfig.DRONE_KAMIKAZE_EXPLOSION_RADIUS.get());
            }
        }

        if (mode == 2 || mode == 3) {
            ParticleTool.spawnHugeExplosionParticles(this.level(), this.position());
        }
    }

    private void createAreaCloud(PotionContents potion, Level level, int duration, float radius) {
        if (potion == null || potion.potion().map(p -> p.value() == Potions.WATER.value()).orElse(false)) return;

        AreaEffectCloud cloud = new AreaEffectCloud(level, this.getX() + 0.75 * getDeltaMovement().x, this.getY() + 0.5 * getBbHeight() + 0.75 * getDeltaMovement().y, this.getZ() + 0.75 * getDeltaMovement().z);

        for (var effect : potion.potion().map(p -> p.value().getEffects()).orElse(new ArrayList<>())) {
            cloud.addEffect(effect);
        }

        cloud.setDuration(duration);
        cloud.setRadius(radius);

        Player controller = EntityFindUtil.findPlayer(this.level(), this.entityData.get(CONTROLLER));
        if (controller != null) {
            cloud.setOwner(controller);
        }
        level.addFreshEntity(cloud);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean canCrushEntities() {
        return false;
    }
}
