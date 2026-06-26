package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.ClientMouseHandler
import com.atsuishio.superbwarfare.tools.angleTo
import com.mojang.math.Axis
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.*

/**
 * 处理载具相关动量、向量和旋转等数据的工具类
 */
object VehicleVecUtils {
    @JvmStatic
    fun transformPosition(transform: Matrix4d, x: Double, y: Double, z: Double): Vector4d =
        transform.transform(Vector4d(x, y, z, 1.0))

    @JvmStatic
    fun getYRotFromVector(vec3: Vec3) =
        Mth.atan2(vec3.x, vec3.z) * (180f / Math.PI)

    @JvmStatic
    fun getXRotFromVector(vec3: Vec3) =
        Mth.atan2(vec3.y, vec3.horizontalDistance()) * (180f / Math.PI)

    @JvmStatic
    fun getSubmergedHeight(entity: Entity) =
        entity.getFluidTypeHeight(entity.level().getFluidState(entity.blockPosition()).fluidType)

    /**
     * 获取四元数实体的局部Y轴（上方向）在世界空间中的单位向量
     */
    @JvmStatic
    fun getUpVec(q: Quaternionf): Vec3 {
        val dir = Vector3f()
        q.positiveY(dir)
        return Vec3(dir.x.toDouble(), dir.y.toDouble(), dir.z.toDouble())
    }

    /**
     * 获取四元数实体的局部Z轴（前方向）在世界空间中的单位向量
     */
    @JvmStatic
    fun getFrontVec(q: Quaternionf): Vec3 {
        val dir = Vector3f()
        q.positiveZ(dir)
        return Vec3(dir.x.toDouble(), dir.y.toDouble(), dir.z.toDouble())
    }

    /**
     * 获取四元数实体的局部X轴（右方向）在世界空间中的单位向量（取反，因为模型坐标系X轴朝左）
     */
    @JvmStatic
    fun getRightVec(q: Quaternionf): Vec3 {
        val dir = Vector3f()
        q.positiveX(dir)
        dir.negate()
        return Vec3(dir.x.toDouble(), dir.y.toDouble(), dir.z.toDouble())
    }

    @JvmStatic
    fun eulerToQuaternion(yaw: Float, pitch: Float, roll: Float): Quaternionf {
        val cy = Math.cos(yaw * 0.5 * Mth.DEG_TO_RAD)
        val sy = Math.sin(yaw * 0.5 * Mth.DEG_TO_RAD)
        val cp = Math.cos(pitch * 0.5 * Mth.DEG_TO_RAD)
        val sp = Math.sin(pitch * 0.5 * Mth.DEG_TO_RAD)
        val cr = Math.cos(roll * 0.5 * Mth.DEG_TO_RAD)
        val sr = Math.sin(roll * 0.5 * Mth.DEG_TO_RAD)

        val q = Quaternionf()
        q.w = (cy * cp * cr + sy * sp * sr).toFloat()
        q.x = (cy * cp * sr - sy * sp * cr).toFloat()
        q.y = (sy * cp * sr + cy * sp * cr).toFloat()
        q.z = (sy * cp * cr - cy * sp * sr).toFloat()

        return q
    }

    @JvmStatic
    fun calculateAngle(move: Vec3, view: Vec3): Double {
        val nMove = move.multiply(1.0, 0.0, 1.0).normalize()
        val nView = view.multiply(1.0, 0.0, 1.0).normalize()
        return nMove.angleTo(nView)
    }

    @JvmStatic
    fun entityEyePos(entity: Entity, partialTicks: Float): Vec3 {
        return Vec3(
            Mth.lerp(partialTicks.toDouble(), entity.xo, entity.x),
            Mth.lerp(partialTicks.toDouble(), entity.yo + entity.eyeHeight, entity.eyeY),
            Mth.lerp(partialTicks.toDouble(), entity.zo, entity.z)
        )
    }

    @JvmStatic
    fun simulate3P(entity: Entity, partialTicks: Float, distance: Double, height: Double): Vec3 {
        return Vec3(
            Mth.lerp(
                partialTicks.toDouble(),
                entity.xo,
                entity.x
            ) - distance * entity.getViewVector(partialTicks).x,
            Mth.lerp(
                partialTicks.toDouble(),
                entity.yo + entity.eyeHeight + height,
                entity.eyeY + height
            ) - distance * entity.getViewVector(partialTicks).y,
            Mth.lerp(
                partialTicks.toDouble(),
                entity.zo,
                entity.z
            ) - distance * entity.getViewVector(partialTicks).z
        )
    }

