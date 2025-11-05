package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.WgMissileWeapon;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class TowEntity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity {

    // 0：无弹无筒，1：有弹有筒，2：无弹有筒（发射后）
    public static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(TowEntity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TowEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.TOW.get(), world);
    }

    public TowEntity(EntityType<TowEntity> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, 0);
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("State", this.entityData.get(STATE));

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(STATE, compound.getInt("State"));

    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();
        if (entityData.get(STATE) == 1) {
            return super.interact(player, hand);
        } else if (entityData.get(STATE) == 0) {
            if (stack.is(ModItems.TOW_MISSILE.get())) {
                entityData.set(STATE, 1);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                level().playSound(null, getOnPos(), ModSounds.TYPE_63_RELOAD.get(), SoundSource.PLAYERS, 1f, random.nextFloat() * 0.1f + 0.9f);
                return InteractionResult.SUCCESS;
            } else {
                return super.interact(player, hand);
            }
        } else {
            entityData.set(STATE, 0);
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public @NotNull List<ItemStack> getRetrieveItems() {
        var list = new ArrayList<ItemStack>();
        list.add(new ItemStack(ModItems.TOW_DEPLOYER.get()));
        return list;
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        return new Vec3(0, Math.min(super.getDeltaMovement().y, 0), 0);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }
    }

    @Override
    public void vehicleShoot(LivingEntity living) {
        if (entityData.get(STATE) != 1) return;
        var wgMissileEntity = ((WgMissileWeapon) getWeapon(0)).create(living);

        wgMissileEntity.setPos(getShootPos(living, 1).x, getShootPos(living, 1).y, getShootPos(living, 1).z);
        wgMissileEntity.shoot(getBarrelVector(1).x, getBarrelVector(1).y, getBarrelVector(1).z, 2, 0f);
        wgMissileEntity.setLauncherVehicle(this.uuid);
        living.level().addFreshEntity(wgMissileEntity);

        Vec3 pos = getShootPos(living, 1).add(getBarrelVector(1).scale(-0.5));

        AABB ab = new AABB(pos, pos).inflate(0.75).move(getBarrelVector(1).scale(-2)).expandTowards(getBarrelVector(1).scale(-5));

        for (var entity : level().getEntities(EntityTypeTest.forClass(Entity.class), ab,
                target -> target != this && target != getFirstPassenger() && target.getVehicle() == null)
        ) {
            entity.hurt(ModDamageTypes.causeBurnDamage(entity.level().registryAccess(), living), 30 - 2 * entity.distanceTo(this));
            double force = 4 - 0.7 * entity.distanceTo(this);
            entity.push(-force * getBarrelVector(1).x, -force * getBarrelVector(1).y, -force * getBarrelVector(1).z);
        }

        if (level() instanceof ServerLevel serverLevel) {
            ParticleTool.spawnMediumCannonMuzzleParticles(getBarrelVector(1).scale(-1), pos, serverLevel, this);
            ParticleTool.spawnMediumCannonMuzzleParticles(getBarrelVector(1), pos, serverLevel, this);
            for (int j = 0; j < 20; j += 4) {
                Mod.queueServerWork(j, () -> ParticleTool.spawnBarrelSmoke(1, serverLevel, getBarrelVector(1), getShootPos(living, 1).add(getBarrelVector(1).scale(1))));
            }
        }

        this.entityData.set(STATE, this.getEntityData().get(STATE) + 1);
    }


    @Override
    public void travel() {
        Entity passenger = this.getFirstPassenger();
        if (passenger != null) {

            float diffY = Mth.wrapDegrees(passenger.getYHeadRot() - this.getYRot());
            float diffX = Mth.wrapDegrees(passenger.getXRot() - this.getXRot());

            this.setYRot(this.getYRot() + Mth.clamp(0.9f * diffY, -90f, 90f));
            this.setXRot(Mth.clamp(this.getXRot() + Mth.clamp(0.9f * diffX, -90f, 90f), -40, 40));
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
    public boolean canShoot(LivingEntity living) {
        return entityData.get(STATE) == 1;
    }

    @Override
    public int zoomFov() {
        return 3;
    }

    @Override
    public int getWeaponHeat(LivingEntity living) {
        return 0;
    }

    @Override
    public Vec3 getBarrelVector(float pPartialTicks) {
        return getViewVector(pPartialTicks);
    }

    @Override
    public Vec3 zoomPos(Entity entity, float ticks) {
        Matrix4f transform = getVehicleFlatTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0.2535875f, 1.33235625f, 0.121875f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 getShootPos(int seatIndex, float ticks) {
        return new Vec3(getX(), getY() + 1.174775f, getZ());
    }

    @Override
    public @NotNull Vec3 getDismountLocationForIndex(LivingEntity passenger, int index) {
        return new Vec3(passenger.getX(), getY(), passenger.getZ());
    }

    @Override
    public void destroy() {
        if (this.level() instanceof ServerLevel level) {
            var x = this.getX();
            var y = this.getY();
            var z = this.getZ();
            level.explode(null, x, y, z, 0, Level.ExplosionInteraction.NONE);
            ItemEntity mortar = new ItemEntity(level, x, (y + 1), z, new ItemStack(ModItems.MORTAR_BARREL.get()));
            mortar.setPickUpDelay(10);
            level.addFreshEntity(mortar);
        }
        super.destroy();
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return zoom ? 0.2 : 0.27;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (isFirstPerson || zoom) {
            return new Vec2(Mth.lerp(partialTicks, yRotO, getYRot()), Mth.lerp(partialTicks, xRotO, getXRot()));
        }
        return super.getCameraRotation(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (isFirstPerson || zoom) {
            return zoomPos(player, partialTicks);
        }
        return super.getCameraPosition(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean useFixedCameraPos(Entity entity) {
        return true;
    }

    @Override
    public boolean hasEnergyStorage() {
        return false;
    }
}
