package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.DamageTypeTool.isGunDamage
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType

object DamageTypeTool {
    @JvmStatic
    fun isGunDamage(source: DamageSource) = source.`is`(ModTags.DamageTypes.GUN_DAMAGE)

    /**
     * 是否是近战伤害：`#superbwarfare:melee` 标签。
     *
     * 标签里含枪械近战（`gun_melee`/`gun_melee_headshot`）与原版 `minecraft:player_attack`，
     * 数据包/其它模组也可以自行往里加。**注意 [isGunDamage] 不包含近战**——
     * 两者是并列的两类，perk 分发要按这个区分走 `onHurtEntity` 还是 `onMeleeAttack`。
     */
    @JvmStatic
    fun isMeleeDamage(source: DamageSource) = source.`is`(ModTags.DamageTypes.MELEE)

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
            || source.`is`(ModDamageTypes.GUN_MELEE_HEADSHOT)

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
                || damageType == ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("tacz", "bullet")
        )
                || damageType == ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("tacz", "bullet_void")
        )
                || damageType == ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("tacz", "bullet_ignore_armor")
        )
                || damageType == ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("tacz", "bullet_void_ignore_armor")
        )
}