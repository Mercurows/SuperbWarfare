package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.block.VehicleAssemblingTableBlock;
import com.atsuishio.superbwarfare.block.property.BlockPart;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.menu.VehicleAssemblingMenu;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isFreeCam;
import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraPitch;
import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraYaw;

public class VehicleAssemblingTableVehicleEntity extends VehicleEntity implements GeoEntity, HasCustomInventoryScreen, MenuProvider {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public float deltaXo;
    public float deltaYo;
    public float deltaX;
    public float deltaY;
    public int jumpCooldown;

    public VehicleAssemblingTableVehicleEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public VehicleAssemblingTableVehicleEntity(PlayMessages.SpawnEntity packet, Level level) {
        this(ModEntities.VEHICLE_ASSEMBLING_TABLE.get(), level);
    }

    public VehicleAssemblingTableVehicleEntity(Level world) {
        this(ModEntities.VEHICLE_ASSEMBLING_TABLE.get(), world);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(ModSounds.WHEEL_STEP.get(), (float) (getDeltaMovement().length() * 2), random.nextFloat() * 0.1f + 1f);
    }

    // 变回方块
    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        if (player.getMainHandItem().is(ModTags.Items.TOOLS_CROWBAR) && !player.isCrouching()) {
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
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.vehicle_assembling_table.warn").withStyle(ChatFormatting.RED), true);
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

        deltaX = getMouseMoveSpeedY();
        if (this.leftInputDown() && this.rightInputDown()) {
            deltaX = 0;
        } else if (this.leftInputDown()) {
            deltaX = -1;
        } else if (this.rightInputDown()) {
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

        this.terrainCompact(1.95f, 1.95f);
        this.refreshDimensions();
    }

    @Override
    public void travel() {
        Entity passenger = this.getFirstPassenger();

        this.entityData.set(POWER, this.entityData.get(POWER) * 0.95f);
        if (passenger == null || isInWater()) {
            setLeftInputDown(false);
            setRightInputDown(false);
            setForwardInputDown(false);
            setBackInputDown(false);
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.96, 1, 0.96));
        } else if (passenger instanceof Player) {

            if (forwardInputDown()) {
                this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.1f, 1f));
            }

            this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * 0.8f);

            if (backInputDown()) {
                this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - (this.entityData.get(POWER) > 0 ? 0.1f : 0.01f), onGround() ? -0.2f : 0.2f));
                if (rightInputDown()) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 0.4f);
                } else if (leftInputDown()) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 0.4f);
                }
            } else {
                if (rightInputDown()) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 0.4f);
                } else if (this.leftInputDown()) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 0.4f);
                }
            }

            // Shift刹车
            if (downInputDown()) {
                this.entityData.set(POWER, 0f);
            }

            // 跳
            if (upInputDown() && onGround() && jumpCooldown == 0) {
                jumpCooldown = 40;
                if (this.level() instanceof ServerLevel server) {
                    server.playSound(null, this.getOnPos(), ModSounds.WHEEL_CHAIR_JUMP.get(), SoundSource.PLAYERS, 2, 1);
                }
                var movement = this.getForward()
                        .multiply(1, 0, 1)
                        .normalize()
                        .scale(0.7);
                this.setDeltaMovement(getDeltaMovement().add(movement.x, 1, movement.z));
            }

            float diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(passenger.getYHeadRot() - this.getYRot()));
            float diffX = Math.clamp(-60f, 60f, Mth.wrapDegrees(passenger.getXRot() - this.getXRot()));

            float addX = Mth.clamp(Math.min((float) Math.max(getDeltaMovement().length() - 0.1, 0.01), 0.9f) * diffX, -4, 4);
            float addZ = this.entityData.get(DELTA_ROT) - (this.onGround() ? 0 : 0.01f) * diffY * (float) getDeltaMovement().length();

            float yRotSync = (float) (-Mth.clamp(50 * this.getDeltaMovement().length(), 2, 4) * this.entityData.get(DELTA_ROT));

            this.setYRot(this.getYRot() + yRotSync);
            this.setXRot(Mth.clamp(this.getXRot() + addX, onGround() ? -12 : -120, onGround() ? 3 : 120));
            this.setZRot(this.getRoll() - 0.2f * addZ);
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

    @Override
    public float getEngineSoundVolume() {
        return (float) getDeltaMovement().length();
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

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return 0.3;
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
    public boolean hasEnergyStorage() {
        return false;
    }

    @Override
    public void openCustomInventoryScreen(@NotNull Player player) {
        player.openMenu(this);
    }

    @Override
    @ParametersAreNonnullByDefault
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new VehicleAssemblingMenu(i, inventory, true);
    }
}
