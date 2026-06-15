package com.atsuishio.superbwarfare.perk

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DamageReduce
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import java.util.function.Supplier

open class AmmoPerk : Perk {
    val bypassArmorRate: Double
    val damageRate: Double
    val speedRate: Double
    val slug: Boolean
    val rgb: FloatArray
    val hideParticle: Boolean
    val mobEffects: () -> ArrayList<MobEffect>

    constructor(builder: Builder) : super(builder.descriptionId, builder.type) {
        this.bypassArmorRate = builder.bypassArmorRate
        this.damageRate = builder.damageRate
        this.speedRate = builder.speedRate
        this.slug = builder.slug
        this.rgb = builder.rgb
        this.hideParticle = builder.hideParticle
        this.mobEffects = { builder.mobEffects }
    }

    constructor(descriptionId: String, type: Type) : this(Builder(descriptionId, type))

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        modifier[GunProp.BYPASSES_ARMOR] = (modifier[GunProp.BYPASSES_ARMOR] + bypassArmorRate).coerceAtLeast(0.0)
        modifier[GunProp.VELOCITY] = (modifier[GunProp.VELOCITY] * speedRate).coerceAtLeast(0.0)

        return if (slug) {
            modifier[GunProp.DAMAGE] *= damageRate * modifier[GunProp.PROJECTILE_AMOUNT]
            modifier[GunProp.PROJECTILE_AMOUNT] = 1
            modifier[GunProp.ZOOM_SPREAD_RATE] = 0.15
        } else {
            modifier[GunProp.DAMAGE] *= damageRate
        }
    }

    override fun modifyProjectile(
        data: GunData,
        instance: PerkInstance,
        entity: Entity
    ) {
        if (entity !is IBulletProperties) return
        entity.setRGB(this.rgb)
        if (this.mobEffects().isEmpty()) return
        val amplifier = this.getEffectAmplifier(instance)
        val duration = this.getEffectDuration(instance)
        val mobEffectInstances = arrayListOf<MobEffectInstance>()
        this.mobEffects()
            .forEach { mobEffectInstances.add(MobEffectInstance(it, duration, amplifier, false, !this.hideParticle)) }
        entity.setEffects(mobEffectInstances)
    }

    open fun getEffectAmplifier(instance: PerkInstance): Int {
        return instance.level - 1
    }

    open fun getEffectDuration(instance: PerkInstance): Int {
        return 70 + 30 * instance.level
    }

    override fun getModifiedDamageReduceRate(reduce: DamageReduce?): Double {
        if (this.slug && reduce?.type == DamageReduce.ReduceType.SHOTGUN) {
            return 0.015
        }
        return super.getModifiedDamageReduceRate(reduce)
    }

    override fun getModifiedDamageReduceMinDistance(reduce: DamageReduce?): Double {
        if (this.slug && reduce?.type == DamageReduce.ReduceType.SHOTGUN) {
            return super.getModifiedDamageReduceMinDistance(reduce) * 2
        }
        return super.getModifiedDamageReduceMinDistance(reduce)
    }

    class Builder(val descriptionId: String, val type: Type) {
        var bypassArmorRate: Double = 0.0
        var damageRate: Double = 1.0
        var speedRate: Double = 1.0
        var slug: Boolean = false
        var rgb = floatArrayOf(1f, 222 / 255f, 39 / 255f)
        var hideParticle: Boolean = false
        val mobEffects = arrayListOf<MobEffect>()

        fun bypassArmorRate(bypassArmorRate: Double): Builder {
            this.bypassArmorRate = bypassArmorRate.coerceIn(-1.0, 1.0)
            return this
        }

        fun damageRate(damageRate: Double): Builder {
            this.damageRate = damageRate.coerceIn(0.0, Double.POSITIVE_INFINITY)
            return this
        }

        fun speedRate(speedRate: Double): Builder {
            this.speedRate = speedRate.coerceIn(0.0, Double.POSITIVE_INFINITY)
            return this
        }

        fun slug(): Builder {
            this.slug = true
            return this
        }

        fun rgb(r: Int, g: Int, b: Int): Builder {
            this.rgb[0] = r / 255f
            this.rgb[1] = g / 255f
            this.rgb[2] = b / 255f
            return this
        }

        fun mobEffect(mobEffect: Supplier<MobEffect>): Builder {
            this.mobEffects += mobEffect.get()
            return this
        }

        fun hideParticle(): Builder {
            this.hideParticle = true
            return this
        }
    }
}