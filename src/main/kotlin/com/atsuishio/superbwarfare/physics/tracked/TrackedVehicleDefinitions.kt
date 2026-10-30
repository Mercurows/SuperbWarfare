package com.atsuishio.superbwarfare.physics.tracked

import com.atsuishio.superbwarfare.Mod
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads and caches the compiled tracked definition for one vehicle id.
 *
 * S2 needs the compiled mechanism on the server, at spawn time, without a test
 * fixture. The definition JSON and the Bedrock model are mod resources, so they
 * are read from the classpath exactly like the GameTest gate does; there is no
 * second copy of the geometry anywhere.
 *
 * Compilation is pure data (no Bullet, no Level), so the result can be cached
 * across entities. A failed compile is cached too: retrying on every spawn would
 * spam the log for a permanently broken definition, and the caller still gets a
 * clear error the first time.
 */
object TrackedVehicleDefinitions {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cache = ConcurrentHashMap<String, Result>()

    /** Compiled spec, or the compile failure for this id. */
    sealed interface Result {
        data class Loaded(val spec: TrackedVehicleSpec) : Result
        data class Failed(val reason: String) : Result
    }

    /** Resource path of the definition for a registry id, e.g. `ztz_99a`. */
    fun definitionPath(vehicleId: String): String {
        val path = vehicleId.substringAfter(':')
        return "data/superbwarfare/sbw/tracked/$path.json"
    }

    /**
     * Classpath path of the Bedrock model referenced by a definition.
     *
     * Authored ids are `superbwarfare:models/...`. The file lives under
     * `assets/superbwarfare/models/...`, so the namespace has to stay in the path.
     */
    fun modelPath(definition: TrackedVehicleDefinition): String {
        val id = definition.model
        val namespace = id.substringBefore(':', "superbwarfare")
        val path = id.substringAfter(':')
        return "assets/$namespace/$path"
    }

    /**
     * Compile once per id. [loader] supplies the raw bytes so tests can inject a
     * different source without touching the classpath.
     */
    fun load(
        vehicleId: String,
        loader: (String) -> String? = ::classpathResource,
    ): Result = cache.computeIfAbsent(vehicleId) { id ->
        val definitionText = loader(definitionPath(id))
            ?: return@computeIfAbsent Result.Failed("missing tracked definition ${definitionPath(id)}")
        val definition = try {
            json.decodeFromString(TrackedVehicleDefinition.serializer(), definitionText)
        } catch (failure: Throwable) {
            return@computeIfAbsent Result.Failed("cannot parse ${definitionPath(id)}: ${failure.message}")
        }
        val modelText = loader(modelPath(definition))
            ?: return@computeIfAbsent Result.Failed("missing tracked model ${modelPath(definition)}")
        val compilation = TrackedVehicleCompiler.compile(id, definition, modelText)
        val spec = compilation.spec
            ?: return@computeIfAbsent Result.Failed(
                compilation.findings
                    .filter { it.severity == AuditSeverity.ERROR }
                    .joinToString("; ") { "${it.code}: ${it.message}" }
                    .ifEmpty { "tracked definition produced no spec" },
            )
        Result.Loaded(spec)
    }

    /** Drop the cache; used by reload and by tests. */
    fun invalidate(vehicleId: String? = null) {
        if (vehicleId == null) cache.clear() else cache.remove(vehicleId)
    }

    private fun classpathResource(path: String): String? {
        val stream = TrackedVehicleDefinitions::class.java.classLoader.getResourceAsStream(path)
            ?: return null
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
