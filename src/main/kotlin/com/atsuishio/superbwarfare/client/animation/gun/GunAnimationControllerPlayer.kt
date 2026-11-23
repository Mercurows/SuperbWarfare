package com.atsuishio.superbwarfare.client.animation.gun

import com.atsuishio.superbwarfare.client.animation.gun.GunAnimationControllers.Blend
import com.atsuishio.superbwarfare.client.animation.gun.GunAnimationControllers.Loaded
import com.atsuishio.superbwarfare.client.animation.gun.GunAnimationControllers.Play
import com.atsuishio.superbwarfare.client.animation.gun.GunAnimationControllers.Rule
import com.atsuishio.superbwarfare.client.animation.gun.GunAnimationControllers.State
import com.atsuishio.superbwarfare.client.animation.gun.GunAnimationControllers.Track
import com.atsuishio.superbwarfare.client.gun.MeleeClientHandler
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.isDrumLevel
import com.atsuishio.superbwarfare.data.gun.showsHandGuard
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.DeployedWeaponHandler
import com.atsuishio.superbwarfare.tools.localPlayer
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.runner.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import net.minecraft.world.InteractionHand
import org.mozillaa.javascript.Context
import org.mozillaa.javascript.Function
import org.mozillaa.javascript.Scriptable
import org.mozillaa.javascript.ScriptableObject

