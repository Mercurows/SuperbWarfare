package com.atsuishio.superbwarfare.entity.projectile

import net.minecraft.world.effect.MobEffectInstance

/**
 * 子弹属性接口 — 定义投射物的核心数据的 getter/setter 方法。
 *
 * 实现类需覆盖对应方法。部分方法提供默认实现。
 */
interface IBulletProperties {
    // 伤害值
    fun getDamage(): Float
    fun setDamage(value: Float)

    // 爆炸伤害
    fun getExplosionDamage(): Float = 0f
    fun setExplosionDamage(value: Float) {}

    // 爆炸半径
    fun getExplosionRadius(): Float = 0f
    fun setExplosionRadius(value: Float) {}

    // 弹射物存活时间
    fun getLife(): Int = 40
    fun setLife(value: Int) {}

    // 爆头倍率
    fun getHeadShot(): Float = 1f
    fun setHeadShot(value: Float) {}

    // 打腿倍率
    fun getLegShot(): Float = 0.5f
    fun setLegShot(value: Float) {}

    // 穿甲倍率
    fun getBypassArmorRate(): Float = 0.0f
    fun setBypassArmorRate(value: Float) {}

    // 击退力度
    fun getKnockback(): Float = 0.05f
    fun setKnockback(value: Float) {}

    // 发射初速度
    fun getVelocity(): Float = 1f
    fun setVelocity(value: Float) {}

    // 是否为野兽弹
    fun isBeast(): Boolean = false
    fun setBeast(value: Boolean) {}

    // 是否属于瞄准发射
    fun isZoom(): Boolean = false
    fun setZoom(value: Boolean) {}

    // 是否强制击退
    fun isForceKnockback(): Boolean = false
    fun setForceKnockback(value: Boolean) {}

    // 造成的燃烧等级
    fun getFireLevel(): Int = 0
    fun setFireLevel(value: Int) {}

    // 是否为龙息弹
    fun isDragonBreath(): Boolean = false
    fun setDragonBreath(value: Boolean) {}

    // 是否能穿墙
    fun isPenetrating(): Boolean = false
    fun setPenetrating(value: Boolean) {}

    // 颜色
    fun setRGB(rgb: FloatArray) {}
    fun getRGB(): FloatArray = floatArrayOf(DEFAULT_R, DEFAULT_G, DEFAULT_B)

    fun setEffect(effects: List<MobEffectInstance>) {}

    fun getCustomGravity(): Float = 0f
    fun setCustomGravity(gravity: Float) {}

    companion object {
        const val DEFAULT_R: Float = 1.0f
        const val DEFAULT_G: Float = 222 / 255f
        const val DEFAULT_B: Float = 39 / 255f
    }
}
