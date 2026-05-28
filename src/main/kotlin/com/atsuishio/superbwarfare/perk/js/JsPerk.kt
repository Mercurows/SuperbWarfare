package com.atsuishio.superbwarfare.perk.js

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DamageReduce
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.script.ScriptManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import org.mozillaa.javascript.Function

class JsPerk(val perkId: String, private val descriptor: PerkDescriptor) : Perk(perkId, descriptor.perkType) {
    private val ammoConfig: AmmoConfig? = if (type == Type.AMMO) {
        AmmoConfig(
            descriptor.bypassArmorRate,
            descriptor.damageRate,
            descriptor.speedRate,
            descriptor.slug,
            descriptor.rgb ?: listOf(255, 222, 39),
        )
    } else null

    private val script: ScriptManager.CustomScript? by lazy {
        descriptor.script?.let { scriptName ->
            val source = loadScriptSource(scriptName) ?: return@lazy null
            ScriptManager.createSafeScript("perk/$perkId", source)?.also { it.exec() }
        }
    }

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        ammoConfig?.let { applyAmmoConfig(modifier, it) }

        val s = script ?: return
        val f = s.scope.get("modifyProperty", s.scope) as? Function ?: return

        val pmcProxy = PmcProxy(modifier)
        val level = modifier.data.perk.getLevel(this).toInt()
        val tag = modifier.data.perk.getTag(this)
        val perkTag = tag?.let { PerkTagProxy(it) }

        f.call(s.context, s.scope, s.scope, arrayOf(pmcProxy, level, perkTag))
    }

    override fun modifyProjectile(data: GunData, instance: PerkInstance, entity: Entity) {
        val config = ammoConfig ?: return
        if (entity is ProjectileEntity) {
            val r = config.rgb
            entity.setRGB(floatArrayOf(r[0] / 255f, r[1] / 255f, r[2] / 255f))
        }
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
    )
}
