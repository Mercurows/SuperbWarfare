package com.atsuishio.superbwarfare.entity.vehicle.ai

import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getSubmergedHeight
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.world.saveddata.TDMSavedData
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.TraceableEntity
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.max

/**
 * 防御塔 AI 系统，负责索敌、威胁评估、目标优先级排序与目标锁定。
 *
 * 使用组合模式注入 [AutoAimableEntity]，替代原有的死板索敌机制。
 * 子类可通过覆盖 [AutoAimableEntity.threatConfig] 定制 AI 行为。
 */
class TowerAI(private val tower: AutoAimableEntity) {

    // region Threat Configuration

    /**
     * 威胁评分配置，控制各因素在目标优先级中的权重。
     */
    data class ThreatConfig(
        /** 距离权重（越近分越高） */
        val distanceWeight: Double = 3.0,
        /** 实体类型权重 */
        val typeWeight: Double = 1.0,
        /** 低血量目标补刀权重 */
        val healthWeight: Double = 0.5,
        /** 弹射物碰撞航线威胁加分 */
        val projectileThreatBonus: Double = 50.0,
        /** 弹射物速度威胁加分（乘数） */
        val projectileSpeedMultiplier: Double = 5.0,
        /** 近期攻击者加分 */
        val recentAttackerBonus: Double = 30.0,
        /** 弹射物轨迹威胁判定距离阈值 */
        val projectileInterceptDistance: Double = 3.0,
        /** 切换目标所需的最小评分差（滞后，防止频繁切换） */
        val targetSwitchHysteresis: Double = 15.0,
        /** 目标最低评分阈值，低于此分数的目标不会被锁定 */
        val minThreatScore: Double = 0.0,
    )

    // endregion

    // region TeamResolver

    /**
     * 集中化队伍 / TDM 判定。
     */
    object TeamResolver {

        /**
         * 判断目标实体对防御塔是否敌对。
         *
         * 逻辑：
         * 1. [Enemy] 生物（僵尸等）→ 敌对
         * 2. TDM 启用 → 敌对
         * 3. 非友方且非同队 → 敌对
         */
        fun isHostile(tower: AutoAimableEntity, target: Entity): Boolean {
            // 排除弹射物（弹射物用 isHostileProjectile 单独处理）
            if (target is Projectile) return false

            val owner = tower.owner ?: return false

            // 目标没有队伍信息 → 无法判定，不视为敌对
            if (target.team == null && target !is Enemy) return false

            // 敌对生物（僵尸/骷髅等）始终敌对
            if (target is Enemy && target is LivingEntity && target.health > 0) return true

            // TDM 覆盖：TDM 列表中的实体始终视为敌对
            if (TDMSavedData.enabledTDM(target)) return true

            // 目标载体里的乘客也检查 TDM
            for (passenger in target.passengers) {
                if (TDMSavedData.enabledTDM(passenger)) return true
            }

            // 友方检查（包含同队、同主人、载具驾驶员同队等）
            if (SeekTool.IS_FRIENDLY.test(owner, target)) return false
            if (SeekTool.IS_FRIENDLY.test(tower, target)) return false

            // 非同队即敌对
            return !target.isAlliedTo(owner)
        }

        /**
         * 判断弹射物对防御塔是否敌对。
         */
        fun isHostileProjectile(tower: AutoAimableEntity, projectile: Projectile): Boolean {
            val owner = tower.owner ?: return false

            val projectileOwner = (projectile as? TraceableEntity)?.owner ?: return false

            // 自己的弹射物不拦截
            if (projectileOwner === owner) return false

            // TDM：弹射物主人在 TDM 列表中
            if (projectileOwner.team != null && TDMSavedData.enabledTDM(projectileOwner)) return true

            // 非同队 = 敌对
            return !projectileOwner.isAlliedTo(owner)
        }

        /**
         * 判断目标是否对防御塔友好。
         */
        fun isFriendly(tower: AutoAimableEntity, target: Entity): Boolean {
            val owner = tower.owner ?: return false

            // TDM 覆盖：TDM 强制敌对，永远不友好
            if (TDMSavedData.enabledTDM(target)) return false

            return SeekTool.IS_FRIENDLY.test(owner, target) || SeekTool.IS_FRIENDLY.test(tower, target)
        }
    }

    // endregion

    // region TargetValidator

    /**
     * 目标合法性验证。
     */
    object TargetValidator {

