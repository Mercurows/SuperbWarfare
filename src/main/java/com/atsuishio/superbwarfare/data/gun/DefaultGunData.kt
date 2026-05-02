package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.annotation.ServerOnly
import com.atsuishio.superbwarfare.data.IDBasedData
import com.atsuishio.superbwarfare.data.ModColor
import com.atsuishio.superbwarfare.data.ObjectToList
import com.atsuishio.superbwarfare.data.StringToObject
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import kotlin.math.max
import kotlin.math.min

@Suppress("unused")
@Serializable
class DefaultGunData : IDBasedData<DefaultGunData> {
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

    // 不要动态修改这玩意，很容易出问题
    @SerialName("MaxDurability")
    var maxDurability = 0

    @ServerOnly
    @SerialName("DurabilityPerShoot")
    var durabilityPerShoot = 1

    @SerialName("MaxEnergy")
    var maxEnergy = 0

    @ServerOnly
    @SerialName("MaxReceiveEnergy")
    var maxReceiveEnergy = -1

    @ServerOnly
    @SerialName("MaxExtractEnergy")
    var maxExtractEnergy = -1

    @SerialName("RecoilX")
    var recoilX = 0.0

    @SerialName("RecoilY")
    var recoilY = 0.0

    @SerialName("Recoil")
    var recoil = 0.0

    @SerialName("RecoilTime")
    var recoilTime = 0

    @SerialName("RecoilForce")
    var recoilForce = 0f

    // x:范围，y：振动时长，z：振幅
    @ServerOnly
    @SerialName("ShootShake")
    var shootShake: SerializedVec3? = null

    @SerialName("DefaultZoom")
    var defaultZoom = 1.25

    @SerialName("MinZoom")
    var minZoom = defaultZoom

    @SerialName("MaxZoom")
    var maxZoom = defaultZoom

    @SerialName("Spread")
    var spread = 0.0

    @JvmField
    @SerialName("Damage")
    var damage = 0.0

    @SerialName("Headshot")
    var headshot = 1.5

    @JvmField
    @SerialName("Velocity")
    var velocity = 0.0

    @SerialName("Magazine")
    var magazine = 0

    @SerialName("Range")
    var range = 128

    @SerialName("MeleeDamage")
    var meleeDamage = 0.0

    @SerialName("MeleeDuration")
    var meleeDuration = 16

    @SerialName("MeleeDamageTime")
    var meleeDamageTime = 6

    @SerialName("MeleeAngle")
    var meleeAngle = 30

    @SerialName("MeleeRange")
    var meleeRange = 0.0

    @JvmField
    @ServerOnly
    @SerialName("Projectile")
    var projectile: StringToObject<ProjectileInfo> = StringToObject(ProjectileInfo())

    fun projectile(): ProjectileInfo {
        return projectile.value
    }

    @ServerOnly
    @SerialName("ShootPos")
    var shootPos = ShootPos()

    @SerialName("SeekWeaponInfo")
    var seekWeaponInfo: SeekWeaponInfo? = null

    @SerialName("AmmoCostPerShoot")
    var ammoCostPerShoot = 1

    @SerialName("ProjectileAmount")
    var projectileAmount = 1

    @SerialName("Weight")
    var weight = 1.0

    @SerialName("DefaultFireMode")
    var defaultFireMode: String = FireMode.SEMI.typeName

    @SerialName("AvailableFireModes")
    var availableFireModes = ObjectToList(StringToObject(FireModeInfo()))

    fun availableFireModes() = availableFireModes.list.map { it.value }

    @SerialName("ReloadTypes")
    var reloadTypes = setOf(ReloadType.MAGAZINE)

    @SerialName("SeekType")
    var seekType: SeekType? = SeekType.NONE

    @SerialName("GunType")
    var gunType = GunType.SPECIAL

    // Nullable!!!
    @SerialName("AutoReload")
    var autoReload: Boolean? = null

    @SerialName("WithdrawAmmoWhenChangeSlot")
    var withdrawAmmoWhenChangeSlot = false

    @SerialName("ZoomReload")
    var zoomReload = true

