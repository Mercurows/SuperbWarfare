package com.atsuishio.superbwarfare.entity.projectile

import net.minecraft.world.effect.MobEffectInstance

/**
 * 子弹属性接口 — 使用 Kotlin 属性风格定义投射物的核心数据。
 *
 * 实现类用 [override var] 覆盖对应属性。部分属性提供默认读写实现。
 *
 * - 必须覆盖：[damage]、[explosionDamage]、[explosionRadius]、[life]
 * - 有默认值：[headShot]、[legShot]、[knockback]、[velocity]、[beast]、[isZoom]、
 *   [forceKnockback]、[fireLevel]、[dragonBreath]
 * - 复合操作保留方法：[setRGB]/[getRGB]、[setFireBullet]、[setEffect]、[setCustomGravity]
 */
interface IBulletProperties {
    var damage: Float
    var explosionDamage: Float
    var explosionRadius: Float
    var life: Int

    var headShot: Float get() = 1f; set(_) {}
    var legShot: Float get() = 0.5f; set(_) {}
    var knockback: Float get() = 0.05f; set(_) {}
    var velocity: Float get() = 1f; set(_) {}
    var beast: Boolean get() = false; set(_) {}
    var isZoom: Boolean get() = false; set(_) {}
    var forceKnockback: Boolean get() = false; set(_) {}
    var fireLevel: Int get() = 0; set(_) {}
    var dragonBreath: Boolean get() = false; set(_) {}

    fun setRGB(rgb: FloatArray) {}

    fun getRGB(): FloatArray = floatArrayOf(DEFAULT_R, DEFAULT_G, DEFAULT_B)

    fun setFireBullet(fireLevel: Int, dragonBreath: Boolean) {}

    fun setEffect(effects: List<MobEffectInstance>) {}

    fun setCustomGravity(gravity: Float) {}

    companion object {
        const val DEFAULT_R: Float = 1.0f
        const val DEFAULT_G: Float = 222 / 255f
        const val DEFAULT_B: Float = 39 / 255f
    }
}
