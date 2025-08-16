package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.VehicleAssemblingTableBlock;
import com.atsuishio.superbwarfare.block.property.BlockPart;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.*;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isFreeCam;
import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraPitch;
import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraYaw;

public class VehicleAssemblingTableVehicleEntity extends MobileVehicleEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float yRotSync;

    public float deltaXo;
    public float deltaYo;

    public float deltaX;
    public float deltaY;

    public int jumpCooldown;

    public VehicleAssemblingTableVehicleEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public VehicleAssemblingTableVehicleEntity(Level world) {
        this(ModEntities.VEHICLE_ASSEMBLING_TABLE.get(), world);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(ModSounds.WHEEL_STEP.get(), (float) (getDeltaMovement().length() * 0.3), random.nextFloat() * 0.1f + 1f);
    }

    // 变回方块
    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        if (player.getMainHandItem().is(ModTags.Items.CROWBAR) && !player.isCrouching()) {
            if (!this.level().isClientSide && this.getPassengers().isEmpty()) {
                var facing = getDirection();
                var currentPos = this.position();
                var targetPos = switch (facing) {
                    case WEST -> currentPos.add(-0.5, 0, -0.5);
                    case EAST -> currentPos.add(0.5, 0, 0.5);
                    case NORTH -> currentPos.add(0.5, 0, -0.5);
                    case SOUTH -> currentPos.add(-0.5, 0, 0.5);
                    default -> currentPos;  // this should never happen
                };
                var targetBlockPos = BlockPos.containing(targetPos);

                var canPlace = true;
                for (var part : BlockPart.values()) {
                    var blockPos = part.relative(targetBlockPos, facing);
                    var blockState = this.level().getBlockState(blockPos);
                    if (!blockState.canBeReplaced()) {
                        canPlace = false;
                        break;
                    }
                }

                if (canPlace) {
                    for (var part : BlockPart.values()) {
                        var blockPos = part.relative(targetBlockPos, facing);
                        var state = ModBlocks.VEHICLE_ASSEMBLING_TABLE.get().defaultBlockState()
                                .setValue(VehicleAssemblingTableBlock.FACING, facing)
                                .setValue(VehicleAssemblingTableBlock.BLOCK_PART, part);

                        this.level().setBlock(blockPos, state, 3);
                    }

                    this.discard();
                    return InteractionResult.SUCCESS;
                } else {
                    // TODO 优化提示
                    player.displayClientMessage(Component.literal("Cannot place!"), true);
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        }
        return super.interact(player, hand);
    }

    @Override
    public void baseTick() {
        deltaXo = deltaX;
        deltaYo = deltaY;
        super.baseTick();

        if (jumpCooldown > 0) {
            jumpCooldown--;
        }

        deltaX = entityData.get(MOUSE_SPEED_Y);
        if (this.leftInputDown && this.rightInputDown) {
            deltaX = 0;
        } else if (this.leftInputDown) {
            deltaX = -1;
        } else if (this.rightInputDown) {
            deltaX = 1;
        }

        float f = onGround() ? 0.85f : 0.9f;
        this.setDeltaMovement(this.getDeltaMovement().multiply(f, f, f));

        if (this.isInWater() && this.tickCount % 4 == 0) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.6, 0.6));
            if (lastTickSpeed > 0.4) {
                this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, this.getFirstPassenger() == null ? this : this.getFirstPassenger()), (float) (20 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))));
            }
        }

        // TODO 正确实现地形适应？
        this.terrainCompact(1f, 1.2f);
        this.refreshDimensions();
    }

    // TODO 调整鼠标转向灵敏度？
    @Override
    public void travel() {
        Entity passenger = this.getFirstPassenger();

        this.entityData.set(POWER, this.entityData.get(POWER) * 0.95f);
        if (passenger == null || isInWater()) {
            this.leftInputDown = false;
            this.rightInputDown = false;
            this.forwardInputDown = false;
            this.backInputDown = false;
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.96, 1, 0.96));
        } else if (passenger instanceof Player) {

            if (forwardInputDown) {
                this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.1f, 1f));
            }

            if (backInputDown) {
                this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - (this.entityData.get(POWER) > 0 ? 0.1f : 0.01f), onGround() ? -0.2f : 0.2f));
            }

            // Shift刹车
            if (downInputDown) {
                this.entityData.set(POWER, 0f);
            }

            // 跳
            if (upInputDown && onGround() && jumpCooldown == 0) {
                jumpCooldown = 50;
                if (this.level() instanceof ServerLevel server) {
                    server.playSound(null, this.getOnPos(), ModSounds.WHEEL_CHAIR_JUMP.get(), SoundSource.PLAYERS, 1, 1);
                }
                var movement = this.getForward()
                        .multiply(1, 0, 1)
                        .normalize()
                        .scale(0.7);
                this.setDeltaMovement(getDeltaMovement().add(movement.x, 1, movement.z));
            }

            float diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(passenger.getYHeadRot() - this.getYRot()));
            float diffX = Math.clamp(-60f, 60f, Mth.wrapDegrees(passenger.getXRot() - this.getXRot()));

            float roll = Mth.abs(Mth.clamp(getRoll() / 60, -1.5f, 1.5f));

            float addY = Mth.clamp(Math.min((this.onGround() ? 1.5f : 0.9f) * (float) Math.max(getDeltaMovement().length() - 0.06, 0.1), 0.9f) * diffY - 0.5f * this.entityData.get(DELTA_ROT), -3 * (roll + 1), 3 * (roll + 1));
            float addX = Mth.clamp(Math.min((float) Math.max(getDeltaMovement().length() - 0.1, 0.01), 0.9f) * diffX, -4, 4);
            float addZ = this.entityData.get(DELTA_ROT) - (this.onGround() ? 0 : 0.01f) * diffY * (float) getDeltaMovement().length();

            float i = getXRot() / 90;

            yRotSync = addY * (1 - Mth.abs(i)) + addZ * i;

            this.setYRot(this.getYRot() + yRotSync);
            this.setXRot(Mth.clamp(this.getXRot() + addX, onGround() ? -12 : -120, onGround() ? 3 : 120));
            this.setZRot(this.getRoll() - addZ * (1 - Mth.abs(i)));
        }

        double powerValue = 0.05 * this.entityData.get(POWER);
        this.setDeltaMovement(this.getDeltaMovement().add(getForward()
                .multiply(1, 0, 1)
                .normalize()
                .multiply(powerValue, powerValue, powerValue))
        );
    }

    @Override
    public boolean engineRunning() {
        return (getFirstPassenger() != null && Math.abs(getDeltaMovement().length()) > 0);
    }

    // TODO 音效？
    @Override
    public SoundEvent getEngineSound() {
        return SoundEvents.EMPTY;
    }

    @Override
    public float getEngineSoundVolume() {
        return (float) getDeltaMovement().length();
    }

    protected void clampRotation(Entity entity) {
        float f = Mth.wrapDegrees(entity.getXRot() - this.getXRot());
        float f1 = Mth.clamp(f, -85.0F, 60F);
        entity.xRotO += f1 - f;
        entity.setXRot(entity.getXRot() + f1 - f);

        entity.setYBodyRot(this.getYRot());
        float f2 = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
        float f3 = Mth.clamp(f2, -45.0F, 45.0F);
        entity.yRotO += f3 - f2;
        entity.setYRot(entity.getYRot() + f3 - f2);
        entity.setYBodyRot(this.getYRot());
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entity) {
        this.clampRotation(entity);
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        // From Immersive_Aircraft
        if (!this.hasPassenger(passenger)) {
            return;
        }

        Matrix4f transform = getVehicleTransform(1);

        float x = -0.4f;
        float y = -1.55f + (float) passenger.getVehicleAttachmentPoint(this).y;
        float z = 0.2f;

        int i = this.getSeatIndex(passenger);

        if (i == 0) {
            Vector4f worldPosition = transformPosition(transform, x, y, z);
            passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z);
        }

        if (passenger != this.getFirstPassenger()) {
            passenger.setXRot(passenger.getXRot() + (getXRot() - xRotO));
        }

        copyEntityData(passenger);
    }

    public void copyEntityData(Entity entity) {
        float i = getXRot() / 90;
        float f = Mth.wrapDegrees(entity.getYRot() - getYRot());
        float g = Mth.clamp(f, -105.0f, 105.0f);

        entity.yRotO += g - f;
        entity.setYRot(entity.getYRot() + g - f + yRotSync * Mth.abs(i));
        entity.setYHeadRot(entity.getYRot());
        entity.setYBodyRot(getYRot());
    }

    @Override
    public Matrix4f getVehicleTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo + 0.5f, getY() + 0.5f), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        transform.rotate(Axis.XP.rotationDegrees(Mth.lerp(ticks, xRotO, getXRot())));
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(ticks, prevRoll, getRoll())));
        return transform;
    }

    @Override
    public void destroy() {
        super.destroy();

        if (level() instanceof ServerLevel) {
            var item = new ItemEntity(level(), this.getX(), this.getY(), this.getZ(), new ItemStack(ModItems.VEHICLE_ASSEMBLING_TABLE.get()));
            item.setPickUpDelay(50);
            this.level().addFreshEntity(item);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // TODO 图标
    @Override
    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/vehicle_icon/tom_6_icon.png");
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return 0.3;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public Pair<Quaternionf, Quaternionf> getPassengerRotation(Entity entity, float tickDelta) {
        return Pair.of(Axis.XP.rotationDegrees(-this.getViewXRot(tickDelta)), Axis.ZP.rotationDegrees(-this.getRoll(tickDelta)));
    }

    @Override
    public @Nullable ResourceLocation getVehicleItemIcon() {
        return Mod.loc("textures/gui/vehicle/type/aircraft.png");
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (isFreeCam(player) && this.getSeatIndex(player) == 0 && Mth.abs((float) (freeCameraYaw * freeCameraPitch)) > 0.01) {
            return new Vec2((float) (getYaw(partialTicks) - 0.5f * Mth.lerp(partialTicks, deltaYo, deltaY) - freeCameraYaw), (float) (getPitch(partialTicks) - 0.5f * Mth.lerp(partialTicks, deltaXo, deltaX) + freeCameraPitch));
        }

        return super.getCameraRotation(partialTicks, player, false, false);
    }

    @Override
    public @NotNull List<ItemStack> getRetrieveItems() {
        return List.of(new ItemStack(ModItems.VEHICLE_ASSEMBLING_TABLE.get()));
    }

    @Override
    public @Nullable ThirdPersonCameraPosition getThirdPersonCameraPosition(int seatIndex) {
        return new ThirdPersonCameraPosition(1.5 * ClientMouseHandler.custom3pDistanceLerp, 0, 0);
    }

    @Override
    public boolean isEnclosed(int index) {
        return true;
    }

    @Override
    public boolean hasEnergyStorage() {
        return false;
    }
}
