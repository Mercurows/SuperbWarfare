package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.projectile.MediumRocketItem;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.util.ArrayList;
import java.util.List;

public class SodayoPickUpRocketEntity extends ArtilleryEntity {
    public static final EntityDataAccessor<List<Integer>> LOADED_AMMO = SynchedEntityData.defineId(SodayoPickUpRocketEntity.class, ModSerializers.INT_LIST_SERIALIZER.get());
    public OBB body1;
    public OBB body2;
    public OBB body3;

    public OBB[] barrel = new OBB[12];
    public OBB turret;
    public OBB wheelLF;
    public OBB wheelRF;
    public OBB wheelLB;
    public OBB wheelRB;
    public int cooldown;

    public SodayoPickUpRocketEntity(EntityType<SodayoPickUpRocketEntity> type, Level world) {
        super(type, world);

        this.body1 = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(1.1875, 0.5, 3.41), new Quaterniond(), OBB.Part.BODY);
        this.body2 = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(1.1875, 0.375, 0.1875), new Quaterniond(), OBB.Part.BODY);
        this.body3 = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(1.1875, 0.094, 0.34375), new Quaterniond(), OBB.Part.BODY);

        this.wheelLF = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.25, 0.5, 0.5), new Quaterniond(), OBB.Part.WHEEL_LEFT);
        this.wheelLB = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.25, 0.5, 0.5), new Quaterniond(), OBB.Part.WHEEL_LEFT);

        this.wheelRF = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.25, 0.5, 0.5), new Quaterniond(), OBB.Part.WHEEL_RIGHT);
        this.wheelRB = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.25, 0.5, 0.5), new Quaterniond(), OBB.Part.WHEEL_RIGHT);

        this.barrel[0] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[1] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[2] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[3] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[4] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[5] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[6] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[7] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[8] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[9] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[10] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[11] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);

        this.turret = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.4765625f, 0.3515625f, 0.7578125f), new Quaterniond(), OBB.Part.TURRET);
    }

    @Override
    public @NotNull DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.25f) * damage);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        var list = new IntArrayList();
        for (int i = 0; i < this.getContainerSize(); i++) {
            list.add(-1);
        }
        builder.define(LOADED_AMMO, list);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setChanged();
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        var stack = player.getMainHandItem();
        var lookingObb = OBB.getLookingObb(player, player.entityInteractionRange());

        if (stack.isEmpty()) {
            // 取出炮弹
            player.swing(InteractionHand.MAIN_HAND);
            if (level() instanceof ServerLevel serverLevel && cooldown == 0) {
                for (int i = 0; i < this.barrel.length; i++) {
                    if (lookingObb == this.barrel[i]) {
                        if (getItems().get(i).isEmpty()) {
                            return super.interact(player, hand);
                        } else {
                            player.addItem(getItems().get(i).copyWithCount(1));
                            Vec3 vec3 = OBB.vector3dToVec3(this.barrel[i].center);
                            serverLevel.playSound(null, vec3.x, vec3.y, vec3.z, ModSounds.TYPE_63_RELOAD.get(), SoundSource.PLAYERS, 1f, random.nextFloat() * 0.1f + 0.9f);
                            cooldown = 5;
                            getItems().set(i, ItemStack.EMPTY);
                            setChanged();
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
        }

        if (stack.getItem() instanceof MediumRocketItem) {
            for (int i = 0; i < this.barrel.length; i++) {
                if (lookingObb == this.barrel[i] && getItems().get(i).isEmpty() && level() instanceof ServerLevel serverLevel && cooldown == 0) {
                    this.setItem(i, stack.copyWithCount(1));
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    Vec3 vec3 = OBB.vector3dToVec3(this.barrel[i].center);
                    serverLevel.playSound(null, vec3.x, vec3.y, vec3.z, ModSounds.TYPE_63_RELOAD.get(), SoundSource.PLAYERS, 1f, random.nextFloat() * 0.1f + 0.9f);
                    cooldown = 5;
                    setChanged();
                }
                player.swing(InteractionHand.MAIN_HAND);
            }
            return InteractionResult.SUCCESS;
        }

        if (cooldown == 0 && !player.isShiftKeyDown() && (stack.is(ModTags.Items.TOOLS_CROWBAR) || stack.is(Items.FLINT_AND_STEEL))) {
            // 发射
            if (lookingAtBarrel(player)) {
                // 精准发射
                for (int i = 0; i < this.barrel.length; i++) {
                    if (lookingObb == this.barrel[i] && getItems().get(i).getItem() instanceof MediumRocketItem) {
                        cooldown = 10;
                        shoot(player, i);
                        getItems().set(i, ItemStack.EMPTY);
                        setChanged();
                    }
                }
                player.swing(InteractionHand.MAIN_HAND);
                return InteractionResult.SUCCESS;
            } else {
                // 顺序发射
                for (int i = 0; i < 12; i++) {
                    if (getItems().get(i).getItem() instanceof MediumRocketItem) {
                        cooldown = 10;
                        shoot(player, i);
                        getItems().set(i, ItemStack.EMPTY);
                        setChanged();

                        player.swing(InteractionHand.MAIN_HAND);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }

        return super.interact(player, hand);
    }

    public boolean lookingAtBarrel(Player player) {
        var lookingObb = OBB.getLookingObb(player, player.entityInteractionRange());

        for (int i = 0; i < 12; i++) {
            if (lookingObb == barrel[i]) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canBind() {
        return true;
    }

    @Override
    public void vehicleShoot(@Nullable LivingEntity living, @NotNull String name) {
        if (this.isWreck()) return;
        // 顺序发射
        for (int i = 0; i < 12; i++) {
            if (getItems().get(i).getItem() instanceof MediumRocketItem && living instanceof Player player && cooldown == 0) {
                shoot(player, i);
                cooldown = 3;
                getItems().set(i, ItemStack.EMPTY);
                setChanged();
            }
        }
    }

    public void shoot(Player player, int i) {
        ItemStack stack = getItems().get(i);

        if (!(stack.getItem() instanceof MediumRocketItem rocketItem)) {
            return;
        }

        var gunData = getGunData(rocketItem.type.toString());
        if (gunData == null) return;

        float shootVelocity = getProjectileVelocity(gunData);
        float shootSpread = getProjectileSpread(gunData);
        float shootGravity = getProjectileGravity(gunData);

        OBB obb = this.barrel[i];
        Vec3 shootPos = OBB.vector3dToVec3(obb.center);

        var entityToSpawn = new MediumRocketEntity(ModEntities.MEDIUM_ROCKET.get(), shootPos.x, shootPos.y, shootPos.z, level(),
                gunData.get(GunProp.DAMAGE).floatValue(), gunData.get(GunProp.EXPLOSION_RADIUS).floatValue(), gunData.get(GunProp.EXPLOSION_DAMAGE).floatValue(),
                0, 0, rocketItem.type, gunData.get(GunProp.SPREAD_AMOUNT), gunData.get(GunProp.SPREAD_ANGLE));
        entityToSpawn.durability(gunData.get(GunProp.AP_DURABILITY));
        entityToSpawn.setGravity(shootGravity);
        entityToSpawn.setOwner(player);

        var barrelVector = getBarrelVector(1);
        entityToSpawn.shoot(barrelVector.x, barrelVector.y, barrelVector.z, shootVelocity, shootSpread);
        level().addFreshEntity(entityToSpawn);

        level().playSound(null, shootPos.x, shootPos.y, shootPos.z, gunData.get(GunProp.SOUND_INFO).fire3P, SoundSource.PLAYERS, gunData.get(GunProp.SOUND_RADIUS).floatValue(), random.nextFloat() * 0.1f + 0.95f);

        AABB ab = new AABB(getBoundingBox().getCenter(), getBoundingBox().getCenter()).inflate(0.75).move(barrelVector.scale(-2)).expandTowards(barrelVector.scale(-5));

        // 尾焰
        for (var entity : level().getEntities(EntityTypeTest.forClass(Entity.class), ab, target -> target != this)) {
            entity.hurt(ModDamageTypes.causeBurnDamage(entity.level().registryAccess(), player), 30 - 2 * entity.distanceTo(this));
            double force = 4 - 0.7 * entity.distanceTo(this);
            entity.push(-force * barrelVector.x, -force * barrelVector.y, -force * barrelVector.z);
        }

        if (level() instanceof ServerLevel serverLevel) {
            ParticleTool.spawnMediumCannonMuzzleParticles(barrelVector.scale(-1), shootPos.add(barrelVector.scale(-0.5)), serverLevel, this);
            ParticleTool.spawnMediumCannonMuzzleParticles(barrelVector.scale(-1), shootPos.add(barrelVector.scale(-1.5)), serverLevel, this);
            ParticleTool.spawnMediumCannonMuzzleParticles(barrelVector, shootPos.add(barrelVector.scale(1.5)), serverLevel, this);
        }

        gunData.shakePlayers(this);
    }

    @Override
    public void baseTick() {
        super.baseTick();

        if (decoyInputDown()) {
            horn();
        }

        if (cooldown > 0) {
            cooldown--;
        }

        this.refreshDimensions();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return false;
    }

    @Override
    public @NotNull List<OBB> getOBBs() {
        return List.of(this.barrel[0], this.barrel[1], this.barrel[2], this.barrel[3], this.barrel[4], this.barrel[5], this.barrel[6], this.barrel[7], this.barrel[8], this.barrel[9], this.barrel[10], this.barrel[11],
                this.turret, this.body1, this.body2, this.body3, this.wheelLB, this.wheelRB, this.wheelLF, this.wheelRF);
    }

    @Override
    public void updateOBB() {
        Matrix4d transform = getVehicleTransform(1);

        Vector4d worldPositionBody1 = transformPosition(transform, 0, 1, 0.593);
        this.body1.center.set(new Vector3f((float) worldPositionBody1.x, (float) worldPositionBody1.y, (float) worldPositionBody1.z));
        this.body1.updateRotation(VectorTool.combineRotations(1, this));

        Vector4d worldPositionBody2 = transformPosition(transform, 0, 1.875, 0.375);
        this.body2.center.set(new Vector3f((float) worldPositionBody2.x, (float) worldPositionBody2.y, (float) worldPositionBody2.z));
        this.body2.updateRotation(VectorTool.combineRotations(1, this));

        Vector4d worldPositionBody3 = transformPosition(transform, 0, 2.15625, 0.9);
        this.body3.center.set(new Vector3f((float) worldPositionBody3.x, (float) worldPositionBody3.y, (float) worldPositionBody3.z));
        this.body3.updateRotation(VectorTool.combineRotations(1, this));

        Vector4d worldPositionWheelLF = transformPosition(transform, 1, 0.5, 2.875);
        this.wheelLF.center.set(new Vector3f((float) worldPositionWheelLF.x, (float) worldPositionWheelLF.y, (float) worldPositionWheelLF.z));
        this.wheelLF.updateRotation(VectorTool.combineRotations(1, this));

        Vector4d worldPositionWheelRF = transformPosition(transform, -1, 0.5, 2.875);
        this.wheelRF.center.set(new Vector3f((float) worldPositionWheelRF.x, (float) worldPositionWheelRF.y, (float) worldPositionWheelRF.z));
        this.wheelRF.updateRotation(VectorTool.combineRotations(1, this));

        Vector4d worldPositionWheelLB = transformPosition(transform, 1, 0.5, -1.28);
        this.wheelLB.center.set(new Vector3f((float) worldPositionWheelLB.x, (float) worldPositionWheelLB.y, (float) worldPositionWheelLB.z));
        this.wheelLB.updateRotation(VectorTool.combineRotations(1, this));

        Vector4d worldPositionWheelRB = transformPosition(transform, -1, 0.5, -1.28);
        this.wheelRB.center.set(new Vector3f((float) worldPositionWheelRB.x, (float) worldPositionWheelRB.y, (float) worldPositionWheelRB.z));
        this.wheelRB.updateRotation(VectorTool.combineRotations(1, this));

        Matrix4d transformB = getBarrelTransform(1);

        double i = 0.24375f;

        setBarrelOBB(0, -0.3659375, 0.244375);
        setBarrelOBB(1, -0.3659375 + i, 0.244375);
        setBarrelOBB(2, -0.3659375 + 2 * i, 0.244375);
        setBarrelOBB(3, -0.3659375 + 3 * i, 0.244375);
        setBarrelOBB(4, -0.3659375, 0.244375 - i);
        setBarrelOBB(5, -0.3659375 + i, 0.244375 - i);
        setBarrelOBB(6, -0.3659375 + 2 * i, 0.244375 - i);
        setBarrelOBB(7, -0.3659375 + 3 * i, 0.244375 - i);
        setBarrelOBB(8, -0.3659375, 0.244375 - 2 * i);
        setBarrelOBB(9, -0.3659375 + i, 0.244375 - 2 * i);
        setBarrelOBB(10, -0.3659375 + 2 * i, 0.244375 - 2 * i);
        setBarrelOBB(11, -0.3659375 + 3 * i, 0.244375 - 2 * i);

        Vector4d worldPositionTurret = transformPosition(transformB, 0, 0, 0.3740625);
        this.turret.center.set(new Vector3f((float) worldPositionTurret.x, (float) worldPositionTurret.y, (float) worldPositionTurret.z));
        this.turret.updateRotation(VectorTool.combineRotationsBarrel(1, this));
    }

    private void setBarrelOBB(int index, double x, double y) {
        Vector4d vec = transformPosition(getBarrelTransform(1), x, y, -0.44625);
        this.barrel[index].center.set(new Vector3f((float) vec.x, (float) vec.y, (float) vec.z));
        this.barrel[index].updateRotation(VectorTool.combineRotationsBarrel(1, this));
    }

    @Override
    public void setChanged() {
        super.setChanged();
        var list = new ArrayList<Integer>();
        for (var item : this.getItems()) {
            if (item.getItem() instanceof MediumRocketItem mediumRocketItem) {
                list.add(mediumRocketItem.type.ordinal());
            } else {
                list.add(-1);
            }
        }
        this.entityData.set(LOADED_AMMO, list);
    }
}
