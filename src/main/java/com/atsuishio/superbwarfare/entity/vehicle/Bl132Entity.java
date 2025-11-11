package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.CannonShellWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import com.mojang.math.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Bl132Entity extends VehicleEntity implements GeoEntity, CannonEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Bl132Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.BL_132.get(), world);
    }

    public Bl132Entity(EntityType<Bl132Entity> type, Level world) {
        super(type, world);
    }



    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(8 + ClientMouseHandler.custom3pDistanceLerp, 1, 0);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 1f) * damage);
    }


    @Override
    public Matrix4f getBarrelTransform(float partialTicks) {
        Matrix4f transformT = getVehicleFlatTransform(partialTicks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0, 2.625f, -0.39375f);

        transformT.translate(worldPosition.x, worldPosition.y, worldPosition.z);

        transformT.rotate(Axis.XP.rotationDegrees(getPitch(partialTicks)));
        return transformT;
    }

    @Override
    public Vec3 getZoomPos(Entity entity, float partialTicks) {
        Matrix4f transform = getBarrelTransform(partialTicks);
        Vector4f worldPosition = transformPosition(transform, 0, 0.6f, 0);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }



    @Override
    public void vehicleShoot(LivingEntity living, int seat) {
        shoot(living, false);
    }

    public void shoot(LivingEntity living, boolean reset) {
        if (getFirstPassenger() != null && getFirstPassenger() != living) return;

        if (living.level() instanceof ServerLevel) {
            if (getAmmoCount(living) == 0 && !InventoryTool.hasCreativeAmmoBox(getFirstPassenger())) return;
            Matrix4f transform = getBarrelTransform(1);

            // 左上炮管

            if (!(getAmmoCount(living) == 0 && !InventoryTool.hasCreativeAmmoBox(getFirstPassenger()))) {
                Vector4f worldPositionL = transformPosition(transform, 1.24625f, 0.5625f, 0);
                summonShell(new Vec3(worldPositionL.x, worldPositionL.y, worldPositionL.z), living, 0.05f);
            }

            // 右上炮管
            Mod.queueServerWork(2, () -> {
                if (getAmmoCount(living) == 0 && !InventoryTool.hasCreativeAmmoBox(getFirstPassenger())) return;
                Vector4f worldPositionR = transformPosition(transform, -1.24625f, 0.5625f, 0);
                summonShell(new Vec3(worldPositionR.x, worldPositionR.y, worldPositionR.z), living, 0.1f);
            });

            // 左下炮管
            Mod.queueServerWork(4, () -> {
                if (getAmmoCount(living) == 0 && !InventoryTool.hasCreativeAmmoBox(getFirstPassenger())) return;
                Vector4f worldPositionLL = transformPosition(transform, 1.24625f, -0.5625f, 0);
                summonShell(new Vec3(worldPositionLL.x, worldPositionLL.y, worldPositionLL.z), living, 0.15f);
            });

            // 右下炮管
            Mod.queueServerWork(6, () -> {
                if (getAmmoCount(living) == 0 && !InventoryTool.hasCreativeAmmoBox(getFirstPassenger())) return;
                Vector4f worldPositionRL = transformPosition(transform, -1.24625f, -0.5625f, 0);
                summonShell(new Vec3(worldPositionRL.x, worldPositionRL.y, worldPositionRL.z), living, 0.2f);
            });


            if (living instanceof ServerPlayer serverPlayer) {
                if (serverPlayer == getFirstPassenger()) {
                    Mod.queueServerWork(70, () -> SoundTool.playLocalSound(serverPlayer, ModSounds.BL_132_RELOAD.get(), 2, 1));
                }
            }


            ShakeClientMessage.sendToNearbyPlayers(this, 20, 15, 15, 45);

        }
    }

    public void summonShell(Vec3 pos, LivingEntity living, float spread) {
        if (living.level() instanceof ServerLevel level) {
            var entityToSpawnLeft = ((CannonShellWeapon) getWeapon(0)).create(living);

            entityToSpawnLeft.setPos(pos.x, pos.y, pos.z);
            entityToSpawnLeft.shoot(this.getLookAngle().x, this.getLookAngle().y, this.getLookAngle().z, 15, spread);
            level.addFreshEntity(entityToSpawnLeft);

            ParticleTool.spawnBigCannonMuzzleParticles(getLookAngle(), new Vec3(pos.x, pos.y, pos.z).add(getLookAngle().scale(7)), level, this);

            for (int i = 0; i < 40; i += 4) {
                Mod.queueServerWork(i, () -> ParticleTool.spawnBarrelSmoke(1, level, getLookAngle(), new Vec3(pos.x, pos.y, pos.z).add(getLookAngle().scale(7))));
            }
        }
    }


//    @Override
//    public void travel() {
//        Entity passenger = this.getFirstPassenger();
//        if (passenger instanceof Player) {
//            entityData.set(YAW, passenger.getYHeadRot());
//            entityData.set(PITCH, passenger.getXRot() - 2f);
//        }
//
//        float diffY = Mth.wrapDegrees(entityData.get(YAW) - this.getYRot());
//        float diffX = Mth.wrapDegrees(entityData.get(PITCH) - this.getXRot());
//
//        turretTurnSound(diffX, diffY, 0.95f);
//
//        this.setYRot(this.getYRot() + Mth.clamp(0.5f * diffY, -1.25f, 1.25f));
//        this.setXRot(Mth.clamp(this.getXRot() + Mth.clamp(0.5f * diffX, -2f, 2f), -85, 5f));
//    }
//
//    private PlayState fire1Predicate(AnimationState<Bl132Entity> event) {
//        if (this.entityData.get(COOL_DOWN) > 70) {
//            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bl_132.fire_1"));
//        }
//        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bl_132.idle"));
//    }
//
//    private PlayState fire2Predicate(AnimationState<Bl132Entity> event) {
//        if (this.entityData.get(BARREL_ANIM_2) > 0) {
//            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bl_132.fire_2"));
//        }
//        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bl_132.idle"));
//    }
//
//    private PlayState fire3Predicate(AnimationState<Bl132Entity> event) {
//        if (this.entityData.get(BARREL_ANIM_3) > 0) {
//            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bl_132.fire_3"));
//        }
//        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bl_132.idle"));
//    }
//
//    private PlayState fire4Predicate(AnimationState<Bl132Entity> event) {
//        if (this.entityData.get(BARREL_ANIM_4) > 0) {
//            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bl_132.fire_4"));
//        }
//        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bl_132.idle"));
//    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
//        data.add(new AnimationController<>(this, "fire1", 0, this::fire1Predicate));
//        data.add(new AnimationController<>(this, "fire2", 0, this::fire2Predicate));
//        data.add(new AnimationController<>(this, "fire3", 0, this::fire3Predicate));
//        data.add(new AnimationController<>(this, "fire4", 0, this::fire4Predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }


    @Override
    public int getMaxStackSize() {
        return 4;
    }


}
