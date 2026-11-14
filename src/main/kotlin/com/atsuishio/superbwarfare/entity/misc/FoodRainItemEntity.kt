package com.atsuishio.superbwarfare.entity.misc

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

open class FoodRainItemEntity(type: EntityType<out FoodRainItemEntity>, level: Level) : ItemEntity(type, level) {
    private val path by lazy { Path(id) }
    private var released = false
    val hidden: Boolean
        get() {
            val cloud = entityData.get(CLOUD)
            return !released && !cloud.isEmpty && age(cloud) < path.launch
        }

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(CLOUD, CompoundTag())
    }

    fun join(center: Vec3, start: Long) {
        entityData.set(CLOUD, CompoundTag().apply {
            putDouble("CenterX", center.x)
            putDouble("CenterY", center.y)
            putDouble("CenterZ", center.z)
            putLong("Start", start)
        })
        setPos(center)
        isNoGravity = true
        setNeverPickUp()
        lifespan = 6000
    }

    private fun age(cloud: CompoundTag) = (level().gameTime - cloud.getLong("Start")) / 20.0

    override fun tick() {
        super.tick()
        val cloud = entityData.get(CLOUD)
        if (released || cloud.isEmpty || isRemoved) return
        val age = age(cloud)
        val center = Vec3(cloud.getDouble("CenterX"), cloud.getDouble("CenterY"), cloud.getDouble("CenterZ"))
        val offset = path.offset(age)
        if (offset == null) {
            deltaMovement = Vec3.ZERO
            setPos(center)
        } else if (age >= path.release) {
            released = true
            isNoGravity = false
            setPickUpDelay(20)
            val at = path.offset(path.release)!!
            setPos(center.add(at))
            deltaMovement = at.subtract(path.offset(path.release - 0.05) ?: at)
            if (!level().isClientSide) entityData.set(CLOUD, CompoundTag())
        } else {
            deltaMovement = Vec3.ZERO
            setPos(center.add(offset))
        }
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        released = true
        isNoGravity = false
        setPickUpDelay(10)
    }

    override fun getBoundingBoxForCulling() = super.getBoundingBoxForCulling().inflate(2.0)

    override fun shouldRenderAtSqrDistance(distance: Double): Boolean {
        val range = 512.0 * getViewScale()
        return distance < range * range
    }

    private class Path(id: Int) {
        private val random = Random(id.toLong() * -7046029254386353131L)
        private val stem = random.nextDouble() < 0.28
        val launch = 0.8 + random.nextDouble() * if (stem) 3.0 else 9.0
        private val speed = 14.0 + random.nextDouble() * 6.0
        private val azimuth = random.nextDouble() * 2.0 * Math.PI
        private val radial = sqrt(random.nextDouble())
        private val minor = 0.35 + 0.65 * sqrt(random.nextDouble())
        private val roll = 0.66 * (0.8 + 0.4 * random.nextDouble())
        private val cycle = random.nextDouble()
        val release = 26.0 + random.nextDouble() * 18.0
        private val join by lazy {
            var age = launch
            while ((age - launch) * speed < stemTop(age) && age < launch + 30.0) age += 0.05
            age
        }

        fun offset(age: Double): Vec3? {
            if (age < launch) return null
            val swirl = azimuth + 0.25 * age
            val top = stemTop(age)
            if (stem) return column(swirl, ((age - launch) * speed * 0.6 + cycle * top) % top, age, top)
            if (age < join) return column(swirl, (age - launch) * speed, age, top)
            val capRadius = capRadius(age)
            val phi = -2.2 - roll * (age - join)
            val minorRadius = minor * 0.42 * capRadius
            val radius = 0.58 * capRadius + minorRadius * cos(phi)
            val heading = azimuth + 0.25 * join + 0.05 * (age - join)
            val ring = Vec3(cos(heading) * radius, capHeight(age) + minorRadius * sin(phi) / 1.15,
                sin(heading) * radius).add(drift(age))
            val blend = ((age - join) / 1.2).coerceIn(0.0, 1.0)
            if (blend >= 1.0) return ring
            val entry = column(swirl, stemTop(join), join, stemTop(join))
            return entry.add(ring.subtract(entry).scale(blend * blend * (3.0 - 2.0 * blend)))
        }

        private fun capRadius(age: Double) = 2.0 + 26.0 * (1.0 - exp(-age * 2.4)) +
                32.0 * (1.0 - exp(-maxOf(age - 1.0, 0.0) / 8.0))

        private fun capHeight(age: Double) = 5.0 + 95.0 * (1.0 - exp(-maxOf(age - 0.7, 0.0) / 8.0))

        private fun stemTop(age: Double) = maxOf(capHeight(age) - 0.48 * capRadius(age), 8.0)

        private fun drift(age: Double) = Vec3(age * 0.24, 0.0, age * 0.11)

        private fun column(swirl: Double, y: Double, age: Double, top: Double): Vec3 {
            val fraction = (y / top).coerceIn(0.0, 1.0)
            val radius = radial * (19.0 - 11.5 * fraction * fraction * (3.0 - 2.0 * fraction)) * 0.6
            return Vec3(cos(swirl) * radius, y, sin(swirl) * radius).add(drift(age).scale(fraction))
        }
    }

    companion object {
        private val CLOUD = SynchedEntityData.defineId(FoodRainItemEntity::class.java, EntityDataSerializers.COMPOUND_TAG)
    }
}
