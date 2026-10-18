package com.atsuishio.superbwarfare.physics.tracked

import java.util.concurrent.ConcurrentHashMap

/**
 * The single registration point for tracked vehicles.
 *
 * Registration says *what a vehicle is made of*. It never grants a vehicle its
 * own physics algorithm, and it never decides on its own that a vehicle is
 * ready to spawn: a definition stays `approved = false` until its compiled
 * mechanism has been audited.
 *
 * The registry is frozen once data loading finishes; adding entries afterwards
 * is a programming error rather than a silent change to live vehicles.
 */
object TrackedVehicleRegistry {

    /** Where a definition came from, for conflict reporting. */
    enum class Source { BUILT_IN, DATA_PACK, ADDON }

    data class Registration(
        val id: String,
        val definition: TrackedVehicleDefinition,
        val source: Source,
        /** True only after the mechanism audit for this exact asset and pitch was accepted. */
        val approved: Boolean = false,
        /** Free-form audit note copied into diagnostics and hand-over reports. */
        val auditNote: String = "",
    )

    private val registrations = LinkedHashMap<String, Registration>()
    private val compiled = ConcurrentHashMap<String, TrackedVehicleSpec>()
    private var frozen = false

    @Synchronized
    fun register(registration: Registration): Result<Unit> {
        if (frozen) return Result.failure(IllegalStateException("registry is frozen"))
        if (registration.id.isBlank()) return Result.failure(IllegalArgumentException("blank vehicle id"))
        val existing = registrations[registration.id]
        if (existing != null) {
            return Result.failure(
                IllegalStateException(
                    "duplicate tracked definition ${registration.id}: ${existing.source} vs ${registration.source}",
                ),
            )
        }
        registrations[registration.id] = registration
        compiled.clear()
        return Result.success(Unit)
    }

    @Synchronized
    fun freeze() {
        frozen = true
    }

    @Synchronized
    fun clear() {
        registrations.clear()
        compiled.clear()
        frozen = false
    }

    @Synchronized
    fun find(id: String): Registration? = registrations[id]

    @Synchronized
    fun all(): List<Registration> = registrations.values.toList()

    @Synchronized
    fun ids(): Set<String> = registrations.keys.toSet()

    @Synchronized
    fun isRegistered(id: String): Boolean = registrations.containsKey(id)

    /**
     * Compiles (and caches) one registered definition against the model text
     * that was actually loaded. A different asset hash produces a different
     * cache entry, so stale audits can never be reused after a model edit.
     */
    fun compile(id: String, modelJson: String): TrackedVehicleSpec? {
        val registration = find(id) ?: return null
        val hash = TrackedVehicleCompiler.sha256(modelJson)
        val key = "$id@$hash"
        compiled[key]?.let { return it }
        val spec = TrackedVehicleCompiler.compile(id, registration.definition, modelJson)
        compiled[key] = spec
        return spec
    }

    /** Last compiled spec for [id], regardless of which asset hash produced it. */
    fun lastCompiled(id: String): TrackedVehicleSpec? =
        compiled.entries.lastOrNull { it.key.startsWith("$id@") }?.value

    fun cachedCompilations(): Int = compiled.size
}
