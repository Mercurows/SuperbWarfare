package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSerializers;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.ArtilleryIndicator;
import com.atsuishio.superbwarfare.item.FiringParameters;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.atsuishio.superbwarfare.tools.RangeTool.calculateLaunchVector;

public class ArtilleryEntity extends GeoVehicleEntity {
    public static final EntityDataAccessor<List<Integer>> BARREL_ANIM = SynchedEntityData.defineId(ArtilleryEntity.class, ModSerializers.INT_LIST_SERIALIZER.get());
    public static final EntityDataAccessor<Vector3f> SHOOT_VEC = SynchedEntityData.defineId(ArtilleryEntity.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Boolean> DEPRESSED = SynchedEntityData.defineId(ArtilleryEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Vector3f> TARGET_POS = SynchedEntityData.defineId(ArtilleryEntity.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Integer> RADIUS = SynchedEntityData.defineId(ArtilleryEntity.class, EntityDataSerializers.INT);

    public ArtilleryEntity(EntityType<?> type, Level world) {
        super(type, world);

        this.entityData.set(BARREL_ANIM, newIntList(Math.max(4, this.getMaxBarrel())));
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        var gunData = getGunData("Main");
        if (gunData == null) return InteractionResult.SUCCESS;

        ItemStack stack = player.getMainHandItem();

        if (stack.getItem() instanceof ArtilleryIndicator indicator) {
            if (player.getRootVehicle() == this) return InteractionResult.FAIL;

            return indicator.bind(stack, player, this);
        }

        if (stack.is(ModTags.Items.TOOLS_CROWBAR) && !player.isShiftKeyDown()) {
            if (gunData.ammo.get() > 0 && player.level() instanceof ServerLevel) {
                vehicleShoot(player, "Main");
            }
            return InteractionResult.SUCCESS;
        }

        if (player.getMainHandItem().getItem() == ModItems.FIRING_PARAMETERS.get() && player.isShiftKeyDown()) {
            setTarget(player.getMainHandItem(), player, "Main");
            return InteractionResult.SUCCESS;
        }

        if (player.getOffhandItem().getItem() == ModItems.FIRING_PARAMETERS.get() && player.isShiftKeyDown()) {
            setTarget(player.getOffhandItem(), player, "Main");
            return InteractionResult.SUCCESS;
        }

        return super.interact(player, hand);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        this.entityData.set(SHOOT_VEC, getForward().toVector3f());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(SHOOT_VEC, getForward().toVector3f())
                .define(DEPRESSED, false)
                .define(TARGET_POS, new Vector3f())
                .define(RADIUS, 0)
                .define(BARREL_ANIM, newIntList(4));
    }

    protected static List<Integer> newIntList(int size) {
        var list = new ArrayList<Integer>();
        for (int i = 0; i < size; i++) {
            list.add(0);
        }
        return list;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("ShootVecX", this.entityData.get(SHOOT_VEC).x);
        compound.putFloat("ShootVecY", this.entityData.get(SHOOT_VEC).y);
        compound.putFloat("ShootVecZ", this.entityData.get(SHOOT_VEC).z);

        compound.putBoolean("Depressed", this.entityData.get(DEPRESSED));
        compound.putInt("Radius", this.entityData.get(RADIUS));
        compound.putFloat("TargetX", this.entityData.get(TARGET_POS).x);
        compound.putFloat("TargetY", this.entityData.get(TARGET_POS).y);
        compound.putFloat("TargetZ", this.entityData.get(TARGET_POS).z);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("ShootVecX") && compound.contains("ShootVecY") && compound.contains("ShootVecZ")) {
            this.entityData.set(SHOOT_VEC, new Vector3f(compound.getFloat("ShootVecX"), compound.getFloat("ShootVecY"), compound.getFloat("ShootVecZ")));
        }
        if (compound.contains("Depressed")) {
            this.entityData.set(DEPRESSED, compound.getBoolean("Depressed"));
        }
        if (compound.contains("Radius")) {
            this.entityData.set(RADIUS, compound.getInt("Radius"));
        }
        if (compound.contains("TargetX") && compound.contains("TargetY") && compound.contains("TargetZ")) {
            this.entityData.set(TARGET_POS, new Vector3f(compound.getFloat("TargetX"), compound.getFloat("TargetX"), compound.getFloat("TargetZ")));
        }
    }

    public void setTarget(ItemStack stack, Entity entity, String weaponName) {
        var data = getGunData(weaponName);
        if (data == null) return;

        var parameters = stack.getOrDefault(ModDataComponents.FIRING_PARAMETERS, new FiringParameters.Parameters());

        double targetX = parameters.pos().getX();
        double targetY = parameters.pos().getY();
        double targetZ = parameters.pos().getZ();
        boolean canAim = true;

        entityData.set(TARGET_POS, new Vector3f((float) targetX, (float) targetY, (float) targetZ));
        entityData.set(DEPRESSED, parameters.isDepressed());
        entityData.set(RADIUS, parameters.radius());
        Vec3 randomPos = VectorTool.randomPos(new Vec3(entityData.get(TARGET_POS)), entityData.get(RADIUS));
        Vec3 launchVector = calculateLaunchVector(getShootPos(weaponName, 1), randomPos, getProjectileVelocity(weaponName), getProjectileGravity(weaponName), entityData.get(DEPRESSED));

        Component component = Component.literal("");
        Component location = Component.translatable("tips.superbwarfare.mortar.position", this.getDisplayName())
                .append(Component.literal(" X:" + FormatTool.format0D(getX()) + " Y:" + FormatTool.format0D(getY()) + " Z:" + FormatTool.format0D(getZ()) + " "));

        if (launchVector == null) {
            canAim = false;
            component = Component.translatable("tips.superbwarfare.mortar.out_of_range");
        } else {
            float angle = (float) -VehicleVecUtils.getXRotFromVector(launchVector);
            if (angle < -getTurretMaxPitch() || angle > -getTurretMinPitch()) {
                canAim = false;
                component = Component.translatable("tips.superbwarfare.mortar.warn", this.getDisplayName());
                if (angle < -getTurretMaxPitch()) {
                    component = Component.translatable("tips.superbwarfare.ballistics.warn");
                }
            }
        }

        if (canAim) {
            entityData.set(SHOOT_VEC, launchVector.toVector3f());
        } else if (entity instanceof Player player) {
            player.displayClientMessage(location.copy().append(component).withStyle(ChatFormatting.RED), false);
        }
    }

    public void resetTarget(String weaponName) {
        var data = getGunData(weaponName);
        if (data == null) return;

        Vec3 randomPos = VectorTool.randomPos(new Vec3(entityData.get(TARGET_POS)), entityData.get(RADIUS));
        Vec3 launchVector = calculateLaunchVector(getShootPos(weaponName, 1), randomPos, getProjectileVelocity(weaponName), getProjectileGravity(weaponName), entityData.get(DEPRESSED));

        if (launchVector == null) {
            return;
        }
        float angle = (float) -VehicleVecUtils.getXRotFromVector(launchVector);
        if (angle > -getTurretMaxPitch() && angle < -getTurretMinPitch()) {
            entityData.set(SHOOT_VEC, launchVector.toVector3f());
        }
    }

    public int getMaxBarrel() {
        var data = getGunData("Main");
        if (data != null) {
            return data.compute().magazine;
        } else {
            return 1;
        }
    }

    @Override
    public void baseTick() {
        super.baseTick();
        for (int i = 0; i < getMaxBarrel(); i++) {
            var animCounters = this.entityData.get(BARREL_ANIM);
            if (i < animCounters.size() && animCounters.get(i) > 0) {
                animCounters.set(i, animCounters.get(i) - 1);
                entityData.set(BARREL_ANIM, animCounters, true);
            }
        }

        // TODO 替换装弹逻辑？
        var gunData = getGunData("Main");
        if (gunData != null && level() instanceof ServerLevel && getNthEntity(getTurretControllerIndex()) instanceof Player player) {
            var ammoCount = InventoryTool.countItem(player, gunData.selectedAmmoConsumer().stack().getItem());
            if (ammoCount > 0) {
                var inStack = this.items.getFirst();
                int count = inStack.getCount();

                if (count < Math.min(this.getMaxStackSize(), inStack.getMaxStackSize())) {
                    this.setItem(0, gunData.selectedAmmoConsumer().stack().copyWithCount(count + 1));
                    InventoryTool.consumeItem(player, gunData.selectedAmmoConsumer().stack().getItem(), 1);
                }
            }
        }
        var controller = getNthEntity(getTurretControllerIndex());

        if (controller != null) {
            entityData.set(SHOOT_VEC, controller.getViewVector(1).toVector3f());
        } else {

            turretAutoAimFromVector(new Vec3(entityData.get(SHOOT_VEC)));
        }
    }

    @Override
    public void vehicleShoot(LivingEntity living, String weaponName) {
        beforeShoot(living);
        super.vehicleShoot(living, weaponName);
    }

    @Override
    public void vehicleShoot(LivingEntity living, UUID uuid, Vec3 targetPos) {
        beforeShoot(living);
        super.vehicleShoot(living, uuid, targetPos);
    }

    public void beforeShoot(LivingEntity living) {
        var data = getGunData("Main");
        if (data != null && data.ammo.get() > 0) {
            var barrelAnim = entityData.get(BARREL_ANIM);
            barrelAnim.set(data.ammo.get() - 1, data.compute().shootAnimationTime);
            entityData.set(BARREL_ANIM, barrelAnim, true);
        }
        if (living.level() instanceof ServerLevel level) {
            ParticleTool.spawnBigCannonMuzzleParticles(getShootVec("Main", 1), getShootPos("Main", 1), level, this);
        }
    }
}
