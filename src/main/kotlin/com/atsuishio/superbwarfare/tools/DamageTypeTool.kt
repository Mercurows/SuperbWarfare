package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModTags
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType

object DamageTypeTool {
    @JvmStatic
    fun isGunDamage(source: DamageSource) = source.`is`(ModTags.DamageTypes.GUN_DAMAGE)

    @JvmStatic
    fun isGunDamage(damageType: ResourceKey<DamageType>, registryAccess: RegistryAccess): Boolean {
        val damageTypeRegistry = registryAccess.registryOrThrow(Registries.DAMAGE_TYPE)
        val holder = damageTypeRegistry.getHolder(damageType).orElse(null)
        return holder != null && holder.`is`(ModTags.DamageTypes.GUN_DAMAGE)
    }

    @JvmStatic
    fun isHeadshotDamage(source: DamageSource) = source.`is`(ModDamageTypes.GUN_FIRE_HEADSHOT)
            || source.`is`(ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE)
            || source.`is`(ModDamageTypes.PROJECTILE_HIT_HEADSHOT)
            || source.`is`(ModDamageTypes.LASER_HEADSHOT)

    @JvmStatic
    fun isGunFireDamage(source: DamageSource) = source.`is`(ModDamageTypes.GUN_FIRE)
            || source.`is`(ModDamageTypes.GUN_FIRE_ABSOLUTE)
            || source.`is`(ModDamageTypes.SHOCK)
            || source.`is`(ModDamageTypes.BURN)
            || source.`is`(ModDamageTypes.LASER)

    @JvmStatic
    fun isModDamage(source: DamageSource): Boolean =
        source.typeHolder().unwrapKey().map { it.location().namespace.equals(Mod.MODID) }.orElseGet { false }

    @JvmStatic
    fun isCompatGunDamage(damageType: ResourceKey<DamageType>, registryAccess: RegistryAccess) =
        isGunDamage(damageType, registryAccess)
                || damageType == ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation("tacz", "bullet"))
                || damageType == ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation("tacz", "bullet_void"))
                || damageType == ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation("tacz", "bullet_ignore_armor")
        )
                || damageType == ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation("tacz", "bullet_void_ignore_armor")
        )
}