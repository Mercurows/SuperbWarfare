package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.Prop
import com.atsuishio.superbwarfare.data.SingleOrList
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.getPerkPriority
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.SOUND_INFO
import com.atsuishio.superbwarfare.data.gun.melee.isMeleeProjectileMarker
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.perk.Perk
import kotlin.math.min
import kotlin.reflect.KProperty1

@Suppress("UNUSED")
class GunProp<T, R>(
    prop: KProperty1<DefaultGunData, T>,
    transform: (T) -> R,
    contextTransform: ((GunData, T) -> R)? = null,
) : Prop<GunData, DefaultGunData, T, R, GunProp<T, R>>(prop, transform, contextTransform) {

    override fun toString() = "GunProp[$serializationName]"

    companion object {
        val entries = mutableListOf<GunProp<*, *>>()

        inline fun <reified T> plainProp(
            prop: KProperty1<DefaultGunData, T>,
        ): GunProp<T, T> {
            return GunProp(prop = prop, transform = { it }).also { entries.add(it) }
        }

        inline fun <reified T, R> complexProp(
            prop: KProperty1<DefaultGunData, T>,
            noinline transform: (T) -> R
        ): GunProp<T, R> {
            return GunProp(prop = prop, transform = transform).also { entries.add(it) }
        }

        fun leveledIntProp(
            prop: KProperty1<DefaultGunData, SingleOrList<Int>>,
        ): GunProp<SingleOrList<Int>, Int> {
            return GunProp(
                prop,
                { it.firstOrNull() ?: 0 },
                { data, values -> values.atMagazineLevel(data.magazineLevel()) }
            ).also { entries.add(it) }
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

        /**
         * 连招重置窗口（tick）：一段近战结束后多少 tick 内再挥击算连招。
         */
        @JvmField
        val MELEE_COMBO_RESET = plainProp(DefaultGunData::meleeComboReset)

        /**
         * 近战判定形状。不写（null）时走旧的圆锥语义，22 把旧枪 json 一行都不用改。
         */
        @JvmField
        val MELEE_HITBOX = plainProp(DefaultGunData::meleeHitbox)

        /**
         * 近战横扫采样。不写（null）= 静态判定。
         */
        @JvmField
        val MELEE_SWEEP = plainProp(DefaultGunData::meleeSweep)

        /**
         * 近战动作序列（循环序列，下标在挥击开始时锁存）。
         */
        @JvmField
        val MELEE_ACTIONS = complexProp(DefaultGunData::meleeActions) { it.list }

        /**
         * 近战攻击音效。
         *
         * 单独一个属性（而不是挂在 [SOUND_INFO] 下），配件才能只覆盖它而不影响开火/换弹音效。
         */
        @JvmField
        val MELEE_SOUND = complexProp(DefaultGunData::meleeSound) { it }

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
        val PROJECTILE_BONE = plainProp(DefaultGunData::projectileBone)

        @JvmField
        val AMMO_COST_PER_SHOOT = plainProp(DefaultGunData::ammoCostPerShoot)

        /** 「其他类型弹药 → 弹药」换算比例：多少外部资源（如 FE）折算成 1 发弹匣弹药 */
        @JvmField
        val FUEL_PER_AMMO = plainProp(DefaultGunData::fuelPerAmmo)

        @JvmField
        val PROJECTILE_AMOUNT = plainProp(DefaultGunData::projectileAmount)

        @JvmField
        val SPREAD_PATTERN = plainProp(DefaultGunData::spreadPattern)

        @JvmField
        val WEIGHT = plainProp(DefaultGunData::weight)

        @JvmField
        val DEFAULT_FIRE_MODE =
            complexProp(DefaultGunData::defaultFireMode) { it.ifEmpty { FireMode.SEMI.typeName } }

        @JvmField
        val AVAILABLE_FIRE_MODES =
            complexProp(DefaultGunData::availableFireModes) { it.list.map { l -> l.value } }

        @JvmField
        val MAGAZINE = GunProp(
            DefaultGunData::magazine,
            { it.firstOrNull() ?: 0 },
            { data, values ->
                if (values.list.size <= 1) {
                    (values.firstOrNull() ?: 0) + data.item.getCustomMagazine(data)
                } else {
                    values.atMagazineLevel(data.magazineLevel())
                }
            }
        ).also { entries.add(it) }

        /** 弹鼓等级列表，供 `GunData.isDrumLevel()` 判断当前等级是否为弹鼓。 */
        @JvmField
        val DRUM_LEVELS = complexProp(DefaultGunData::drumLevels) { it }

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
        val MIN_ZOOM = plainProp(DefaultGunData::minZoom)

        @JvmField
        val MAX_ZOOM = plainProp(DefaultGunData::maxZoom)

        @JvmField
        val BOUND_BONES = plainProp(DefaultGunData::boundBones)

        @JvmField
        val BOUND_BONES_YAW = plainProp(DefaultGunData::boundBonesYaw)

        @JvmField
        val BOUND_BONES_PITCH = plainProp(DefaultGunData::boundBonesPitch)

        @JvmField
        val BURST_AMOUNT = plainProp(DefaultGunData::burstAmount)

        @JvmField
        val BYPASSES_ARMOR = plainProp(DefaultGunData::bypassesArmor)

        @JvmField
        val AMMO_CONSUMER = complexProp(
            DefaultGunData::ammoConsumers
        ) { it.list.map { l -> l.value } }

        @JvmField
        val NORMAL_RELOAD_TIME = leveledIntProp(DefaultGunData::normalReloadTime)

        @JvmField
        val EMPTY_RELOAD_TIME = leveledIntProp(DefaultGunData::emptyReloadTime)

        @JvmField
        val BOLT_ACTION_TIME = leveledIntProp(DefaultGunData::boltActionTime)

        @JvmField
        val PREPARE_TIME = leveledIntProp(DefaultGunData::prepareTime)

        @JvmField
        val PREPARE_LOAD_TIME = leveledIntProp(DefaultGunData::prepareLoadTime)

        @JvmField
        val PREPARE_AMMO_LOAD_TIME = leveledIntProp(DefaultGunData::prepareAmmoLoadTime)

        @JvmField
        val PREPARE_EMPTY_TIME = leveledIntProp(DefaultGunData::prepareEmptyTime)

        @JvmField
        val ITERATIVE_TIME = leveledIntProp(DefaultGunData::iterativeTime)

        @JvmField
        val ITERATIVE_AMMO_LOAD_TIME = leveledIntProp(DefaultGunData::iterativeAmmoLoadTime)

        @JvmField
        val ITERATIVE_LOAD_AMOUNT = plainProp(DefaultGunData::iterativeLoadAmount)

        @JvmField
        val FINISH_TIME = leveledIntProp(DefaultGunData::finishTime)

        @JvmField
        val BURST_COOLDOWN = plainProp(DefaultGunData::burstCooldown)

        @JvmField
        val RPM_ADD_AFTER_SHOOT = plainProp(DefaultGunData::rpmAddAfterShoot)

        @JvmField
        val CUSTOM_RPM_MIN = plainProp(DefaultGunData::minCustomRpm)

        @JvmField
        val CUSTOM_RPM_MAX = plainProp(DefaultGunData::maxCustomRpm)

        @JvmField
        val SOUND_RADIUS = plainProp(DefaultGunData::soundRadius)

        @JvmField
        val RPM = plainProp(DefaultGunData::rpm)

        /**
         * 全局射速倍率。
         *
         * 作用于**最终射速**（`基础 RPM + 每发累加值`），而不是基础 RPM——
         * 否则乘法类效果会被 RpmAddAfterShoot 那套加法累加值稀释。
         */
        @JvmField
        val RPM_MULTIPLIER = plainProp(DefaultGunData::rpmMultiplier)

        @JvmField
        val EXPLOSION_DAMAGE = plainProp(DefaultGunData::explosionDamage)

        @JvmField
        val EXPLOSION_RADIUS = plainProp(DefaultGunData::explosionRadius)

        @JvmField
        val GRAVITY = plainProp(DefaultGunData::gravity)

        @JvmField
        val SHOOT_DELAY = plainProp(DefaultGunData::shootDelay)

        @JvmField
        val SHOOT_DELAY_TIME = plainProp(DefaultGunData::shootDelayTime)

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
        val USE_NACELLE_CAMERA = plainProp(DefaultGunData::useNacelleCamera)

        @JvmField
        val OPEN_BOLT = plainProp(DefaultGunData::openBolt)

        @JvmField
        val HAS_BARREL_BULLET = plainProp(DefaultGunData::hasBarrelBullet)

        @JvmField
        val TACTICAL_RELOAD = plainProp(DefaultGunData::tacticalReload)

        @JvmField
        val DRAW_TIME = plainProp(DefaultGunData::drawTime)

        @JvmField
        val HAS_BIPOD = plainProp(DefaultGunData::hasBipod)

        /**
         * 武器进入瞄准的时间，单位是tick
         *
         * 主创否定了将进入瞄准和退出瞄准时间分离的设计，现在ZOOM_OUT_TIME = ZOOM_TIME * 0.75f
         * 这导致无法制作瞄准时间1秒，退出瞄准时间需要10秒的池沼瞄准镜
         * 如果以后哪天要做这种功能了，记得带上这段注释来打脸
         */
        @JvmField
        val ZOOM_TIME = plainProp(DefaultGunData::zoomTime)

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
        val ICON = plainProp(DefaultGunData::icon)

        @JvmField
        val CROSSHAIR = complexProp(DefaultGunData::crosshair) { it.ifEmpty { "@GunDefault" } }

        @JvmField
        val CROSSHAIR_ZOOMING = complexProp(DefaultGunData::crosshairZooming) { it.ifEmpty { "@Empty" } }

        @JvmField
        val CROSSHAIR_COLOR = plainProp(DefaultGunData::crosshairColor)

        // 注意Nullable
        @JvmField
        val NAME = plainProp(DefaultGunData::name)

        @JvmField
        val SHOOT_POS = plainProp(DefaultGunData::shootPos)

        @JvmField
        val SEEK_WEAPON_INFO = plainProp(DefaultGunData::seekWeaponInfo)

        @JvmField
        val PROJECTILE_DUMMY_INFO = plainProp(DefaultGunData::projectileDummyInfo)

        @JvmField
        val SOUND_INFO = plainProp(DefaultGunData::soundInfo)

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

        @JvmField
        val UNDERWATER_MOTION_SCALE = plainProp(DefaultGunData::underwaterMotionScale)

        @JvmField
        val EXPLOSION_DESTROY = plainProp(DefaultGunData::explosionDestroy)

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
            modify(MELEE_COMBO_RESET) { it.coerceAtLeast(0) }
            modify(ZOOM_SPREAD_RATE) { it.coerceIn(0.0, 1.0) }

            modify(RANGE) { it.coerceAtLeast(1) }
            modify(MELEE_DAMAGE_TIME) { min(modifier[MELEE_DURATION] - 1, it) }
            modify(AMMO_COST_PER_SHOOT) { it.coerceAtLeast(0) }
            modify(FUEL_PER_AMMO) { it.coerceAtLeast(0) }
            modify(PROJECTILE_AMOUNT) { it.coerceAtLeast(0) }
            modify(WEIGHT) { it.coerceAtLeast(1.0) }

            modify(MAGAZINE) {
                // 近战专属枪械（显式 `@melee`，或兼容回退的 `ProjectileAmount <= 0 && MeleeDamage > 0`）
                // 没有弹匣概念：不走开火链路，也不该参与换弹/备弹显示
                if (modifier[PROJECTILE].itemId.isMeleeProjectileMarker() ||
                    (modifier[PROJECTILE_AMOUNT] <= 0 && modifier[MELEE_DAMAGE] > 0)
                ) {
                    0
                } else {
                    it.coerceAtLeast(0)
                }
            }

            modify(BURST_AMOUNT) { it.coerceAtLeast(0) }
            modify(RPM) { it.coerceIn(1, 114514) }
            modify(RPM_MULTIPLIER) { it.coerceAtLeast(0.0) }
            modify(UNDERWATER_MOTION_SCALE) { it.coerceIn(0.0f, 1.0f) }

            modify(DRAW_TIME) { it.coerceAtLeast(1) }
            modify(ZOOM_TIME) { it.coerceAtLeast(1) }
        }
    }
}
