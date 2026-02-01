package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.ModColor
import com.atsuishio.superbwarfare.data.Prop
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.getPerkPriority
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.perk.Perk
import net.minecraft.util.Mth
import net.minecraftforge.registries.RegistryManager
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty1

@Suppress("UNUSED")
class GunProp<T, R> private constructor(
    prop: KMutableProperty1<DefaultGunData, T>,
    transform: (T) -> R,
    limiter: PropModifyContext<GunData, DefaultGunData, R>.() -> R = { value },
) :
    Prop<GunData, DefaultGunData, T, R, GunProp<T, R>>(prop, transform, limiter) {

    companion object {
        private fun <T> plainProp(
            prop: KMutableProperty1<DefaultGunData, T>,
            limiter: PropModifyContext<GunData, DefaultGunData, T>.() -> T = { value },
        ): GunProp<T, T> {
            return GunProp(prop, { it }, limiter)
        }

        @JvmField
        val MAX_DURABILITY = plainProp(DefaultGunData::maxDurability) { max(0, value) }

        @JvmField
        val DURABILITY_PER_SHOOT = plainProp(DefaultGunData::durabilityPerShoot) { max(0, value) }

        @JvmField
        val MAX_ENERGY = plainProp(DefaultGunData::maxEnergy) { max(0, value) }

        @JvmField
        val MAX_RECEIVE_ENERGY = plainProp(DefaultGunData::maxReceiveEnergy) {
            val maxEnergy = modifier.get(MAX_ENERGY)
            val value = Mth.clamp(value, -1, maxEnergy)
            if (value < 0) maxEnergy else value
        }

        @JvmField
        val MAX_EXTRACT_ENERGY = plainProp(DefaultGunData::maxExtractEnergy) {
            val maxEnergy = modifier.get(MAX_ENERGY)
            val value = Mth.clamp(value, -1, maxEnergy)
            if (value < 0) maxEnergy else value
        }

        @JvmField
        val RECOIL_X = plainProp(DefaultGunData::recoilX)

        @JvmField
        val RECOIL_Y = plainProp(DefaultGunData::recoilY)

        @JvmField
        val RECOIL = plainProp(DefaultGunData::recoil)

        @JvmField
        val RECOIL_TIME = plainProp(DefaultGunData::recoilTime)

        @JvmField
        val RECOIL_FORCE = plainProp(DefaultGunData::recoilForce)

        @JvmField
        val SHOOT_SHAKE = plainProp(DefaultGunData::shootShake)

        @JvmField
        val SPREAD = plainProp(DefaultGunData::spread)

        @JvmField
        val DAMAGE = plainProp(DefaultGunData::damage)

        @JvmField
        val HEADSHOT = plainProp(DefaultGunData::headshot)

        @JvmField
        val VELOCITY = plainProp(DefaultGunData::velocity)

        @JvmField
        val MELEE_DAMAGE = plainProp(DefaultGunData::meleeDamage)

        @JvmField
        val MELEE_DURATION = plainProp(DefaultGunData::meleeDuration) { max(1, value) }

        @JvmField
        val MELEE_RANGE = plainProp(DefaultGunData::meleeRange)

        @JvmField
        val MELEE_ANGLE = plainProp(DefaultGunData::meleeAngle) { value.coerceIn(1, 180) }

        @JvmField
        val ZOOM_SPREAD_RATE = plainProp(DefaultGunData::zoomSpreadRate) { value.coerceIn(0.0, 1.0) }

        @JvmField
        val SEEK_TIME = plainProp(DefaultGunData::seekTime)

        @JvmField
        val SEEK_ANGLE = plainProp(DefaultGunData::seekAngle)

        @JvmField
        val SEEK_RANGE = plainProp(DefaultGunData::seekRange)

        @JvmField
        val MIN_TARGET_HEIGHT = plainProp(DefaultGunData::minTargetHeight)

        @JvmField
        val MAX_TARGET_HEIGHT = plainProp(DefaultGunData::maxTargetHeight)

        @JvmField
        val RANGE = plainProp(DefaultGunData::range) { max(1, value) }

        @JvmField
        val MELEE_DAMAGE_TIME =
            plainProp(DefaultGunData::meleeDamageTime) { min(modifier.get(MELEE_DURATION) - 1, value) }

        @JvmField
        val PROJECTILE = GunProp(DefaultGunData::projectile, { it.value!! })

        @JvmField
        val AMMO_COST_PER_SHOOT = plainProp(DefaultGunData::ammoCostPerShoot) { max(0, value) }

        @JvmField
        val PROJECTILE_AMOUNT = plainProp(DefaultGunData::projectileAmount) { max(0, value) }

        @JvmField
        val WEIGHT = plainProp(DefaultGunData::weight) { max(1.0, value) }

        @JvmField
        val DEFAULT_FIRE_MODE = GunProp(DefaultGunData::defaultFireMode, { it.ifEmpty { FireMode.SEMI.name!! }!! })

        @JvmField
        val AVAILABLE_FIRE_MODES =
            GunProp(DefaultGunData::availableFireModes, { it?.list?.map { l -> l.value!! } ?: listOf() })

        @JvmField
        val MAGAZINE = plainProp(DefaultGunData::magazine) {
            if (modifier.get(PROJECTILE_AMOUNT) <= 0 && modifier.get(MELEE_DAMAGE) > 0) 0 else max(0, value)
        }

        @JvmField
        val RELOAD_TYPES = GunProp(DefaultGunData::reloadTypes, { it?.filterNotNull().orEmpty() })

        @JvmField
        val SEEK_TYPE = GunProp(DefaultGunData::seekType, { it ?: SeekType.NONE })

        @JvmField
        val GUN_TYPE = GunProp(DefaultGunData::gunType, { it ?: GunType.SPECIAL })

        // 注意Nullable
        @JvmField
        val AUTO_RELOAD = plainProp(DefaultGunData::autoReload)

        @JvmField
        val WITHDRAW_AMMO_WHEN_CHANGE_SLOT = plainProp(DefaultGunData::withdrawAmmoWhenChangeSlot)

        @JvmField
        val ZOOM_RELOAD = plainProp(DefaultGunData::zoomReload)

        @JvmField
        val CLEAR_HOLD_PROGRESS_AFTER_SHOOT = plainProp(DefaultGunData::clearHoldProgressAfterShoot)

        @JvmField
        val DEFAULT_ZOOM = plainProp(DefaultGunData::defaultZoom)

        @JvmField
        val BURST_AMOUNT = plainProp(DefaultGunData::burstAmount) { max(0, value) }

        @JvmField
        val BYPASSES_ARMOR = plainProp(DefaultGunData::bypassesArmor)

        @JvmField
        val AMMO_CONSUMER = GunProp(
            DefaultGunData::ammoConsumers,
            { it?.list?.map { l -> l.value!!.also { consumer -> consumer.init() } } ?: listOf() }
        )

        @JvmField
        val NORMAL_RELOAD_TIME = plainProp(DefaultGunData::normalReloadTime)

        @JvmField
        val EMPTY_RELOAD_TIME = plainProp(DefaultGunData::emptyReloadTime)

        @JvmField
        val BOLT_ACTION_TIME = plainProp(DefaultGunData::boltActionTime)

        @JvmField
        val PREPARE_TIME = plainProp(DefaultGunData::prepareTime)

        @JvmField
        val PREPARE_LOAD_TIME = plainProp(DefaultGunData::prepareLoadTime)

        @JvmField
        val PREPARE_AMMO_LOAD_TIME = plainProp(DefaultGunData::prepareAmmoLoadTime)

        @JvmField
        val PREPARE_EMPTY_TIME = plainProp(DefaultGunData::prepareEmptyTime)

        @JvmField
        val ITERATIVE_TIME = plainProp(DefaultGunData::iterativeTime)

        @JvmField
        val ITERATIVE_AMMO_LOAD_TIME = plainProp(DefaultGunData::iterativeAmmoLoadTime)

        @JvmField
        val ITERATIVE_LOAD_AMOUNT = plainProp(DefaultGunData::iterativeLoadAmount)

        @JvmField
        val FINISH_TIME = plainProp(DefaultGunData::finishTime)

        @JvmField
        val BURST_COOLDOWN = plainProp(DefaultGunData::burstCooldown)

        @JvmField
        val SOUND_RADIUS = plainProp(DefaultGunData::soundRadius)

        @JvmField
        val RPM = plainProp(DefaultGunData::rpm) { value.coerceIn(1, 114514) }

        @JvmField
        val EXPLOSION_DAMAGE = plainProp(DefaultGunData::explosionDamage)

        @JvmField
        val EXPLOSION_RADIUS = plainProp(DefaultGunData::explosionRadius)

        @JvmField
        val GRAVITY = plainProp(DefaultGunData::gravity)

        @JvmField
        val SHOOT_DELAY = plainProp(DefaultGunData::shootDelay)

        @JvmField
        val HEAT_PER_SHOOT = plainProp(DefaultGunData::heatPerShoot)

        @JvmField
        val NATURAL_COOLDOWN = plainProp(DefaultGunData::naturalCooldown)

        @JvmField
        val IN_WATER_COOLDOWN_RATE = plainProp(DefaultGunData::inWaterCooldownRate)

        @JvmField
        val IN_SNOW_COOLDOWN_RATE = plainProp(DefaultGunData::inSnowCooldownRate)

        @JvmField
        val IN_FIRE_COOLDOWN_RATE = plainProp(DefaultGunData::inFireCooldownRate)

        @JvmField
        val IN_LAVA_COOLDOWN_RATE = plainProp(DefaultGunData::inLavaCooldownRate)

        @JvmField
        val AVAILABLE_PERKS = GunProp(DefaultGunData::availablePerks, {
            val availablePerks = mutableListOf<Perk>()
            val perkNames = it?.list?.filterNotNull().orEmpty().ifEmpty { return@GunProp availablePerks }

            val sortedNames = perkNames.distinct().sortedWith { s1, s2 ->
                val p1 = getPerkPriority(s1)
                val p2 = getPerkPriority(s2)
                if (p1 != p2) {
                    return@sortedWith p1.compareTo(p2)
                } else {
                    return@sortedWith s1.compareTo(s2)
                }
            }

            val perks = RegistryManager.ACTIVE.getRegistry(ModPerks.PERK_KEY).getEntries()

            val perkValues = perks.mapNotNull { obj -> obj?.value }
            val perkKeys = perks.mapNotNull { perk -> perk?.key?.location().toString() }

            for (name in sortedNames) {
                if (name.startsWith("@")) {
                    when (name.substring(1)) {
                        "Ammo" -> Perk.Type.AMMO
                        "Functional" -> Perk.Type.FUNCTIONAL
                        "Damage" -> Perk.Type.DAMAGE
                        else -> null
                    }?.let { type ->
                        availablePerks.addAll(perkValues.filter { it.type == type })
                    }
                } else if (name.startsWith("!")) {
                    val n = name.substring(1)
                    val index = perkKeys.indexOf(n)
                    if (index != -1) {
                        availablePerks.remove(perkValues[index])
                    } else {
                        Mod.LOGGER.info("Perk {} not found", n)
                    }
                } else {
                    val index = perkKeys.indexOf(name)
                    if (index != -1) {
                        availablePerks.add(perkValues[index])
                    } else {
                        Mod.LOGGER.info("Perk {} not found", name)
                    }
                }
            }
            return@GunProp availablePerks.toList()
        })

        @JvmField
        val ICON = GunProp(DefaultGunData::icon, { it ?: GunItem.DEFAULT_ICON })

        @JvmField
        val CROSSHAIR = GunProp(DefaultGunData::crosshair, { it.ifEmpty { "@GunDefault" }!! })

        @JvmField
        val CROSSHAIR_ZOOMING = GunProp(DefaultGunData::crosshairZooming, { it.ifEmpty { "@Empty" }!! })

        @JvmField
        val CROSSHAIR_COLOR = GunProp(DefaultGunData::crosshairColor, { it ?: ModColor() })

        // 注意Nullable
        @JvmField
        val NAME = plainProp(DefaultGunData::name)

        @JvmField
        val SHOOT_POS = GunProp(DefaultGunData::shootPos, { it ?: ShootPos() })

        @JvmField
        val SEEK_WEAPON_INFO = plainProp(DefaultGunData::seekWeaponInfo)

        @JvmField
        val SOUND_INFO = GunProp(DefaultGunData::soundInfo, { it ?: SoundInfo() })

        @JvmField
        val SHOOT_ANIMATION_TIME = plainProp(DefaultGunData::shootAnimationTime)

        @JvmField
        val SPREAD_AMOUNT = plainProp(DefaultGunData::spreadAmount)

        @JvmField
        val SPREAD_ANGLE = plainProp(DefaultGunData::spreadAngle)

        @JvmField
        val ADD_SHOOTER_DELTA_MOVEMENT = plainProp(DefaultGunData::addShooterDeltaMovement)

        @JvmField
        val SHELL_TYPE = plainProp(DefaultGunData::shellType)
    }
}