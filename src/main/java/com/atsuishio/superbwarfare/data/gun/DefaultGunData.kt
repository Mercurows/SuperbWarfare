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
    /**
     * 枪械自身可存储的能量上限（FE）。
     *
     * 对能量类武器（`AmmoType` 为 `FE` / `RF` / `energy`）这是唯一的「弹源」：
     * 背包型直接从这里按 [ammoCostPerShoot] 扣，弹匣型在换弹时按 [ammoCostPerShoot] 折算成
     * [magazine] 的发数。弹匣型请保证 `maxEnergy >= 单次装填发数 * ammoCostPerShoot`，
     * 否则一个满弹匣也得靠多次装填凑齐。
     */
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
    /**
     * 弹匣容量（按弹匣配件等级分档）。
     *
     * `<= 0` 表示**背包型**：不使用弹匣，开火时直接从背包（或对能量类武器而言从 [maxEnergy]）
     * 扣除 [ammoCostPerShoot]，不能换弹，也不能退弹。
     *
     * `> 0` 表示**弹匣型**：开火扣弹匣发数，换弹时才按 [ammoCostPerShoot] 把能量折算成发数装填。
     * 能量类武器即由此字段决定是哪种形态，见 `GunData.useBackpackAmmo` / `GunData.isEnergyMagazine`。
     */
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
    /**
     * 近战距离：**叠加**在 `MeleeHitbox.Range` 之上的额外距离。
     *
     * 判定用的总距离是 `Range + MeleeRange + player.getEntityReach()`；形状没写 `Range` 时
     * 基数取 0，也就是"距离就由这里决定"。配件要加近战距离就是加这个属性。
     */
    @SerialName("MeleeRange")
    val meleeRange: Double = 0.0,
    /**
     * 一段近战动作结束后多少 tick 内再挥击算连招。
     *
     * 窗口内再挥击进下一段，超时回到第 0 段。
     */
    @SerialName("MeleeComboReset")
    val meleeComboReset: Int = 15,
    /**
     * 近战判定形状（JSON 顶层 `MeleeHitbox`）。
     *
     * 不写时走旧的圆锥语义：`MeleeAngle` 总张角、上下不限、距离 = `player.getEntityReach() + MeleeRange`。
     */
    @SerialName("MeleeHitbox")
    val meleeHitbox: MeleeHitbox? = null,
    /**
     * 近战横扫采样（JSON 顶层 `MeleeSweep`）。不写 = 静态判定。
     */
    @SerialName("MeleeSweep")
    val meleeSweep: MeleeSweep? = null,
    /**
     * 近战动作序列（JSON 顶层 `MeleeActions`）。
     *
     * **循环序列**：下标在挥击开始时锁存，越过末尾回到第 0 段；每段可独立指定
     * 动画/时长/结算 tick/判定/伤害/音效。
     */
    @SerialName("MeleeActions")
    val meleeActions: SingleOrList<MeleeAction> = SingleOrList(),
    /**
     * 近战攻击音效。
     *
     * 与 [soundInfo] 分开，是为了让配件/弹药/开火模式能**只**改近战音效：
     * 属性覆盖的合并粒度是顶层属性，写 `"SoundInfo"` 会整块替换掉开火与换弹音效。
     */
    @SerialName("MeleeSound")
    val meleeSound: MeleeSound = MeleeSound(),
    @JvmField
    @SerialName("Projectile")
    val projectile: StringOrObject<ProjectileInfo> = StringOrObject(ProjectileInfo()),
    /**
    * Bones of the model that draw the round currently loaded in the weapon.
    *
    * A single name or a list of them, so one ammo type can draw several bones at once (for example
    * a warhead bone plus its "_dummy" counterpart).
    *
    * Each ammo consumer may override this with its own bones (see [AmmoConsumer.projectileBone]);
    * the names of all of them are collected by [GunData.projectileBoneNames] so the renderer can
    * hide every bone except the ones of the selected ammo type.
    */
    @SerialName("ProjectileBone")
    val projectileBone: SingleOrList<String> = SingleOrList<String>(),
    @SerialName("ShootPos")
    val shootPos: ShootPos = ShootPos(),
    @SerialName("SeekWeaponInfo")
    val seekWeaponInfo: SeekWeaponInfo? = null,
    @SerialName("ProjectileDummyInfo")
    val projectileDummyInfo: ProjectileDummyInfo? = null,
    /**
     * 每次开火消耗的弹药量（**主来源口径**：一次开火扣几个弹药单位）。
     *
     * 物品类武器就是「扣几发子弹」；能量类武器下随形态变化：
     * - 背包型（[magazine] `<= 0`）：没有弹匣，每发直接扣这么多 FE；
     * - 弹匣型（[magazine] `> 0` 且 [fuelPerAmmo] `> 0`）：每发扣这么多**发弹匣弹药**，
     *   能量只在换弹/退弹时按 [fuelPerAmmo] 折算，所以这里应当写 `1`
     *   —— 不要再拿它当 FE 用量，那是 [fuelPerAmmo] 的职责。
     */
    @SerialName("AmmoCostPerShoot")
    val ammoCostPerShoot: Int = 1,
    /**
     * 「其他类型弹药 → 弹药」的换算比例：多少点外部资源折算成 **1 发**弹匣弹药。
     *
     * 目前用于能量类武器（`AmmoType` 为 `FE` / `RF` / `energy`）的弹匣形态：
     * - 换弹时，备弹能量按 `能量 / fuelPerAmmo` 折算成能装填几发；
     * - 退弹时，弹匣剩余发数按 `发数 * fuelPerAmmo` 折回能量。
     *
     * 只有 [magazine] `> 0` 且本字段 `> 0` 才是弹匣型能量武器
     * （见 `GunData.isEnergyMagazine`）。背包型没有弹匣，不使用本字段，
     * 它的每发消耗由 [ammoCostPerShoot] 直接表示。
     */
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
    // TODO(fire-mode): Keep this as legacy compatibility until HOLD uses ChargeInfo reset semantics.
    @SerialName("ClearHoldProgressAfterShoot")
    val clearHoldProgressAfterShoot: Boolean = false,
    @SerialName("BurstAmount")
    val burstAmount: Int = 0,
    @SerialName("BypassesArmor")
    val bypassesArmor: Double = 0.0,
    @SerialName("AmmoType")
    val ammoConsumers: SingleOrList<StringOrObject<AmmoConsumer>> = SingleOrList(),
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
    val zoomSpreadRate: Double = 0.1,
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
    /**
     * 返回一份收敛到合法区间的副本。
     *
     * 原实现是就地修改字段；改成不可变 value 之后只能返回新实例。当前仓库内没有调用方，
     * 保留是为了不丢掉这段收敛逻辑（IDBasedData.limit() 的默认实现是空操作）。
     */
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
        )
    }

    companion object {
        val DEFAULT_ICON: ResourceLocation = loc("textures/gun_icon/default_icon.png")

    }
}