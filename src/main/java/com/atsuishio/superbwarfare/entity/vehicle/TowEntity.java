package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class TowEntity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity {

    // 是否已装填弹药
    public static final EntityDataAccessor<Boolean> LOADED = SynchedEntityData.defineId(TowEntity.class, EntityDataSerializers.BOOLEAN);
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
        this.entityData.define(LOADED, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("State", this.entityData.get(LOADED));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(LOADED, compound.getBoolean("State"));
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        var gunData = getGunData(0);
        if (gunData == null) return InteractionResult.SUCCESS;

        var stack = player.getMainHandItem();
        if (gunData.hasEnoughAmmoToShoot(player)) {
            entityData.set(LOADED, true);
            return super.interact(player, hand);
        }

        if (!entityData.get(LOADED)) {
            if (!gunData.selectedAmmoConsumer().isAmmoItem(stack)) {
                return super.interact(player, hand);
            }

            if (level() instanceof ServerLevel serverLevel) {
                modifyGunData(0, data -> data.reloadAmmo(player));
                entityData.set(LOADED, true);

                serverLevel.playSound(null, getOnPos(), ModSounds.TYPE_63_RELOAD.get(), SoundSource.PLAYERS, 1f, random.nextFloat() * 0.1f + 0.9f);
            }
        } else {
            entityData.set(LOADED, false);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Entity getAmmoSupplier() {
        return getFirstPassenger();
    }

    @Override
    public @NotNull List<ItemStack> getRetrieveItems() {
        var list = new ArrayList<ItemStack>();
        list.add(new ItemStack(ModItems.TOW_DEPLOYER.get()));

        var data = getGunData(0);
        if (entityData.get(LOADED) && data != null) {
            var stack = data.selectedAmmoConsumer().stack().copyWithCount(data.withdrawAmmoCount());
            if (!stack.isEmpty()) {
                list.add(stack.copy());
            }
        }

        return list;
    }

    @Override
    public void vehicleShoot(LivingEntity living, int seat) {
        super.vehicleShoot(living, seat);

        var barrelVector = getBarrelVector(1);

        var pos = getShootPos(living, 1).add(barrelVector.scale(-0.5));
        var ab = new AABB(pos, pos).inflate(0.75).move(barrelVector.scale(-2)).expandTowards(barrelVector.scale(-5));

        // 尾焰伤害
        for (var entity : level().getEntities(EntityTypeTest.forClass(Entity.class), ab,
                target -> target != this && target != getFirstPassenger() && target.getVehicle() == null)
        ) {
            entity.hurt(ModDamageTypes.causeBurnDamage(entity.level().registryAccess(), living), 30 - 2 * entity.distanceTo(this));
            double force = 4 - 0.7 * entity.distanceTo(this);
            entity.push(-force * barrelVector.x, -force * barrelVector.y, -force * barrelVector.z);
        }

        // 粒子效果
        if (level() instanceof ServerLevel serverLevel) {
            ParticleTool.spawnMediumCannonMuzzleParticles(barrelVector.scale(-1), pos, serverLevel, this);
            ParticleTool.spawnMediumCannonMuzzleParticles(barrelVector, pos, serverLevel, this);
            for (int j = 0; j < 20; j += 4) {
                Mod.queueServerWork(j, () -> ParticleTool.spawnBarrelSmoke(1, serverLevel, barrelVector, getShootPos(living, 1).add(barrelVector.scale(1))));
            }
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
}
