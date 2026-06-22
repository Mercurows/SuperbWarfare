package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.world.saveddata.TDMSavedData
import net.minecraft.core.BlockPos
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.*
import net.minecraft.world.entity.decoration.HangingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.util.TriPredicate
import java.util.function.BiPredicate
import java.util.function.Predicate

infix fun <T> Predicate<T>.and(other: Predicate<T>): Predicate<T> =
    Predicate { this.test(it) && other.test(it) }

infix fun <T> Predicate<T>.or(other: Predicate<T>): Predicate<T> =
    Predicate { this.test(it) || other.test(it) }

operator fun <T> Predicate<T>.not(): Predicate<T> =
    Predicate { !this.test(it) }

@Suppress("unused")
object SeekTool {
    /** 判断实体是否存活 */
    @JvmField
    val IS_ALIVE: Predicate<Entity?> = Predicate { it?.isAlive == true }

    /** 判断实体不是旁观者 */
    @JvmField
    val NOT_SPECTATOR: Predicate<Entity?> = Predicate { e -> e == null || e !is Player || !e.isSpectator }

    /** 判定实体是否位于黑名单中 */
    @JvmField
    val IN_BLACKLIST: Predicate<Entity?> = Predicate { it?.type?.`is`(ModTags.EntityTypes.SEEK_BLACKLIST) == true }

    /** 基础类型过滤：排除 HangingEntity、Display、不可摧毁弹射物 */
    @JvmField
    val BASIC_TYPE_FILTER: Predicate<Entity?> = Predicate { e ->
        e != null && e !is HangingEntity && e !is Display &&
                (e !is Projectile || e.type.`is`(ModTags.EntityTypes.DESTROYABLE_PROJECTILE))
    }

    /** 基础实体过滤 = 存活 ∧ 非旁观 ∧ 基础类型 ∧ 非黑名单 */
    @JvmField
    val BASIC_FILTER: Predicate<Entity?> = IS_ALIVE and NOT_SPECTATOR and BASIC_TYPE_FILTER and !IN_BLACKLIST

    /** 判断实体是否无敌 */
    @JvmField
    val IS_INVULNERABLE: Predicate<Entity?> = Predicate { e ->
        e != null && (e.isInvulnerable || (e is Player && (e.isCreative || e.isSpectator)))
    }

    /** 判断实体是否不是玩家 */
    @JvmField
    val NOT_PLAYER: Predicate<Entity?> = Predicate { it == null || it !is Player }

    /** 判断实体是否在地面上（离地 0 米） */
    @JvmField
    val ON_GROUND: Predicate<Entity?> = Predicate { it != null && ON_GROUND_HEIGHT.test(it, 0.0) }

    /** 判断实体是否不在烟雾中 */
    @JvmField
    val NOT_IN_SMOKE: Predicate<Entity?> = Predicate { e ->
        if (e == null) return@Predicate false
        val box = e.boundingBox.inflate(8.0)
        e.level().getEntities(
            EntityTypeTest.forClass(Entity::class.java), box
        ) { it is SmokeDecoyEntity && e !is SmokeDecoyEntity }.isEmpty()
    }

    /** 带自定义范围的烟雾检测 */
    @JvmField
    val NOT_IN_SMOKE_WITH_RANGE: BiPredicate<Entity?, Double> = BiPredicate { e, range ->
        if (e == null) return@BiPredicate false
        e.level().getEntities(
            EntityTypeTest.forClass(Entity::class.java), e.boundingBox.inflate(range)
        ) { it is SmokeDecoyEntity }.isEmpty()
    }

    /** 判断实体是否在离地面 height 米的范围内 */
    @JvmField
    val ON_GROUND_HEIGHT: BiPredicate<Entity?, Double> = BiPredicate { entity, height ->
        if (entity == null) return@BiPredicate false
        val level = entity.level()
        val y = entity.y
        val minY = level.minBuildHeight
        val maxY = level.maxBuildHeight
        if (y < minY || y > maxY) return@BiPredicate false

        var onGround = false
        val aabb = entity.boundingBox.expandTowards(0.0, -height, 0.0)
        BlockPos.betweenClosedStream(aabb).forEach { pos ->
            if (pos.y in minY..maxY && !level.getBlockState(pos).isAir) onGround = true
        }
        entity.onGround() || entity.isInWater || onGround
    }