    @SerialName("ClearHoldProgressAfterShoot")
    var clearHoldProgressAfterShoot = false

    @SerialName("BurstAmount")
    var burstAmount = 0

    @SerialName("BypassesArmor")
    var bypassesArmor = 0.0

    @SerialName("AmmoType")
    var ammoConsumers: ObjectToList<StringToObject<AmmoConsumer>> = ObjectToList()

    @Transient
    @kotlinx.serialization.Transient
    private var ammoConsumersCache: List<AmmoConsumer>? = null

    fun getProcessedAmmoConsumers(): List<AmmoConsumer> {
        if (ammoConsumersCache == null) {
            this.ammoConsumersCache = this.ammoConsumers.list
                .map { c ->
                    if (!c.value.initialized()) {
                        c.value.init()
                    }
                    c.value
                }
                .filter { c ->
                    if (c.type == AmmoConsumer.AmmoConsumeType.INVALID) {
                        Mod.LOGGER.warn("invalid ammo string {} for {}", c.ammo, this.id)
                        return@filter false
                    }
                    true
                }
        }

        return this.ammoConsumersCache!!
    }

    @Transient
    @kotlinx.serialization.Transient
    private var fireModesCache: List<FireModeInfo>? = null

    val fireModes: List<FireModeInfo>
        get() {
            if (fireModesCache == null) {
                this.fireModesCache = this.availableFireModes.list
                    .map { c ->
                        c.value.init()
                        c.value
                    }
            }

            return this.fireModesCache!!
        }

    @SerialName("NormalReloadTime")
    var normalReloadTime = 0

    @SerialName("EmptyReloadTime")
    var emptyReloadTime = 0

    @SerialName("BoltActionTime")
    var boltActionTime = 0

    @SerialName("PrepareTime")
    var prepareTime = 0

    @SerialName("PrepareLoadTime")
    var prepareLoadTime = 0

    // 单发装填时的上弹时间
    @SerialName("PrepareAmmoLoadTime")
    var prepareAmmoLoadTime = 1

    @SerialName("PrepareEmptyTime")
    var prepareEmptyTime = 0

    // 每次单发装填用时的
    @SerialName("IterativeTime")
    var iterativeTime = 0

    // 单发装填时的上弹时间，在reload.iterativeLoadTimer等于该值时上弹
    @SerialName("IterativeAmmoLoadTime")
    var iterativeAmmoLoadTime = 1

    // 单次单发装填上弹数量
    @SerialName("IterativeLoadAmount")
    var iterativeLoadAmount = 1

    @SerialName("FinishTime")
    var finishTime = 0

    // 连发模式下的射击间隔时间
    @SerialName("BurstCooldown")
    var burstCooldown = 30

    @ServerOnly
    @SerialName("SoundRadius")
    var soundRadius = 0.0

    @SerialName("RPM")
    var rpm = 600

    @SerialName("ExplosionDamage")
    var explosionDamage = 0.0

    @SerialName("ExplosionRadius")
    var explosionRadius = 0.0

    @SerialName("Gravity")
    var gravity = 0.05

    @SerialName("ShootDelay")
    var shootDelay = 0

    @ServerOnly
    @SerialName("HeatPerShoot")
    var heatPerShoot = 0.0

    @SerialName("AvailablePerks")
    var availablePerks = ObjectToList(
        "@Ammo",
        "superbwarfare:field_doctor",
        "superbwarfare:powerful_attraction",
        "superbwarfare:intelligent_chip",
        "superbwarfare:monster_hunter",
        "superbwarfare:vorpal_weapon",
        "!superbwarfare:micro_missile",
        "!superbwarfare:longer_wire",
        "!superbwarfare:cupid_arrow"
    )

    fun availablePerks(): List<String> {
        return availablePerks.list
    }

    @ServerOnly
    @SerialName("DamageReduce")
    var damageReduce: DamageReduce = DamageReduce()

    // 自然情况下每tick减少的热量
    @ServerOnly
    @SerialName("NaturalCooldown")
    var naturalCooldown = 0.25

    // 在水中或雨中时的散热比例
    @ServerOnly
    @SerialName("InWaterCooldownRate")
    var inWaterCooldownRate = 1.1