        /**
         * 综合验证目标是否可以被锁定攻击。
         */
        fun isValidTarget(
            tower: AutoAimableEntity,
            target: Entity,
            pos: Vec3,
            minAngle: Double,
            maxAngle: Double,
            minRange: Double,
            maxRange: Double,
        ): Boolean {
            // 基础排除
            if (target === tower) return false
            if (target is TargetEntity) return false
            if (!target.isAlive) return false
            if (target is LivingEntity && target.health <= 0) return false
            if (target is VehicleEntity && target.isWreck) return false

            // 距离范围
            val distSqr = target.distanceToSqr(tower)
            if (distSqr <= minRange * minRange) return false
            if (distSqr > maxRange * maxRange) return false

            // 俯仰角范围
            if (!AutoAimableEntity.canAim(pos, target, minAngle, maxAngle)) return false

            // 水中/掩体中
            if (getSubmergedHeight(target) > target.bbHeight) return false

            // 烟雾遮挡
            if (!SeekTool.NOT_IN_SMOKE.test(target)) return false

            // 黑名单
            if (SeekTool.IN_BLACKLIST.test(target)) return false

            // 玩家排除（旁观/创造）
            if (target is Player && (target.isSpectator || target.isCreative)) return false

            // 无敌
            if (SeekTool.IS_INVULNERABLE.test(target)) return false

            // 弹射物特殊检查
            if (target is Projectile) {
                return isValidProjectileTarget(tower, target, pos)
            }

            // 视线检查
            if (!checkLineOfSight(tower, target, pos)) return false

            // 敌对检查
            return TeamResolver.isHostile(tower, target)
        }

        /**
         * 验证弹射物是否为有威胁的目标。
         * 包含轨迹分析：判断弹射物是否正飞向防御塔。
         */
        fun isValidProjectileTarget(tower: AutoAimableEntity, projectile: Projectile, pos: Vec3): Boolean {
            // 排除已落地或静止的弹射物
            if (projectile.onGround()) return false
            if (projectile.deltaMovement.lengthSqr() < 0.0001) return false

            // 排除特定类型的弹射物
            if (projectile is SmallCannonShellEntity) return false

            // 大小检查（过小的弹射物不值得拦截）
            val towerConfig = tower.threatConfig
            if (projectile.bbWidth < 0.25 && projectile.bbHeight < 0.25) return false

            // 敌对检查
            if (!TeamResolver.isHostileProjectile(tower, projectile)) return false

            // 视线检查
            if (!checkLineOfSight(tower, projectile, pos)) return false

            // 轨迹威胁分析：弹射物是否正飞向防御塔
            return isOnCollisionCourse(tower, projectile, towerConfig.projectileInterceptDistance)
        }

        /**
         * 轨迹威胁分析：判断弹射物是否在碰撞航线上。
         *
         * 算法：计算弹射物运动射线与防御塔包围盒的最近距离，
         * 如果小于阈值且弹射物正朝防御塔移动，则视为威胁。
         */
        fun isOnCollisionCourse(tower: AutoAimableEntity, projectile: Projectile, threshold: Double): Boolean {
            val projPos = projectile.position()
            val velocity = projectile.deltaMovement
            val speed = velocity.length()

            if (speed < 0.01) return false

            val direction = velocity.normalize()
            val towerCenter = tower.boundingBox.center

            // 弹射物到防御塔的向量
            val toTower = towerCenter.subtract(projPos)

            // 弹射物是否正在朝防御塔方向移动
            val dotProduct = toTower.normalize().dot(direction)
            if (dotProduct < -0.3) return false // 弹射物在远离

            // 计算弹射物射线与防御塔包围盒的最近距离
            val closestDist = rayToAABBDistance(projPos, direction, tower.boundingBox.inflate(threshold))

            return closestDist <= threshold
        }

        /**
         * 计算射线到 AABB 的最短距离。
         */
        private fun rayToAABBDistance(rayOrigin: Vec3, rayDir: Vec3, aabb: AABB): Double {
            // 找到 AABB 上离射线最近的点
            // 使用 AABB 到射线的最短距离近似
            val center = aabb.center
            val toCenter = center.subtract(rayOrigin)

            // 投影到射线方向上
            val t = toCenter.dot(rayDir)
            val closestOnRay = if (t <= 0) {
                rayOrigin
            } else {
                rayOrigin.add(rayDir.scale(t))
            }

            // 将最近点钳制到 AABB 内
            val clamped = Vec3(
                closestOnRay.x.coerceIn(aabb.minX, aabb.maxX),
                closestOnRay.y.coerceIn(aabb.minY, aabb.maxY),
                closestOnRay.z.coerceIn(aabb.minZ, aabb.maxZ),
            )

            return clamped.distanceTo(closestOnRay)
        }