    /** 判断实体离地高度是否在 [min, max] 区间 */
    @JvmField
    val IN_HEIGHT_RANGE: TriPredicate<Entity?, Double, Double> = TriPredicate { entity, min, max ->
        if (entity == null) return@TriPredicate true
        val level = entity.level()
        val pos = entity.onPos
        val y = pos.y
        val minY = level.minBuildHeight
        val maxY = level.maxBuildHeight
        if (y !in minY..maxY) return@TriPredicate true
        if (level.isClientSide && level.getEntity(entity.id) == null) return@TriPredicate true

        var height = 0
        while (true) {
            height++
            if (height !in minY..maxY) return@TriPredicate false
            if (!level.getBlockState(pos.offset(0, -height, 0)).isAir) break
        }
        height >= min && height <= max
    }

    /** 判断两个实体是否在同一队伍 */
    @JvmField
    val IN_SAME_TEAM: BiPredicate<Entity?, Entity?> = BiPredicate { self, target ->
        self != null && target != null &&
                (self === target || (target.team != null && !TDMSavedData.enabledTDM(target) && target.team == self.team))
    }

    /** 判断目标是否为友方无人机 */
    @JvmField
    val IS_FRIENDLY_DRONE: BiPredicate<Entity?, Entity?> = BiPredicate { self, target ->
        if (self !is Player || target == null) return@BiPredicate false
        val stack = self.mainHandItem
        var myDrone: DroneEntity? = null
        if (stack.`is`(ModItems.MONITOR.get()) &&
            stack.orCreateTag.getBoolean("Using") &&
            stack.orCreateTag.getBoolean("Linked")
        ) {
            myDrone = EntityFindUtil.findDrone(self.level(), stack.orCreateTag.getString("LinkedDrone"))
        }
        target is DroneEntity &&
                target !== myDrone &&
                target.controller != null &&
                IN_SAME_TEAM.test(target, target.controller)
    }

    /** 判断两个实体是否是友方关系 */
    @JvmField
    val IS_FRIENDLY: BiPredicate<Entity?, Entity?> = BiPredicate { self, target ->
        if (target == null) return@BiPredicate false
        if (IN_SAME_TEAM.test(self, target)) return@BiPredicate true
        if (target is OwnableEntity && target.owner != null && IN_SAME_TEAM.test(
                self,
                target.owner
            )
        ) return@BiPredicate true
        if (IS_FRIENDLY_DRONE.test(self, target)) return@BiPredicate true
        if (target.passengers.any { IN_SAME_TEAM.test(self, it) }) return@BiPredicate true
        if (target is VehicleEntity) {
            val lastDriver = EntityFindUtil.findEntity(target.level(), target.lastDriverUUID)
            if (lastDriver != null && IN_SAME_TEAM.test(self, lastDriver)) return@BiPredicate true
        }
        false
    }

    /** 判断某实体是否是自己的 */
    @JvmField
    val IS_OWNER: BiPredicate<Entity?, Entity?> = BiPredicate { self, target ->
        when (target) {
            is TraceableEntity -> target.owner === self
            is OwnableEntity -> target.owner === self
            else -> false
        }
    }

    /** 判断某实体是否不是自己的 */
    @JvmField
    val IS_NOT_OWNER: BiPredicate<Entity?, Entity?> = BiPredicate { self, target ->
        when (target) {
            is TraceableEntity -> target.owner !== self
            is OwnableEntity -> target.owner !== self
            else -> true
        }
    }

