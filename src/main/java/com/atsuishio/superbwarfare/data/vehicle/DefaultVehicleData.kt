package com.atsuishio.superbwarfare.data.vehicle

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.annotation.ServerOnly
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.data.*
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.vehicle.subdata.*
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModify
import com.atsuishio.superbwarfare.serialization.kserializer.*
import com.atsuishio.superbwarfare.tools.toKxJson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.common.ModConfigSpec
import java.util.*
import kotlin.math.max

@Suppress("unused")
@Serializable
class DefaultVehicleData : IDBasedData<DefaultVehicleData> {
    @Transient
    @kotlinx.serialization.Transient
    private var id = ""

    @JvmField
    @Transient
    @kotlinx.serialization.Transient
    var isDefaultData: Boolean = true

    override fun getId(): String {
        return this.id
    }

    override fun setId(id: String) {
        this.id = id
    }

    @SerialName("MaxHealth")
    var maxHealth: Float = 50f

    @ServerOnly
    @SerialName("RepairCooldown")
    var repairCooldown: Int = getConfigOrDefault(VehicleConfig.REPAIR_COOLDOWN)

    @ServerOnly
    @SerialName("RepairAmount")
    var repairAmount: Float = getConfigOrDefault(VehicleConfig.REPAIR_AMOUNT).toFloat()

    /**
     * 开始自动扣血时的血量比例
     */
    @ServerOnly
    @SerialName("SelfHurtPercent")
    var selfHurtPercent: Float = 0.1f

    /**
     * 自动扣血每tick扣血量
     */
    @ServerOnly
    @SerialName("SelfHurtAmount")
    var selfHurtAmount: Float = 0.1f

    @SerialName("MaxEnergy")
    var maxEnergy: Int = Int.MAX_VALUE

    @SerialName("OBB")
    var obb: MutableList<OBBInfo> = mutableListOf()

    @SerialName("Seats")
    private var seats: ObjectToList<SeatInfo>? = ObjectToList()

    fun seats(): MutableList<SeatInfo> {
        if (seats == null) return mutableListOf()
        return Collections.unmodifiableList(seats!!.list)
    }

    @SerialName("UpStep")
    var upStep: Float = 0f

    @JvmField
    @SerialName("TrackDistanceMultiply")
    var trackDistanceMultiply: Double = 1.0

    @SerialName("KeepChunkLoaded")
    var keepChunkLoaded: Boolean = true

    @SerialName("MouseSensitivity")
    var mouseSensitivity: Double = 0.4

    @SerialName("PassengerRenderScale")
    var passengerRenderScale: Float = 1f

    @SerialName("AllowFreeCam")
    var allowFreeCam: Boolean = false

    @SerialName("HasDecoy")
    var hasDecoy: Boolean = false

    @JvmField
    @ServerOnly
    @SerialName("ApplyDefaultDamageModifiers")
    var applyDefaultDamageModifiers: Boolean = true

    @ServerOnly
    @SerialName("SendHitParticles")
    var sendHitParticles: Boolean = true

    @JvmField
    @ServerOnly
    @SerialName("DamageModifiers")
    var damageModifiers: ObjectToList<StringToObject<DamageModify>> = ObjectToList()

    @ServerOnly
    @SerialName("Mass")
    var mass: Float = 1f

    @ServerOnly
    @SerialName("DestroyInfo")
    var destroyInfo: DestroyInfo = DestroyInfo()

    @SerialName("SeekInfo")
    var seekInfo: SeekInfo? = null

    @SerialName("VehicleContainerType")
    var vehicleContainerType: VehicleContainerType = VehicleContainerType.MEDIUM

    @SerialName("HasUpgradeSlots")
    var hasUpgradeSlots: Boolean = false

    @SerialName("VehicleIcon")
    var vehicleIcon: SerializedResourceLocation = loc("textures/gun_icon/default_icon.png")

    @SerialName("ContainerIcon")
    var containerIcon: SerializedResourceLocation? = null

    @SerialName("HUDColor")
    var hudColor: ModColor = ModColor(0x66FF00)

    @SerialName("Type")
    var type: VehicleType = VehicleType.EMPTY

    @SerialName("EngineType")
    var engineType: EngineType = EngineType.EMPTY

    @SerialName("EngineInfo")
    var engineInfo: SerializedGsonObject = JsonObject()

    // 引擎音效
    @SerialName("EngineSound")
    var engineSound: SerializedSoundEvent = SoundEvents.EMPTY

    // 喇叭音效
    @SerialName("HornSound")
    var hornSound: SerializedSoundEvent = SoundEvents.EMPTY

    // 第三人称视角
    @SerialName("ThirdPersonCameraPos")
    var thirdPersonCameraPos: SerializedVec3 = Vec3(0.0, 1.0, 3.0)

