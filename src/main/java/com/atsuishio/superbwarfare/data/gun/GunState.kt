@file:OptIn(ExperimentalSerializationApi::class)

package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.gun.GunState.Companion.isNewerRevision
import com.atsuishio.superbwarfare.serialization.decodeFromCompoundTag
import com.atsuishio.superbwarfare.serialization.encodeToCompoundTag
import com.atsuishio.superbwarfare.serialization.structured.StructuredUUID
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.nbt.CompoundTag

/**
 * Snapshot of one gun's persisted state.
 *
 * This is the single source of truth for every field [GunData] declares directly. [GunData] exposes it
 * through [GunData.state] for reads and [GunData.update] for writes; the value wrappers
 * (`IntValue`/`BooleanValue`/...) that older code still uses are thin proxies over it, and a write
 * therefore reaches the gun stack immediately.
 *
 * Declared immutable even though 1.20 keeps its data in a mutable NBT tag: [GunData.update] applies a
 * change by producing the next value and writing it into that tag, which is what keeps a change
 * atomic (never observed half-applied) and lets [GunData] tell which fields actually changed.
 *
 * Serialization goes through the project's NBT kotlinx format ([encodeToCompoundTag] /
 * [decodeFromCompoundTag]) with `encodeDefaults = false`, so a field equal to its default is left out of
 * the tag exactly like the old `value == defaultValue -> remove(key)` idiom did. Key names come from
 * [SerialName] and are a persistence/wire format — they must not be renamed. Unknown keys in the tag
 * (ammo slots, `CustomRPM`, ...) are ignored by the decoder, which is what lets this model cover only
 * part of the sub-compound.
 *
 * [uuid] uses [StructuredUUID], which stores the native NBT UUID (`IntArrayTag`) — the very form
 * `CompoundTag.putUUID` writes and older stacks already contain.
 *
 * Still tag-backed (owned by sub-data handlers, not modelled here): the ammo-slot table, the perk and
 * attachment sub-compounds, and loose keys such as `CustomRPM`.
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
     *
     * A `Long` on purpose: ordering is what [isNewerRevision] relies on, so the counter must have room.
     * It is also read as a `Long` from tags written earlier as an `IntTag`, since NBT numeric tags
     * widen on read.
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
    val hideBulletChain: Boolean = false,
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
    @SerialName("Zooming")
    val zooming: Boolean = false,
    @SerialName("weaponPitch")
    val weaponPitch: Double = 0.0,
    @SerialName("weaponYaw")
    val weaponYaw: Double = 0.0,

    // ---- reload / bolt / charge ----
    // Kept flat (not nested in Kotlin) because the tag layout is flat: these keys sit directly in the
    // `GunData` sub-compound. Names match the tag-backed implementations they replace, including the
    // doubled "Time" that `Timer(tag, "BoltActionTime")` produced and the "Start" prefix of `Starter`.
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
) {

    /**
     * The subset of this state that computed gun properties depend on.
     *
     * Declared as a data class so the comparison below is generated from its fields: adding a field here
     * is enough, it cannot be forgotten in a hand-written `||` chain.
     *
     * Conservative on purpose — a field belongs here whenever any `modifyProperty` implementation can
     * observe it. That is why `ammo`/`virtualAmmo` are included even though they are "runtime state":
     * `HighImpactReserves` (`Perk` and its JS variant) reads ammo while computing properties.
     */
    data class Structural(
        val override: String,
        val defaultDataId: String,
        val selectedAmmoType: Int,
        val selectedFireMode: Int?,
        val level: Int,
        val ammo: Int,
        val virtualAmmo: Int,
    )

    /** Read-only view of the fields that can invalidate the PMC cache; see [Structural]. */
    val structural: Structural
        get() = Structural(override, defaultDataId, selectedAmmoType, selectedFireMode, level, ammo, virtualAmmo)

    /**
     * Whether going from [other] to this state can change computed gun properties, i.e. whether the PMC
     * cache has to be rebuilt. [equals] only answers "did anything change at all".
     */
    fun structurallyDiffersFrom(other: GunState) = structural != other.structural

    /**
     * Writes this state into [tag], which is expected to be the live `GunData` sub-compound that also
     * carries the still tag-backed sections.
     *
     * Own keys are dropped first so that a field returning to its default actually disappears, then the
     * encoded state is merged in — the tag instance itself must stay alive because sub-data handlers
     * hold on to it, and it is the tag the owning stack serializes, so this write is the save.
     */
    fun writeInto(tag: CompoundTag) {
        SERIALIZED_KEYS.forEach {
            tag.remove(it)
        }

        val encoded = encodeToCompoundTag(serializer(), this, encodeDefaults = false)
        for (key in encoded.allKeys) {
            encoded.get(key)?.let { tag.put(key, it) }
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
         *
         * Uses serial-number arithmetic (`RFC 1982`-style signed distance) instead of `candidate >
         * current`, so the answer stays correct when the counter wraps: `MIN - MAX` wraps to `1`, which
         * is still "newer", while a genuinely older snapshot has a negative distance. A wrong answer is
         * harmless anyway — adoption is a pure optimization, so the worst case is one extra [GunData]
         * construction, never wrong data.
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

        /** Reads a state out of its serialized form; missing keys fall back to the field defaults. */
        @JvmStatic
        fun fromTag(tag: CompoundTag): GunState = decodeFromCompoundTag(serializer(), tag)
    }
}
