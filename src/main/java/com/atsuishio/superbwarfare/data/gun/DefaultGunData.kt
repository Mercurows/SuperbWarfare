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
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.util.Mth
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
    @SerializedName("MaxDurability")
    @SerialName("MaxDurability")
    var maxDurability = 0

    @ServerOnly
    @SerializedName("DurabilityPerShoot")
    @SerialName("DurabilityPerShoot")
    var durabilityPerShoot = 1

    @SerializedName("MaxEnergy")
    @SerialName("MaxEnergy")
    var maxEnergy = 0

    @ServerOnly
    @SerializedName("MaxReceiveEnergy")
    @SerialName("MaxReceiveEnergy")
    var maxReceiveEnergy = -1

    @ServerOnly
    @SerializedName("MaxExtractEnergy")
    @SerialName("MaxExtractEnergy")
    var maxExtractEnergy = -1

    @SerializedName("RecoilX")
    @SerialName("RecoilX")
    var recoilX = 0.0

    @SerializedName("RecoilY")
    @SerialName("RecoilY")
    var recoilY = 0.0

    @SerializedName("Recoil")
    @SerialName("Recoil")
    var recoil = 0.0

    @SerializedName("RecoilTime")
    @SerialName("RecoilTime")
    var recoilTime = 0

    @SerializedName("RecoilForce")
    @SerialName("RecoilForce")
    var recoilForce = 0f

    // x:范围，y：振动时长，z：振幅
    @ServerOnly
    @SerializedName("ShootShake")
    @SerialName("ShootShake")
    var shootShake: SerializedVec3? = null

    @SerializedName("DefaultZoom")
    @SerialName("DefaultZoom")
    var defaultZoom = 1.25

    @SerializedName("MinZoom")
    @SerialName("MinZoom")
    var minZoom = defaultZoom

    @SerializedName("MaxZoom")
    @SerialName("MaxZoom")
    var maxZoom = defaultZoom

    @SerializedName("Spread")
    @SerialName("Spread")
    var spread = 0.0

    @JvmField
    @SerializedName("Damage")
    @SerialName("Damage")
    var damage = 0.0

    @SerializedName("Headshot")
    @SerialName("Headshot")
    var headshot = 1.5

    @JvmField
    @SerializedName("Velocity")
    @SerialName("Velocity")
    var velocity = 0.0

    @SerializedName("Magazine")
    @SerialName("Magazine")
    var magazine = 0

    @SerializedName("Range")
    @SerialName("Range")
    var range = 128

    @SerializedName("MeleeDamage")
    @SerialName("MeleeDamage")
    var meleeDamage = 0.0

    @SerializedName("MeleeDuration")
    @SerialName("MeleeDuration")
    var meleeDuration = 16

    @SerializedName("MeleeDamageTime")
    @SerialName("MeleeDamageTime")
    var meleeDamageTime = 6

    @SerializedName("MeleeAngle")
    @SerialName("MeleeAngle")
    var meleeAngle = 30

    @SerializedName("MeleeRange")
    @SerialName("MeleeRange")
    var meleeRange = 0.0

    @JvmField
    @ServerOnly
    @SerializedName("Projectile")
    @SerialName("Projectile")
    var projectile: StringToObject<ProjectileInfo> = StringToObject<ProjectileInfo>(ProjectileInfo())

    fun projectile(): ProjectileInfo {
        return projectile.value
    }

    @ServerOnly
    @SerializedName("ShootPos")
    @SerialName("ShootPos")
    var shootPos = ShootPos()

    @SerializedName("SeekWeaponInfo")
    @SerialName("SeekWeaponInfo")
    var seekWeaponInfo: SeekWeaponInfo? = null

    @SerializedName("AmmoCostPerShoot")
    @SerialName("AmmoCostPerShoot")
    var ammoCostPerShoot = 1

    @SerializedName("ProjectileAmount")
    @SerialName("ProjectileAmount")
    var projectileAmount = 1

    @SerializedName("Weight")
    @SerialName("Weight")
    var weight = 1.0

    @SerializedName("DefaultFireMode")
    @SerialName("DefaultFireMode")
    var defaultFireMode: String = FireMode.SEMI.typeName

    @SerializedName("AvailableFireModes")
    @SerialName("AvailableFireModes")
    var availableFireModes = ObjectToList(StringToObject(FireModeInfo()))

    fun availableFireModes() = availableFireModes.list.map { it.value }

    @SerializedName("ReloadTypes")
    @SerialName("ReloadTypes")
    var reloadTypes = setOf(ReloadType.MAGAZINE)

    @SerializedName("SeekType")
    @SerialName("SeekType")
    var seekType: SeekType? = SeekType.NONE

    @SerializedName("GunType")
    @SerialName("GunType")
    var gunType = GunType.SPECIAL

    // Nullable!!!
    @SerializedName("AutoReload")
    @SerialName("AutoReload")
    var autoReload: Boolean? = null

    @SerializedName("WithdrawAmmoWhenChangeSlot")
    @SerialName("WithdrawAmmoWhenChangeSlot")
    var withdrawAmmoWhenChangeSlot = false

    @SerializedName("ZoomReload")
    @SerialName("ZoomReload")
    var zoomReload = true

    @SerializedName("ClearHoldProgressAfterShoot")
    @SerialName("ClearHoldProgressAfterShoot")
    var clearHoldProgressAfterShoot = false

    @SerializedName("BurstAmount")
    @SerialName("BurstAmount")
    var burstAmount = 0

    @SerializedName("BypassesArmor")
    @SerialName("BypassesArmor")
    var bypassesArmor = 0.0

    @SerializedName("AmmoType")
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

    @SerializedName("NormalReloadTime")
    @SerialName("NormalReloadTime")
    var normalReloadTime = 0

    @SerializedName("EmptyReloadTime")
    @SerialName("EmptyReloadTime")
    var emptyReloadTime = 0

    @SerializedName("BoltActionTime")
    @SerialName("BoltActionTime")
    var boltActionTime = 0

    @SerializedName("PrepareTime")
    @SerialName("PrepareTime")
    var prepareTime = 0

    @SerializedName("PrepareLoadTime")
    @SerialName("PrepareLoadTime")
    var prepareLoadTime = 0

    // 单发装填时的上弹时间
    @SerializedName("PrepareAmmoLoadTime")
    @SerialName("PrepareAmmoLoadTime")
    var prepareAmmoLoadTime = 1

    @SerializedName("PrepareEmptyTime")
    @SerialName("PrepareEmptyTime")
    var prepareEmptyTime = 0

    // 每次单发装填用时的
    @SerializedName("IterativeTime")
    @SerialName("IterativeTime")
    var iterativeTime = 0

    // 单发装填时的上弹时间，在reload.iterativeLoadTimer等于该值时上弹
    @SerializedName("IterativeAmmoLoadTime")
    @SerialName("IterativeAmmoLoadTime")
    var iterativeAmmoLoadTime = 1

    // 单次单发装填上弹数量
    @SerializedName("IterativeLoadAmount")
    @SerialName("IterativeLoadAmount")
    var iterativeLoadAmount = 1

    @SerializedName("FinishTime")
    @SerialName("FinishTime")
    var finishTime = 0

    // 连发模式下的射击间隔时间
    @SerializedName("BurstCooldown")
    @SerialName("BurstCooldown")
    var burstCooldown = 30

    @ServerOnly
    @SerializedName("SoundRadius")
    @SerialName("SoundRadius")
    var soundRadius = 0.0

    @SerializedName("RPM")
    @SerialName("RPM")
    var rpm = 600

    @SerializedName("ExplosionDamage")
    @SerialName("ExplosionDamage")
    var explosionDamage = 0.0

    @SerializedName("ExplosionRadius")
    @SerialName("ExplosionRadius")
    var explosionRadius = 0.0

    @SerializedName("Gravity")
    @SerialName("Gravity")
    var gravity = 0.05

    @SerializedName("ShootDelay")
    @SerialName("ShootDelay")
    var shootDelay = 0

    @ServerOnly
    @SerializedName("HeatPerShoot")
    @SerialName("HeatPerShoot")
    var heatPerShoot = 0.0

    @SerializedName("AvailablePerks")
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
    @SerializedName("DamageReduce")
    @SerialName("DamageReduce")
    var damageReduce: DamageReduce = DamageReduce()

    // 自然情况下每tick减少的热量
    @ServerOnly
    @SerializedName("NaturalCooldown")
    @SerialName("NaturalCooldown")
    var naturalCooldown = 0.25

    // 在水中或雨中时的散热比例
    @ServerOnly
    @SerializedName("InWaterCooldownRate")
    @SerialName("InWaterCooldownRate")
    var inWaterCooldownRate = 1.1

    // 在细雪中时的散热比例
    @ServerOnly
    @SerializedName("InSnowCooldownRate")
    @SerialName("InSnowCooldownRate")
    var inSnowCooldownRate = 1.5

    // 在火焰中时的散热比例
    @ServerOnly
    @SerializedName("InFireCooldownRate")
    @SerialName("InFireCooldownRate")
    var inFireCooldownRate = 0.6

    // 在岩浆中时的散热比例
    @ServerOnly
    @SerializedName("InLavaCooldownRate")
    @SerialName("InLavaCooldownRate")
    var inLavaCooldownRate = 0.2

    // 瞄准时的扩散比例
    @SerializedName("ZoomSpreadRate")
    @SerialName("ZoomSpreadRate")
    var zoomSpreadRate = 0.1

    @SerializedName("SeekTime")
    @SerialName("SeekTime")
    var seekTime = 20

    @SerializedName("SeekAngle")
    @SerialName("SeekAngle")
    var seekAngle = 10.0

    @SerializedName("SeekRange")
    @SerialName("SeekRange")
    var seekRange = 384.0

    @SerializedName("MaxGuidedRange")
    @SerialName("MaxGuidedRange")
    var maxGuidedRange = 1024.0

    @SerializedName("CanGuidedByRadar")
    @SerialName("CanGuidedByRadar")
    var canGuidedByRadar = true

    @SerializedName("AffectedByStealthTarget")
    @SerialName("AffectedByStealthTarget")
    var affectedByStealthTarget = true

    @SerializedName("MinTargetHeight")
    @SerialName("MinTargetHeight")
    var minTargetHeight = 0.0

    @SerializedName("MaxTargetHeight")
    @SerialName("MaxTargetHeight")
    var maxTargetHeight = 114514.0

    @SerializedName("SoundInfo")
    @SerialName("SoundInfo")
    var soundInfo: SoundInfo = SoundInfo()

    @ServerOnly
    @SerializedName("ShootAnimationTime")
    @SerialName("ShootAnimationTime")
    var shootAnimationTime = 0

    @ServerOnly
    @SerializedName("SpreadAmount")
    @SerialName("SpreadAmount")
    var spreadAmount = 10

    @ServerOnly
    @SerializedName("ApDurability")
    @SerialName("ApDurability")
    var apDurability = 50

    @ServerOnly
    @SerializedName("SpreadAngle")
    @SerialName("SpreadAngle")
    var spreadAngle = 15

    @ServerOnly
    @SerializedName("ShellType")
    @SerialName("ShellType")
    var shellType: String = "Default"

    @ServerOnly
    @SerializedName("ProjectileLife")
    @SerialName("ProjectileLife")
    var projectileLife = 400

    @SerializedName("AddShooterDeltaMovement")
    @SerialName("AddShooterDeltaMovement")
    var addShooterDeltaMovement = false

    @SerializedName("Icon")
    @SerialName("Icon")
    var icon: SerializedResourceLocation = loc("textures/gun_icon/default_icon.png")

    /*
     * 准星类型
     * 预制的字段有：
     * @Empty - 空
     * @Custom - 自定义
     * @GunDefault - 默认枪械准星
     * @VehicleDefault - 默认载具准星
     */
    @SerializedName("Crosshair")
    @SerialName("Crosshair")
    var crosshair = "@GunDefault"

    // 瞄准时的准星，默认为空，仅用于部分载具
    @SerializedName("CrosshairZooming")
    @SerialName("CrosshairZooming")
    var crosshairZooming = "@Empty"

    @SerializedName("CrosshairColor")
    @SerialName("CrosshairColor")
    var crosshairColor: ModColor = ModColor()

    @SerializedName("Name")
    @SerialName("Name")
    var name: String? = null

    override fun limit() {
        maxDurability = max(0, maxDurability)
        durabilityPerShoot = max(0, durabilityPerShoot)
        maxEnergy = max(0, maxEnergy)

        var temp = Mth.clamp(maxReceiveEnergy, -1, maxEnergy)
        maxReceiveEnergy = if (temp < 0) maxEnergy else temp

        temp = Mth.clamp(maxExtractEnergy, -1, maxEnergy)
        maxExtractEnergy = if (temp < 0) maxEnergy else temp

        meleeDuration = max(1, meleeDuration)

        meleeAngle = Mth.clamp(meleeAngle, 1, 180)

        zoomSpreadRate = Mth.clamp(zoomSpreadRate, 0.0, 1.0)
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
        rpm = Mth.clamp(rpm, 1, 114514)
    }
}
