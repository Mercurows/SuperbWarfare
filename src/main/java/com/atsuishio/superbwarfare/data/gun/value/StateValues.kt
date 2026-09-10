package com.atsuishio.superbwarfare.data.gun.value

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunState
import net.minecraft.nbt.CompoundTag

/**
 * Value wrappers backed by the immutable [GunState] instead of a [CompoundTag].
 *
 * They exist for compatibility: [GunData] keeps its `@JvmField` fields (same names, same types) so both
 * existing source and already-compiled callers keep working, while the actual storage moved to
 * [GunState]. Reads go through the state snapshot and writes go through [GunData.update], which persists
 * automatically — so a caller can no longer forget to `save()`.
 *
 * The backing tag of the sureperclass is a shared dummy: every accessor that matters is overridden here.
 */
private val DUMMY_TAG: CompoundTag = CompoundTag()

open class StateIntValue(
    private val owner: GunData,
    private val reader: (GunState) -> Int,
    private val writer: (GunState, Int) -> GunState,
    defaultValue: Int = 0,
) : IntValue(DUMMY_TAG, "", defaultValue) {

    override fun get(): Int = reader(owner.state)

    override fun set(value: Int) {
        if (reader(owner.state) == value) return
        owner.update { writer(it, value) }
    }
}

/**
 * Variant for optional keys: an absent entry means "fall back to [defaultValue]".
 *
 * Used by `SelectedFireMode`, where an absent key means "use the owning item's default fire mode"
 * (the index is computed by [GunData] once its properties are available).
 */
open class StateDefaultedIntValue(
    private val owner: GunData,
    private val reader: (GunState) -> Int?,
    private val writer: (GunState, Int) -> GunState,
    defaultValue: Int = 0,
) : IntValue(DUMMY_TAG, "", defaultValue) {

    override fun get(): Int = reader(owner.state) ?: defaultValue

    override fun set(value: Int) {
        if (get() == value) return
        owner.update { writer(it, value) }
    }
}

open class StateDoubleValue(
    private val owner: GunData,
    private val reader: (GunState) -> Double,
    private val writer: (GunState, Double) -> GunState,
    defaultValue: Double = 0.0,
) : DoubleValue(DUMMY_TAG, "", defaultValue) {

    override fun get(): Double = reader(owner.state)

    override fun set(value: Double) {
        if (reader(owner.state) == value) return
        owner.update { writer(it, value) }
    }
}

open class StateBooleanValue(
    private val owner: GunData,
    private val reader: (GunState) -> Boolean,
    private val writer: (GunState, Boolean) -> GunState,
    defaultValue: Boolean = false,
) : BooleanValue(DUMMY_TAG, "", defaultValue) {

    override fun get(): Boolean = reader(owner.state)

    override fun set(value: Boolean) {
        if (reader(owner.state) == value) return
        owner.update { writer(it, value) }
    }
}

open class StateStringValue(
    private val owner: GunData,
    private val reader: (GunState) -> String,
    private val writer: (GunState, String) -> GunState,
    defaultValue: String = "",
) : StringValue(DUMMY_TAG, "", defaultValue) {

    override fun get(): String = reader(owner.state)

    override fun set(value: String) {
        if (reader(owner.state) == value) return
        owner.update { writer(it, value) }
    }
}

/**
 * [Timer] over [GunState]: the "value <= 0 means reset" rule of the old tag-backed timer is kept here
 * (`<= 0` maps onto the field's default, so the key is omitted exactly like `tag.remove(name)` did).
 */
open class StateTimer(
    private val owner: GunData,
    private val reader: (GunState) -> Int,
    private val writer: (GunState, Int) -> GunState,
    name: String,
) : Timer(DUMMY_TAG, name) {

    override fun get(): Int = reader(owner.state)

    override fun set(time: Int) {
        val value = if (time <= 0) 0 else time
        if (reader(owner.state) == value) return
        owner.update { writer(it, value) }
    }
}

/** [Starter] over [GunState]. */
open class StateStarter(
    private val owner: GunData,
    private val reader: (GunState) -> Boolean,
    private val writer: (GunState, Boolean) -> GunState,
    name: String,
) : Starter(DUMMY_TAG, name) {

    override fun shouldStart(): Boolean = reader(owner.state)

    override fun markStart() {
        if (reader(owner.state)) return
        owner.update { writer(it, true) }
    }

    override fun finish() {
        if (!reader(owner.state)) return
        owner.update { writer(it, false) }
    }
}
