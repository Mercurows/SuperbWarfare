@file:OptIn(ExperimentalSerializationApi::class)

package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.serialization.decodeFromCompoundTag
import com.atsuishio.superbwarfare.serialization.encodeToCompoundTag
import com.atsuishio.superbwarfare.serialization.structured.StructuredUUID
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.nbt.CompoundTag

/**
 * Snapshot of one gun's persisted state
 */
@Serializable
data class GunState(
    // ---- identity / baseline ----
    @SerialName("UUID")
    val uuid: StructuredUUID? = null,
    @SerialName("DefaultData")
    val defaultDataId: String = "",

    /**
     * Monotonic revision of the persisted state, advanced by `GunData.persist` whenever the content
     * actually changes.
     */
    @SerialName("Revision")
    val revision: Long = 0,

    // ---- structural: changing these can change computed gun properties ----
    @SerialName("Override")
    val override: String = "",

    @SerialName("SelectedAmmoType")
    val selectedAmmoType: Int = 0,

    /** `null` means the key is absent, i.e. "use the owning item's default fire mode". */
    @SerialName("SelectedFireMode")
    val selectedFireMode: Int? = null,

    @SerialName("Level")
    val level: Int = 0,

    @SerialName("Zooming")
    val zooming: Boolean = false,

    // ---- runtime state ----
    @SerialName("Ammo")
    val ammo: Int = 0,
    @SerialName("VirtualAmmo")
    val virtualAmmo: Int = 0,
    @SerialName("BackupAmmoCount")
    val backupAmmoCount: Int = 0,
    @SerialName("FireIndex")
    val fireIndex: Int = 0,
    @SerialName("BurstAmount")
    val burstAmount: Int = 0,
    @SerialName("Exp")
    val exp: Double = 0.0,
    @SerialName("IsEmpty")
    val isEmpty: Boolean = false,
    @SerialName("CloseHammer")
    val closeHammer: Boolean = false,
    @SerialName("CloseStrike")
    val closeStrike: Boolean = false,
    @SerialName("Stopped")
    val stopped: Boolean = false,
    @SerialName("ForceStop")
    val forceStop: Boolean = false,
    @SerialName("LoadIndex")
    val loadIndex: Int = 0,
    @SerialName("HoldOpen")
    val holdOpen: Boolean = false,
    @SerialName("HideBulletChain")
    val hideBulletChain: Boolean = true,
    @SerialName("Sensitivity")
    val sensitivity: Int = 0,
    @SerialName("Heat")
    val heat: Double = 0.0,
    @SerialName("ShootAnimationTimer")
    val shootAnimationTimer: Int = 0,
    @SerialName("ShootTimer")
    val shootTimer: Int = 0,
    @SerialName("OverHeat")
    val overHeat: Boolean = false,
    @SerialName("weaponPitch")
    val weaponPitch: Double = 0.0,
    @SerialName("weaponYaw")
    val weaponYaw: Double = 0.0,

    // ---- reload / bolt / charge ----
    @SerialName("ReloadState")
    val reloadState: Int = 0,
    @SerialName("ReloadStage")
    val reloadStage: Int = 0,
    @SerialName("ReloadTime")
    val reloadTime: Int = 0,
    @SerialName("ReloadTotalTime")
    val reloadTotalTime: Int = 0,
    @SerialName("PrepareTime")
    val prepareTime: Int = 0,
    @SerialName("PrepareLoadTime")
    val prepareLoadTime: Int = 0,
    @SerialName("IterativeLoadTime")
    val iterativeLoadTime: Int = 0,
    @SerialName("FinishTime")
    val finishTime: Int = 0,
    @SerialName("ReloadFinishTotalTime")
    val reloadFinishTotalTime: Int = 0,
    @SerialName("StartReload")
    val startReload: Boolean = false,
    @SerialName("StartSingleReload")
    val startSingleReload: Boolean = false,
    @SerialName("StartStage3Forcefully")
    val startStage3Forcefully: Boolean = false,
    @SerialName("NeedBoltAction")
    val needBoltAction: Boolean = false,
    @SerialName("BoltActionTimeTime")
    val boltActionTime: Int = 0,
    @SerialName("BoltActionTotalTime")
    val boltActionTotalTime: Int = 0,
    @SerialName("ChargeTime")
    val chargeTime: Int = 0,
    @SerialName("StartCharge")
    val startCharge: Boolean = false,

    // ---- 主/副武器切换----
    /**
     * 当前操控的是哪一把枪：空 = 主武器，否则是副武器所在的 `AttachmentType` 枚举名（如 `"SUBWEAPON"`）
     */
    @SerialName("ActiveSlot")
    val activeSlot: String = "",

    /**
     * [activeSlot] 指向的那把副武器**所属宿主枪**的 UUID
     */
    @SerialName("ActiveOwner")
    val activeOwner: String = "",
) {
    data class Structural(
        val override: String,
        val defaultDataId: String,
        val selectedAmmoType: Int,
        val selectedFireMode: Int?,
        val level: Int,
        val ammo: Int,
        val virtualAmmo: Int,
        val zooming: Boolean,
    )

    /** Read-only view of the fields that can invalidate the PMC cache; see [Structural]. */
    val structural: Structural
        get() = Structural(
            override, defaultDataId, selectedAmmoType, selectedFireMode, level, ammo, virtualAmmo, zooming
        )

    /**
     * Whether going from [other] to this state can change computed gun properties, i.e. whether the PMC
     * cache has to be rebuilt. [equals] only answers "did anything change at all".
     */
    fun structurallyDiffersFrom(other: GunState) = structural != other.structural

    /**
     * Writes this state into [tag], which is expected to be the live `GunData` sub-compound that also
     * carries the still tag-backed sections.
     */
    fun writeInto(tag: CompoundTag) {
        val encoded = encodeToCompoundTag(serializer(), this, encodeDefaults = false)

        locked(tag) {
            for (key in SERIALIZED_KEYS) {
                val value = encoded.get(key)
                if (value == null) {
                    tag.remove(key)
                } else if (tag.get(key) != value) {
                    tag.put(key, value)
                }
            }
        }
    }

    companion object {
        /** A fresh gun's state, all keys absent. */
        @JvmField
        val EMPTY: GunState = GunState()

        /** Identity key inside the gun tag, written by `GunItem.init` through [GunData.update]. */
        const val KEY_UUID: String = "UUID"

        /** [defaultDataId] key inside the gun tag; also written by pre-construction stack stamping. */
        const val KEY_DEFAULT_DATA: String = "DefaultData"

        /** [revision] key inside the gun tag. */
        const val KEY_REVISION: String = "Revision"

        /** [GunData]'s root key inside the item's custom data. */
        const val KEY_GUN_DATA: String = "GunData"

        /**
         * Whether [candidate] is a newer revision than [current].
         */
        @JvmStatic
        fun isNewerRevision(candidate: Long, current: Long): Boolean =
            candidate != current && candidate - current > 0

        /**
         * Serial names of every modelled field, taken from the serializer descriptor so they cannot drift
         * from the [SerialName] annotations above.
         */
        private val SERIALIZED_KEYS: Array<String> = serializer().descriptor.let { descriptor ->
            Array(descriptor.elementsCount) { descriptor.getElementName(it) }
        }

        /**
         * Runs [block] while holding the lock that guards the state entries of [tag].
         */
        fun <T> locked(tag: CompoundTag, block: () -> T): T = synchronized(tag, block)

        /** Reads a state out of its serialized form; missing keys fall back to the field defaults. */
        @JvmStatic
        fun fromTag(tag: CompoundTag): GunState = locked(tag) { decodeFromCompoundTag(serializer(), tag) }
    }
}