    /**
     * 将有炮塔的载具驾驶员的面朝方向设置为炮塔角度
     *
     * @param player 载具驾驶员
     */
    @JvmStatic
    fun setDriverAngle(vehicle: VehicleEntity, player: Player) {
        if (vehicle.hasTurret()) {
            val barrelVector = vehicle.getBarrelVector(1f)

            val xRot = getXRotFromVector(barrelVector)
            val yRot = getYRotFromVector(barrelVector)

            player.xRotO = -xRot.toFloat()
            player.xRot = -xRot.toFloat()
            player.yRotO = -yRot.toFloat()
            player.yRot = -yRot.toFloat()
            player.setYHeadRot(-yRot.toFloat())
        } else {
            player.xRotO = vehicle.xRot
            player.xRot = vehicle.xRot
            player.yRotO = vehicle.yRot
            player.yRot = vehicle.yRot
        }
    }

    /**
     * 计算载具受伤来源的角度
     *
     * @param vehicle    载具
     * @param source     伤害来源
     * @param multiplier 伤害倍率
     * @return 角度
     */
    @JvmStatic
    fun getDamageSourceAngle(vehicle: VehicleEntity, source: DamageSource, multiplier: Float): Float {
        var attacker = source.entity
        if (attacker == null) {
            attacker = source.directEntity
        }

        if (attacker != null) {
            val toVec = Vec3(
                vehicle.x,
                vehicle.y + vehicle.bbHeight / 2,
                vehicle.z
            ).vectorTo(attacker.position()).normalize()
            return Math.max(1f - multiplier * toVec.dot(vehicle.getViewVector(1f)), 0.5).toFloat()
        }
        return 1f
    }