        /**
         * 检查目标和炮塔之间是否有障碍物遮挡。
         */
        fun checkLineOfSight(tower: AutoAimableEntity, target: Entity, pos: Vec3): Boolean {
            return tower.level().clip(
                ClipContext(
                    pos, target.boundingBox.center,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, tower
                )
            ).type != HitResult.Type.BLOCK
        }
    }

    // endregion

    // region TargetPrioritizer

    /**
     * 目标优先级评分器。
     */
    object TargetPrioritizer {

        /**
         * 对目标进行威胁评分。
         *
         * @return 评分（越高越优先），返回 null 表示该目标不应被考虑
         */
        fun evaluateThreat(tower: AutoAimableEntity, target: Entity): Double? {
            val config = tower.threatConfig

            val distance = target.distanceTo(tower)
            val maxRange = tower.data().compute().seekInfo?.maxSeekRange ?: 64.0

            // 距离评分（归一化到 [0, 1]，越近越高）
            val distanceScore = max(0.0, 1.0 - distance / maxRange) * 100.0 * config.distanceWeight

            // 实体类型评分
            val typeScore = getEntityTypeScore(target) * config.typeWeight

            // 生命值评分（低血量目标优先补刀）
            val healthScore = if (target is LivingEntity) {
                val healthPercent = target.health / target.maxHealth
                (1.0 - healthPercent) * 100.0 * config.healthWeight
            } else {
                0.0
            }

            // 弹射物威胁加成
            var projectileBonus = 0.0
            if (target is Projectile) {
                // 碰撞航线判定
                if (TargetValidator.isOnCollisionCourse(tower, target, config.projectileInterceptDistance)) {
                    projectileBonus += config.projectileThreatBonus
                }
                // 弹射物速度加成
                projectileBonus += target.deltaMovement.length() * config.projectileSpeedMultiplier
            }

            // 近期攻击者加成
            var attackerBonus = 0.0
            val lastAttacker = EntityFindUtil.findEntity(tower.level(), tower.lastAttackerUUID)
            if (lastAttacker != null && target == lastAttacker) {
                attackerBonus = config.recentAttackerBonus
            }

            return distanceScore + typeScore + healthScore + projectileBonus + attackerBonus
        }

        /**
         * 获取实体类型基础分数。
         */
        private fun getEntityTypeScore(target: Entity): Double {
            return when (target) {
                is Player -> 100.0
                is VehicleEntity -> 75.0
                is Enemy -> 50.0
                is Projectile -> 30.0
                is LivingEntity -> 25.0
                else -> 10.0
            }
        }

        /**
         * 从候选目标列表中选择最佳目标。
         *
         * @return 评分最高的目标，如果所有目标评分都低于阈值则返回 null
         */
        fun selectBestTarget(tower: AutoAimableEntity, candidates: List<Entity>): Entity? {
            val config = tower.threatConfig

            return candidates
                .mapNotNull { target ->
                    val score = evaluateThreat(tower, target) ?: return@mapNotNull null
                    target to score
                }
                .filter { (_, score) -> score >= config.minThreatScore }
                .maxByOrNull { (_, score) -> score }
                ?.first
        }

        /**
         * 判断是否应该从当前目标切换到新目标。
         * 使用滞后机制防止频繁切换。
         */
        fun shouldSwitchTarget(
            tower: AutoAimableEntity,
            currentTarget: Entity?,
            newTarget: Entity?,
        ): Boolean {
            if (newTarget == null) return false
            if (currentTarget == null) return true
            if (currentTarget == newTarget) return false

            val config = tower.threatConfig

            val currentScore = evaluateThreat(tower, currentTarget) ?: return true
            val newScore = evaluateThreat(tower, newTarget) ?: return false

            // 新目标必须显著优于当前目标才切换
            return newScore > currentScore + config.targetSwitchHysteresis
        }
    }

    // endregion

    // region TargetTracker

