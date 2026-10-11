package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.data.IDBasedData
import com.atsuishio.superbwarfare.data.ModColor
import com.atsuishio.superbwarfare.data.SingleOrList
import com.atsuishio.superbwarfare.data.StringOrObject
import com.atsuishio.superbwarfare.data.gun.melee.MeleeAction
import com.atsuishio.superbwarfare.data.gun.melee.MeleeHitbox
import com.atsuishio.superbwarfare.data.gun.melee.MeleeSweep
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import kotlin.math.max
import kotlin.math.min

@Suppress("unused")
@Serializable
data class DefaultGunData(
    // 不要动态修改这玩意，很容易出问题
    @SerialName("MaxDurability")
    val maxDurability: Int = 0,
    @SerialName("DurabilityPerShoot")
    val durabilityPerShoot: Int = 1,
    @SerialName("MaxEnergy")
    val maxEnergy: Int = 0,
    @SerialName("MaxReceiveEnergy")
    val maxReceiveEnergy: Int = -1,
    @SerialName("MaxExtractEnergy")
    val maxExtractEnergy: Int = -1,
    @SerialName("RecoilX")
    val recoilX: Double = 0.0,
    @SerialName("RecoilY")
    val recoilY: Double = 0.0,
    @SerialName("Recoil")
    val recoil: Double = 0.0,
    @SerialName("RecoilTime")
    val recoilTime: Int = 0,
    @SerialName("RecoilForce")
    val recoilForce: Float = 0f,
    // x:范围，y：振动时长，z：振幅
    @SerialName("ShootShake")
    val shootShake: SerializedVec3? = null,
    @SerialName("Sway")
    val sway: Double = 1.0,
    @SerialName("DefaultZoom")
    val defaultZoom: Double = 1.25,
    @SerialName("BoundBones")
    val boundBones: SingleOrList<String>? = SingleOrList(),
    @SerialName("BoundBonesYaw")
    val boundBonesYaw: SingleOrList<String>? = SingleOrList(),
    @SerialName("BoundBonesPitch")
    val boundBonesPitch: SingleOrList<String>? = SingleOrList(),
    @SerialName("MinZoom")
    val minZoom: Double = defaultZoom,
    @SerialName("MaxZoom")
    val maxZoom: Double = defaultZoom,
    @SerialName("Spread")
    val spread: Double = 0.0,
    @JvmField
    @SerialName("Damage")
    val damage: Double = 0.0,
    @SerialName("Headshot")
    val headshot: Double = 1.5,
    @JvmField
    @SerialName("Velocity")
    val velocity: Double = 0.0,
    @SerialName("Magazine")
    val magazine: SingleOrList<Int> = SingleOrList(0),
    // 属于弹鼓的弹匣等级，这些等级使用弹鼓专属的换弹动画与 ActionSteps 时间线
    @SerialName("DrumLevels")
    val drumLevels: SingleOrList<Int> = SingleOrList(),
    @SerialName("Range")
    val range: Int = 128,
    @SerialName("MeleeDamage")
    val meleeDamage: Double = 0.0,
    @SerialName("MeleeDuration")
    val meleeDuration: Int = 16,
    @SerialName("MeleeDamageTime")
    val meleeDamageTime: Int = 6,
    @SerialName("MeleeAngle")
    val meleeAngle: Int = 30,
    @SerialName("MeleeRange")
    val meleeRange: Double = 2.0,
    @SerialName("MeleeHeadshot")
    val meleeHeadshot: Double = 2.0,
    @SerialName("MeleeLegshot")
    val meleeLegshot: Double = 0.5,
    @SerialName("MeleeComboReset")
    val meleeComboReset: Int = 15,
    @SerialName("MeleeHitbox")
    val meleeHitbox: MeleeHitbox? = null,
    @SerialName("MeleeSweep")
    val meleeSweep: MeleeSweep? = null,
    @SerialName("MeleeActions")
    val meleeActions: SingleOrList<MeleeAction> = SingleOrList(),
    @SerialName("MeleeSound")
    val meleeSound: MeleeSound = MeleeSound(),
    @JvmField
    @SerialName("Projectile")
    val projectile: StringOrObject<ProjectileInfo> = StringOrObject(ProjectileInfo()),
    @SerialName("ProjectileBone")
    val projectileBone: SingleOrList<String> = SingleOrList<String>(),
    @SerialName("ShootPos")
    val shootPos: ShootPos = ShootPos(),
    @SerialName("SeekWeaponInfo")
    val seekWeaponInfo: SeekWeaponInfo? = null,
    @SerialName("ProjectileDummyInfo")
    val projectileDummyInfo: ProjectileDummyInfo? = null,
    @SerialName("AmmoCostPerShoot")
    val ammoCostPerShoot: Int = 1,
    @SerialName("FuelPerAmmo")
    val fuelPerAmmo: Int = 0,
    @SerialName("ProjectileAmount")
    val projectileAmount: Int = 1,
    @SerialName("SpreadPattern")
    val spreadPattern: ProjectileSpreadPattern? = null,
    @SerialName("Weight")
    val weight: Double = 1.0,
    @SerialName("DefaultFireMode")
    val defaultFireMode: String = FireMode.SEMI.typeName,
    @SerialName("AvailableFireModes")
    val availableFireModes: SingleOrList<StringOrObject<FireModeInfo>> = SingleOrList(StringOrObject(FireModeInfo())),
    @SerialName("ReloadTypes")
    val reloadTypes: Set<ReloadType> = setOf(ReloadType.MAGAZINE),
    @SerialName("SeekType")
    val seekType: SeekType? = SeekType.NONE,
    @SerialName("GunType")
    val gunType: GunType = GunType.SPECIAL,
    // Nullable!!!
    @SerialName("AutoReload")
    val autoReload: Boolean? = null,
    @SerialName("WithdrawAmmoWhenChangeSlot")
    val withdrawAmmoWhenChangeSlot: Boolean = false,
    @SerialName("ZoomReload")
    val zoomReload: Boolean = true,
    @SerialName("HasBipod")
    val hasBipod: Boolean = false,
    @SerialName("Deployable")
    val deployable: Boolean = false,
    // TODO(fire-mode): Keep this as legacy compatibility until HOLD uses ChargeInfo reset semantics.
    @SerialName("ClearHoldProgressAfterShoot")
    val clearHoldProgressAfterShoot: Boolean = false,
    @SerialName("BurstAmount")
    val burstAmount: Int = 0,
    @SerialName("BypassesArmor")
    val bypassesArmor: Double = 0.0,
    @SerialName("AmmoType")
    val ammoConsumers: SingleOrList<StringOrObject<AmmoConsumer>> = SingleOrList(),
    @SerialName("ChargeAction")
    val chargeActions: SingleOrList<ChargeAction> = SingleOrList(),
    @SerialName("UseNacelleCamera")
    val useNacelleCamera: Boolean = false,
    @SerialName("OpenBolt")
    val openBolt: Boolean = false,
    @SerialName("HasBarrelBullet")
    val hasBarrelBullet: Boolean = false,
    @SerialName("TacticalReload")
    val tacticalReload: Boolean = false,
    @SerialName("DrawTime")
    val drawTime: Int = 7,
    @SerialName("ZoomTime")
    val zoomTime: Int = 3,
    @SerialName("NormalReloadTime")
    val normalReloadTime: SingleOrList<Int> = SingleOrList(0),
    @SerialName("EmptyReloadTime")
    val emptyReloadTime: SingleOrList<Int> = SingleOrList(0),
    @SerialName("BoltActionTime")
    val boltActionTime: SingleOrList<Int> = SingleOrList(0),
    @SerialName("PrepareTime")
    val prepareTime: SingleOrList<Int> = SingleOrList(0),
    @SerialName("PrepareLoadTime")
    val prepareLoadTime: SingleOrList<Int> = SingleOrList(0),
    // 单发装填时的上弹时间
    @SerialName("PrepareAmmoLoadTime")
    val prepareAmmoLoadTime: SingleOrList<Int> = SingleOrList(1),
    @SerialName("PrepareEmptyTime")
    val prepareEmptyTime: SingleOrList<Int> = SingleOrList(0),
    // 每次单发装填用时的
    @SerialName("IterativeTime")
    val iterativeTime: SingleOrList<Int> = SingleOrList(0),
    // 单发装填时的上弹时间，在reload.iterativeLoadTimer等于该值时上弹
    @SerialName("IterativeAmmoLoadTime")
    val iterativeAmmoLoadTime: SingleOrList<Int> = SingleOrList(1),
    // 单次单发装填上弹数量
    @SerialName("IterativeLoadAmount")
    val iterativeLoadAmount: Int = 1,
    @SerialName("FinishTime")
    val finishTime: SingleOrList<Int> = SingleOrList(0),
    @SerialName("ActionSteps")
    val actionSteps: SingleOrList<GunActionStep> = SingleOrList(),
    // 连发模式下的射击间隔时间
    @SerialName("BurstCooldown")
    val burstCooldown: Int = 30,
    // 每次开火后叠加到自定义射速上的增量（可为负）
    @SerialName("RpmAddAfterShoot")
    val rpmAddAfterShoot: Int = 0,
    // 自定义射速（叠加在 RPM 上的偏移量）的下限，可为负
    @SerialName("MinCustomRpm")
    val minCustomRpm: Int = -600,
    // 自定义射速（叠加在 RPM 上的偏移量）的上限
    @SerialName("MaxCustomRpm")
    val maxCustomRpm: Int = 600,
    @SerialName("SoundRadius")
    val soundRadius: Double = 0.0,
    @SerialName("RPM")
    val rpm: Int = 600,
    // 全局射速倍率：最终射速 = (RPM + 每发累加值) * RpmMultiplier
    @SerialName("RpmMultiplier")
    val rpmMultiplier: Double = 1.0,
    @SerialName("ExplosionDamage")
    val explosionDamage: Double = 0.0,
    @SerialName("ExplosionRadius")
    val explosionRadius: Double = 0.0,
    @SerialName("Gravity")
    val gravity: Double = 0.03,
    @SerialName("ShootDelay")
    val shootDelay: Int = 0,
    @SerialName("ShootDelayTime")
    val shootDelayTime: Int = 0,
    @SerialName("HeatPerShoot")
    val heatPerShoot: Double = 0.0,
    @SerialName("AvailablePerks")
    val availablePerks: SingleOrList<String> = SingleOrList(
        "@Ammo",
        "superbwarfare:field_doctor",
        "superbwarfare:powerful_attraction",
        "superbwarfare:intelligent_chip",
        "superbwarfare:monster_hunter",
        "superbwarfare:vorpal_weapon",
        "!superbwarfare:micro_missile",
        "!superbwarfare:longer_wire",
        "!superbwarfare:cupid_arrow"
    ),
    @SerialName("AvailableAttachments")
    val availableAttachments: Map<String, List<StringOrObject<AttachmentOption>>> = emptyMap(),

    @SerialName("AttachmentConflicts")
    val attachmentConflicts: Map<String, List<String>> = emptyMap(),
    @SerialName("DamageReduce")
    val damageReduce: DamageReduce = DamageReduce(),
    // 自然情况下每tick减少的热量
    @SerialName("NaturalCooldown")
    val naturalCooldown: Double = 0.25,
    // 在水中或雨中时的散热比例
    @SerialName("InWaterCooldownRate")
    val inWaterCooldownRate: Double = 1.1,
    // 在细雪中时的散热比例
    @SerialName("InSnowCooldownRate")
    val inSnowCooldownRate: Double = 1.5,
    // 在火焰中时的散热比例
    @SerialName("InFireCooldownRate")
    val inFireCooldownRate: Double = 0.6,
    // 在岩浆中时的散热比例
    @SerialName("InLavaCooldownRate")
    val inLavaCooldownRate: Double = 0.2,
    // 瞄准时的扩散比例
    @SerialName("ZoomSpreadRate")
    val zoomSpreadRate: Double = 0.05,
    @SerialName("SeekTime")
    val seekTime: Int = 20,
    @SerialName("SeekAngle")
    val seekAngle: Double = 10.0,
    @SerialName("SeekRange")
    val seekRange: Double = 384.0,
    @SerialName("MaxGuidedRange")
    val maxGuidedRange: Double = 1024.0,
    @SerialName("CanGuidedByRadar")
    val canGuidedByRadar: Boolean = true,
    @SerialName("AffectedByStealthTarget")
    val affectedByStealthTarget: Boolean = true,
    @SerialName("MinTargetHeight")
    val minTargetHeight: Double = 0.0,
    @SerialName("MaxTargetHeight")
    val maxTargetHeight: Double = 114514.0,
    @SerialName("SoundInfo")
    val soundInfo: SoundInfo = SoundInfo(),
    @SerialName("ShootAnimationTime")
    val shootAnimationTime: Int = 0,
    @SerialName("ShootAnimation")
    val shootAnimation: String? = null,
    @SerialName("SpreadAmount")
    val spreadAmount: Int = 10,
    @SerialName("ApDurability")
    val apDurability: Int = 50,
    @SerialName("SpreadAngle")
    val spreadAngle: Int = 15,
    @SerialName("ShellType")
    val shellType: String = "Default",
    @SerialName("ProjectileLife")
    val projectileLife: Int = 400,
    // 投射物分裂次数：爆炸消失后分裂出的产物再减一，0 表示不分裂
    @SerialName("ProjectileSplitCount")
    val projectileSplitCount: Int = 0,
    // 每次分裂出的产物个数
    @SerialName("ProjectileSplitAmount")
    val projectileSplitAmount: Int = 4,
    @SerialName("AddShooterDeltaMovement")
    val addShooterDeltaMovement: Boolean = false,
    @SerialName("Icon")
    val icon: SerializedResourceLocation = DEFAULT_ICON,
    /*
    * 准星类型
    * 预制的字段有：
    * @Empty - 空
    * @Custom - 自定义
    * @GunDefault - 默认枪械准星
    * @VehicleDefault - 默认载具准星
    */
    @SerialName("Crosshair")
    val crosshair: String = "@GunDefault",
    // 瞄准时的准星，默认为空，仅用于部分载具
    @SerialName("CrosshairZooming")
    val crosshairZooming: String = "@Empty",
    @SerialName("CrosshairColor")
    val crosshairColor: ModColor = ModColor(),
    @SerialName("Name")
    val name: String? = null,
    @SerialName("UnderwaterMotionScale")
    val underwaterMotionScale: Float = 0.75f,
    @SerialName("ExplosionDestroy")
    val explosionDestroy: Boolean = true,
    @SerialName("Knockback")
    val knockback: Float = 0.05f,
    // 经验获取倍率：结算枪械经验时乘上这个值
    @SerialName("ExpMultiplier")
    val expMultiplier: Double = 1.0,
    // 投射物生命汲取：枪械投射物伤害命中生物时，按此比例给发射者回血
    @SerialName("ProjectileLifesteal")
    val projectileLifesteal: Double = 0.0,
    // 近战生命汲取：枪械近战伤害命中生物时，按此比例给攻击者回血
    @SerialName("MeleeLifesteal")
    val meleeLifesteal: Double = 0.0,
) : IDBasedData<DefaultGunData> {
    @Transient
    @kotlinx.serialization.Transient
    var itemId: String = ""

    override fun getId() = itemId

    override fun setId(id: String) {
        this.itemId = id
    }

    @Transient
    @kotlinx.serialization.Transient
    var isDefaultData = true

    fun projectile(): ProjectileInfo {
        return projectile.value
    }

    fun availableFireModes() = availableFireModes.list.map { it.value }

    @Transient
    @kotlinx.serialization.Transient
    private var fireModesCache: List<FireModeInfo>? = null

    val fireModes: List<FireModeInfo>
        get() {
            if (fireModesCache == null) {
                this.fireModesCache = this.availableFireModes.list.map { c -> c.value }
            }

            return this.fireModesCache!!
        }

    fun availablePerks(): List<String> {
        return availablePerks.list
    }

    fun clamped(): DefaultGunData {
        val clampedMaxEnergy = max(0, maxEnergy)
        val clampedMeleeDuration = max(1, meleeDuration)
        val clampedProjectileAmount = max(0, projectileAmount)
        return copy(
            maxDurability = max(0, maxDurability),
            durabilityPerShoot = max(0, durabilityPerShoot),
            maxEnergy = clampedMaxEnergy,
            maxReceiveEnergy = maxReceiveEnergy.coerceIn(-1, clampedMaxEnergy).let { if (it < 0) clampedMaxEnergy else it },
            maxExtractEnergy = maxExtractEnergy.coerceIn(-1, clampedMaxEnergy).let { if (it < 0) clampedMaxEnergy else it },
            meleeDuration = clampedMeleeDuration,
            meleeAngle = meleeAngle.coerceIn(1, 180),
            meleeComboReset = max(0, meleeComboReset),
            meleeHeadshot = max(0.0, meleeHeadshot),
            meleeLegshot = max(0.0, meleeLegshot),
            zoomSpreadRate = zoomSpreadRate.coerceIn(0.0, 1.0),
            range = max(1, range),
            meleeDamageTime = min(clampedMeleeDuration - 1, meleeDamageTime),
            ammoCostPerShoot = max(0, ammoCostPerShoot),
            fuelPerAmmo = max(0, fuelPerAmmo),
            projectileAmount = clampedProjectileAmount,
            weight = max(1.0, weight),
            magazine = if (clampedProjectileAmount == 0 && meleeDamage > 0) {
                SingleOrList(0)
            } else {
                SingleOrList(magazine.list.map { max(0, it) }.toMutableList())
            },
            seekType = seekType ?: SeekType.NONE,
            burstAmount = max(0, burstAmount),
            rpm = rpm.coerceIn(1, 114514),
            underwaterMotionScale = underwaterMotionScale.coerceIn(0.0f, 1.0f),
            expMultiplier = max(0.0, expMultiplier),
            projectileLifesteal = max(0.0, projectileLifesteal),
            meleeLifesteal = max(0.0, meleeLifesteal),
        )
    }

    companion object {
        val DEFAULT_ICON: ResourceLocation = loc("textures/gun_icon/default_icon.png")

    }
}