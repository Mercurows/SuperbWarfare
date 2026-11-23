package com.atsuishio.superbwarfare.client.animation.gun

import com.atsuishio.superbwarfare.Mod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import org.mozillaa.javascript.*

/** Immutable resource definitions are shared. Runners and JS globals belong to a held item. */
object GunAnimationControllers : ResourceManagerReloadListener {
    @Serializable
    data class Definition(
        @SerialName("FormatVersion") val version: Int,
        @SerialName("Base") val base: String? = null,
        @SerialName("Script") val script: String? = null,
        @SerialName("Tracks") val tracks: List<Track>
    )

    @Serializable
    data class Track(
        @SerialName("Id") val id: String,
        @SerialName("After") val after: String = "final",
        @SerialName("Blend") val blend: Blend = Blend.Override,
        @SerialName("ReplaceActions") val replacements: List<GunAnimationState> = emptyList(),
        @SerialName("OwnsSprint") val ownsSprint: Boolean = false,
        @SerialName("InitialState") val initial: String,
        @SerialName("Select") val select: List<Rule> = emptyList(),
        @SerialName("States") val states: List<State>
    )

    enum class Blend { Override, Additive }
    enum class Play { Loop, OnceHold, OnceStop }

    @Serializable
    data class State(
        @SerialName("Id") val id: String,
        @SerialName("Clip") val clip: String? = null,
        @SerialName("ClipSlot") val slot: String? = null,
        @SerialName("ClipSelector") val selector: String? = null,
        @SerialName("Play") val play: Play = Play.OnceHold,
        @SerialName("Speed") val speed: JsonPrimitive = JsonPrimitive(1),
        @SerialName("Weight") val weight: JsonPrimitive = JsonPrimitive(1),
        @SerialName("Timing") val timing: String? = null,
        @SerialName("RestartKey") val restart: String? = null,
        @SerialName("TakesHands") val takesHands: Boolean = false,
        @SerialName("SuspendSelect") val suspendSelect: Boolean = false,
        @SerialName("FadeInSeconds") val fadeIn: Float = 0f,
        @SerialName("FadeOutSeconds") val fadeOut: Float = 0f,
        @SerialName("FreezeOnFadeOut") val freeze: Boolean = false,
        @SerialName("OnEnter") val enter: String? = null,
        @SerialName("OnExit") val exit: String? = null,
        @SerialName("Transitions") val transitions: List<Rule> = emptyList()
    )

    @Serializable
    data class Rule(
        @SerialName("To") val to: String,
        @SerialName("On") val on: String? = null,
        @SerialName("When") val condition: String? = null,
        @SerialName("AfterSeconds") val after: Double? = null
    )

    class Loaded(val id: ResourceLocation, val definition: Definition, val compiled: Script?, val functions: Set<String>)

    private var entries: Map<ResourceLocation, Loaded> = emptyMap()
    private val reported = mutableSetOf<String>()
    var generation = 0L
        private set

    fun lookup(ref: String): Loaded? {
        val id = ResourceLocation.tryParse(ref)
        val entry = id?.let(entries::get)
        if (entry == null && reported.add("missing/$ref")) Mod.LOGGER.error("Missing or invalid animation controller '{}'", ref)
        return entry
    }

    fun report(id: ResourceLocation, message: String) {
        if (reported.add("$id/$message")) Mod.LOGGER.error("Animation controller {}: {}", id, message)
    }

    private class BudgetExceeded : Error()
    private val factory = object : ContextFactory() {
        override fun makeContext(): Context = super.makeContext().apply {
            isInterpretedMode = true
            maximumInterpreterStackDepth = 128
            instructionObserverThreshold = 100_000
            setClassShutter { false }
        }
        override fun observeInstructionCount(cx: Context, instructionCount: Int) { throw BudgetExceeded() }
    }

    fun <T> withContext(block: (Context) -> T): T = try {
        factory.call { cx ->
            check(cx.factory === factory) { "Unexpected nested Rhino context" }
            block(cx)
        }
    } catch (_: BudgetExceeded) {
        throw IllegalArgumentException("JS instruction budget exceeded")
    }

