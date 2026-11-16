package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.entity.projectile.FlareDecoyEntity
import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getXRotFromVector
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getYRotFromVector
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.transformPosition
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.EntityFindUtil.findEntity
import com.atsuishio.superbwarfare.tools.InventoryTool
import com.atsuishio.superbwarfare.tools.RangeTool.calculateFiringSolution
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4d
import org.joml.Vector3d
import kotlin.math.*

/**
 * 用于处理载具武器瞄准或其他战斗相关方法的工具类
 */
object VehicleWeaponUtils {
    private const val TURRET_VIEW_PITCH_LIMIT = 79.5f
    private val turretViewElevationLimit = sin(Math.toRadians(TURRET_VIEW_PITCH_LIMIT.toDouble()))
    private val barrelElevationLimit = sin(Math.toRadians(79.9))

    fun clampTurretViewPitch(pitch: Float): Float = Mth.clamp(pitch, -TURRET_VIEW_PITCH_LIMIT, TURRET_VIEW_PITCH_LIMIT)

    fun followTurretView(vehicle: VehicleEntity, player: Player) {
        val view = Vec3.directionFromRotation(clampTurretViewPitch(player.xRot), player.yRot)
        val hull = vehicle.getVehicleTransformWithCustomPitch(1f)
        val local = Matrix4d(hull).invert().transformDirection(Vector3d(view.x, view.y, view.z))
        val damageScale = if (vehicle.turretDamaged) 0.2f else 1f
        val oldYaw = vehicle.turretYRot
        val oldPitch = vehicle.turretXRot
        val (minYaw, maxYaw) = vehicle.effectiveTurretYawRange()
        val minPitch = -vehicle.turretMaxPitch + vehicle.customTurretMaxPitch
        val maxPitch = -vehicle.turretMinPitch - vehicle.customTurretMinPitch

        val targetYaw = if (local.x * local.x + local.z * local.z > 1e-8) {
            val localYaw = Math.toDegrees(atan2(local.x, local.z)).toFloat()
            Mth.clamp(oldYaw + Mth.wrapDegrees(localYaw - oldYaw), minYaw, maxYaw)
        } else {
            Mth.clamp(oldYaw, minYaw, maxYaw)
        }
        val yawSpeed = vehicle.turretTurnYSpeed * damageScale
        vehicle.turretYRot = Mth.clamp(
            oldYaw + Mth.clamp(targetYaw - oldYaw, -yawSpeed, yawSpeed),
            -vehicle.turretMaxYaw,
            -vehicle.turretMinYaw
        )

        val pitchSpeed = vehicle.turretTurnXSpeed * damageScale
        if (pitchSpeed > 0f) {
            val localPitch = Math.toDegrees(asin((-local.y).coerceIn(-1.0, 1.0))).toFloat()
            val targetPitch = limitBarrelElevation(
                hull, targetYaw, Mth.clamp(localPitch, minPitch, maxPitch), minPitch, maxPitch, turretViewElevationLimit
            )
            val pitch = Mth.clamp(oldPitch + Mth.clamp(targetPitch - oldPitch, -pitchSpeed, pitchSpeed), minPitch, maxPitch)
            // 用实际偏航检查世界俯仰，给视线目标留出 0.4° 余量。
            val limitedPitch = limitBarrelElevation(hull, vehicle.turretYRot, pitch, minPitch, maxPitch, barrelElevationLimit)
            vehicle.turretXRot = pitch + Mth.clamp(limitedPitch - pitch, -8f, 8f)
        }

        if (vehicle.turretYRot != oldYaw || vehicle.turretXRot != oldPitch) {
            vehicle.turretTurnSound(vehicle.turretXRot - oldPitch, vehicle.turretYRot - oldYaw, 0.95f)
        }
    }

    private fun limitBarrelElevation(
        hull: Matrix4d, yaw: Float, pitch: Float, minPitch: Float, maxPitch: Float, elevationLimit: Double
    ): Float {
        val yawRadians = Math.toRadians(yaw.toDouble())
        val cosPitchWeight = hull.m01() * sin(yawRadians) + hull.m21() * cos(yawRadians)
        val sinPitchWeight = -hull.m11()
        val amplitude = hypot(cosPitchWeight, sinPitchWeight)
        if (amplitude <= elevationLimit) return pitch

        // 炮管世界 Y 分量为 A cos(pitch) + B sin(pitch)，求最近的允许俯仰角。
        val phase = atan2(sinPitchWeight, cosPitchWeight)
        val angle = Math.toRadians(pitch.toDouble())
        val offset = Math.IEEEremainder(angle - phase, Math.PI)
        val forbiddenHalfWidth = acos(elevationLimit / amplitude)
        if (abs(offset) >= forbiddenHalfWidth) return pitch

        val lower = Math.toDegrees(angle - offset - forbiddenHalfWidth).toFloat()
        val upper = Math.toDegrees(angle - offset + forbiddenHalfWidth).toFloat()
        val lowerPitch = lower.coerceAtLeast(minPitch)
        val upperPitch = upper.coerceAtMost(maxPitch)
        if (lower >= minPitch && upper <= maxPitch) {
            return if (pitch - lower <= upper - pitch) lower else upper
        }
        if (lower >= minPitch) return lower
        if (upper <= maxPitch) return upper
        // 机械限位挡住了两个交点，使用更接近安全范围的一侧。
        return if (lowerPitch - lower <= upper - upperPitch) lowerPitch else upperPitch
    }

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