    @JvmStatic
    fun calculateAngle(entityA: Entity, entityB: Entity): Double {
        val start = Vec3(
            entityA.x - entityB.x,
            entityA.y - entityB.y,
            entityA.z - entityB.z
        )
        return VectorTool.calculateAngle(start, entityB.lookAngle)
    }

    @JvmStatic
    fun calculateAngle(pos: Vec3?, vec3: Vec3?, entityA: Entity): Double {
        if (pos == null || vec3 == null) return Double.MAX_VALUE
        return VectorTool.calculateAngle(pos.vectorTo(entityA.position()), vec3)
    }

    @JvmStatic
    fun getEntitiesWithinRange(pos: BlockPos?, level: Level, range: Double): List<Entity> {
        if (pos == null) return emptyList()
        return EntityFindUtil.getEntities(level).all.asSequence()
            .filter { e ->
                e.distanceToSqr(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) <= range * range
                        && BASIC_FILTER.test(e)
                        && NOT_IN_SMOKE.test(e)
                        && !e.type.`is`(ModTags.EntityTypes.DECOY)
            }
            .toList()
    }

    class Builder @JvmOverloads constructor(private val entity: Entity, excludeSelf: Boolean = true) {
        private val filters: MutableList<Predicate<Entity>> = mutableListOf()
        private var canGuidedByRadarFlag: Boolean = false

        init {
            if (excludeSelf) {
                filters.add { it !== entity }
            }
        }

        /**
         * 将原先 5 个 build 方法中各自复制的 Entity 源组装逻辑收敛至此。
         * 只有这里关心 ClientSyncedEntityHandler 合并逻辑。
         */
        private fun entitySource(): Sequence<Entity> {
            val base = EntityFindUtil.getEntities(entity.level()).all.asSequence()
            if (!entity.level().isClientSide || !canGuidedByRadarFlag) return base
            val synced = ClientSyncedEntityHandler.getSyncedHostileEntities(entity.level())
            return if (synced.isEmpty()) base else base + synced.asSequence()
        }

        private fun evaluate(): Sequence<Entity> =
            entitySource().filter { e -> filters.all { it.test(e) } }

        fun build(): List<Entity> = evaluate().toList()

        fun buildSeekWeapon(canGuidedByRadar: Boolean): List<Entity> {
            canGuidedByRadarFlag = canGuidedByRadar
            return evaluate().toList()
        }

        fun buildWithClosest(): Entity? =
            evaluate().minByOrNull { calculateAngle(it, entity) }

        fun buildWithClosestSeekWeapon(canGuidedByRadar: Boolean): Entity? {
            canGuidedByRadarFlag = canGuidedByRadar
            return evaluate().minByOrNull { calculateAngle(it, entity) }
        }

        fun buildWithClosest(pos: Vec3?, vec3: Vec3?, canGuidedByRadar: Boolean): Entity? {
            canGuidedByRadarFlag = canGuidedByRadar
            if (pos == null || vec3 == null) return null
            return evaluate().minByOrNull { calculateAngle(pos, vec3, it) }
        }

        fun notItsVehicle(): Builder = apply {
            filters.add { it.vehicle !== entity }
        }

        fun withinRange(range: Double): Builder = apply {
            filters.add { e ->
                val synced = ClientSyncedEntityHandler.getSyncedHostileEntities(entity.level())
                if (synced.isNotEmpty() && entity.level().isClientSide && entity is Player &&
                    (entity.level().getEntity(e.id) == null || e in synced)
                ) {
                    return@add true
                }
                distanceCheck(e, entity.eyePosition, range)
            }
        }

        fun withinRangeSeekWeapon(
            range: Double,
            maxGuidedRange: Double,
            affectedByStealthTarget: Boolean,
            canGuidedByRadar: Boolean
        ): Builder = apply {
            filters.add { e ->
                if (canGuidedByRadar) {
                    val synced = ClientSyncedEntityHandler.getSyncedHostileEntities(entity.level())
                    if (synced.isNotEmpty() && entity.level().isClientSide && entity is Player &&
                        (entity.level().getEntity(e.id) == null || e in synced)
                    ) {
                        return@add e.position().distanceToSqr(entity.eyePosition) <= maxGuidedRange * maxGuidedRange
                    }
                }
                if (e is VehicleEntity && affectedByStealthTarget) {
                    return@add distanceCheck(e, entity.eyePosition, range)
                }
                e.position().distanceToSqr(entity.eyePosition) <= range * range
            }
        }