    fun invoke(function: org.mozillaa.javascript.Function, cx: Context, scope: Scriptable,
               snapshot: Scriptable, event: Scriptable?): Any? = try {
        function.call(cx, scope, scope, arrayOf(snapshot, event))
    } catch (_: BudgetExceeded) {
        throw IllegalArgumentException("JS instruction budget exceeded")
    }

    override fun onResourceManagerReload(manager: ResourceManager) {
        val directory = "animation_controllers/gun"
        val next = mutableMapOf<ResourceLocation, Loaded>()
        reported.clear()
        for ((path, resource) in manager.listResources(directory) { it.path.endsWith(".json") }) {
            val id = ResourceLocation(path.namespace, path.path.removePrefix("$directory/").removeSuffix(".json"))
            try {
                val definition = resource.openAsReader().use { Json.decodeFromString<Definition>(it.readText()) }
                val names = validate(definition)
                val script = definition.script?.let {
                    val key = ResourceLocation(it)
                    val file = ResourceLocation(key.namespace, "scripts/animation_controllers/${key.path}.js")
                    val source = manager.getResourceOrThrow(file).openAsReader().use { reader -> reader.readText() }
                    withContext { cx -> cx.compileString(source, file.toString(), 1, null) }
                }
                val loaded = Loaded(id, definition, script, names)
                if (script != null || names.isNotEmpty()) withContext { cx -> createScope(cx, loaded) }
                next[id] = loaded
            } catch (e: Exception) {
                report(id, e.message ?: e.javaClass.simpleName)
                entries[id]?.let { next[id] = it }
            }
        }
        entries = next
        generation++
    }

    fun createScope(cx: Context, loaded: Loaded): ScriptableObject {
        val scope = cx.initSafeStandardObjects()
        loaded.compiled?.exec(cx, scope, scope)
        for (name in loaded.functions) require(scope.get(name, scope) is org.mozillaa.javascript.Function) { "Missing JS function '$name'" }
        return scope
    }

    private fun validate(definition: Definition): Set<String> {
        require(definition.version == 1) { "Unsupported FormatVersion ${definition.version}" }
        require(definition.base == null || definition.base == "superbwarfare:default") { "Unknown Base ${definition.base}" }
        require(definition.tracks.isNotEmpty()) { "Empty Tracks" }
        val trackIds = mutableSetOf<String>()
        val replaced = mutableSetOf<GunAnimationState>()
        val functions = mutableSetOf<String>()
        for (track in definition.tracks) {
            require(track.id.isNotBlank() && trackIds.add(track.id)) { "Duplicate or empty track '${track.id}'" }
            require(track.after in setOf("action", "final")) { "${track.id}: unknown After '${track.after}'" }
            require(track.replacements.all { replaced.add(it) } && (track.replacements.isEmpty() ||
                (definition.base != null && track.after == "action"))) { "${track.id}: invalid or overlapping ReplaceActions" }
            val ids = track.states.map { it.id }.toSet()
            require(ids.size == track.states.size && ids.none { it.isBlank() } && track.initial in ids) { "${track.id}: invalid States or InitialState" }
            for (rule in track.select + track.states.flatMap { it.transitions }) {
                require(rule.to in ids && (rule.after ?: 0.0) >= 0.0) { "${track.id}: invalid transition to ${rule.to}" }
                require(rule.on == null || rule.on in setOf("Shot", "Draw", "ClipFinished")) { "${track.id}: unknown event ${rule.on}" }
                rule.condition?.let(functions::add)
            }
            require(track.select.all { it.on == null }) { "${track.id}: Select only accepts conditions and timers" }
            for (state in track.states) {
                require(listOfNotNull(state.clip, state.slot, state.selector).size <= 1) { "${track.id}/${state.id}: competing clip sources" }
                require(state.fadeIn >= 0 && state.fadeOut >= 0) { "${track.id}/${state.id}: negative fade" }
                functions.addAll(listOfNotNull(state.selector, state.restart, state.enter, state.exit))
                for (number in listOf(state.speed, state.weight)) {
                    if (number.isString) functions.add(number.content)
                    else require(number.doubleOrNull?.isFinite() == true) { "${track.id}/${state.id}: expected a number or function name" }
                }
            }
        }
        for (name in functions) require(callbackName.matches(name)) { "Expected lowerCamelCase JS callback, got '$name'" }
        return functions
    }

    private val callbackName = Regex("[a-z][a-zA-Z0-9]*")
}
