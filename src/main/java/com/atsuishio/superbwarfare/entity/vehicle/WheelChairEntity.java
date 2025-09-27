package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.advancement.CriteriaRegister;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.quatern.QuaternionHelper;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.OBB;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class WheelChairEntity extends VehicleEntity implements GeoEntity, OBBEntity {
    public OBB obb;
    // 使用更清晰的命名
    private float pitchVelocity = 0;  // 绕X轴 - 俯仰
    private float yawVelocity = 0;    // 绕Y轴 - 偏航
    private float rollVelocity = 0;   // 绕Z轴 - 滚转

    private final float ANGULAR_DAMPING = 0.95f;
    private final float MAX_ANGULAR_VELOCITY = 2.0f;

    // 当前和目标旋转
    private QuaternionHelper currentRotation = QuaternionHelper.IDENTITY;
    private QuaternionHelper targetRotation = QuaternionHelper.IDENTITY;
    private QuaternionHelper prevRotation = QuaternionHelper.IDENTITY;

    // 插值
    private float rotationLerp = 1.0f;
    private final float LERP_SPEED = 0.2f;

    @Override
    public boolean hasMenu() {
        return false;
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public int jumpCoolDown;
    public int handBusyTime;

    public WheelChairEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.WHEEL_CHAIR.get(), world);
    }

    public WheelChairEntity(EntityType<WheelChairEntity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(0.5f, 0.5f, 0.5f), new Quaternionf(), OBB.Part.BODY);
        this.currentRotation = QuaternionHelper.IDENTITY;
        this.targetRotation = QuaternionHelper.IDENTITY;
        this.prevRotation = QuaternionHelper.IDENTITY;
    }

    @Override
    public void playerTouch(Player pPlayer) {
        if (this.position().distanceTo(pPlayer.position()) > 1.4 || pPlayer == this.getFirstPassenger()) return;
        if (!this.level().isClientSide
                && pPlayer.getY() < this.getY() + this.getBbHeight()
                && pPlayer.getY() + pPlayer.getBbHeight() > this.getY()
        ) {
            double entitySize = pPlayer.getBbWidth() * pPlayer.getBbHeight();
            double thisSize = this.getBbWidth() * this.getBbHeight();
            double f = Math.min(entitySize / thisSize, 2);
            this.setDeltaMovement(this.getDeltaMovement().add(new Vec3(pPlayer.position().vectorTo(this.position()).toVector3f()).scale(0.5 * f * pPlayer.getDeltaMovement().length())));
            this.setYRot(pPlayer.getYHeadRot());
        }
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(0.5 * ClientMouseHandler.custom3pDistanceLerp, 0, 0);
    }

    @Override
    public boolean shouldSendHitParticles() {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(ModSounds.WHEEL_STEP.get(), (float) (getDeltaMovement().length() * 0.3), random.nextFloat() * 0.15f + 1);
    }

    @Override
    public void baseTick() {
        if (jumpCoolDown > 0 && onGround()) {
            jumpCoolDown--;
        }

        if (handBusyTime > 0) {
            handBusyTime--;
        }

        super.baseTick();

        this.updateOBB();
        if (this.onGround()) {
            float f = (float) Mth.clamp(0.85f + 0.05f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90, 0.01, 0.99);
            this.setDeltaMovement(this.getDeltaMovement().multiply(f, 0.99, f));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.99, 0.99, 0.99));
        }
        this.setSprinting(this.getDeltaMovement().horizontalDistance() > 0.15);
        attractEntity();
        this.terrainCompact(0.9f, 1.2f);
        inertiaRotate(10f);

        this.refreshDimensions();

        // 保存上一帧旋转用于插值
        prevRotation = currentRotation;

        // 处理物理和输入
        updateVehiclePhysics();
        handleControlInput();

        // 应用旋转
        applyRotation();