        fun withinRange(vec3: Vec3?, range: Double): Builder = apply {
            if (vec3 == null) return@apply
            filters.add { distanceCheck(it, vec3, range) }
        }

        fun overRange(range: Double): Builder = apply {
            filters.add { e ->
                val multiplier = (e as? VehicleEntity)?.computed()?.trackDistanceMultiply ?: 1.0
                val effectiveRange = range * multiplier
                e.position().distanceToSqr(entity.eyePosition) > effectiveRange * effectiveRange
            }
        }

        fun sameTeam(): Builder = apply {
            filters.add { IN_SAME_TEAM.test(entity, it) }
        }

        fun differentTeam(): Builder = apply {
            filters.add { !IN_SAME_TEAM.test(entity, it) }
        }

        fun friendly(): Builder = apply {
            filters.add { IS_FRIENDLY.test(entity, it) }
        }

        fun notFriendly(): Builder = apply {
            filters.add { !IS_FRIENDLY.test(entity, it) }
        }

        fun notPlayer(): Builder = apply {
            filters.add { NOT_PLAYER.test(it) }
        }

        fun blackList(): Builder = apply {
            filters.add { IN_BLACKLIST.test(it) }
        }

        fun smokeFilter(): Builder = apply {
            filters.add { NOT_IN_SMOKE.test(it) }
        }

        fun baseFilter(): Builder = apply {
            filters.add { BASIC_FILTER.test(it) }
        }

        fun onGround(height: Double): Builder = apply {
            filters.add { ON_GROUND_HEIGHT.test(it, height) }
        }

        fun withinAngle(angle: Double): Builder = apply {
            filters.add { calculateAngle(it, entity) < angle }
        }

        fun withinAngle(pos: Vec3?, vec3: Vec3?, angle: Double): Builder = apply {
            if (pos == null || vec3 == null) return@apply
            filters.add { calculateAngle(pos, vec3, it) < angle }
        }

        fun `is`(clazz: Class<out Entity>): Builder = apply {
            filters.add(clazz::isInstance)
        }

        fun isNot(clazz: Class<out Entity>): Builder = apply {
            filters.add { !clazz.isInstance(it) }
        }

        fun `is`(tagKey: TagKey<EntityType<*>>): Builder = apply {
            filters.add { it.type.`is`(tagKey) }
        }

        fun isNot(tagKey: TagKey<EntityType<*>>): Builder = apply {
            filters.add { !it.type.`is`(tagKey) }
        }

        fun isOwner(): Builder = apply {
            filters.add { IS_OWNER.test(entity, it) }
        }

        fun isNotOwner(): Builder = apply {
            filters.add { IS_NOT_OWNER.test(entity, it) }
        }

        fun noClip(): Builder = apply {
            filters.add { e ->
                entity.level()
                    .clip(
                        ClipContext(
                            entity.eyePosition, e.eyePosition,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity
                        )
                    )
                    .type != HitResult.Type.BLOCK
            }
        }

        fun vehicleNoClip(entity: Entity?): Builder = apply {
            if (entity == null) return@apply
            filters.add { _ ->
                val self = this.entity
                if (self is VehicleEntity) {
                    self.level()
                        .clip(
                            ClipContext(
                                self.getZoomPos(entity, 1.0f), self.getZoomPos(entity, 1.0f),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self
                            )
                        )
                        .type != HitResult.Type.BLOCK
                } else false
            }
        }

        fun hasVehicle(): Builder = apply {
            filters.add { it.vehicle != null }
        }

