package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import net.minecraft.core.Holder
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

object ModDamageTypes {
    // Gun Damage Types
    // @formatter:off
    @JvmField val GUN_FIRE = registerDamageType("gunfire")
    @JvmField val GUN_FIRE_ABSOLUTE = registerDamageType("gunfire_absolute")
    @JvmField val GUN_FIRE_HEADSHOT = registerDamageType("gunfire_headshot")
    @JvmField val GUN_FIRE_HEADSHOT_ABSOLUTE = registerDamageType("gunfire_headshot_absolute")
    @JvmField val LASER = registerDamageType("laser")
    @JvmField val LASER_HEADSHOT = registerDamageType("laser_headshot")
    @JvmField val BURN = registerDamageType("burn")
    @JvmField val SHOCK = registerDamageType("shock")
    @JvmField val PROJECTILE_HIT = registerDamageType("projectile_hit")
    @JvmField val PROJECTILE_HIT_HEADSHOT = registerDamageType("projectile_hit_headshot")
    @JvmField val PROJECTILE_EXPLOSION = registerDamageType("projectile_explosion")
    @JvmField val REPAIR_TOOL = registerDamageType("repair_tool")
    @JvmField val SUPER_STAR_HIT = registerDamageType("super_star_hit")
    @JvmField val SUPER_STAR_SLASH = registerDamageType("super_star_slash")
    // @formatter:on

    // Other Damage Types
    // @formatter:off
    @JvmField val MINE = registerDamageType("mine")
    @JvmField val BEAST = registerDamageType("beast")
    @JvmField val CUSTOM_EXPLOSION = registerDamageType("custom_explosion")
    @JvmField val DRONE_HIT = registerDamageType("drone_hit")
    @JvmField val LASER_STATIC = registerDamageType("laser_static")
    @JvmField val VEHICLE_STRIKE = registerDamageType("vehicle_strike")
    @JvmField val AIR_CRASH = registerDamageType("air_crash")
    @JvmField val LUNGE_MINE = registerDamageType("lunge_mine")
    @JvmField val VEHICLE_EXPLOSION = registerDamageType("vehicle_explosion")
    @JvmField val GRAPESHOT_HIT = registerDamageType("grapeshot_hit")
    @JvmField val PHOSPHORUS_FIRE = registerDamageType("phosphorus_fire")
    @JvmField val AMMO_CONSUMPTION = registerDamageType("ammo_consumption")
    // @formatter:on

    // @formatter:off
    @JvmStatic
    fun causeGunFireDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(GUN_FIRE), directEntity, attacker)
    }

    @JvmStatic
    fun causeGunFireHeadshotDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(GUN_FIRE_HEADSHOT), directEntity, attacker)
    }

    @JvmStatic
    fun causeMineDamage(registryAccess: RegistryAccess, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(MINE), attacker)
    }

    @JvmStatic
    fun causeShockDamage(registryAccess: RegistryAccess, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(SHOCK), attacker)
    }

    @JvmStatic
    fun causeBurnDamage(registryAccess: RegistryAccess, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(BURN), attacker)
    }

    @JvmStatic
    fun causeRepairToolDamage(registryAccess: RegistryAccess, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(REPAIR_TOOL), attacker)
    }

    /**
     * 仅用于枪械可以发射的投射物造成的爆炸伤害
     */
    @JvmStatic
    fun causeProjectileExplosionDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(PROJECTILE_EXPLOSION), directEntity, attacker)
    }

    /**
     * 仅用于枪械可以发射的投射物造成的直击伤害
     */
    @JvmStatic
    fun causeProjectileHitDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(PROJECTILE_HIT), directEntity, attacker)
    }

    @JvmStatic
    fun causeProjectileHitHeadshotDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(PROJECTILE_HIT_HEADSHOT), directEntity, attacker)
    }

    @JvmStatic
    fun causeGunFireAbsoluteDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(GUN_FIRE_ABSOLUTE), directEntity, attacker)
    }

    @JvmStatic
    fun causeGunFireHeadshotAbsoluteDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(GUN_FIRE_HEADSHOT_ABSOLUTE), directEntity, attacker)
    }

    @JvmStatic
    fun causeGrapeShotHitDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(GRAPESHOT_HIT), directEntity, attacker)
    }

    @JvmStatic
    fun causeSuperStarHitDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(SUPER_STAR_HIT), directEntity, attacker)
    }

    @JvmStatic
    fun causeSuperStarSlashDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(SUPER_STAR_SLASH), directEntity, attacker)
    }

    /**
     * 用于其他实体造成的爆炸伤害
     */
    @JvmStatic
    fun causeCustomExplosionDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(CUSTOM_EXPLOSION), directEntity, attacker)
    }

    @JvmStatic
    fun causeDroneHitDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(DRONE_HIT), directEntity, attacker)
    }

    @JvmStatic
    fun causeLaserDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(LASER), directEntity, attacker)
    }

    @JvmStatic
    fun causeLaserStaticDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(LASER_STATIC), directEntity, attacker)
    }

    @JvmStatic
    fun causeLaserHeadshotDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(LASER_HEADSHOT), directEntity, attacker)
    }

    @JvmStatic
    fun causeVehicleStrikeDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(VEHICLE_STRIKE), directEntity, attacker)
    }

    @JvmStatic
    fun causeAirCrashDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(AIR_CRASH), directEntity, attacker)
    }

    @JvmStatic
    fun causeLungeMineDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(LUNGE_MINE), directEntity, attacker)
    }

    @JvmStatic
    fun causeVehicleExplosionDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(VEHICLE_EXPLOSION), directEntity, attacker)
    }

    @JvmStatic
    fun causeBeastDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(BEAST), directEntity, attacker)
    }

    @JvmStatic
    fun causePhosphorusFireDamage(registryAccess: RegistryAccess, directEntity: Entity?, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(PHOSPHORUS_FIRE), directEntity, attacker)
    }

    @JvmStatic
    fun causeAmmoConsumptionDamage(registryAccess: RegistryAccess, attacker: Entity?): DamageSource {
        return DamageMessages(registryAccess.registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(AMMO_CONSUMPTION), attacker)
    }
    // @formatter:on

    fun registerDamageType(name: String): ResourceKey<DamageType> {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Mod.loc(name))
    }

    private class DamageMessages : DamageSource {
        constructor(typeReference: Holder.Reference<DamageType>) : super(typeReference)

        constructor(typeReference: Holder.Reference<DamageType>, entity: Entity?) : super(typeReference, entity)

        constructor(typeReference: Holder.Reference<DamageType>, directEntity: Entity?, attacker: Entity?) : super(
            typeReference,
            directEntity,
            attacker
        )

        override fun getLocalizedDeathMessage(pLivingEntity: LivingEntity): Component {
            val entity = this.entity ?: this.getDirectEntity()
            if (entity == null) {
                return Component.translatable("death.attack." + this.msgId, pLivingEntity.displayName)
            } else if (entity is LivingEntity && entity.mainHandItem.hasCustomHoverName()) {
                return Component.translatable(
                    "death.attack." + this.msgId + ".item",
                    pLivingEntity.displayName,
                    entity.displayName,
                    entity.mainHandItem.getDisplayName()
                )
            } else {
                return Component.translatable(
                    "death.attack." + this.msgId + ".entity",
                    pLivingEntity.displayName,
                    entity.displayName
                )
            }
        }
    }
}