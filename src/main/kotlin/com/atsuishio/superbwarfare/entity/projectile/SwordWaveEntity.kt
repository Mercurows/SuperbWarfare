package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import kotlin.math.roundToInt

open class SwordWaveEntity(type: EntityType<out SwordWaveEntity>, level: Level) : FastThrowableProjectile(type, level) {
    open var roll by ROLL
    open var rgb by RGB

    init {
        lifeValue = 60
        velocityValue = LAUNCH_SPEED
        gravityValue = 0f
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(ROLL, 0f)
        entityData.define(RGB, 0xE6F2FF)
    }

    override fun getDefaultItem() = Items.AIR

    override fun canPassThroughFluid() = true

    override fun getRGB() = floatArrayOf(
        ((rgb ushr 16) and 255) / 255f,
        ((rgb ushr 8) and 255) / 255f,
        (rgb and 255) / 255f,
    )

    override fun setRGB(rgb: FloatArray) {
        if (rgb.size < 3 || !rgb[0].isFinite() || !rgb[1].isFinite() || !rgb[2].isFinite()) return
        fun channel(index: Int) = (rgb[index].coerceIn(0f, 1f) * 255).roundToInt()
        this.rgb = (channel(0) shl 16) or (channel(1) shl 8) or channel(2)
    }

    override fun projectileMove(level: Level) {
        if (isRemoved) return
        val movement = deltaMovement
        setPos(x + movement.x, y + movement.y, z + movement.z)
        val speed = movement.length()
        if (speed > CRUISE_SPEED) {
            val deceleration = (velocityValue - CRUISE_SPEED).coerceAtLeast(0.0) / DECELERATION_TICKS
            deltaMovement = movement.scale((speed - deceleration).coerceAtLeast(CRUISE_SPEED) / speed)
        }
    }

    companion object {
        private const val LAUNCH_SPEED = 0.5f
        private const val CRUISE_SPEED = 0.2
        private const val DECELERATION_TICKS = 20

        @JvmField val ROLL = SynchedEntityData.defineId(SwordWaveEntity::class.java, EntityDataSerializers.FLOAT)
        @JvmField val RGB = SynchedEntityData.defineId(SwordWaveEntity::class.java, EntityDataSerializers.INT)
    }
}