        fun noVehicle(): Builder = apply {
            filters.add { it.vehicle == null }
        }

        fun sizeBiggerThan(size: Double): Builder = apply {
            filters.add { it.boundingBox.size >= size }
        }

        /** 与 sizeBiggerThan 语义相同，保留以兼容旧调用 */
        fun sizeGreaterThan(size: Double): Builder = sizeBiggerThan(size)

        fun custom(predicate: Predicate<Entity>): Builder = apply {
            filters.add(predicate)
        }

        fun custom(predicate: BiPredicate<Entity, Entity>): Builder = apply {
            filters.add { predicate.test(entity, it) }
        }

        fun heightRange(min: Double, max: Double): Builder = apply {
            filters.add { IN_HEIGHT_RANGE.test(it, min, max) }
        }

        companion object {
            /**
             * 提取 VehicleEntity 距离倍率计算为纯函数，消除各处重复的 instanceof 判断。
             */
            private fun distanceCheck(e: Entity, from: Vec3, range: Double): Boolean {
                val multiplier = (e as? VehicleEntity)?.computed()?.trackDistanceMultiply ?: 1.0
                val effectiveRange = range * multiplier
                return e.position().distanceToSqr(from) <= effectiveRange * effectiveRange
            }
        }
    }

    /**
     * Kotlin DSL 风格查询入口：
     * ```
     * val result = seekQuery(player) {
     *     withinRange(32.0)
     *     withinAngle(16.0)
     *     baseFilter()
     *     smokeFilter()
     *     noVehicle()
     *     notFriendly()
     *     noClip()
     * }.buildWithClosest()
     * ```
     */
    fun seekQuery(entity: Entity, excludeSelf: Boolean = true, block: Builder.() -> Unit): Builder =
        Builder(entity, excludeSelf).apply(block)

    @JvmStatic
    fun seekEntity(entity: Entity, range: Double, angle: Double): Entity? =
        Builder(entity)
            .withinRange(range).withinAngle(angle)
            .baseFilter().smokeFilter().noVehicle().noClip()
            .buildWithClosest()

    @JvmStatic
    fun seekLivingEntity(entity: Entity, range: Double, angle: Double): Entity? =
        Builder(entity)
            .withinRange(range).withinAngle(angle)
            .baseFilter().smokeFilter().noVehicle()
            .notFriendly().isNotOwner().noClip()
            .buildWithClosest()

    @JvmStatic
    fun seekLivingEntities(entity: Entity, seekRange: Double, seekAngle: Double): List<Entity> =
        Builder(entity)
            .withinRange(seekRange).withinAngle(seekAngle)
            .baseFilter().smokeFilter().noVehicle()
            .notFriendly().noClip()
            .build()

    @JvmStatic
    fun seekLivingEntitiesThroughWall(entity: Entity, range: Double, angle: Double): List<Entity> =
        Builder(entity)
            .withinRange(range).withinAngle(angle)
            .baseFilter().noVehicle().notFriendly()
            .build()

    @JvmStatic
    fun seekEntityThroughWall(entity: Entity, range: Double, angle: Double): Entity? =
        Builder(entity)
            .withinRange(range).withinAngle(angle)
            .baseFilter().noVehicle().notFriendly()
            .buildWithClosest()
}

/** 搜索最近的存活敌对实体 */
fun Entity.seekLiving(range: Double, angle: Double): Entity? =
    SeekTool.seekLivingEntity(this, range, angle)

/** 搜索所有存活敌对实体 */
fun Entity.seekLivingList(range: Double, angle: Double): List<Entity> =
    SeekTool.seekLivingEntities(this, range, angle)

/** 搜索最近的任意实体 */
fun Entity.seekAny(range: Double, angle: Double): Entity? =
    SeekTool.seekEntity(this, range, angle)

/** DSL 查询入口 */
fun Entity.seekQuery(block: SeekTool.Builder.() -> Unit): SeekTool.Builder =
    SeekTool.seekQuery(this, block = block)
