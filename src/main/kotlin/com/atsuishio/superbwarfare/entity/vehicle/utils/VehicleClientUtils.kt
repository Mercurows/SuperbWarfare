package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getXRotFromVector
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getYRotFromVector
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.transformPosition
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.ClientMouseHandler
import com.atsuishio.superbwarfare.tools.maxZoom
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/**
 * 处理载具客户端专用方法的工具类
 */
object VehicleClientUtils {

    /**
     * 获取视角旋转
     *
     * @param vehicle       载具
     * @param partialTicks  部分刻
     * @param player        玩家
     * @param zoom          是否在载具上瞄准
     * @param isFirstPerson 是否是第一人称视角
     * @return 视角旋转角度，如果不需要自定义视角则返回null
     */
    @OnlyIn(Dist.CLIENT)
    @JvmStatic
    fun getCameraRotation(
        vehicle: VehicleEntity,
        partialTicks: Float,
        player: Player,
        zoom: Boolean,
        isFirstPerson: Boolean
    ): Vec2? {
        val index = vehicle.getSeatIndex(player)
        val seat = vehicle.computed().seats().getOrNull(index)
        val gunData = vehicle.getGunData(player)
        if (seat != null) {
            // 新第三人称：使用骨骼位置，但旋转仍按旧 UseAircraftCamera（yaw/pitch + 自由视角，不带 roll）
            if (!isFirstPerson && !zoom && VehicleCameraBoneUtils.getThirdPersonBone(seat) != null &&
                VehicleCameraBoneUtils.getThirdPersonBoneOffset(vehicle, partialTicks, index) != null
            ) {
                return Vec2(
                    (vehicle.getYaw(partialTicks) - ClientMouseHandler.freeCameraYaw).toFloat(),
                    (vehicle.getPitch(partialTicks) + ClientMouseHandler.freeCameraPitch).toFloat()
                )
            }
            // 骨骼视角（新系统）：由 VehicleBoneCamera 在更底层直接接管 Camera 的位置与四元数朝向，
            // 这里不再返回任何欧拉角/手写相机数据，避免旧链路参与
            if (VehicleCameraBoneUtils.getCameraBone(seat) != null) return null

            val data = seat.cameraPos
            if (data != null) {
                if (zoom && gunData != null && gunData.get(GunProp.SHOOT_POS).viewDirection != null) {
                    return if (ClientEventHandler.isNacelleCam(player)) {
                        Vec2(
                            (-getYRotFromVector(vehicle.getViewVec(player, partialTicks)).toFloat() - ClientMouseHandler.nacelleCameraYaw).toFloat(),
                            (-getXRotFromVector(vehicle.getViewVec(player, partialTicks)).toFloat() + ClientMouseHandler.nacelleCameraPitch).toFloat()
                        )
                    } else return Vec2(
                        -getYRotFromVector(vehicle.getViewVec(player, partialTicks)).toFloat(),
                        -getXRotFromVector(vehicle.getViewVec(player, partialTicks)).toFloat()
                    )
                }
                if (vehicle.useAircraftCamera(index)) {
                    return if (ClientEventHandler.isNacelleCam(player)) {
                        Vec2(
                            (vehicle.getYaw(partialTicks) - ClientMouseHandler.nacelleCameraYaw).toFloat(),
                            (vehicle.getPitch(partialTicks) + ClientMouseHandler.nacelleCameraPitch).toFloat()
                        )
                    } else {
                        Vec2(
                            (vehicle.getYaw(partialTicks) - ClientMouseHandler.freeCameraYaw).toFloat(),
                            (vehicle.getPitch(partialTicks) + ClientMouseHandler.freeCameraPitch).toFloat()
                        )
                    }
                }
                if (zoom || isFirstPerson) {
                    if (vehicle.hasTurret() && vehicle.turretControllerIndex == index) {
                        val barrelVector = vehicle.getBarrelVector(partialTicks)
                        if (barrelVector.lengthSqr() > 1.0E-8) {
                            return Vec2(
                                -getYRotFromVector(barrelVector).toFloat(),
                                -getXRotFromVector(barrelVector).toFloat()
                            )
                        }
                    }
                    return Vec2(
                        -getYRotFromVector(vehicle.cameraDirection(player, partialTicks)).toFloat(),
                        -getXRotFromVector(vehicle.cameraDirection(player, partialTicks)).toFloat()
                    )
                }
            } else {
                return null
            }
        }
        return null
    }