        if (vehicle.entityData.get(VehicleEntity.TURRET_DAMAGED)) {
            ySpeed *= 0.2f
            xSpeed *= 0.2f
        }

        val min = -ySpeed
        val max = ySpeed

        vehicle.turretXRot = Mth.clamp(
            vehicle.turretXRot + Mth.clamp(0.75f * diffX, -xSpeed, xSpeed),
            -vehicle.turretMaxPitch + vehicle.customTurretMaxPitch,
            -vehicle.turretMinPitch - vehicle.customTurretMinPitch
        )
        vehicle.turretYRot = Mth.clamp(
            vehicle.turretYRot - Mth.clamp(1f * diffY, min, max),
            -vehicle.turretMaxYaw,
            -vehicle.turretMinYaw
        )

        vehicle.turretTurnSound(
            vehicle.turretXRot - vehicle.turretXRotO,
            vehicle.turretYRot - vehicle.turretYRotO,
            0.95f
        )

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
            val gravity = target.getAttributeValue(Attributes.GRAVITY)
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
            if (vehicle.decoyCount > 0) {
                for (i in 0..7) {
                    val smokeDecoyEntity = SmokeDecoyEntity(vehicle.level())
                    smokeDecoyEntity.setPos(vehicle.x, vehicle.y + vehicle.bbHeight, vehicle.z)
                    smokeDecoyEntity.decoyShoot(vehicle, vec3.yRot((-78.75f + 22.5f * i) * Mth.DEG_TO_RAD), 4f, 8f)
                    smokeDecoyEntity.deltaMovement.add(vehicle.deltaMovement)
                    vehicle.level().addFreshEntity(smokeDecoyEntity)
                }

                vehicle.level()
                    .playSound(null, vehicle, ModSounds.DECOY_RELEASE.get(), vehicle.soundSource, 1f, 1f)
                vehicle.decoyCount--
                if (vehicle.decoyCount == 0) {
                    vehicle.decoyReloadCoolDown = vehicle.getDecoyReloadTime()
                }
            }
            vehicle.decoyInputDown = false
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
            if (vehicle.decoyCount > 0) {
                val transform = vehicle.getVehicleTransform(1f)
                val worldPositionO = transformPosition(transform, 0.0, 0.0, 0.0)
                val worldPosition = transformPosition(transform, 1.0, -0.2, 0.6)
                val worldPosition2 = transformPosition(transform, -1.0, -0.2, 0.6)

                val shootVecO = Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z)
                val shootVec1 = Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
                val shootVec2 = Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z)

                shootDecoy(vehicle, shootVecO.vectorTo(shootVec1).normalize())
                shootDecoy(vehicle, shootVecO.vectorTo(shootVec2).normalize())
                vehicle.decoyCount--
                if (vehicle.decoyCount == 0) {
                    vehicle.decoyReloadCoolDown = vehicle.getDecoyReloadTime()
                }
            }
            vehicle.decoyInputDown = false
        }
    }

    @JvmStatic
    fun shootDecoy(vehicle: VehicleEntity, shootVec: Vec3) {
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
            ModSounds.DECOY_RELEASE.get(),
            vehicle.soundSource,
            3f,
            1f
        )
    }

    @JvmStatic
    fun reloadDecoy(vehicle: VehicleEntity) {
        if (vehicle.hasSmokeDecoy()) {
            vehicle.decoyCount = 1
            // 存在创造弹药盒时不消耗诱饵物品
            if (!vehicle.hasCreativeAmmoBoxCached()) {
                InventoryTool.consumeItem(vehicle, ModItems.VEHICLE_SMOKE_AMMO.get(), 1)
            }
        } else {
            if (vehicle.hasCreativeAmmoBoxCached()) {
                vehicle.decoyCount = vehicle.computed().decoyMagazineSize
            } else {
                val decoyMagazineCount = vehicle.decoyItemCount.coerceAtMost(vehicle.computed().decoyMagazineSize)
                vehicle.decoyCount += decoyMagazineCount
                InventoryTool.consumeItem(vehicle, ModItems.FLYING_FLARE_AMMO.get(), decoyMagazineCount)
            }
        }
        vehicle.level().playSound(null, vehicle, ModSounds.DECOY_RELOAD.get(), vehicle.soundSource, 2f, 1f)
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
                val gravity = target.getAttributeValue(Attributes.GRAVITY)
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