    // 在细雪中时的散热比例
    @ServerOnly
    @SerialName("InSnowCooldownRate")
    var inSnowCooldownRate = 1.5

    // 在火焰中时的散热比例
    @ServerOnly
    @SerialName("InFireCooldownRate")
    var inFireCooldownRate = 0.6

    // 在岩浆中时的散热比例
    @ServerOnly
    @SerialName("InLavaCooldownRate")
    var inLavaCooldownRate = 0.2

    // 瞄准时的扩散比例
    @SerialName("ZoomSpreadRate")
    var zoomSpreadRate = 0.1

    @SerialName("SeekTime")
    var seekTime = 20

    @SerialName("SeekAngle")
    var seekAngle = 10.0

    @SerialName("SeekRange")
    var seekRange = 384.0

    @SerialName("MaxGuidedRange")
    var maxGuidedRange = 1024.0

    @SerialName("CanGuidedByRadar")
    var canGuidedByRadar = true

    @SerialName("AffectedByStealthTarget")
    var affectedByStealthTarget = true

    @SerialName("MinTargetHeight")
    var minTargetHeight = 0.0

    @SerialName("MaxTargetHeight")
    var maxTargetHeight = 114514.0

    @SerialName("SoundInfo")
    var soundInfo: SoundInfo = SoundInfo()

    @ServerOnly
    @SerialName("ShootAnimationTime")
    var shootAnimationTime = 0

    @ServerOnly
    @SerialName("SpreadAmount")
    var spreadAmount = 10

    @ServerOnly
    @SerialName("ApDurability")
    var apDurability = 50

    @ServerOnly
    @SerialName("SpreadAngle")
    var spreadAngle = 15

    @ServerOnly
    @SerialName("ShellType")
    var shellType: String = "Default"

    @ServerOnly
    @SerialName("ProjectileLife")
    var projectileLife = 400

    @SerialName("AddShooterDeltaMovement")
    var addShooterDeltaMovement = false

    @SerialName("Icon")
    var icon: SerializedResourceLocation = DEFAULT_ICON

    /*
     * 准星类型
     * 预制的字段有：
     * @Empty - 空
     * @Custom - 自定义
     * @GunDefault - 默认枪械准星
     * @VehicleDefault - 默认载具准星
     */
    @SerialName("Crosshair")
    var crosshair = "@GunDefault"

    // 瞄准时的准星，默认为空，仅用于部分载具
    @SerialName("CrosshairZooming")
    var crosshairZooming = "@Empty"

    @SerialName("CrosshairColor")
    var crosshairColor: ModColor = ModColor()

    @SerialName("Name")
    var name: String? = null

    override fun limit() {
        maxDurability = max(0, maxDurability)
        durabilityPerShoot = max(0, durabilityPerShoot)
        maxEnergy = max(0, maxEnergy)

        var temp = maxReceiveEnergy.coerceIn(-1, maxEnergy)
        maxReceiveEnergy = if (temp < 0) maxEnergy else temp

        temp = maxExtractEnergy.coerceIn(-1, maxEnergy)
        maxExtractEnergy = if (temp < 0) maxEnergy else temp

        meleeDuration = max(1, meleeDuration)

        meleeAngle = meleeAngle.coerceIn(1, 180)

        zoomSpreadRate = zoomSpreadRate.coerceIn(0.0, 1.0)
        range = max(1, range)

        meleeDamageTime = min(meleeDuration - 1, meleeDamageTime)

        ammoCostPerShoot = max(0, ammoCostPerShoot)
        projectileAmount = max(0, projectileAmount)
        weight = max(1.0, weight)

        magazine = if (projectileAmount == 0 && meleeDamage > 0) {
            0
        } else {
            max(0, magazine)
        }

        if (seekType == null) {
            seekType = SeekType.NONE
        }

        burstAmount = max(0, burstAmount)
        rpm = rpm.coerceIn(1, 114514)
    }

    companion object {
        val DEFAULT_ICON: ResourceLocation = loc("textures/gun_icon/default_icon.png")

    }
}