    /**
     * 目标追踪器，管理目标获取、验证与切换。
     */
    inner class TargetTracker {

        /** 当前锁定的目标 UUID 字符串 */
        var currentTargetUUID: String
            get() = tower.targetUUID
            private set(value) {
                tower.targetUUID = value
            }

        /** 目标切换计时器 */
        var changeTargetTimer: Int
            get() = tower.changeTargetTimer
            set(value) {
                tower.changeTargetTimer = value
            }

        /** 上次搜索目标的 tick */
        private var lastSeekTick: Int = 0

        /**
         * 验证当前锁定目标是否仍然有效。
         * 如果无效则清除锁定。
         *
         * @param barrelPos 炮管根部位置，用于视线检查
         * @return true 如果目标有效
         */
        fun validateCurrentTarget(barrelPos: Vec3): Boolean {
            val targetUUID = currentTargetUUID
            if (targetUUID.isEmpty()) return false

            val target = EntityFindUtil.findEntity(tower.level(), targetUUID) ?: run {
                clearTarget()
                return false
            }

            if (!target.isAlive) {
                clearTarget()
                return false
            }

            if (target is LivingEntity && target.health <= 0) {
                clearTarget()
                return false
            }

            if (target is VehicleEntity && target.isWreck) {
                clearTarget()
                return false
            }

            if (SeekTool.IS_INVULNERABLE.test(target)) {
                clearTarget()
                return false
            }

            if (!SeekTool.NOT_IN_SMOKE.test(target)) {
                clearTarget()
                return false
            }

            val seekInfo = tower.data().compute().seekInfo ?: return false
            val distance = target.distanceTo(tower)
            if (distance < seekInfo.minSeekRange || distance > seekInfo.maxSeekRange) {
                clearTarget()
                return false
            }

            // 视线检查：目标是否被方块遮挡
            if (!TargetValidator.checkLineOfSight(tower, target, barrelPos)) {
                clearTarget()
                return false
            }

            // 检查目标是否骑到了其他载具上
            val targetVehicle = target.vehicle
            if (targetVehicle != null && targetVehicle !is AutoAimableEntity) {
                currentTargetUUID = targetVehicle.stringUUID
            }

            return true
        }

        /**
         * 扫描周围实体并获取最佳目标。
         * 仅在达到搜索间隔时执行扫描。
         *
         * @param pos 炮管根部位置
         * @param minAngle 最小俯仰角
         * @param maxAngle 最大俯仰角
         * @param minRange 最小搜索距离
         * @param maxRange 最大搜索距离
         * @param minTargetSize 最小目标体积
         * @param seekIterative 搜索间隔 (tick)
         */
        fun acquireTarget(
            pos: Vec3,
            minAngle: Double,
            maxAngle: Double,
            minRange: Double,
            maxRange: Double,
            minTargetSize: Double,
            seekIterative: Int,
        ) {
            // 非搜索 tick 时跳过
            if (tower.tickCount - lastSeekTick < seekIterative) return
            lastSeekTick = tower.tickCount

            // 收集有效候选目标
            val seekRangeSqr = maxRange * maxRange
            val minRangeSqr = minRange * minRange

            val candidates = tower.level().getEntitiesOfClass(
                Entity::class.java,
                AABB(pos, pos).inflate(maxRange)
            ) { true }
                .filter { target ->
                    val distSqr = target.distanceToSqr(tower)
                    distSqr > minRangeSqr && distSqr <= seekRangeSqr
                            // 显式检查炮管到目标之间是否有方块遮挡
                            && TargetValidator.checkLineOfSight(tower, target, pos)
                            && TargetValidator.isValidTarget(tower, target, pos, minAngle, maxAngle, minRange, maxRange)
                }

            if (candidates.isEmpty()) return

            // 选出最佳目标
            val bestTarget = TargetPrioritizer.selectBestTarget(tower, candidates) ?: return

            // 检查是否需要切换目标
            val currentTarget = if (currentTargetUUID.isNotEmpty()) {
                EntityFindUtil.findEntity(tower.level(), currentTargetUUID)
            } else null

            if (TargetPrioritizer.shouldSwitchTarget(tower, currentTarget, bestTarget)) {
                currentTargetUUID = bestTarget.stringUUID
                changeTargetTimer = 0
                tower.consumeEnergy(tower.data().compute().seekInfo?.seekEnergyCost ?: 0)
            }
        }

        /**
         * 清除当前目标。
         */
        fun clearTarget() {
            currentTargetUUID = ""
            changeTargetTimer = 0
        }
    }

    // endregion

    // region Public API

    /** 目标追踪器实例 */
    val tracker: TargetTracker = TargetTracker()

    // endregion
}