    /**
     * 获取载具视角向量
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 视角向量
     */
    @JvmStatic
    fun getViewVec(vehicle: VehicleEntity, entity: Entity, partialTicks: Float): Vec3 {
        val data = vehicle.getGunData(vehicle.getSeatIndex(entity)) ?: return vehicle.getViewVector(partialTicks)

        val stringOrVec3 = data.get(GunProp.SHOOT_POS).viewDirection

        if (stringOrVec3 == null) {
            return vehicle.getShootVec(entity, partialTicks)
        } else if (stringOrVec3.isString) {
            if (stringOrVec3.string == "Bomb") {
                val bombHitPosO = ClientEventHandler.bombHitPosO
                val bombHitPos = ClientEventHandler.bombHitPos
                val bombHitPosX = Mth.lerp(partialTicks.toDouble(), bombHitPosO.x, bombHitPos.x)
                val bombHitPosY = Mth.lerp(partialTicks.toDouble(), bombHitPosO.y, bombHitPos.y)
                val bombHitPosZ = Mth.lerp(partialTicks.toDouble(), bombHitPosO.z, bombHitPos.z)
                return getViewPos(vehicle, entity, partialTicks).vectorTo(Vec3(bombHitPosX, bombHitPosY, bombHitPosZ))
            }
            return vehicle.getVectorFromString(stringOrVec3.string!!, partialTicks, vehicle.getSeatIndex(entity))
        } else {
            val vec3 = stringOrVec3.vec3!!
            val worldPosition = transformPosition(
                vehicle.getTransformFromString(data.get(GunProp.SHOOT_POS).transform, partialTicks),
                vec3.x + stringOrVec3.vec3.x,
                vec3.y + stringOrVec3.vec3.y,
                vec3.z + stringOrVec3.vec3.z
            )

            val worldPositionO = transformPosition(
                vehicle.getTransformFromString(data.get(GunProp.SHOOT_POS).transform, partialTicks),
                vec3.x,
                vec3.y,
                vec3.z
            )

            val startPos = Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z)
            val endPos = Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
            return startPos.vectorTo(endPos).normalize()
        }
    }

    @JvmStatic
    fun getViewPos(vehicle: VehicleEntity, entity: Entity, partialTicks: Float): Vec3 {
        val data = vehicle.getGunData(vehicle.getSeatIndex(entity)) ?: return entityEyePos(entity, partialTicks)

        val vec3 = data.get(GunProp.SHOOT_POS).viewPosition

        return if (vec3 == null) {
            vehicle.getCameraPos(entity, partialTicks)
        } else {
            val worldPosition = transformPosition(
                vehicle.getTransformFromString(data.get(GunProp.SHOOT_POS).transform, partialTicks),
                vec3.x,
                vec3.y,
                vec3.z
            )
            Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
        }
    }

    /**
     * 获取载具锁定向量
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 视角向量
     */
    @JvmStatic
    fun getSeekVec(vehicle: VehicleEntity, entity: Entity?, partialTicks: Float): Vec3? {
        val data = vehicle.getGunData(vehicle.getSeatIndex(entity)) ?: return vehicle.getViewVector(partialTicks)

        val stringOrVec3 = data.get(GunProp.SEEK_WEAPON_INFO)?.seekDirection

        if (stringOrVec3 == null) {
            return vehicle.getShootVec(entity, partialTicks)
        } else if (stringOrVec3.isString) {
            return vehicle.getVectorFromString(stringOrVec3.string!!, partialTicks, vehicle.getSeatIndex(entity))
        } else {
            val vec3 = stringOrVec3.vec3!!
            val worldPosition = transformPosition(
                vehicle.getTransformFromString(data.get(GunProp.SHOOT_POS).transform, partialTicks),
                vec3.x + stringOrVec3.vec3.x,
                vec3.y + stringOrVec3.vec3.y,
                vec3.z + stringOrVec3.vec3.z
            )

            val worldPositionO = transformPosition(
                vehicle.getTransformFromString(data.get(GunProp.SHOOT_POS).transform, partialTicks),
                vec3.x,
                vec3.y,
                vec3.z
            )

            val startPos = Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z)
            val endPos = Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
            return startPos.vectorTo(endPos).normalize()
        }
    }

    /**
     * 获取载具射击向量
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 射击向量
     */
    @JvmStatic
    fun getShootVec(vehicle: VehicleEntity, entity: Entity?, partialTicks: Float): Vec3 {
        val data = vehicle.getGunData(vehicle.getSeatIndex(entity)) ?: return vehicle.getViewVector(partialTicks)

        val stringOrVec3 = data.fireDirection()

        if (stringOrVec3.isString) {
            return vehicle.getVectorFromString(stringOrVec3.string!!, partialTicks, vehicle.getSeatIndex(entity))
        } else {
            val vec3 = data.firePosition()

            val worldPosition = transformPosition(
                vehicle.getTransformFromString(data.get(GunProp.SHOOT_POS).transform, partialTicks),
                vec3.x + stringOrVec3.vec3!!.x,
                vec3.y + stringOrVec3.vec3.y,
                vec3.z + stringOrVec3.vec3.z
            )

            val worldPositionO = transformPosition(
                vehicle.getTransformFromString(data.get(GunProp.SHOOT_POS).transform, partialTicks),
                vec3.x,
                vec3.y,
                vec3.z
            )

            val startPos = Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z)
            val endPos = Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
            return startPos.vectorTo(endPos).normalize()
        }
    }

    @JvmStatic
    fun getShootVec(vehicle: VehicleEntity, weaponName: String, partialTicks: Float): Vec3 {
        val data = vehicle.getGunData(weaponName) ?: return vehicle.getViewVector(partialTicks)

        val stringOrVec3 = data.fireDirection()

        if (stringOrVec3.isString) {
            return vehicle.getVectorFromString(stringOrVec3.string, partialTicks)
        } else {
            val vec3 = data.firePosition()

            val worldPosition = transformPosition(
                vehicle.getTransformFromString(data.get(GunProp.SHOOT_POS).transform, partialTicks),
                vec3.x + stringOrVec3.vec3!!.x,
                vec3.y + stringOrVec3.vec3.y,
                vec3.z + stringOrVec3.vec3.z
            )

            val worldPositionO = transformPosition(
                vehicle.getTransformFromString(data.get(GunProp.SHOOT_POS).transform, partialTicks),
                vec3.x,
                vec3.y,
                vec3.z
            )

            val startPos = Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z)
            val endPos = Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
            return startPos.vectorTo(endPos).normalize()
        }
    }

    /**
     * 获取载具默认炮管向量
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 射击向量
     */
    @JvmStatic
    fun getDefaultBarrelDirection(vehicle: VehicleEntity, entity: Entity?, partialTicks: Float): Vec3? {
        val data = vehicle.getGunData(vehicle.getSeatIndex(entity)) ?: return null

        val direct = data.get(GunProp.SHOOT_POS).defaultBarrelDirection

        if (direct != null) {
            val vec3 = direct.vec3!!
            val worldPosition = transformPosition(
                vehicle.getTransformFromString(data.get(GunProp.SHOOT_POS).defaultTransform, partialTicks),
                vec3.x + direct.vec3.x,
                vec3.y + direct.vec3.y,
                vec3.z + direct.vec3.z
            )

            val worldPositionO = transformPosition(
                vehicle.getTransformFromString(data.get(GunProp.SHOOT_POS).defaultTransform, partialTicks),
                vec3.x, vec3.y, vec3.z
            )

            val startPos = Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z)
            val endPos = Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
            return startPos.vectorTo(endPos).normalize()
        }
        return null
    }

    @JvmStatic
    fun getDefaultBarrelDirection(vehicle: VehicleEntity, weaponName: String, partialTicks: Float): Vec3? {
        val data = vehicle.getGunData(weaponName) ?: return vehicle.getViewVector(partialTicks)

        val direct = data.get(GunProp.SHOOT_POS).defaultBarrelDirection

        if (direct != null) {
            val vec3 = direct.vec3!!
            val worldPosition = transformPosition(
                vehicle.getTransformFromString(data.get(GunProp.SHOOT_POS).defaultTransform, partialTicks),
                vec3.x + direct.vec3.x,
                vec3.y + direct.vec3.y,
                vec3.z + direct.vec3.z
            )

            val worldPositionO = transformPosition(
                vehicle.getTransformFromString(data.get(GunProp.SHOOT_POS).defaultTransform, partialTicks),
                vec3.x, vec3.y, vec3.z
            )

            val startPos = Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z)
            val endPos = Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
            return startPos.vectorTo(endPos).normalize()
        }
        return null
    }

    /**
     * 获取乘客在载具上的摄像机位置
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 摄像机位置
     */
    @JvmStatic
    fun getCameraPos(vehicle: VehicleEntity, entity: Entity, partialTicks: Float): Vec3 {
        val index = vehicle.getSeatIndex(entity)
        val seat = vehicle.computed().seats().getOrNull(index) ?: return entityEyePos(entity, partialTicks)

        val data = seat.cameraPos ?: return entityEyePos(entity, partialTicks)

        if (data.useSimulate3P) {
            val simulate3PPos = data.simulate3PPos
            return simulate3P(entity, partialTicks, simulate3PPos.x.toDouble(), simulate3PPos.y.toDouble())
        }
        if (data.useFixedCameraPos) {
            val vec3 = data.position
            val worldPosition =
                transformPosition(vehicle.getTransformFromString(data.transform, partialTicks), vec3.x, vec3.y, vec3.z)
            return Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
        }

        return entityEyePos(entity, partialTicks)
    }

    /**
     * 获取乘客在载具上的摄像机方向
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 摄像机方向
     */
    @JvmStatic
    fun getCameraDirection(vehicle: VehicleEntity, entity: Entity, partialTicks: Float): Vec3 {
        val index = vehicle.getSeatIndex(entity)
        val seat = vehicle.computed().seats().getOrNull(index) ?: return entity.getViewVector(partialTicks)

        val data = seat.cameraPos ?: return entity.getViewVector(partialTicks)

        if (data.useSimulate3P) {
            return entity.getViewVector(partialTicks)
        }

        val stringOrVec3 = data.direction
        if (stringOrVec3.isString) {
            return if (stringOrVec3.string == "Default") {
                if (ClientEventHandler.zoomVehicle) {
                    vehicle.getZoomDirection(entity, partialTicks)
                } else {
                    entity.getViewVector(partialTicks)
                }
            } else {
                vehicle.getVectorFromString(stringOrVec3.string!!, partialTicks, vehicle.getSeatIndex(entity))
            }
        } else {
            val vec3 = data.position

            val worldPosition = transformPosition(
                vehicle.getTransformFromString(data.transform, partialTicks),
                vec3.x + stringOrVec3.vec3!!.x,
                vec3.y + stringOrVec3.vec3.y,
                vec3.z + stringOrVec3.vec3.z
            )

            val startPos = getCameraPos(vehicle, entity, partialTicks)
            val endPos = Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
            return startPos.vectorTo(endPos).normalize()
        }
    }

    /**
     * 获取载具瞄准的坐标
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 瞄准坐标
     */
    @JvmStatic
    fun getZoomPos(vehicle: VehicleEntity, entity: Entity, partialTicks: Float): Vec3 {
        val index = vehicle.getSeatIndex(entity)
        val seat = vehicle.computed().seats().getOrNull(index) ?: return entityEyePos(entity, partialTicks)

        val data = seat.cameraPos ?: return entityEyePos(entity, partialTicks)

        val vec3 = data.zoomPosition
        return if (vec3 != null) {
            val worldPosition = transformPosition(
                vehicle.getTransformFromString(data.transform, partialTicks), vec3.x, vec3.y, vec3.z
            )
            Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
        } else {
            getCameraPos(vehicle, entity, partialTicks)
        }
    }

    /**
     * 获取载具瞄准的方向
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 瞄准方向
     */
    @JvmStatic
    fun getZoomDirection(vehicle: VehicleEntity, entity: Entity, partialTicks: Float): Vec3 {
        val index = vehicle.getSeatIndex(entity)
        val seat = vehicle.computed().seats().getOrNull(index) ?: return entity.getViewVector(partialTicks)

        val data = seat.cameraPos ?: return entity.getViewVector(partialTicks)

        val stringOrVec3 = data.zoomDirection
        if (stringOrVec3 != null) {
            return if (stringOrVec3.isString) {
                vehicle.getVectorFromString(stringOrVec3.string!!, partialTicks, vehicle.getSeatIndex(entity))
            } else {
                val vec3 = data.zoomPosition ?: Vec3.ZERO
                val worldPosition = transformPosition(
                    vehicle.getTransformFromString(data.transform, partialTicks),
                    vec3.x + stringOrVec3.vec3!!.x,
                    vec3.y + stringOrVec3.vec3.y,
                    vec3.z + stringOrVec3.vec3.z
                )

                val startPos = vehicle.getShootPos(entity, partialTicks)
                val endPos = Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
                startPos.vectorTo(endPos).normalize()
            }
        }
        return entity.getViewVector(partialTicks)
    }

    // From Immersive_Aircraft
    @JvmStatic
    fun getVehicleYOffsetTransform(vehicle: VehicleEntity, partialTicks: Float): Matrix4d {
        val transform = Matrix4d()
        transform.translate(
            Mth.lerp(partialTicks.toDouble(), vehicle.xo, vehicle.x),
            Mth.lerp(
                partialTicks.toDouble(),
                vehicle.yo + vehicle.rotateOffsetHeight,
                vehicle.y + vehicle.rotateOffsetHeight
            ),
            Mth.lerp(partialTicks.toDouble(), vehicle.zo, vehicle.z)
        )
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, vehicle.yRotO, vehicle.yRot)))
        transform.rotate(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, vehicle.xRotO, vehicle.xRot)))
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, vehicle.prevRoll, vehicle.roll)))
        return transform
    }

    @JvmStatic
    fun getVehicleFlatTransform(vehicle: VehicleEntity, partialTicks: Float): Matrix4d {
        val transform = Matrix4d()
        transform.translate(
            Mth.lerp(partialTicks.toDouble(), vehicle.xo, vehicle.x),
            Mth.lerp(partialTicks.toDouble(), vehicle.yo, vehicle.y),
            Mth.lerp(partialTicks.toDouble(), vehicle.zo, vehicle.z)
        )
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, vehicle.yRotO, vehicle.yRot)))
        return transform
    }

    @JvmStatic
    fun getClientVehicleTransform(vehicle: VehicleEntity, partialTicks: Float): Matrix4d {
        val transform = Matrix4d()
        transform.translate(
            Mth.lerp(partialTicks.toDouble(), vehicle.xo, vehicle.x),
            Mth.lerp(
                partialTicks.toDouble(),
                vehicle.yo + vehicle.rotateOffsetHeight,
                vehicle.y + vehicle.rotateOffsetHeight
            ),
            Mth.lerp(partialTicks.toDouble(), vehicle.zo, vehicle.z)
        )
        transform.rotate(
            Axis.YP.rotationDegrees(
                (-Mth.lerp(
                    partialTicks,
                    vehicle.yRotO,
                    vehicle.yRot
                ) + ClientMouseHandler.freeCameraYaw).toFloat()
            )
        )
        transform.rotate(
            Axis.XP.rotationDegrees(
                (Mth.lerp(
                    partialTicks,
                    vehicle.xRotO,
                    vehicle.xRot
                ) + ClientMouseHandler.freeCameraPitch).toFloat()
            )
        )
        return transform
    }

    /**
     * 获取炮塔的旋转矩阵
     *
     * @param vehicle      载具
     * @param partialTicks 客户端ticks
     * @return 旋转矩阵
     */
    @JvmStatic
    fun getTurretTransform(vehicle: VehicleEntity, partialTicks: Float): Matrix4d {
        val transformV = vehicle.getVehicleTransformWithCustomPitch(partialTicks)

        val transform = Matrix4d()
        val pos = vehicle.turretPos ?: return transformV
        val worldPosition = transformPosition(
            transform,
            pos.x,
            pos.y,
            pos.z
        )

        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z)
        transformV.rotate(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, vehicle.turretYRotO, vehicle.turretYRot)))
        return transformV
    }

    /**
     * 获取炮塔的向量
     *
     * @param vehicle      载具
     * @param partialTicks 客户端ticks
     * @return 炮塔向量
     */
    @JvmStatic
    fun getTurretVector(vehicle: VehicleEntity, partialTicks: Float): Vec3 {
        val transform = getTurretTransform(vehicle, partialTicks)
        val rootPosition = transformPosition(transform, 0.0, 0.0, 0.0)
        val targetPosition = transformPosition(transform, 0.0, 0.0, 1.0)
        return Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(
            Vec3(
                targetPosition.x,
                targetPosition.y,
                targetPosition.z
            )
        )
    }

    @JvmStatic
    fun getBarrelTransform(vehicle: VehicleEntity, partialTicks: Float): Matrix4d {
        val transformT = getTurretTransform(vehicle, partialTicks)

        val transform = Matrix4d()
        val pos = vehicle.barrelPosition
        val worldPosition = transformPosition(
            transform,
            pos!!.x,
            pos.y,
            pos.z
        )

        transformT.translate(worldPosition.x, worldPosition.y, worldPosition.z)
        val x = Mth.lerp(partialTicks, vehicle.turretXRotO, vehicle.turretXRot)

        transformT.rotate(Axis.XP.rotationDegrees(x))
        return transformT
    }

    @JvmStatic
    fun getGunTransform(vehicle: VehicleEntity, partialTicks: Float): Matrix4d {
        val transformT = getTurretTransform(vehicle, partialTicks)

        val transform = Matrix4d()
        val pos = vehicle.passengerWeaponStationPosition ?: return transformT
        val worldPosition = transformPosition(
            transform,
            pos.x,
            pos.y,
            pos.z
        )

        transformT.translate(worldPosition.x, worldPosition.y, worldPosition.z)
        transformT.rotate(
            Axis.YP.rotationDegrees(
                Mth.lerp(partialTicks, vehicle.gunYRotO, vehicle.gunYRot) - Mth.lerp(
                    partialTicks,
                    vehicle.turretYRotO,
                    vehicle.turretYRot
                )
            )
        )
        return transformT
    }

    @JvmStatic
    fun getPassengerWeaponStationBarrelTransform(vehicle: VehicleEntity, partialTicks: Float): Matrix4d {
        val transformG = getGunTransform(vehicle, partialTicks)

        val transform = Matrix4d()
        val pos = vehicle.passengerWeaponStationBarrelPosition
        val worldPosition = transformPosition(
            transform,
            pos!!.x,
            pos.y,
            pos.z
        )

        transformG.translate(worldPosition.x, worldPosition.y, worldPosition.z)

        val x = Mth.lerp(partialTicks, vehicle.gunXRotO, vehicle.gunXRot)

        transformG.rotate(Axis.XP.rotationDegrees(x))
        return transformG
    }

    @JvmStatic
    fun getPassengerWeaponStationVector(vehicle: VehicleEntity, partialTicks: Float): Vec3 {
        val transform = getPassengerWeaponStationBarrelTransform(vehicle, partialTicks)
        val rootPosition = transformPosition(transform, 0.0, 0.0, 0.0)
        val targetPosition = transformPosition(transform, 0.0, 0.0, 1.0)
        return Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(
            Vec3(
                targetPosition.x,
                targetPosition.y,
                targetPosition.z
            )
        )
    }
}
