package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.entity.projectile.FlareDecoyEntity
import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getXRotFromVector
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getYRotFromVector
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.transformPosition
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.EntityFindUtil.findEntity
import com.atsuishio.superbwarfare.tools.RangeTool.calculateFiringSolution
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.ForgeMod

/**
 * 用于处理载具武器瞄准或其他战斗相关方法的工具类
 */
object VehicleWeaponUtils {
    /**
     * 根据操控者调整载具炮塔角度
     * 
     * @param vehicle 载具
     */
    @JvmStatic
    fun adjustTurretAngle(vehicle: VehicleEntity) {
        if (vehicle.isWreck) return
        val driver = vehicle.getNthEntity(vehicle.turretControllerIndex)
        val pos = vehicle.barrelPosition
        if (driver != null && pos != null) {
            val aimPos = vehicle.boundingBox.center.add(driver.getViewVector(1f).scale(512.0))

            val transform = vehicle.getTurretTransform(1f)
            val worldPosition = transformPosition(transform, pos.x, pos.y, pos.z)

            val aimVec = Vec3(worldPosition.x, worldPosition.y, worldPosition.z).vectorTo(aimPos)
            turretAutoAimFromVector(vehicle, aimVec)
        }
    }

    /**
     * 根据方向向量，使炮塔自动瞄准
     * 
     * @param shootVec 需要让炮塔以这个角度发射的向量
     */
    @JvmStatic
    fun turretAutoAimFromVector(vehicle: VehicleEntity, shootVec: Vec3) {
        if (vehicle.isWreck) return
        var ySpeed = vehicle.turretTurnYSpeed
        var xSpeed = vehicle.turretTurnXSpeed

        val barrelVector = vehicle.getBarrelVector(1f)
        val diffY = Mth.wrapDegrees(-getYRotFromVector(shootVec) + getYRotFromVector(barrelVector)).toFloat()
        val diffX = Mth.wrapDegrees(-getXRotFromVector(shootVec) + getXRotFromVector(barrelVector)).toFloat()

        if (vehicle.getEntityData().get(VehicleEntity.TURRET_DAMAGED)) {
            ySpeed *= 0.2f
            xSpeed *= 0.2f
        }

        val min = -ySpeed
        val max = ySpeed

        vehicle.turretXRot = Mth.clamp(
            vehicle.turretXRot + Mth.clamp(0.75f * diffX, -xSpeed, xSpeed),
            -vehicle.turretMaxPitch,
            -vehicle.turretMinPitch
        )
        vehicle.turretYRot = Mth.clamp(
            vehicle.turretYRot - Mth.clamp(1f * diffY, min, max),
            -vehicle.turretMaxYaw,
            -vehicle.turretMinYaw
        )

        vehicle.turretTurnSound(vehicle.turretXRot - vehicle.turretXRotO, vehicle.turretYRot - vehicle.turretYRotO, 0.95f)

        vehicle.turretYRotLock = Mth.clamp(-1f * diffY, min, max)
    }

    /**
     * 根据UUID，使炮塔自动瞄准
     * 
     * @param uuid    目标的UUID字符串
     * @param pLiving 操控载具的实体
     */
    @JvmStatic
    fun turretAutoAimFromUuid(vehicle: VehicleEntity, uuid: String?, pLiving: LivingEntity) {
        if (vehicle.isWreck) return
        var target = findEntity(vehicle.level(), uuid) ?: return

        if (target.vehicle != null) {
            target = target.vehicle!!
        }

        val targetPos = target.boundingBox.center
        var targetVel = target.deltaMovement

        if (target is LivingEntity) {
            val gravity = target.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get())
            targetVel = targetVel.add(0.0, gravity, 0.0)
        }

        if (target is Player) {
            targetVel = targetVel.multiply(2.0, 1.0, 2.0)
        }

