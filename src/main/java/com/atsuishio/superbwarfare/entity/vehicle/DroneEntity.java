package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.data.drone_attachment.DroneAttachmentData;
import com.atsuishio.superbwarfare.entity.projectile.C4Entity;
import com.atsuishio.superbwarfare.entity.projectile.LaserEntity;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.Monitor;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.stream.Collectors;

import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraPitch;
import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraYaw;

public class DroneEntity extends VehicleEntity implements GeoEntity {

    @Override
    public boolean hasMenu() {
        return false;
    }
    public static final EntityDataAccessor<Boolean> LINKED = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> CONTROLLER = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_KAMIKAZE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DELTA_X_ROT = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> DISPLAY_ENTITY = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<CompoundTag> DISPLAY_ENTITY_TAG = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.COMPOUND_TAG);

    // scale[3], offset[3], rotation[3], xLength, zLength, tickCount
    public static final EntityDataAccessor<List<Float>> DISPLAY_DATA = SynchedEntityData.defineId(DroneEntity.class, ModSerializers.FLOAT_LIST_SERIALIZER.get());
    public static final EntityDataAccessor<Integer> MAX_AMMO = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);

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
    public VehicleType vehicleType = VehicleType.DRONE;

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
        var data = new DroneAttachmentData();

        builder.define(DELTA_X_ROT, 0f)
                .define(CONTROLLER, "undefined")
                .define(LINKED, false)
                .define(IS_KAMIKAZE, false)
                .define(DISPLAY_ENTITY, "")
                .define(DISPLAY_DATA, List.of(
                        data.scale()[0], data.scale()[1], data.scale()[2],
                        data.offset()[0], data.offset()[1], data.offset()[2],
                        data.rotation()[0], data.rotation()[1], data.rotation()[2],
                        data.xLength, data.zLength,
                        (float) data.tickCount
                ))
                .define(DISPLAY_ENTITY_TAG, new CompoundTag())
                .define(MAX_AMMO, 1);
    }

    @Override
    public boolean causeFallDamage(float l, float d, @NotNull DamageSource source) {
        return false;
    }

    @Override
    public boolean shouldSendHitParticles() {
        return false;
    }

    @Override
    public boolean shouldSendHitSounds() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Linked", this.entityData.get(LINKED));
        compound.putString("Controller", this.entityData.get(CONTROLLER));
        compound.putInt("Ammo", this.entityData.get(AMMO));
        compound.putBoolean("KamikazeMode", this.entityData.get(IS_KAMIKAZE));
        compound.putInt("MaxAmmo", this.entityData.get(MAX_AMMO));
        compound.putString("DisplayEntity", this.entityData.get(DISPLAY_ENTITY));
        compound.putString("DisplayEntityTag", this.entityData.get(DISPLAY_ENTITY_TAG).toString());
        compound.putString("DisplayData", this.entityData.get(DISPLAY_DATA).stream().map(Object::toString).collect(Collectors.joining(",")));

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
            this.entityData.set(IS_KAMIKAZE, compound.getBoolean("KamikazeMode"));
        if (compound.contains("Item"))
            this.currentItem = ItemStack.parse(level().registryAccess(), compound.getCompound("Item"))
                    .orElse(ItemStack.EMPTY);
        if (compound.contains("MaxAmmo"))
            this.entityData.set(MAX_AMMO, compound.getInt("MaxAmmo"));
        if (compound.contains("DisplayEntity"))
            this.entityData.set(DISPLAY_ENTITY, compound.getString("DisplayEntity"));
        if (compound.contains("DisplayEntityTag"))
            this.entityData.set(DISPLAY_ENTITY_TAG, compound.getCompound("DisplayEntityTag"));
        if (compound.contains("DisplayData"))
            this.entityData.set(DISPLAY_DATA, Arrays.stream(compound.getString("DisplayData").split(",")).map(Float::valueOf).collect(Collectors.toList()));
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

        if (this.fire && this.entityData.get(AMMO) > 0) {
            if (!this.entityData.get(IS_KAMIKAZE)) {
                this.entityData.set(AMMO, this.entityData.get(AMMO) - 1);
                if (controller != null && this.level() instanceof ServerLevel) {
                    droneDrop(controller);
                }
            } else {
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
        var data = CustomData.DRONE_ATTACHMENT.get(getItemId(this.currentItem));
        if (data == null) return;
        var dropEntity = EntityType.byString(data.dropEntity())
                .map(type -> type.create(this.level()))
                .orElse(null);
        if (dropEntity == null) return;

        if (player != null && dropEntity instanceof Projectile projectile) {
            projectile.setOwner(player);
        }

        var tag = TagDataParser.parse(data.dropData(), name -> {
            if (player == null) return StringTag.valueOf(name);

            var uuid = player.getUUID();
            return switch (name) {
                case "@sbw:owner" -> NbtUtils.createUUID(uuid);
                case "@sbw:owner_string_lower" ->
                        StringTag.valueOf(uuid.toString().replace("-", "").toLowerCase(Locale.ROOT));
                case "@sbw:owner_string_upper" ->
                        StringTag.valueOf(uuid.toString().replace("-", "").toUpperCase(Locale.ROOT));
                default -> StringTag.valueOf(name);
            };
        });
        dropEntity.load(tag);

        var dropPos = data.dropPosition();
        dropEntity.setPos(this.getX() + dropPos[0], this.getY() + dropPos[1], this.getZ() + dropPos[2]);

        var vec3 = (new Vec3(0.2 * this.getDeltaMovement().x, 0.2 * this.getDeltaMovement().y, 0.2 * this.getDeltaMovement().z));
        dropEntity.setDeltaMovement(vec3);
        double d0 = vec3.horizontalDistance();
        dropEntity.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) java.lang.Math.PI)));
        dropEntity.setXRot((float) (Mth.atan2(vec3.y, d0) * (double) (180F / (float) java.lang.Math.PI)));
        dropEntity.yRotO = dropEntity.getYRot();
        dropEntity.xRotO = dropEntity.getXRot();

        this.level().addFreshEntity(dropEntity);
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
        } else if (player.isCrouching()) {
            if (stack.isEmpty() || stack.is(ModTags.Items.TOOLS_CROWBAR)) {
                // 无人机拆除
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.DRONE.get()));

                // 返还弹药
                for (int index0 = 0; index0 < this.entityData.get(AMMO); index0++) {
                    ItemHandlerHelper.giveItemToPlayer(player, this.currentItem.copy());
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
            }
        } else {
            if (stack.isEmpty()) {
                // 返还单个弹药
                int ammo = this.entityData.get(AMMO);
                if (ammo > 0) {
                    ItemHandlerHelper.giveItemToPlayer(player, this.currentItem.copy());
                    this.entityData.set(AMMO, ammo - 1);
                    if (ammo == 1) {
                        this.entityData.set(DISPLAY_ENTITY, "");
                        this.entityData.set(MAX_AMMO, 1);
                        this.entityData.set(IS_KAMIKAZE, false);
                        this.currentItem = ItemStack.EMPTY;
                    }
                }
            } else {
                // 自定义挂载
                var itemID = getItemId(stack);
                var attachmentData = CustomData.DRONE_ATTACHMENT.get(itemID);

                // 是否能挂载该物品
                if (attachmentData != null && this.entityData.get(AMMO) < attachmentData.count()) {
                    if (this.entityData.get(DISPLAY_ENTITY).equals(attachmentData.displayEntity())
                            && ItemStack.matches(this.currentItem, stack.copyWithCount(1))
                    ) {
                        // 同种物品挂载
                        this.entityData.set(AMMO, this.entityData.get(AMMO) + 1);

                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                        if (player instanceof ServerPlayer serverPlayer) {
                            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.BULLET_SUPPLY.get(), SoundSource.PLAYERS, 0.5F, 1);
                        }
                    } else if (this.entityData.get(AMMO) == 0) {
                        // 不同种物品挂载
                        this.currentItem = stack.copyWithCount(1);
                        this.entityData.set(DISPLAY_ENTITY, attachmentData.displayEntity());
                        this.entityData.set(AMMO, this.entityData.get(AMMO) + 1);
                        this.entityData.set(IS_KAMIKAZE, attachmentData.isKamikaze);
                        this.entityData.set(MAX_AMMO, attachmentData.count());

                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                        if (player instanceof ServerPlayer serverPlayer) {
                            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.BULLET_SUPPLY.get(), SoundSource.PLAYERS, 0.5F, 1);
                        }

                        var scale = attachmentData.scale();
                        var offset = attachmentData.offset();
                        var rotation = attachmentData.rotation();

                        if (attachmentData.displayData() != null) {
                            this.entityData.set(DISPLAY_ENTITY_TAG, TagDataParser.parse(attachmentData.displayData(), name -> {
                                var uuid = player.getUUID();
                                return switch (name) {
                                    case "@sbw:owner" -> NbtUtils.createUUID(uuid);
                                    case "@sbw:owner_string_lower" ->
                                            StringTag.valueOf(uuid.toString().replace("-", "").toLowerCase(Locale.ROOT));
                                    case "@sbw:owner_string_upper" ->
                                            StringTag.valueOf(uuid.toString().replace("-", "").toUpperCase(Locale.ROOT));
                                    default -> StringTag.valueOf(name);
                                };
                            }));
                        }

                        this.entityData.set(DISPLAY_DATA, List.of(
                                scale[0], scale[1], scale[2],
                                offset[0], offset[1], offset[2],
                                rotation[0], rotation[1], rotation[2],
                                attachmentData.xLength, attachmentData.zLength,
                                (float) attachmentData.tickCount
                        ));
                    }
                }
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

            this.setDeltaMovement(this.getDeltaMovement().multiply(0.965, 0.7, 0.965));
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
            setDeltaMovement(new Vec3(getDeltaMovement().x, 0.05 * holdTickY, getDeltaMovement().z));
        } else if (down) {
            holdTickY++;
            this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.02f * Math.min(holdTickY, 5), this.onGround() ? 0 : 0.06f));
            setDeltaMovement(new Vec3(getDeltaMovement().x, -0.05 * holdTickY, getDeltaMovement().z));
        } else {
            holdTickY = 0;
        }

        if (!(up || down)) {
            if (this.getDeltaMovement().y() < 0) {
                this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.005f, 0.2f));
            } else {
                this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - (this.onGround() ? 0.0005f : 0.005f), 0.02f));
            }
        }

        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * 0.7f);
        this.entityData.set(DELTA_X_ROT, this.entityData.get(DELTA_X_ROT) * 0.7f);

        this.setZRot(Mth.clamp(this.getRoll() - this.entityData.get(DELTA_ROT), -30, 30));
        this.setBodyXRot(Mth.clamp(this.getBodyPitch() - this.entityData.get(DELTA_X_ROT), -30, 30));

        setDeltaMovement(getDeltaMovement().add(0.0f, this.entityData.get(POWER) * 0.6, 0.0f));

        Vector3f direction = getRightDirection().mul(this.entityData.get(DELTA_ROT));
        setDeltaMovement(getDeltaMovement().add(new Vec3(direction.x, direction.y, direction.z).scale(0.017)));

        Vector3f directionZ = getForwardDirection().mul(-this.entityData.get(DELTA_X_ROT));
        setDeltaMovement(getDeltaMovement().add(new Vec3(directionZ.x, directionZ.y, directionZ.z).scale(0.017)));

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
            var attachedEntity = this.entityData.get(DISPLAY_ENTITY);
            if (!attachedEntity.isEmpty() && 20 * lastTickSpeed > this.getHealth()) {
                var data = CustomData.DRONE_ATTACHMENT.get(getItemId(this.currentItem));
                if (data != null) {
                    if (data.isKamikaze) {
                        EntityType.byString(attachedEntity).ifPresent(entityType -> {
                            var bomb = entityType.create(this.level());
                            DamageHandler.doDamage(target, ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), bomb, player), data.hitDamage);
                            target.invulnerableTime = 0;
                        });
                    } else {
                        DamageHandler.doDamage(target, ModDamageTypes.causeDroneHitDamage(this.level().registryAccess(), this, player), (float) (5 * lastTickSpeed));
                    }
                }

                if (player != null && player.getMainHandItem().is(ModItems.MONITOR.get())) {
                    var stack = player.getMainHandItem();
                    var tag = NBTTool.getTag(stack);
                    Monitor.disLink(tag, player);
                    NBTTool.saveTag(stack, tag);
                }
            }
            this.hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.EXPLOSION), Objects.requireNonNullElse(player, this)), (float) ((!this.entityData.get(DISPLAY_ENTITY).isEmpty() ? 20 : 4) * lastTickSpeed));
        }
    }

    @Override
    public boolean engineRunning() {
        return Math.abs(this.entityData.get(POWER)) > 0.05;
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.DRONE_SOUND.get();
    }

    @Override
    public float getEngineSoundVolume() {

        if (Math.abs(this.entityData.get(POWER)) <= 0.05) {
            return 0;
        }

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

    static String getItemId(ItemStack stack) {
        return stack.getItem().toString();
    }

    @Override
    @Nullable
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.DRONE.get());
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

        var data = CustomData.DRONE_ATTACHMENT.get(getItemId(this.currentItem));
        if (data != null) {
            if (data.isKamikaze) {
                kamikazeExplosion();
            } else {
                if (this.level() instanceof ServerLevel) {
                    int count = this.entityData.get(AMMO);
                    for (int i = 0; i < count; i++) {
                        droneDrop(controller);
                    }
                }
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

    private void kamikazeExplosion() {
        Entity attacker = EntityFindUtil.findEntity(this.level(), this.entityData.get(LAST_ATTACKER_UUID));
        Player controller = EntityFindUtil.findPlayer(this.level(), this.entityData.get(CONTROLLER));

        assert controller != null;

        // 挂载实体的数据
        var attachedEntity = this.entityData.get(DISPLAY_ENTITY);
        if (attachedEntity.isEmpty()) return;

        var data = CustomData.DRONE_ATTACHMENT.get(getItemId(this.currentItem));
        if (data == null) return;

        var bomb = EntityType.byString(attachedEntity)
                .map(entityType -> entityType.create(this.level()))
                .orElse(null);
        if (bomb == null) return;

        float radius = data.explosionRadius;
        ParticleTool.ParticleType particleType;

        if (radius < 4) {
            particleType = ParticleTool.ParticleType.SMALL;
        } else if (radius >= 4 && radius < 10) {
            particleType = ParticleTool.ParticleType.MEDIUM;
        } else if (radius >= 10 && radius < 20) {
            particleType = ParticleTool.ParticleType.HUGE;
        } else {
            particleType = ParticleTool.ParticleType.GIANT;
        }

        createCustomExplosion()
                .source(bomb)
                .attacker(attacker)
                .damage(data.explosionDamage)
                .radius(radius)
                .withParticleType(particleType)
                .explode();

        // TODO 药水迫击炮炮弹
//        if (mode == 1) {
//            ParticleTool.spawnMediumExplosionParticles(this.level(), this.position());
//
//            if (this.currentItem.getItem() instanceof MortarShell) {
//                this.createAreaCloud(this.currentItem.get(DataComponents.POTION_CONTENTS), this.level(), ExplosionConfig.DRONE_KAMIKAZE_EXPLOSION_DAMAGE.get(), ExplosionConfig.DRONE_KAMIKAZE_EXPLOSION_RADIUS.get());
//            }
//        }
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

    public float getRotX(float tickDelta) {
        return this.getPitch(tickDelta);
    }

    public float getRotY(float tickDelta) {
        return this.getYaw(tickDelta);
    }

    public float getRotZ(float tickDelta) {
        return this.getRoll(tickDelta);
    }

    public Matrix4f getClientVehicleTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo, getY()), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees((float) (-Mth.lerp(ticks, yRotO, getYRot()) + freeCameraYaw)));
        transform.rotate(Axis.XP.rotationDegrees((float) (Mth.lerp(ticks, xRotO, getXRot()) + freeCameraPitch)));
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(ticks, prevRoll, getRoll())));
        return transform;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        return new Vec2((float) (getRotY(partialTicks) - freeCameraYaw), (float) (getRotX(partialTicks) + freeCameraPitch));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        Matrix4f transform = getClientVehicleTransform(partialTicks);
        Vector4f maxCameraPosition = transformPosition(transform, 0, 0.75f, -2 - 0.2f * (float) ClientMouseHandler.custom3pDistanceLerp);
        return CameraTool.getMaxZoom(transform, maxCameraPosition);
    }

    public Entity getController() {
        return EntityFindUtil.findPlayer(this.level(), this.entityData.get(CONTROLLER));
    }

    @Override
    public boolean hasEnergyStorage() {
        return false;
    }
}
