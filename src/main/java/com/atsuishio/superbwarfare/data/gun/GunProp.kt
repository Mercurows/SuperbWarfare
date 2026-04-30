package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.Prop
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.getPerkPriority
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.perk.Perk
import kotlin.math.min
import kotlin.reflect.KMutableProperty1

@Suppress("UNUSED")
class GunProp<T, R>(
    prop: KMutableProperty1<DefaultGunData, T>,
    transform: (T) -> R,
) : Prop<GunData, DefaultGunData, T, R, GunProp<T, R>>(prop, transform) {

    override fun toString() = "GunProp[$serializationName]"

    companion object {
        val entries = mutableListOf<GunProp<*, *>>()

        inline fun <reified T> plainProp(
            prop: KMutableProperty1<DefaultGunData, T>,
        ): GunProp<T, T> {
            return GunProp(prop) { it }.also { entries.add(it) }
        }

        inline fun <reified T, R> complexProp(
            prop: KMutableProperty1<DefaultGunData, T>,
            noinline transform: (T) -> R
        ): GunProp<T, R> {
            return GunProp(prop, transform).also { entries.add(it) }
        }


        @JvmField
        val MAX_DURABILITY = plainProp(DefaultGunData::maxDurability)

        @JvmField
        val DURABILITY_PER_SHOOT = plainProp(DefaultGunData::durabilityPerShoot)

        @JvmField
        val MAX_ENERGY = plainProp(DefaultGunData::maxEnergy)

        @JvmField
        val MAX_RECEIVE_ENERGY = plainProp(DefaultGunData::maxReceiveEnergy)

        @JvmField
        val MAX_EXTRACT_ENERGY = plainProp(DefaultGunData::maxExtractEnergy)

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
        val MELEE_DURATION = plainProp(DefaultGunData::meleeDuration)

        @JvmField
        val MELEE_RANGE = plainProp(DefaultGunData::meleeRange)

        @JvmField
        val MELEE_ANGLE = plainProp(DefaultGunData::meleeAngle)

        @JvmField
        val ZOOM_SPREAD_RATE = plainProp(DefaultGunData::zoomSpreadRate)

        @JvmField
        val SEEK_TIME = plainProp(DefaultGunData::seekTime)

        @JvmField
        val SEEK_ANGLE = plainProp(DefaultGunData::seekAngle)

        @JvmField
        val SEEK_RANGE = plainProp(DefaultGunData::seekRange)

        @JvmField
        val MAX_GUIDED_RANGE = plainProp(DefaultGunData::maxGuidedRange)

        @JvmField
        val CAN_GUIDED_BY_RADAR = plainProp(DefaultGunData::canGuidedByRadar)

        @JvmField
        val AFFECTED_BY_STEALTH_TARGET = plainProp(DefaultGunData::affectedByStealthTarget)

        @JvmField
        val MIN_TARGET_HEIGHT = plainProp(DefaultGunData::minTargetHeight)

        @JvmField
        val MAX_TARGET_HEIGHT = plainProp(DefaultGunData::maxTargetHeight)

        @JvmField
        val RANGE = plainProp(DefaultGunData::range)

        @JvmField
        val MELEE_DAMAGE_TIME =
            plainProp(DefaultGunData::meleeDamageTime)

        @JvmField
        val PROJECTILE = complexProp(DefaultGunData::projectile) { it.value }

        @JvmField
        val AMMO_COST_PER_SHOOT = plainProp(DefaultGunData::ammoCostPerShoot)

        @JvmField
        val PROJECTILE_AMOUNT = plainProp(DefaultGunData::projectileAmount)

        @JvmField
        val WEIGHT = plainProp(DefaultGunData::weight)

        @JvmField
        val DEFAULT_FIRE_MODE =
            complexProp(DefaultGunData::defaultFireMode) { it.ifEmpty { FireMode.SEMI.typeName } }

        @JvmField
        val AVAILABLE_FIRE_MODES =
            complexProp(DefaultGunData::availableFireModes) { it.list.map { l -> l.value } }

        @JvmField
        val MAGAZINE = plainProp(DefaultGunData::magazine)

        @JvmField
        val RELOAD_TYPES = complexProp(DefaultGunData::reloadTypes) { it }

        @JvmField
        val SEEK_TYPE = complexProp(DefaultGunData::seekType) { it ?: SeekType.NONE }

        @JvmField
        val GUN_TYPE = complexProp(DefaultGunData::gunType) { it }

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
        val BURST_AMOUNT = plainProp(DefaultGunData::burstAmount)

        @JvmField
        val BYPASSES_ARMOR = plainProp(DefaultGunData::bypassesArmor)

        @JvmField
        val AMMO_CONSUMER = complexProp(
            DefaultGunData::ammoConsumers
        ) { it.list.map { l -> l.value.also { consumer -> consumer.init() } } }

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
        val RPM = plainProp(DefaultGunData::rpm)

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
        val AVAILABLE_PERKS = complexProp(DefaultGunData::availablePerks) {
            val availablePerks = mutableListOf<Perk>()
            val perkNames = it.list.ifEmpty { return@complexProp availablePerks }

            val sortedNames = perkNames.distinct().sortedWith { s1, s2 ->
                val p1 = getPerkPriority(s1)
                val p2 = getPerkPriority(s2)
                if (p1 != p2) {
                    return@sortedWith p1.compareTo(p2)
                } else {
                    return@sortedWith s1.compareTo(s2)
                }
            }

            val perks = ModPerks.PERK_REGISTRY.entrySet()

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
                        availablePerks.addAll(perkValues.filter { perk -> perk.type == type })
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
            return@complexProp availablePerks.toList()
        }

        @JvmField
        val ICON = complexProp(DefaultGunData::icon) { it }

        @JvmField
        val CROSSHAIR = complexProp(DefaultGunData::crosshair) { it.ifEmpty { "@GunDefault" } }

        @JvmField
        val CROSSHAIR_ZOOMING = complexProp(DefaultGunData::crosshairZooming) { it.ifEmpty { "@Empty" } }

        @JvmField
        val CROSSHAIR_COLOR = complexProp(DefaultGunData::crosshairColor) { it }

        // 注意Nullable
        @JvmField
        val NAME = plainProp(DefaultGunData::name)

        @JvmField
        val SHOOT_POS = complexProp(DefaultGunData::shootPos) { it }

        @JvmField
        val SEEK_WEAPON_INFO = plainProp(DefaultGunData::seekWeaponInfo)

        @JvmField
        val SOUND_INFO = complexProp(DefaultGunData::soundInfo) { it }

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

        @JvmField
        val PROJECTILE_LIFE = plainProp(DefaultGunData::projectileLife)

        @JvmField
        val AP_DURABILITY = plainProp(DefaultGunData::apDurability)

        // TODO 会不会有点屎...
        fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) = with(modifier) {
            modify(MAX_DURABILITY) { it.coerceAtLeast(0) }
            modify(DURABILITY_PER_SHOOT) { it.coerceAtLeast(0) }
            modify(MAX_ENERGY) { it.coerceAtLeast(0) }
            modify(MAX_RECEIVE_ENERGY) {
                val maxEnergy = modifier[MAX_ENERGY]
                val value = it.coerceIn(-1, maxEnergy)
                if (value < 0) maxEnergy else value
            }
            modify(MAX_EXTRACT_ENERGY) {
                val maxEnergy = modifier[MAX_ENERGY]
                val value = it.coerceIn(-1, maxEnergy)
                if (value < 0) maxEnergy else value
            }

            modify(MELEE_DURATION) { it.coerceAtLeast(1) }
            modify(MELEE_ANGLE) { it.coerceIn(1, 180) }
            modify(ZOOM_SPREAD_RATE) { it.coerceIn(0.0, 1.0) }

            modify(RANGE) { it.coerceAtLeast(1) }
            modify(MELEE_DAMAGE_TIME) { min(modifier[MELEE_DURATION] - 1, it) }
            modify(AMMO_COST_PER_SHOOT) { it.coerceAtLeast(0) }
            modify(PROJECTILE_AMOUNT) { it.coerceAtLeast(0) }
            modify(WEIGHT) { it.coerceAtLeast(1.0) }

            modify(MAGAZINE) {
                if (modifier[PROJECTILE_AMOUNT] <= 0 && modifier[MELEE_DAMAGE] > 0) 0 else it.coerceAtLeast(0)
            }

            modify(BURST_AMOUNT) { it.coerceAtLeast(0) }
            modify(RPM) { it.coerceIn(1, 114514) }
        }
    }
}