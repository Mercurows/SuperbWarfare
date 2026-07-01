package com.atsuishio.superbwarfare.perk.js

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DamageReduce
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties
import com.atsuishio.superbwarfare.perk.IAmmoStat
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.script.ScriptManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.registries.ForgeRegistries

open class JsPerk(val perkId: String, private val descriptor: PerkDescriptor) : Perk(perkId, descriptor.perkType),
    IAmmoStat {
    override val damageRate: Double get() = descriptor.damageRate

    override val speedRate: Double get() = descriptor.speedRate

    override val slug: Boolean get() = descriptor.slug

    private val ammoConfig: AmmoConfig? by lazy {
        if (type == Type.AMMO) {
            val effects = mutableListOf<MobEffect>()
            descriptor.mobEffects?.forEach { name ->
                val rl = ResourceLocation.tryParse(name) ?: return@forEach
                val effect = ForgeRegistries.MOB_EFFECTS.getValue(rl)
                if (effect != null) {
                    effects.add(effect)
                } else {
                    Mod.LOGGER.warn("Unknown mob effect '{}' in perk '{}'", name, perkId)
                }
            }
            AmmoConfig(
                descriptor.bypassArmorRate,
                descriptor.damageRate,
                descriptor.speedRate,
                descriptor.slug,
                descriptor.rgb ?: listOf(255, 222, 39),
                effects,
                descriptor.hideParticle,
            )
        } else null
    }

    private val script: ScriptManager.CustomScript? by lazy {
        descriptor.script?.let { scriptName ->
            val source = loadScriptSource(scriptName) ?: return@lazy null
            ScriptManager.createSafeScript("sbw/perk/$perkId", source)?.also { it.exec() }
        }
    }

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        ammoConfig?.let { applyAmmoConfig(modifier, it) }

        val s = script ?: return

        val pmcProxy = PmcProxy(modifier)
        val level = modifier.data.perk.getLevel(this).toInt()
        val tag = modifier.data.perk.getTag(this) ?: return
        val perkTag = PerkTagProxy(tag)
        val gunDataProxy = GunDataProxy(modifier.data)

        s.callFunction("modifyProperty", pmcProxy, level, perkTag, gunDataProxy)
    }

    override fun modifyProjectile(data: GunData, instance: PerkInstance, entity: Entity) {
        val config = ammoConfig ?: return
        if (entity is IBulletProperties) {
            val r = config.rgb
            entity.setRGB(floatArrayOf(r[0] / 255f, r[1] / 255f, r[2] / 255f))
            if (config.mobEffects.isNotEmpty()) {
                val amplifier = getEffectAmplifier(instance)
                val duration = getEffectDuration(instance)
                val instances = config.mobEffects.map {
                    MobEffectInstance(it, duration, amplifier, false, !config.hideParticle)
                }
                entity.setEffects(instances)
            }
        }

        val s = script ?: return

        val proxy = ProjectileProxy(entity)
        val level = instance.level.toInt()

        s.callFunction("modifyProjectile", proxy, level, data.isShotgun)
    }

    override fun getModifiedDamage(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ): Float {
        val s = script ?: return damage

        val level = instance.level.toInt()
        val targetInfo = TargetProxy(target)
        val tag = data.perk.getTag(this) ?: return damage
        val perkTag = PerkTagProxy(tag)
        val sourceProxy = DamageSourceProxy(source)
        val result = s.callFunction("getModifiedDamage", damage, targetInfo, level, perkTag, sourceProxy)
            ?: return damage
        return (result as? Number)?.toFloat() ?: damage
    }

    override fun tick(data: GunData, instance: PerkInstance, entity: Entity?) {
        val s = script ?: return

        val tag = data.perk.getTag(this) ?: return
        val perkTag = PerkTagProxy(tag)
        val gunDataProxy = GunDataProxy(data)
        val entityProxy = EntityProxy(entity)
        val level = instance.level.toInt()

        s.callFunction("tick", perkTag, level, gunDataProxy, entityProxy)
    }

    override fun onKill(
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        val s = script ?: return

        val tag = data.perk.getTag(this) ?: return
        val perkTag = PerkTagProxy(tag)
        val gunDataProxy = GunDataProxy(data)
        val targetProxy = EntityProxy(target)
        val sourceProxy = DamageSourceProxy(source)
        val level = instance.level.toInt()

        s.callFunction("onKill", perkTag, level, gunDataProxy, targetProxy, sourceProxy)
    }

    override fun preReload(data: GunData, instance: PerkInstance, entity: Entity?) {
        val s = script ?: return

        val tag = data.perk.getTag(this) ?: return
        val perkTag = PerkTagProxy(tag)
        val gunDataProxy = GunDataProxy(data)
        val entityProxy = EntityProxy(entity)
        val level = instance.level.toInt()

        s.callFunction("preReload", perkTag, level, gunDataProxy, entityProxy)
    }

    override fun postReload(data: GunData, instance: PerkInstance, entity: Entity?) {
        val s = script ?: return

        val tag = data.perk.getTag(this) ?: return
        val perkTag = PerkTagProxy(tag)
        val gunDataProxy = GunDataProxy(data)
        val entityProxy = EntityProxy(entity)
        val level = instance.level.toInt()

        s.callFunction("postReload", perkTag, level, gunDataProxy, entityProxy)
    }

    override fun onHurtEntity(
        damage: Float,
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        val s = script ?: return

        val tag = data.perk.getTag(this) ?: return
        val perkTag = PerkTagProxy(tag)
        val gunDataProxy = GunDataProxy(data)
        val targetProxy = EntityProxy(target)
        val sourceProxy = DamageSourceProxy(source)
        val level = instance.level.toInt()

        s.callFunction("onHurtEntity", damage, perkTag, level, gunDataProxy, targetProxy, sourceProxy)
    }

    override fun getModifiedCustomRPM(rpm: Int, data: GunData, instance: PerkInstance): Int {
        val s = script ?: return rpm

        val gunDataProxy = GunDataProxy(data)
        val level = instance.level.toInt()
        val result = s.callFunction("getModifiedCustomRPM", rpm, level, gunDataProxy) ?: return rpm
        return (result as? Number)?.toInt() ?: rpm
    }

    override fun onMeleeAttack(
        data: GunData,
        instance: PerkInstance,
        target: Entity,
        source: DamageSource
    ) {
        val s = script ?: return

        val tag = data.perk.getTag(this) ?: return
        val perkTag = PerkTagProxy(tag)
        val gunDataProxy = GunDataProxy(data)
        val targetProxy = EntityProxy(target)
        val sourceProxy = DamageSourceProxy(source)
        val level = instance.level.toInt()

        s.callFunction("onMeleeAttack", perkTag, level, gunDataProxy, targetProxy, sourceProxy)
    }

    override fun onMeleeSwing(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        val s = script ?: return

        val tag = data.perk.getTag(this) ?: return
        val perkTag = PerkTagProxy(tag)
        val gunDataProxy = GunDataProxy(data)
        val entityProxy = EntityProxy(entity)
        val level = instance.level.toInt()

        s.callFunction("onMeleeSwing", perkTag, level, gunDataProxy, entityProxy)
    }

    override fun onChangeSlot(
        data: GunData,
        instance: PerkInstance,
        living: Entity?
    ) {
        val s = script ?: return

        val tag = data.perk.getTag(this) ?: return
        val perkTag = PerkTagProxy(tag)
        val gunDataProxy = GunDataProxy(data)
        val entityProxy = EntityProxy(living)
        val level = instance.level.toInt()

        s.callFunction("onChangeSlot", perkTag, level, gunDataProxy, entityProxy)
    }

    override fun onHit(
        attacker: LivingEntity,
        data: GunData,
        instance: PerkInstance,
        target: Entity
    ) {
        val s = script ?: return

        val tag = data.perk.getTag(this) ?: return
        val perkTag = PerkTagProxy(tag)
        val gunDataProxy = GunDataProxy(data)
        val attackerProxy = EntityProxy(attacker)
        val targetProxy = EntityProxy(target)
        val level = instance.level.toInt()

        s.callFunction("onHit", attackerProxy, level, gunDataProxy, targetProxy, perkTag)
    }

    fun getEffectAmplifier(instance: PerkInstance): Int {
        val s = script ?: return instance.level - 1

        val level = instance.level.toInt()
        val result = s.callFunction("getEffectAmplifier", level) ?: return instance.level - 1
        return (result as? Number)?.toInt() ?: (instance.level - 1)
    }

    fun getEffectDuration(instance: PerkInstance): Int {
        val s = script ?: return 70 + 30 * instance.level

        val level = instance.level.toInt()
        val result = s.callFunction("getEffectDuration", level) ?: return 70 + 30 * instance.level
        return (result as? Number)?.toInt() ?: (70 + 30 * instance.level)
    }

    override fun getModifiedDamageReduceRate(reduce: DamageReduce?): Double {
        val config = ammoConfig ?: return super.getModifiedDamageReduceRate(reduce)
        if (config.slug && reduce?.type == DamageReduce.ReduceType.SHOTGUN) {
            return 0.015
        }
        return super.getModifiedDamageReduceRate(reduce)
    }

    override fun getModifiedDamageReduceMinDistance(reduce: DamageReduce?): Double {
        val config = ammoConfig ?: return super.getModifiedDamageReduceMinDistance(reduce)
        if (config.slug && reduce?.type == DamageReduce.ReduceType.SHOTGUN) {
            return super.getModifiedDamageReduceMinDistance(reduce) * 2
        }
        return super.getModifiedDamageReduceMinDistance(reduce)
    }

    private fun applyAmmoConfig(modifier: PMC<GunData, DefaultGunData>, config: AmmoConfig) {
        val pmc = PmcProxy(modifier)
        pmc.add("BypassesArmor", config.bypassArmorRate)
        pmc.clampMin("BypassesArmor", 0.0)
        pmc.mul("Velocity", config.speedRate)
        pmc.clampMin("Velocity", 0.0)

        if (config.slug) {
            pmc.mul("Damage", config.damageRate * (pmc.get("ProjectileAmount") as Number).toDouble())
            pmc.set("ProjectileAmount", 1)
            pmc.set("ZoomSpreadRate", 0.15)
        } else {
            pmc.mul("Damage", config.damageRate)
        }
    }

    private fun loadScriptSource(location: ResourceLocation): String? {
        val path = "/data/${location.namespace}/${location.path}"
        return try {
            JsPerk::class.java.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            Mod.LOGGER.error("Failed to load perk script: $path", e)
            null
        }
    }

    private class AmmoConfig(
        val bypassArmorRate: Double,
        val damageRate: Double,
        val speedRate: Double,
        val slug: Boolean,
        val rgb: List<Int>,
        val mobEffects: List<MobEffect>,
        val hideParticle: Boolean,
    )
}