class GunAnimationControllerPlayer(
    private val loaded: Loaded,
    private val clips: Map<String, BedrockAnimation>,
    private val slot: (String) -> String?,
    private val timing: (String, BedrockAnimation) -> Float,
    private val collect: (AnimationRunner) -> Unit
) {
    private val scope = loaded.compiled?.let { GunAnimationControllers.withContext { cx -> GunAnimationControllers.createScope(cx, loaded) } }
    private val tracks = loaded.definition.tracks.map(::ActiveTrack)
    private data class Event(val name: String, val index: Long = 0, val clip: String? = null)
    private val pending = mutableListOf(Event("Draw"))
    private var shots = 0L
    private var clockNanos = 0L
    private var snapshot: Scriptable? = null
    val id get() = loaded.id
    val standalone = loaded.definition.base == null
    val ownsSprint get() = tracks.any { it.active && it.spec.ownsSprint }
    val takesHands get() = tracks.any { it.active && it.state.takesHands }
    fun replaces(target: GunAnimationState?) = tracks.any { it.active && target in it.spec.replacements }
    fun shot(clip: String?) { pending.add(Event("Shot", ++shots, clip)) }

    private fun obj(cx: Context, vararg values: Pair<String, Any?>, prototype: Scriptable? = null): ScriptableObject {
        val obj = cx.newObject(scope) as ScriptableObject
        for ((key, value) in values) obj.defineProperty(key, value, ScriptableObject.READONLY)
        if (prototype != null) obj.prototype = prototype
        obj.sealObject()
        return obj
    }

    fun update(data: GunData, hand: InteractionHand, target: GunAnimationState?, delta: Float) {
        advance(target, delta) { cx ->
            val reload = obj(cx, "active" to data.reloading(), "stage" to data.reload.stage(),
                "empty" to data.reload.empty(), "loadIndex" to data.loadIndex.get(),
                "prepareLoadTicks" to data.reload.prepareLoadTimer.get(), "ticks" to data.reload.time())
            obj(cx, "reload" to reload, "ammo" to data.ammo.get(), "heat" to data.heat.get(),
                "rpm" to ClientEventHandler.effectiveRpm(data), "fireMode" to data.selectedFireModeInfo().name,
                "editing" to ClientEventHandler.isEditing, "chargeProgress" to ClientEventHandler.chargeProgress,
                "charging" to ClientEventHandler.chargeActive, "meleeIndex" to MeleeClientHandler.swingSerial,
                "bolting" to (data.bolt.actionTimer.get() > 0), "holdOpen" to data.holdOpen.get(),
                "closeStrike" to data.closeStrike.get(),
                "zoom" to ClientEventHandler.zoomTime, "holdingFire" to ClientEventHandler.holdingFireKey,
                "sprinting" to (localPlayer?.isSprinting == true), "onGround" to (localPlayer?.onGround() == true),
                "deployed" to (localPlayer?.let(DeployedWeaponHandler::isDeployed) == true),
                "hasDrum" to data.isDrumLevel(), "hasHandGuard" to data.showsHandGuard(),
                "hand" to hand.name, "baseAction" to target?.name, "deltaSeconds" to delta,
                "magazine" to data.get(GunProp.MAGAZINE))
        }
    }

    internal fun advance(target: GunAnimationState?, delta: Float, snapshotOf: (Context) -> Scriptable) {
        clockNanos += (delta * 1e9).toLong()
        // An inherited action override has no work (or JS snapshot) outside its action window.
        if (tracks.none { !it.failed && (it.active || it.appliesTo(target)) }) {
            pending.clear()
            return
        }
        fun advanceTracks(cx: Context?) {
            snapshot = cx?.let(snapshotOf)
            for (track in tracks) {
                if (track.failed) continue
                track.active = track.appliesTo(target)
                try {
                    if (track.active) track.update(cx, delta) else track.reset(cx)
                } catch (e: Exception) {
                    track.failed = true
                    track.active = false
                    GunAnimationControllers.report(loaded.id, "${track.spec.id}/${track.state.id}: ${e.message}")
                    val primary = tracks.firstOrNull { it.spec.id == "action" } ?: tracks.first()
                    if (standalone && track === primary) throw e
                }
            }
        }
        try {
            if (scope == null) advanceTracks(null) else GunAnimationControllers.withContext { advanceTracks(it) }
        } finally {
            pending.clear()
            snapshot = null
        }
    }

    fun blend(after: String, lower: Pose, mix: (Pose, Pose, Blend, Float) -> Pose): Pose {
        var result = lower
        for (track in tracks) if (track.active && track.spec.after == after) result = track.blend(result, mix)
        return result
    }

    private inner class ActiveTrack(val spec: Track) {
        private val states = spec.states.associateBy { it.id }
        var state = states.getValue(spec.initial)
        var active = false
        var failed = false
        private var initialized = false
        private var age = 0.0
        private var runner: AnimationRunner? = null
        private var lastKey: Any? = null
        private var weight = 1f
        private var previous: AnimationRunner? = null
        private var previousState: State? = null
        private var fadeWeight = 0f
        private var finished = false
        private var event: Event? = null
        private var view: Scriptable? = null

        fun appliesTo(target: GunAnimationState?) = spec.replacements.isEmpty() || target in spec.replacements

        private fun call(cx: Context?, name: String): Any? {
            requireNotNull(cx)
            val fn = scope!!.get(name, scope) as? Function ?: error("Missing JS function $name")
            val progress = runner?.let { it.progress.toDouble() / it.maxProgress.coerceAtLeast(1L) } ?: 0.0
            val ctx = view ?: obj(cx, "state" to state.id, "stateSeconds" to age, "progress" to progress,
                prototype = snapshot).also { view = it }
            val ev = event?.let { obj(cx, "type" to it.name, "shotIndex" to it.index.toDouble(), "resolvedClip" to it.clip) }
            return GunAnimationControllers.invoke(fn, cx, scope, ctx, ev)
        }

        private fun number(cx: Context?, value: JsonPrimitive): Float {
            val raw = if (value.isString) (call(cx, value.content) as? Number)?.toDouble()
                ?: error("${value.content} must return a number") else value.double
            return raw.toFloat().also { require(it.isFinite()) { "Non-finite playback parameter" } }
        }

        private fun matches(cx: Context?, rule: Rule): Boolean {
            if (rule.on != event?.name || age < (rule.after ?: 0.0)) return false
            return rule.condition?.let { call(cx, it) as? Boolean ?: error("$it must return true or false") } ?: true
        }

        private fun clip(cx: Context?): BedrockAnimation? {
            val source = state
            val name = when {
                source.selector != null -> call(cx, source.selector) as? CharSequence ?: error("Expected clip name")
                source.slot != null -> slot(source.slot) ?: error("Missing ClipSlot '${source.slot}'")
                else -> source.clip ?: return null
            }.toString()
            return clips[name] ?: error("Missing clip '$name'")
        }

        private fun startClip(cx: Context?, preserve: Boolean = false) {
            val old = runner
            val asset = clip(cx)
            runner = asset?.let {
                val r = AnimationRunner(it, AnimationContext(it.specifiedEndTimeS))
                val rate = speed(cx, it)
                r.progress = when {
                    preserve && old != null && old.maxProgress > 0 -> (r.maxProgress * (old.progress.toDouble() / old.maxProgress)).toLong()
                    rate < 0 -> r.maxProgress
                    else -> 0L
                }
                r.state = when (state.play) {
                    Play.Loop -> LoopingState { clockNanos }
                    Play.OnceHold -> PlayingState({ clockNanos }, { PauseState() })
                    Play.OnceStop -> PlayingState({ clockNanos }, { StopState() })
                }
                r
            }
            finished = false
        }

        private fun speed(cx: Context?, asset: BedrockAnimation): Float =
            (number(cx, state.speed) * (state.timing?.let { timing(it, asset) } ?: 1f)).also { require(it.isFinite()) }

        private fun enter(cx: Context?, next: State) {
            if (initialized) {
                state.exit?.let { call(cx, it) }
                previous = runner?.takeIf { state.fadeOut > 0 }
                previousState = state
                fadeWeight = weight
            }
            initialized = true
            state = next
            age = 0.0
            runner = null
            view = null
            state.enter?.let { call(cx, it) }
            lastKey = state.restart?.let { call(cx, it) }
            startClip(cx)
        }

        fun reset(cx: Context?) {
            if (initialized) state.exit?.let { call(cx, it) }
            initialized = false
            state = states.getValue(spec.initial)
            previous = null
            runner = null
            view = null
        }

        private fun transition(cx: Context?, input: Event?): Boolean {
            event = input
            val rule = state.transitions.firstOrNull { matches(cx, it) } ?: return false
            enter(cx, states.getValue(rule.to))
            return true
        }

        fun update(cx: Context?, delta: Float) {
            view = null
            event = null
            if (!initialized) enter(cx, state) else age += delta
            for (input in pending) transition(cx, input)
            event = null
            if (!transition(cx, null) && !state.suspendSelect) {
                val selected = spec.select.firstOrNull { matches(cx, it) }
                if (selected != null && selected.to != state.id) enter(cx, states.getValue(selected.to))
            }
            val clipSlot = state.slot
            val key = state.restart?.let { call(cx, it) }
            require(key == null || key is Number || key is Boolean || key is CharSequence) { "RestartKey must return a scalar" }
            if (key != lastKey) {
                lastKey = key
                startClip(cx)
            } else if (clipSlot != null && slot(clipSlot) != (runner?.animation as? BedrockAnimation)?.name) {
                startClip(cx, preserve = true)
            }
            updateWeight(cx)
            previous?.let {
                if (age >= (previousState?.fadeOut ?: 0f)) previous = null
                else if (previousState?.freeze == false) it.tick()
            }
            val r = runner ?: return
            val rate = speed(cx, r.animation as BedrockAnimation)
            when (val playback = r.state) {
                is PlayingState -> playback.speed = rate
                is LoopingState -> playback.speed = rate
            }
            // A held endpoint can move again when the author reverses the speed.
            if (r.state is PauseState && ((rate < 0 && r.progress > 0) || (rate > 0 && r.progress < r.maxProgress))) {
                r.state = PlayingState({ clockNanos }, { PauseState() }).also { it.speed = rate }
                finished = false
            }
            r.tick()
            if (weight > 0f && rate != 0f) collect(r)
            if (!finished && (r.state is PauseState || r.state is StopState)) {
                finished = true
                if (transition(cx, Event("ClipFinished"))) updateWeight(cx)
                event = null
            }
        }

        private fun updateWeight(cx: Context?) {
            val fade = if (state.fadeIn > 0) (age / state.fadeIn).coerceAtMost(1.0).toFloat() else 1f
            weight = number(cx, state.weight).coerceIn(0f, 1f) * fade
        }

        fun blend(lower: Pose, mix: (Pose, Pose, Blend, Float) -> Pose): Pose {
            var pose = lower
            previous?.let { old ->
                val remaining = 1f - (age / previousState!!.fadeOut).toFloat()
                pose = mix(pose, old.evaluate(), spec.blend, fadeWeight * remaining)
            }
            val current = runner ?: return pose
            if (weight <= 0 || current.state is StopState) return pose
            return mix(pose, current.evaluate(), spec.blend, weight)
        }
    }
}