        val targetVec = calculateFiringSolution(
            vehicle.getShootPos(pLiving, 1f).subtract(
                vehicle.getShootVec(pLiving, 1f).scale(vehicle.getShootPos(pLiving, 1f).distanceTo(pLiving.position()))
            ),
            targetPos,
            targetVel,
            vehicle.getProjectileVelocity(pLiving).toDouble(),
            vehicle.getProjectileGravity(pLiving).toDouble()
        )
        vehicle.turretAutoAimFromVector(targetVec)
    }

    /**
     * 发射烟雾诱饵
     * 
     * @param vehicle 载具
     * @param vec3    发射方向
     */
    @JvmStatic
    fun releaseSmokeDecoy(vehicle: VehicleEntity, vec3: Vec3) {
        if (vehicle.decoyInputDown) {
            if (vehicle.decoyReady && vehicle.level() is ServerLevel) {
                for (i in 0..7) {
                    val smokeDecoyEntity = SmokeDecoyEntity(vehicle.level())
                    smokeDecoyEntity.setPos(vehicle.x, vehicle.y + vehicle.bbHeight, vehicle.z)
                    smokeDecoyEntity.decoyShoot(vehicle, vec3.yRot((-78.75f + 22.5f * i) * Mth.DEG_TO_RAD), 4f, 8f)
                    vehicle.level().addFreshEntity(smokeDecoyEntity)
                }

                vehicle.level()
                    .playSound(null, vehicle, ModSounds.DECOY_RELEASE.get(), vehicle.soundSource, 1f, 1f)
                vehicle.decoyReloadCoolDown = 500
                vehicle.decoyReady = false
            }
            vehicle.decoyInputDown = false
        }

        if (!vehicle.decoyReady && vehicle.decoyReloadCoolDown == 0 && vehicle.level() is ServerLevel) {
            vehicle.decoyReady = true
            vehicle.level().playSound(null, vehicle, ModSounds.DECOY_RELOAD.get(), vehicle.soundSource, 1f, 1f)
            vehicle.decoyReloadCoolDown = 500
        }
    }

    /**
     * 发射热诱弹
     * 
     * @param vehicle 载具
     */
    @JvmStatic
    fun releaseDecoy(vehicle: VehicleEntity) {
        if (vehicle.decoyInputDown) {
            if (vehicle.decoyReady && vehicle.level() is ServerLevel) {
                var i = 0
                while (i < 54) {
                    val finalI = i
                    queueServerWork(i) {
                        val transform = vehicle.getVehicleTransform(1f)
                        val worldPositionO = transformPosition(transform, 0.0, 0.0, 0.0)
                        val worldPosition = transformPosition(transform, 1.0, -0.2, 0.6)
                        val worldPosition2 = transformPosition(transform, -1.0, -0.2, 0.6)

                        val shootVecO = Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z)
                        val shootVec1 = Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
                        val shootVec2 = Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z)

                        shootDecoy(vehicle, shootVecO.vectorTo(shootVec1).normalize(), finalI == 6)
                        shootDecoy(vehicle, shootVecO.vectorTo(shootVec2).normalize(), finalI == 6)
                    }
                    i += 6
                }

                vehicle.decoyReloadCoolDown = 400
                vehicle.decoyReady = false
            }
            vehicle.decoyInputDown = false
        }
        if (!vehicle.decoyReady && vehicle.decoyReloadCoolDown == 0 && vehicle.level() is ServerLevel) {
            vehicle.decoyReady = true
            vehicle.level().playSound(null, vehicle, ModSounds.DECOY_RELOAD.get(), vehicle.soundSource, 1f, 1f)
            vehicle.decoyReloadCoolDown = 400
        }
    }

    @JvmStatic
    fun shootDecoy(vehicle: VehicleEntity, shootVec: Vec3, first: Boolean) {
        val flareDecoyEntity = FlareDecoyEntity(vehicle.level())

        flareDecoyEntity.setPos(
            vehicle.x + vehicle.deltaMovement.x,
            vehicle.y + 0.5 + vehicle.deltaMovement.y,
            vehicle.z + vehicle.deltaMovement.z
        )
        flareDecoyEntity.decoyShoot(vehicle, shootVec, (vehicle.deltaMovement.length() * 0.3f + 0.7).toFloat(), 8f)

        vehicle.level().addFreshEntity(flareDecoyEntity)
        vehicle.level().playSound(
            null,
            vehicle,
            if (first) ModSounds.DECOY_RELEASE_FIRST.get() else ModSounds.DECOY_RELEASE.get(),
            vehicle.soundSource,
            2f,
            1f
        )
    }

    /**
     * 根据UUID，使乘客位武器自动瞄准
     *
     * @param vehicle 载具
     * @param uuid    目标的UUID字符串
     * @param pLiving 操控载具的实体
     */
    @JvmStatic
    fun passengerWeaponAutoAimFormUuid(vehicle: VehicleEntity, uuid: String?, pLiving: LivingEntity) {
        var target = findEntity(vehicle.level(), uuid)
        if (target != null) {
            if (target.vehicle != null) {
                target = target.vehicle
            }

            val targetPos = target!!.boundingBox.center
            var targetVel = target.deltaMovement

            if (target is LivingEntity) {
                val gravity = target.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get())
                targetVel = targetVel.add(0.0, gravity, 0.0)
            }

            if (target is Player) {
                targetVel = targetVel.multiply(2.0, 1.0, 2.0)
            }

            val targetVec = calculateFiringSolution(
                vehicle.getShootPos(pLiving, 1f).subtract(
                    vehicle.getShootVec(pLiving, 1f).scale(
                        vehicle.getShootPos(pLiving, 1f).distanceTo(pLiving.position())
                    )
                ),
                targetPos,
                targetVel,
                vehicle.getProjectileVelocity(pLiving).toDouble(),
                vehicle.getProjectileGravity(pLiving).toDouble()
            )
            passengerWeaponAutoAimFormVector(vehicle, targetVec)
        }
    }

    /**
     * 根据方向向量，使乘客位武器自动瞄准
     *
     * @param vehicle  载具
     * @param shootVec 需要让武器站以这个角度发射的向量
     */
    @JvmStatic
    fun passengerWeaponAutoAimFormVector(vehicle: VehicleEntity, shootVec: Vec3) {
        val ySpeed = vehicle.passengerWeaponYSpeed
        val xSpeed = vehicle.passengerWeaponXSpeed
        val diffY = Mth.wrapDegrees(
            -getYRotFromVector(shootVec) + getYRotFromVector(
                vehicle.getPassengerWeaponStationVector(1f)
            )
        ).toFloat()
        val diffX = Mth.wrapDegrees(
            -getXRotFromVector(shootVec) + getXRotFromVector(
                vehicle.getPassengerWeaponStationVector(1f)
            )
        ).toFloat()

        vehicle.gunXRot = Mth.clamp(
            vehicle.gunXRot + Mth.clamp(diffX, -xSpeed, xSpeed),
            -vehicle.passengerWeaponMaxPitch,
            -vehicle.passengerWeaponMinPitch
        )
        vehicle.gunYRot = Mth.clamp(
            vehicle.gunYRot - Mth.clamp(diffY, -ySpeed, ySpeed),
            -vehicle.passengerWeaponMaxYaw,
            -vehicle.passengerWeaponMinYaw
        )

        vehicle.turretTurnSound(vehicle.gunXRot - vehicle.gunXRotO, vehicle.gunYRot - vehicle.gunYRotO, 0.95f)
    }

    /**
     * 根据操控者调整乘客武器站角度
     *
     * @param vehicle 载具
     */
    @JvmStatic
    fun adjustWeaponControllerAngle(vehicle: VehicleEntity) {
        val entity = vehicle.getNthEntity(vehicle.passengerWeaponStationControllerIndex)
        val pos = vehicle.passengerWeaponStationBarrelPosition
        if (entity != null && pos != null) {
            val aimPos = vehicle.boundingBox.center.add(entity.getViewVector(1f).scale(512.0))

            val transform = vehicle.getGunTransform(1f)
            val worldPosition = transformPosition(transform, pos.x, pos.y, pos.z)

            val aimVec = Vec3(worldPosition.x, worldPosition.y, worldPosition.z).vectorTo(aimPos)
            passengerWeaponAutoAimFormVector(vehicle, aimVec)
        }

        if (entity == null) {
            vehicle.gunYRot += vehicle.turretYRotLock
        }
    }
}