    /**
     * 获取视角位置
     *
     * @param vehicle       载具
     * @param partialTicks  部分刻
     * @param player        玩家
     * @param zoom          是否在载具上瞄准
     * @param isFirstPerson 是否是第一人称视角
     * @return 视角位置，如果不需要自定义视角则返回null
     */
    @OnlyIn(Dist.CLIENT)
    @JvmStatic
    fun getCameraPosition(
        vehicle: VehicleEntity,
        partialTicks: Float,
        player: Player,
        zoom: Boolean,
        isFirstPerson: Boolean
    ): Vec3? {
        val index = vehicle.getSeatIndex(player)
        val seat = vehicle.computed().seats().getOrNull(index)
        if (seat != null) {
            if (!isFirstPerson && !zoom && VehicleCameraBoneUtils.getThirdPersonBone(seat) != null) {
                val offset = VehicleCameraBoneUtils.getThirdPersonBoneOffset(vehicle, partialTicks, index) ?: return null
                val transform = vehicle.getClientVehicleTransform(partialTicks)
                val maxCameraPosition = transformPosition(
                    transform,
                    offset.x,
                    offset.y + 0.1 * ClientMouseHandler.custom3pDistanceLerp,
                    offset.z - ClientMouseHandler.custom3pDistanceLerp
                )
                return maxCameraPosition.maxZoom(transform)
            }
            // 骨骼视角（新系统）：由 VehicleBoneCamera 底层接管，这里直接交给原版/旧链路
            if (VehicleCameraBoneUtils.getCameraBone(seat) != null) return null

            val data = seat.cameraPos
            val gunData = vehicle.getGunData(player)
            if (data != null) {
                if (zoom || isFirstPerson) {
                    return if (zoom) {
                        if (gunData != null && gunData.get(GunProp.SHOOT_POS).viewPosition != null) {
                            vehicle.getViewPos(player, partialTicks)
                        } else {
                            vehicle.getZoomPos(player, partialTicks)
                        }
                    } else {
                        vehicle.getCameraPos(player, partialTicks)
                    }
                } else if (vehicle.useAircraftCamera(index)) {
                    val transform = vehicle.getClientVehicleTransform(partialTicks)
                    val maxCameraPosition = transformPosition(
                        transform,
                        data.aircraftCameraPos.x,
                        data.aircraftCameraPos.y + 0.1 * ClientMouseHandler.custom3pDistanceLerp,
                        data.aircraftCameraPos.z - ClientMouseHandler.custom3pDistanceLerp
                    )
                    return maxCameraPosition.maxZoom(transform)
                }
            }
            return null
        }
        return null
    }

    /**
     * 是否使用载具固定视角
     *
     * @param vehicle 载具
     * @param entity  乘客实体
     * @return 是否使用固定视角
     */
    @OnlyIn(Dist.CLIENT)
    @JvmStatic
    fun useFixedCameraPos(vehicle: VehicleEntity, entity: Entity?): Boolean {
        val index = vehicle.getSeatIndex(entity)
        val seat = vehicle.computed().seats().getOrNull(index) ?: return false
        val data = seat.cameraPos ?: return false
        return data.useFixedCameraPos
    }

    /**
     * 客户端第一人称弹药HUD组件
     */
    @OnlyIn(Dist.CLIENT)
    @JvmStatic
    fun firstPersonAmmoComponent(vehicle: VehicleEntity, data: GunData, player: Player?): Component {
        val name = data.get(GunProp.NAME)
        if (name.isNullOrBlank()) return Component.empty()

        val ammoCount = vehicle.getAmmoCount(player)
        return Component.translatable(name, if (ammoCount == Int.MAX_VALUE) "∞" else ammoCount)
    }

    /**
     * 客户端第三人称弹药HUD组件
     */
    @OnlyIn(Dist.CLIENT)
    @JvmStatic
    fun thirdPersonAmmoComponent(vehicle: VehicleEntity, data: GunData, player: Player?): Component {
        return firstPersonAmmoComponent(vehicle, data, player)
    }
}
