package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback
import com.atsuishio.superbwarfare.entity.multipartRoot
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.EnderDragonPart
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.entity.PartEntity
import net.minecraftforge.network.NetworkHooks
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

open class LaserBeamEntity(type: EntityType<out LaserBeamEntity>, level: Level) :
    Projectile(type, level), IBulletProperties {

    private var damageValue = 0f
    private var headShotMultiplier = 1f
    private var fired = false
    var power by POWER
        private set
    var startTime by START_TIME
        private set
    var ownerId by OWNER_ID
        private set
    private var endX by END_X
    private var endY by END_Y
    private var endZ by END_Z
    private var visualLife by LIFE
    var hitBlock by HIT_BLOCK
        private set
    private var widthValue by WIDTH
    var beamColor: Int by COLOR

    /** Full hit diameter in blocks; an omitted value follows the charged visual core. */
    var beamWidth: Double
        get() = if (widthValue < 0) radiusForPower(power.toDouble()) * 2 else widthValue.toDouble()
        set(value) { widthValue = if (value.isFinite()) value.coerceIn(0.0, 32.0).toFloat() else 0f }

    val beamEnd: Vec3 get() = position().add(endX.toDouble(), endY.toDouble(), endZ.toDouble())
    val beamRadius: Double get() = beamWidth * 0.5

    init {
        noPhysics = true
        isNoGravity = true
        noCulling = true
    }

    override fun defineSynchedData() {
        entityData.define(POWER, 1f)
        entityData.define(START_TIME, 0L)
        entityData.define(OWNER_ID, -1)
        entityData.define(END_X, 0f)
        entityData.define(END_Y, 0f)
        entityData.define(END_Z, 0f)
        entityData.define(LIFE, 8)
        entityData.define(HIT_BLOCK, false)
        entityData.define(WIDTH, -1f)
        entityData.define(COLOR, 0xFF0614)
    }

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> = NetworkHooks.getEntitySpawningPacket(this)
    override fun getDamage() = damageValue
    override fun setDamage(value: Float) {
        damageValue = value.coerceAtLeast(0f)
    }

    override fun getHeadShot() = headShotMultiplier
    override fun setHeadShot(value: Float) {
        headShotMultiplier = value.coerceAtLeast(0f)
    }

    override fun getLife() = visualLife
    override fun setLife(value: Int) {
        visualLife = value.coerceIn(4, 20)
    }

    override fun isPenetrating() = true

    override fun setRGB(rgb: FloatArray) {
        if (rgb.size < 3) return
        fun channel(value: Float) = if (value.isFinite()) (value.coerceIn(0f, 1f) * 255).toInt() else 0
        beamColor = (channel(rgb[0]) shl 16) or (channel(rgb[1]) shl 8) or channel(rgb[2])
    }

    override fun getRGB() = floatArrayOf(
        ((beamColor shr 16) and 255) / 255f, ((beamColor shr 8) and 255) / 255f, (beamColor and 255) / 255f
    )

    @Deprecated("Deprecated in Java")
    override fun isPushedByFluid() = false

    /** Called after the normal gun pipeline has applied damage, perks, position and firing direction. */
    fun prepareShot(range: Double, chargePower: Double) {
        power = chargePower.coerceIn(0.0, 1.0).toFloat()
        startTime = level().gameTime
        ownerId = owner?.id ?: -1
        val direction = deltaMovement.normalize()
        val end = position().add(direction.scale(range.coerceAtLeast(0.0)))
        val hit = level().clip(ClipContext(position(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
        hitBlock = hit.type == HitResult.Type.BLOCK
        val offset = hit.location.subtract(position())
        endX = offset.x.toFloat()
        endY = offset.y.toFloat()
        endZ = offset.z.toFloat()
        deltaMovement = Vec3.ZERO
    }

    override fun tick() {
        super.tick()
        if (level().isClientSide) return
        if (!fired) {
            // Mark before dealing damage: piercing and the visual lifetime must never repeat the shot.
            fired = true
            fireBeam()
        }
        if (level().gameTime - startTime >= visualLife) discard()
    }

    open fun canDamage(target: Entity): Boolean {
        val root = target.multipartRoot
        val shooter = owner
        return target.isAlive && root.isAlive && !root.isSpectator && target.canBeHitByProjectile()
                && root !== shooter && root !is Projectile
                && (target is PartEntity<*> || !target.isMultipartEntity)
                && !(root is Player && root.isCreative)
                && !(root is Player && shooter is Player && !shooter.canHarmPlayer(root))
                && !SeekTool.IS_FRIENDLY.test(shooter, root)
                && (shooter == null || !shooter.isPassengerOfSameVehicle(root))
    }

    data class BeamIntersection(val axis: Vec3, val contact: Vec3)

    private data class BeamHit(val target: Entity, val point: Vec3, val headshot: Boolean)

    private fun fireBeam() {
        val start = position()
        val end = beamEnd
        val offset = end.subtract(start)
        val segments = ceil(offset.length() / 16.0).toInt().coerceAtLeast(1)
        val examined = hashSetOf<Int>()
        val hits = linkedMapOf<Int, BeamHit>()
        val source = ModDamageTypes.causeLaserDamage(level().registryAccess(), this, owner)
        val headshotSource = ModDamageTypes.causeLaserHeadshotDamage(level().registryAccess(), this, owner)
        var headshotHit = false
        fun examine(candidate: Entity) {
            if (!examined.add(candidate.id) || !canDamage(candidate)) return
            val hit = hitPosition(candidate, start, end) ?: return
            val rootId = candidate.multipartRoot.id
            val previous = hits[rootId]
            if (previous == null || start.distanceToSqr(hit.point) < start.distanceToSqr(previous.point)) hits[rootId] = hit
        }
        // Short query boxes avoid scanning a huge diagonal bounding box for a 128-block ray.
        for (segment in 0 until segments) {
            val from = start.add(offset.scale(segment.toDouble() / segments))
            val to = start.add(offset.scale((segment + 1.0) / segments))
            for (candidate in level().getEntities(this, AABB(from, to).inflate(beamRadius + 1.0), ::canDamage)) {
                examine(candidate)
            }
        }
        // Rewound players can lie completely outside the current-position segment queries.
        (level() as? ServerLevel)?.players()?.forEach(::examine)
        // Select the nearest intersected part per parent, independent of query/part iteration order.
        for ((target, _, headshot) in hits.values) {
            if (!canDamage(target)) continue
            val root = target.multipartRoot
            val knockback = if (root is LivingEntity) ICustomKnockback.getInstance(root) else null
            val previousStrength = knockback?.`superbWarfare$getKnockbackStrength`()
            knockback?.`superbWarfare$setKnockbackStrength`(0.0)
            try {
                root.invulnerableTime = 0
                target.invulnerableTime = 0
                val hurt = target.forceHurt(
                    if (headshot) headshotSource else source,
                    getDamage() * if (headshot) getHeadShot() else 1f
                )
                if (hurt && headshot) headshotHit = true
            } finally {
                if (previousStrength != null) knockback.`superbWarfare$setKnockbackStrength`(previousStrength)
            }
        }
        if (headshotHit) (owner as? ServerPlayer)?.sendPacket(ClientIndicatorMessage(1, 5))
    }

    private fun hitPosition(target: Entity, start: Vec3, end: Vec3): BeamHit? {
        val root = target.multipartRoot
        var baseY = root.y
        var eyeHeight = root.eyeHeight.toDouble()
        val hit: BeamIntersection
        if (target is OBBEntity && !target.enableAABB()) {
            hit = target.getOBBs().asSequence()
                .filter { it.part != OBB.Part.COLLISION }
                .mapNotNull { intersectBeam(it, start, end, beamRadius) }
                .minByOrNull { start.distanceToSqr(it.axis) } ?: return null
        } else {
            val bounds = if (target is ServerPlayer) HitboxHelper.getCompensatedBoundingBox(target, owner) else target.boundingBox
            if (target is ServerPlayer) {
                baseY = 0.0
                eyeHeight = HitboxHelper.getCompensatedEyeY(target, owner)
            }
            hit = intersectBeam(bounds, start, end, beamRadius) ?: return null
        }
        // Width must not let an end cap or a side of the beam reach through solid cover.
        val block = level().clip(ClipContext(start, hit.contact, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
        if (block.type == HitResult.Type.BLOCK && start.distanceToSqr(block.location) + 1e-8 < start.distanceToSqr(hit.contact)) return null
        val headshot = if (target is EnderDragonPart) target === target.parentMob.head
        else root is LivingEntity && isHeadshot(hit.contact.y - baseY, eyeHeight)
        return BeamHit(target, hit.axis, headshot)
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        if (tag.contains("Damage")) setDamage(tag.getFloat("Damage"))
        if (tag.contains("HeadShot")) setHeadShot(tag.getFloat("HeadShot"))
        if (tag.contains("Life")) setLife(tag.getInt("Life"))
        if (tag.contains("Power")) power = tag.getFloat("Power").coerceIn(0f, 1f)
        if (tag.contains("StartTime")) startTime = tag.getLong("StartTime")
        if (tag.contains("Fired")) fired = tag.getBoolean("Fired")
        if (tag.contains("EndX")) endX = tag.getFloat("EndX")
        if (tag.contains("EndY")) endY = tag.getFloat("EndY")
        if (tag.contains("EndZ")) endZ = tag.getFloat("EndZ")
        if (tag.contains("HitBlock")) hitBlock = tag.getBoolean("HitBlock")
        if (tag.contains("BeamWidth")) beamWidth = tag.getDouble("BeamWidth")
        if (tag.contains("Color")) beamColor = tag.getInt("Color") and 0xFFFFFF
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        tag.putFloat("Damage", getDamage())
        tag.putFloat("HeadShot", getHeadShot())
        tag.putInt("Life", getLife())
        tag.putFloat("Power", power)
        tag.putLong("StartTime", startTime)
        tag.putBoolean("Fired", fired)
        tag.putFloat("EndX", endX)
        tag.putFloat("EndY", endY)
        tag.putFloat("EndZ", endZ)
        tag.putBoolean("HitBlock", hitBlock)
        if (widthValue >= 0) tag.putDouble("BeamWidth", beamWidth)
        tag.putInt("Color", beamColor)
    }

    companion object {
        /** Finite round beam vs box; avoids the square corners of an inflated AABB. */
        fun intersectBeam(bounds: AABB, start: Vec3, end: Vec3, radius: Double): BeamIntersection? {
            val r = radius.coerceAtLeast(0.0)
            val delta = end.subtract(start)
            val origins = doubleArrayOf(start.x, start.y, start.z)
            val directions = doubleArrayOf(delta.x, delta.y, delta.z)
            val lows = doubleArrayOf(bounds.minX, bounds.minY, bounds.minZ)
            val highs = doubleArrayOf(bounds.maxX, bounds.maxY, bounds.maxZ)
            val breaks = mutableListOf(0.0, 1.0)
            for (axis in 0..2) {
                if (directions[axis] == 0.0) continue
                for (plane in doubleArrayOf(lows[axis], highs[axis])) {
                    val t = (plane - origins[axis]) / directions[axis]
                    if (t > 0.0 && t < 1.0) breaks.add(t)
                }
            }
            breaks.sort()
            // The squared distance to a box is quadratic between crossings of its six face planes.
            for (i in 0 until breaks.lastIndex) {
                val low = breaks[i]
                val high = breaks[i + 1]
                val middle = (low + high) * 0.5
                var a = 0.0
                var b = 0.0
                var c = -r * r
                for (axis in 0..2) {
                    val at = origins[axis] + directions[axis] * middle
                    val boundary = if (at < lows[axis]) lows[axis] else if (at > highs[axis]) highs[axis] else continue
                    val offset = origins[axis] - boundary
                    a += directions[axis] * directions[axis]
                    b += offset * directions[axis]
                    c += offset * offset
                }
                val t = if (a < 1e-20) {
                    if (c > 1e-10) continue
                    low
                } else {
                    val discriminant = b * b - a * c
                    if (discriminant < -1e-10) continue
                    val root = sqrt(discriminant.coerceAtLeast(0.0))
                    val entry = max(low, (-b - root) / a)
                    val exit = min(high, (-b + root) / a)
                    if (entry > exit + 1e-10) continue
                    entry.coerceIn(low, high)
                }
                val point = start.add(delta.scale(t))
                return BeamIntersection(point, closestPoint(bounds, point))
            }
            return null
        }

        fun intersectBeam(obb: OBB, start: Vec3, end: Vec3, radius: Double): BeamIntersection? {
            val inverse = Quaterniond(obb.rotation).invert()
            fun local(point: Vec3): Vec3 {
                val v = Vector3d(point.x, point.y, point.z).sub(obb.center).rotate(inverse)
                return Vec3(v.x, v.y, v.z)
            }
            fun world(point: Vec3): Vec3 {
                val v = Vector3d(point.x, point.y, point.z).rotate(obb.rotation).add(obb.center)
                return Vec3(v.x, v.y, v.z)
            }
            val e = obb.extents
            val hit = intersectBeam(AABB(-e.x, -e.y, -e.z, e.x, e.y, e.z), local(start), local(end), radius) ?: return null
            return BeamIntersection(world(hit.axis), world(hit.contact))
        }

        fun closestPoint(bounds: AABB, point: Vec3) = Vec3(
            point.x.coerceIn(bounds.minX, bounds.maxX), point.y.coerceIn(bounds.minY, bounds.maxY),
            point.z.coerceIn(bounds.minZ, bounds.maxZ)
        )

        private fun radiusForPower(power: Double): Double = 0.025 + 0.155 * sqrt(power.coerceIn(0.0, 1.0))

        // Match the head region used by ProjectileEntity and IAdvancedHitDetection.
        private fun isHeadshot(relativeHitY: Double, eyeHeight: Double): Boolean =
            relativeHitY > eyeHeight - 0.25 && relativeHitY < eyeHeight + 0.3

        @JvmField
        val POWER: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val START_TIME: EntityDataAccessor<Long> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.LONG)

        @JvmField
        val OWNER_ID: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.INT)

        @JvmField
        val END_X: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val END_Y: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val END_Z: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val LIFE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.INT)

        @JvmField
        val HIT_BLOCK: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.BOOLEAN)

        private val WIDTH = SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.FLOAT)
        private val COLOR = SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.INT)
    }
}