    @SerialName("HasLowHealthWarning")
    var hasLowHealthWarning: Boolean = true

    @SerialName("RotateOffsetHeight")
    var rotateOffsetHeight: Float = 0f

    @SerialName("Weapons")
    private var weapons: MutableMap<String, SerializedGsonObject> = mutableMapOf()

    @Transient
    @kotlinx.serialization.Transient
    private var processedWeapons: MutableMap<String, DefaultGunData>? = null

    fun weapons(): MutableMap<String, DefaultGunData> {
        if (processedWeapons != null) return processedWeapons!!

        val map = hashMapOf<String, DefaultGunData>()

        for (entry in weapons.entries) {
            var value = entry.value
            value = value.deepCopy()

            val primitive = value.get("Template")

            if (primitive is JsonPrimitive && primitive.isString) {
                value.remove("Template")
                val templateValue = weapons[primitive.getAsString()]
                if (templateValue != null) {
                    val newValue = templateValue.deepCopy()
                    for (kv in value.entrySet()) {
                        newValue.add(kv.key, kv.value)
                    }
                    value = newValue
                }
            }

            map[entry.key] =
                DataLoader.JSON.decodeFromJsonElement(
                    DefaultGunData.serializer(),
                    value.asJsonObject.toKxJson()
                )
        }

        processedWeapons = Collections.unmodifiableMap(map)
        return processedWeapons!!
    }

    /**
     * 碰撞等级，范围是0~4
     * 0 - 无法撞坏方块
     * 1 - 允许撞坏软方块
     * 2 - 允许撞坏普通方块
     * 3 - 允许撞坏硬方块
     * 4 - 允许野兽撞击模式
     */
    @SerialName("CollisionLevel")
    var collisionLevel: CollisionLevel = CollisionLevel()

    // 主武器位
    @SerialName("TurretPos")
    var turretPos: SerializedVec3? = null

    @SerialName("TurretTurnSpeed")
    var turretTurnSpeed: SerializedVec2 = Vec2(5f, 5f)

    @SerialName("TurretYawRange")
    var turretYawRange: SerializedVec2 = Vec2(-514f, 514f)

    @SerialName("TurretPitchRange")
    var turretPitchRange: SerializedVec2 = Vec2(-10f, 30f)

    @SerialName("TurretControllerIndex")
    var turretControllerIndex: Int = 0

    @SerialName("TurretCustomPitch")
    var turretCustomPitch: Float = 0f

    @SerialName("HudType")
    var hudType: String = "@Empty"

    @SerialName("BarrelPos")
    var barrelPos: SerializedVec3 = Vec3.ZERO

    // 乘客位武器
    @SerialName("PassengerWeaponStationPos")
    var passengerWeaponStationPos: SerializedVec3? = null

    @SerialName("PassengerWeaponStationBarrelPos")
    var passengerWeaponStationBarrelPos: SerializedVec3 = Vec3.ZERO

    @SerialName("PassengerWeaponStationTurnSpeed")
    var passengerWeaponStationTurnSpeed: SerializedVec2 = Vec2(5f, 5f)

    @SerialName("PassengerWeaponStationYawRange")
    var passengerWeaponStationYawRange: SerializedVec2 = Vec2(-514f, 514f)

    @SerialName("PassengerWeaponStationPitchRange")
    var passengerWeaponStationPitchRange: SerializedVec2 = Vec2(-10f, 30f)

    @SerialName("PassengerWeaponStationControllerIndex")
    var passengerWeaponStationControllerIndex: Int = 1

    @JvmField
    @SerialName("UsePassengerCreativeAmmoBox")
    var usePassengerCreativeAmmoBox: Boolean = true

    @SerialName("Gravity")
    var gravity: Double = 0.06

    @SerialName("TerrainCompat")
    var terrainCompat: MutableList<SerializedVec3> = mutableListOf()

    @SerialName("TerrainCompatRotateRate")
    var terrainCompatRotateRate: Float = 1f

    // 受惯性影响的旋转幅度
    @SerialName("InertiaRotateRate")
    var inertiaRotateRate: Float = 0f

    override fun limit() {
        this.maxHealth = max(this.maxHealth, 0f)
        this.repairCooldown = max(this.repairCooldown, 0)
        this.maxEnergy = max(this.maxEnergy, 0)
        this.obb = this.obb.map {
            it.limit()
            it
        }.toMutableList()

        this.collisionLevel.level = this.collisionLevel.level.coerceIn(0, 4)
    }

    companion object {
        private fun <T> getConfigOrDefault(config: ModConfigSpec.ConfigValue<T>): T {
            return try {
                config.get()
            } catch (exception: Exception) {
                config.getDefault()
            }
        }
    }
}