//        // 更新Minecraft的欧拉角（用于兼容性）
//        updateEulerAngles();
    }

    public boolean hasEnoughSpaceFor(Entity pEntity) {
        return pEntity.getBbWidth() < this.getBbWidth();
    }

    public void attractEntity() {
        List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0.2F, -0.01F, 0.2F));
        if (!list.isEmpty()) {
            boolean flag = !this.level().isClientSide && !(this.getControllingPassenger() instanceof Player);

            for (Entity entity : list) {
                if (!entity.hasPassenger(this) && flag && !entity.isPassenger() && this.hasEnoughSpaceFor(entity) && (entity instanceof LivingEntity || entity instanceof MortarEntity) && !(entity instanceof WaterAnimal) && !(entity instanceof Player)) {
                    entity.startRiding(this);
                }
            }
        }
    }

    @Override
    protected void addPassenger(@NotNull Entity pPassenger) {
        super.addPassenger(pPassenger);

        if (pPassenger instanceof ServerPlayer player
                && (player.getMainHandItem().getItem() == ModItems.ELECTRIC_BATON.get()
                || player.getOffhandItem().getItem() == ModItems.ELECTRIC_BATON.get())
        ) {
            CriteriaRegister.OTTO_SPRINT.trigger(player);
        }
    }

    @Override
    public void travel() {

        applyYawTorque(1f);
        applyPitchTorque(0.3f);
        applyRollTorque(0.2f);


//        Entity passenger = this.getFirstPassenger();
//
//        float diffY = 0;
//
//        if (passenger == null) {
//            this.leftInputDown = false;
//            this.rightInputDown = false;
//            this.forwardInputDown = false;
//            this.backInputDown = false;
//        } else if (passenger instanceof Player) {
//            diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(passenger.getYHeadRot() - this.getYRot()));
//            this.setYRot(this.getYRot() + Mth.clamp(0.4f * diffY, -5f, 5f));
//
//            float direct = (90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90;
//            this.setZRot((float) (this.getRoll() + direct * diffY * 0.2 * this.getDeltaMovement().length()));
//        }
//
//        if (this.forwardInputDown) {
//            this.entityData.set(POWER, this.entityData.get(POWER) + (sprintInputDown ? 0.02f : 0.01f));
//            if (this.getEnergy() <= 0 && passenger instanceof Player player) {
//                moveWithOutPower(player, true);
//            }
//        }
//
//        if (this.backInputDown) {
//            this.entityData.set(POWER, this.entityData.get(POWER) - 0.01f);
//            if (this.getEnergy() <= 0 && passenger instanceof Player player) {
//                moveWithOutPower(player, false);
//            }
//        }
//
//        if (this.upInputDown && this.onGround() && this.getEnergy() > 400 && jumpCoolDown == 0) {
//            if (passenger instanceof ServerPlayer serverPlayer) {
//                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.WHEEL_CHAIR_JUMP.get(), SoundSource.PLAYERS, 1, 1);
//            }
//            this.consumeEnergy(VehicleConfig.WHEELCHAIR_JUMP_ENERGY_COST.get());
//            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.6, 0));
//            jumpCoolDown = 3;
//        }
//
//        if (this.forwardInputDown || this.backInputDown) {
//            this.consumeEnergy(VehicleConfig.WHEELCHAIR_MOVE_ENERGY_COST.get());
//        }
//
//        if (passenger instanceof Player player && player.level().isClientSide && this.handBusyTime > 0) {
//            var localPlayer = Minecraft.getInstance().player;
//            if (localPlayer != null && player.getUUID().equals(localPlayer.getUUID())) {
//                localPlayer.handsBusy = true;
//            }
//        }
//
//        this.entityData.set(POWER, this.entityData.get(POWER) * 0.87f);
//
//        double s0 = getDeltaMovement().dot(this.getViewVector(1));
//
//        this.setLeftWheelRot((float) (this.getLeftWheelRot() - 1.25 * s0) - 0.015f * Mth.clamp(0.4f * diffY, -5f, 5f));
//        this.setRightWheelRot((float) (this.getRightWheelRot() - 1.25 * s0) + 0.015f * Mth.clamp(0.4f * diffY, -5f, 5f));
//
//        float power = this.entityData.get(POWER) * Mth.clamp(1 + (s0 > 0 ? 1 : -1) * getXRot() / 35, 0, 2);
//        this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale((this.onGround() ? 1 : 0.1) * power)));
    }

    public void moveWithOutPower(Player player, boolean forward) {
        this.entityData.set(POWER, this.entityData.get(POWER) + (forward ? 0.015f : -0.015f));
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.BOAT_PADDLE_LAND, SoundSource.PLAYERS, 1, 1);
        }
        player.causeFoodExhaustion(0.03F);

        this.handBusyTime = 4;
        this.forwardInputDown = false;
        this.backInputDown = false;
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.WHEEL_CHAIR_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return getEnergy() > 0 ? entityData.get(POWER) : 0;
    }

    protected void clampRotation(Entity entity) {
        entity.setYBodyRot(this.getYRot());
        float f2 = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
        float f3 = Mth.clamp(f2, -90F, 90.0F);
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

        float x = 0f;
        float y = 0.35f;
        float z = 0f;

        int i = this.getSeatIndex(passenger);

        if (i == 0) {
            Vector4f worldPosition = transformPosition(transform, x, y, z);
            passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z);
        }

        if (passenger != this.getFirstPassenger()) {
            passenger.setXRot(passenger.getXRot() + (getXRot() - xRotO));
        }
    }

    @Override
    public float rotateYOffset() {
        return 0.4f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/vehicle_icon/wheel_chair_icon.png");
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public Pair<Quaternionf, Quaternionf> getPassengerRotation(Entity entity, float tickDelta) {
        return Pair.of(Axis.XP.rotationDegrees(-this.getViewXRot(tickDelta)), Axis.ZP.rotationDegrees(-this.getRoll(tickDelta)));
    }

    @Override
    public @Nullable ResourceLocation getVehicleItemIcon() {
        return Mod.loc("textures/gui/vehicle/type/otto.png");
    }

    @Override
    public VehicleType getVehicleType() {
        return VehicleType.CAR;
    }

    private void updateVehiclePhysics() {
        // 应用阻尼
        pitchVelocity *= ANGULAR_DAMPING;
        yawVelocity *= ANGULAR_DAMPING;
        rollVelocity *= ANGULAR_DAMPING;

        // 限制最大角速度
        pitchVelocity = Mth.clamp(pitchVelocity, -MAX_ANGULAR_VELOCITY, MAX_ANGULAR_VELOCITY);
        yawVelocity = Mth.clamp(yawVelocity, -MAX_ANGULAR_VELOCITY, MAX_ANGULAR_VELOCITY);
        rollVelocity = Mth.clamp(rollVelocity, -MAX_ANGULAR_VELOCITY, MAX_ANGULAR_VELOCITY);
    }

    private void handleControlInput() {
        // 这里处理玩家输入，可以根据你的控制方案修改
        // 示例：根据按键应用扭矩
    }

    private void applyRotation() {
        // 如果角速度很小，直接使用目标旋转
        if (Math.abs(pitchVelocity) < 0.01f &&
                Math.abs(yawVelocity) < 0.01f &&
                Math.abs(rollVelocity) < 0.01f) {

            if (rotationLerp < 1.0f) {
                rotationLerp += LERP_SPEED;
                currentRotation = prevRotation.slerp(targetRotation, rotationLerp);
            } else {
                currentRotation = targetRotation;
            }
            return;
        }

        // 根据角速度创建旋转增量
        // 注意：这里使用局部坐标系下的旋转
        QuaternionHelper deltaRotation = createRotationDelta();

        // 应用旋转增量（在局部坐标系中）
        targetRotation = targetRotation.multiply(deltaRotation).normalize();

        // 开始插值
        rotationLerp = 0f;

        // 直接插值到新旋转
        if (rotationLerp < 1.0f) {
            rotationLerp += LERP_SPEED;
            currentRotation = prevRotation.slerp(targetRotation, rotationLerp);
        } else {
            currentRotation = targetRotation;
        }
    }

    /**
     * 创建基于角速度的旋转增量（在载具的局部坐标系中）
     */
    private QuaternionHelper createRotationDelta() {
        // 计算帧时间相关的旋转角度
        float deltaPitch = pitchVelocity * 0.05f;
        float deltaYaw = yawVelocity * 0.05f;
        float deltaRoll = rollVelocity * 0.05f;

        // 创建各个轴的旋转四元数
        QuaternionHelper pitchRot = QuaternionHelper.fromPitch(deltaPitch);
        QuaternionHelper yawRot = QuaternionHelper.fromYaw(deltaYaw);
        QuaternionHelper rollRot = QuaternionHelper.fromRoll(deltaRoll);

        // 正确的旋转顺序：先Roll，再Pitch，最后Yaw（ZYX顺序）
        // 这是载具控制的常用顺序
        return rollRot.multiply(pitchRot).multiply(yawRot);
    }

    private void updateEulerAngles() {
        Vector3f euler = currentRotation.toEulerAngles();

        // 转换为度
        float pitchDeg = (float) Math.toDegrees(euler.x());
        float yawDeg = (float) Math.toDegrees(euler.y());
        float rollDeg = (float) Math.toDegrees(euler.z());

        // 设置到实体（Minecraft使用Yaw和Pitch）
        this.setYRot(-yawDeg);
        this.setXRot(pitchDeg);
        this.setZRot(rollDeg);
    }

    // ========== 修正后的控制方法 ==========

    /**
     * 应用俯仰扭矩（绕X轴）- 前后倾斜
     */
    public void applyPitchTorque(float torque) {
        this.pitchVelocity += torque;
    }

    /**
     * 应用偏航扭矩（绕Y轴）- 左右转向
     */
    public void applyYawTorque(float torque) {
        this.yawVelocity += torque;
    }

    /**
     * 应用滚转扭矩（绕Z轴）- 左右滚转
     */
    public void applyRollTorque(float torque) {
        this.rollVelocity += torque;
    }

    /**
     * 直接设置目标欧拉角
     */
    public void setTargetEulerAngles(float pitch, float yaw, float roll) {
        QuaternionHelper newRotation = QuaternionHelper.fromVehicleEulerAngles(
                (float) Math.toRadians(pitch),
                (float) Math.toRadians(yaw),
                (float) Math.toRadians(roll)
        );
        setTargetRotation(newRotation);
    }

    /**
     * 直接设置目标四元数旋转
     */
    public void setTargetRotation(QuaternionHelper rotation) {
        this.targetRotation = rotation.normalize();
        this.rotationLerp = 0f; // 开始插值
    }

    /**
     * 立即设置旋转（无插值）
     */
    public void setRotationImmediate(QuaternionHelper rotation) {
        this.targetRotation = rotation.normalize();
        this.currentRotation = targetRotation;
        this.rotationLerp = 1.0f;
        updateEulerAngles();
    }

    /**
     * 获取用于渲染的插值旋转
     */
    public QuaternionHelper getRenderRotation(float partialTicks) {
        if (partialTicks == 1.0f || rotationLerp >= 1.0f) {
            return currentRotation;
        }

        float interpolatedLerp = rotationLerp - LERP_SPEED + LERP_SPEED * partialTicks;
        interpolatedLerp = Mth.clamp(interpolatedLerp, 0, 1);

        return prevRotation.slerp(currentRotation, interpolatedLerp);
    }

    /**
     * 获取当前欧拉角（度）
     */
    public Vector3f getEulerAnglesDeg() {
        Vector3f eulerRad = currentRotation.toEulerAngles();
        return new Vector3f(
                (float) Math.toDegrees(eulerRad.x()),
                (float) Math.toDegrees(eulerRad.y()),
                (float) Math.toDegrees(eulerRad.z())
        );
    }

    /**
     * 获取角速度
     */
    public Vector3f getAngularVelocity() {
        return new Vector3f(pitchVelocity, yawVelocity, rollVelocity);
    }

    // ========== 辅助方法 ==========

    /**
     * 稳定载具（减少旋转）
     */
    public void stabilize() {
        float stability = 0.1f;
        Vector3f euler = getEulerAnglesDeg();

        // 施加反向扭矩使载具回归水平
        applyPitchTorque(-euler.x() * stability);
        applyRollTorque(-euler.z() * stability);
    }

    /**
     * 重置为水平状态（保持当前偏航）
     */
    public void levelOut() {
        Vector3f euler = getEulerAnglesDeg();
        setTargetEulerAngles(0, euler.y(), 0);
    }

    /**
     * 完全重置旋转
     */
    public void resetRotation() {
        setTargetEulerAngles(0, this.getYRot(), 0);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
    }

    // 添加NBT保存和加载方法
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("VehicleRotation")) {
            CompoundTag rotationTag = compound.getCompound("VehicleRotation");
            float x = rotationTag.getFloat("x");
            float y = rotationTag.getFloat("y");
            float z = rotationTag.getFloat("z");
            float w = rotationTag.getFloat("w");
            this.currentRotation = new QuaternionHelper(x, y, z, w);
            this.targetRotation = this.currentRotation;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        CompoundTag rotationTag = new CompoundTag();
        rotationTag.putFloat("x", currentRotation.getX());
        rotationTag.putFloat("y", currentRotation.getY());
        rotationTag.putFloat("z", currentRotation.getZ());
        rotationTag.putFloat("w", currentRotation.getW());
        compound.put("VehicleRotation", rotationTag);
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Quaternionf quaternionf = new Quaternionf(currentRotation.getX(), currentRotation.getY(), currentRotation.getZ(), currentRotation.getW());

        Vector4f worldPosition = transformPosition(transform, 0, rotateYOffset(), 0);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(quaternionf);

    }
}
