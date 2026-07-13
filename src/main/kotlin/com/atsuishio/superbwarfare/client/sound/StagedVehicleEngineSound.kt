package com.atsuishio.superbwarfare.client.sound

import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.VehicleEngineSoundLayer
import com.atsuishio.superbwarfare.init.VehicleEngineTransientSound
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.registries.DeferredHolder
import kotlin.math.abs
import kotlin.math.max

/**
 * A layered engine mixer. Every layer follows the vehicle but has its own continuously calculated
 * gain, allowing seamless transitions without restarting short loop files.
 */
object StagedVehicleEngineSound {
    private enum class Kind { GROUND, HELICOPTER, AIRCRAFT }

    private data class Profile(val soundSet: String, val kind: Kind)

    private val profiles = mapOf(
        // Land vehicles
        "wheel_chair" to Profile("wheel_chair", Kind.GROUND),
        "lav_150" to Profile("lav", Kind.GROUND),
        "lav_25" to Profile("lav", Kind.GROUND),
        "lav_ad" to Profile("lav", Kind.GROUND),
        "bmp_2" to Profile("bmp", Kind.GROUND),
        "bradley" to Profile("bradley", Kind.GROUND),
        "ztz_99a" to Profile("t90", Kind.GROUND),
        "t_90a" to Profile("t90", Kind.GROUND),
        "m_1a_2" to Profile("abrams", Kind.GROUND),
        "yx_100" to Profile("heavy", Kind.GROUND),
        "prism_tank" to Profile("heavy", Kind.GROUND),
        "plz_05" to Profile("artillery", Kind.GROUND),
        "sodayo_pick_up" to Profile("pickup", Kind.GROUND),
        "sodayo_pick_up_hmg" to Profile("pickup", Kind.GROUND),
        "sodayo_pick_up_rocket" to Profile("pickup", Kind.GROUND),
        "sodayo_pick_up_tow" to Profile("pickup", Kind.GROUND),
        "truck" to Profile("truck", Kind.GROUND),

        // Real aircraft only. Fictional aircraft deliberately retain their original sounds.
        "ah_6" to Profile("ah6", Kind.HELICOPTER),
        "mi_28" to Profile("mi28", Kind.HELICOPTER),
        "ju_87" to Profile("ju87", Kind.AIRCRAFT),
        "a_10a" to Profile("a10", Kind.AIRCRAFT),
        "ac_130h" to Profile("ac130", Kind.AIRCRAFT)
    )

    /** Returns false when the vehicle has no staged profile, so the caller can play the old loop. */
    fun play(vehicle: VehicleEntity): Boolean {
        val entityId = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.type).path
        val profile = profiles[entityId] ?: return false
        val sounds = ModSounds.VEHICLE_ENGINE_SOUNDS[profile.soundSet] ?: return false

        var played = false
        sounds.forEach { (layer, _) ->
            resolveSound(sounds, profile.kind, layer)?.let { sound ->
                mc.soundManager.play(LayerSound(sound, vehicle, profile.kind, layer))
                played = true
            }
        }
        return played
    }

    fun playStart(vehicle: VehicleEntity) = playTransient(vehicle, starting = true)

    fun playStop(vehicle: VehicleEntity) = playTransient(vehicle, starting = false)

    private fun playTransient(vehicle: VehicleEntity, starting: Boolean) {
        val entityId = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.type).path
        val profile = profiles[entityId] ?: return
        if (profile.kind != Kind.GROUND) return

        val transients = ModSounds.VEHICLE_ENGINE_TRANSIENT_SOUNDS[profile.soundSet] ?: return
        val layers = ModSounds.VEHICLE_ENGINE_SOUNDS[profile.soundSet]
        val externalType = if (starting) {
            VehicleEngineTransientSound.START_EXTERNAL
        } else {
            VehicleEngineTransientSound.STOP_EXTERNAL
        }
        val internalType = if (starting) {
            VehicleEngineTransientSound.START_INTERNAL
        } else {
            VehicleEngineTransientSound.STOP_INTERNAL
        }

        val external = resolveTransientSound(transients, layers, externalType, internalType, internal = false)
        val internal = resolveTransientSound(transients, layers, internalType, externalType, internal = true)
        external?.let { mc.soundManager.play(TransientSound(it, vehicle, internal = false)) }
        internal?.let { mc.soundManager.play(TransientSound(it, vehicle, internal = true)) }
    }

    private fun resolveTransientSound(
        transients: Map<VehicleEngineTransientSound, DeferredHolder<SoundEvent, SoundEvent>>,
        layers: Map<VehicleEngineSoundLayer, DeferredHolder<SoundEvent, SoundEvent>>?,
        preferred: VehicleEngineTransientSound,
        counterpart: VehicleEngineTransientSound,
        internal: Boolean
    ): SoundEvent? {
        transients[preferred]?.get()?.takeIf(::isAvailable)?.let { return it }
        transients[counterpart]?.get()?.takeIf(::isAvailable)?.let { return it }

        val idleLayer = if (internal) {
            VehicleEngineSoundLayer.IDLE_INTERNAL
        } else {
            VehicleEngineSoundLayer.IDLE_EXTERNAL
        }
        return layers?.get(idleLayer)?.get()?.takeIf(::isAvailable)
    }

    /**
     * Resource packs may omit individual layers. Resolve a usable event before constructing the
     * looping instance so a missing Distant/INT/EXT entry cannot make that part of the mix silent.
     */
    private fun resolveSound(
        sounds: Map<VehicleEngineSoundLayer, DeferredHolder<SoundEvent, SoundEvent>>,
        kind: Kind,
        layer: VehicleEngineSoundLayer
    ): SoundEvent? {
        sounds[layer]?.get()?.takeIf(::isAvailable)?.let { return it }

        val distanceFallback = when (layer) {
            VehicleEngineSoundLayer.DISTANCE -> ModSounds.ENGINE_FALLBACK_GROUND_DISTANCE.get()
            VehicleEngineSoundLayer.ROTOR_DISTANCE -> ModSounds.ENGINE_FALLBACK_ROTOR_DISTANCE.get()
            VehicleEngineSoundLayer.TURBINE_DISTANCE -> ModSounds.ENGINE_FALLBACK_TURBINE_DISTANCE.get()
            VehicleEngineSoundLayer.DISTANCE_FRONT,
            VehicleEngineSoundLayer.DISTANCE_MIDDLE,
            VehicleEngineSoundLayer.DISTANCE_REAR -> ModSounds.ENGINE_FALLBACK_AIRCRAFT_DISTANCE.get()
            else -> null
        }
        distanceFallback?.takeIf(::isAvailable)?.let { return it }

        return fallbackLayers(kind, layer)
            .asSequence()
            .mapNotNull { sounds[it]?.get() }
            .firstOrNull(::isAvailable)
    }

    private fun isAvailable(sound: SoundEvent): Boolean =
        (mc.soundManager.getSoundEvent(sound.location)?.weight ?: 0) > 0

    private fun fallbackLayers(kind: Kind, layer: VehicleEngineSoundLayer): List<VehicleEngineSoundLayer> =
        when (layer) {
            VehicleEngineSoundLayer.IDLE_EXTERNAL -> listOf(VehicleEngineSoundLayer.IDLE_INTERNAL)
            VehicleEngineSoundLayer.IDLE_INTERNAL -> listOf(VehicleEngineSoundLayer.IDLE_EXTERNAL)
            VehicleEngineSoundLayer.DRIVE_EXTERNAL -> listOf(
                VehicleEngineSoundLayer.IDLE_EXTERNAL,
                VehicleEngineSoundLayer.DRIVE_INTERNAL
            )
            VehicleEngineSoundLayer.DRIVE_INTERNAL -> listOf(
                VehicleEngineSoundLayer.IDLE_INTERNAL,
                VehicleEngineSoundLayer.DRIVE_EXTERNAL
            )
            VehicleEngineSoundLayer.RELEASE_EXTERNAL -> listOf(
                VehicleEngineSoundLayer.DRIVE_EXTERNAL,
                VehicleEngineSoundLayer.IDLE_EXTERNAL
            )
            VehicleEngineSoundLayer.RELEASE_INTERNAL -> listOf(
                VehicleEngineSoundLayer.DRIVE_INTERNAL,
                VehicleEngineSoundLayer.IDLE_INTERNAL
            )
            VehicleEngineSoundLayer.ROTOR_EXTERNAL -> listOf(
                VehicleEngineSoundLayer.TURBINE_EXTERNAL,
                VehicleEngineSoundLayer.ROTOR_INTERNAL
            )
            VehicleEngineSoundLayer.ROTOR_INTERNAL -> listOf(
                VehicleEngineSoundLayer.TURBINE_INTERNAL,
                VehicleEngineSoundLayer.ROTOR_EXTERNAL
            )
            VehicleEngineSoundLayer.TURBINE_EXTERNAL -> listOf(
                VehicleEngineSoundLayer.ROTOR_EXTERNAL,
                VehicleEngineSoundLayer.TURBINE_INTERNAL
            )
            VehicleEngineSoundLayer.TURBINE_INTERNAL -> listOf(
                VehicleEngineSoundLayer.ROTOR_INTERNAL,
                VehicleEngineSoundLayer.TURBINE_EXTERNAL
            )
            VehicleEngineSoundLayer.DISTANCE -> listOf(
                VehicleEngineSoundLayer.DRIVE_EXTERNAL,
                VehicleEngineSoundLayer.IDLE_EXTERNAL
            )
            VehicleEngineSoundLayer.ROTOR_DISTANCE -> listOf(
                VehicleEngineSoundLayer.ROTOR_EXTERNAL,
                VehicleEngineSoundLayer.TURBINE_EXTERNAL
            )
            VehicleEngineSoundLayer.TURBINE_DISTANCE -> listOf(
                VehicleEngineSoundLayer.TURBINE_EXTERNAL,
                VehicleEngineSoundLayer.ROTOR_EXTERNAL
            )
            VehicleEngineSoundLayer.DISTANCE_FRONT,
            VehicleEngineSoundLayer.DISTANCE_MIDDLE,
            VehicleEngineSoundLayer.DISTANCE_REAR -> when (kind) {
                Kind.AIRCRAFT -> listOf(
                    VehicleEngineSoundLayer.DISTANCE_MIDDLE,
                    VehicleEngineSoundLayer.DRIVE_EXTERNAL,
                    VehicleEngineSoundLayer.IDLE_EXTERNAL
                ).filterNot { it == layer }
                else -> emptyList()
            }
        }

    private class LayerSound(
        sound: SoundEvent,
        private val vehicle: VehicleEntity,
        private val kind: Kind,
        private val layer: VehicleEngineSoundLayer
    ) : AbstractTickableSoundInstance(sound, SoundSource.AMBIENT, vehicle.random) {
        private var fade = 0f
        private var smoothedVolume = 0f
        private var smoothedPitch = 1f

        init {
            looping = true
            delay = 0
            volume = 0f
            pitch = 1f
            x = vehicle.x
            y = vehicle.y
            z = vehicle.z
        }

        /**
         * Layers intentionally start muted and fade in on their first ticks. Without this override
         * SoundEngine rejects them at play time because their initial volume is zero.
         */
        override fun canStartSilent(): Boolean = true

        override fun tick() {
            val client = Minecraft.getInstance()
            if (vehicle.isRemoved || client.player == null) {
                stop()
                return
            }

            fade = Mth.clamp(fade + if (vehicle.engineSoundActive()) 0.08f else -0.08f, 0f, 1f)
            if (fade <= 0f && !vehicle.engineSoundActive()) {
                stop()
                return
            }

            x = vehicle.x
            y = vehicle.y
            z = vehicle.z

            val targetVolume = when (kind) {
                Kind.GROUND -> groundVolume(client)
                Kind.HELICOPTER -> helicopterVolume(client)
                Kind.AIRCRAFT -> aircraftVolume(client)
            } * fade

            smoothedVolume = Mth.lerp(0.32f, smoothedVolume, targetVolume)
            smoothedPitch = Mth.lerp(0.1f, smoothedPitch, targetPitch())
            volume = smoothedVolume
            pitch = smoothedPitch
        }

        private fun groundVolume(client: Minecraft): Float {
            val context = listenerContext(client)
            val load = groundLoad()
            val turningUnderLoad = vehicle.engineInfo is EngineInfo.Track && abs(vehicle.deltaRot) > 0.02f
            val accelerating = vehicle.forwardInputDown || vehicle.backInputDown || turningUnderLoad
            val motion = smoothstep(0.02f, 0.1f, load)
            val releaseMix = if (!accelerating) smoothstep(0.03f, 0.12f, load) else 0f
            val idle = 1f - motion
            val release = motion * releaseMix
            val drive = motion * (1f - releaseMix)
            val base = baseVolume() * (0.78f + 0.28f * load)

            return base * when (layer) {
                VehicleEngineSoundLayer.IDLE_EXTERNAL -> idle * context.external * context.close
                VehicleEngineSoundLayer.IDLE_INTERNAL -> idle * context.internal
                VehicleEngineSoundLayer.DRIVE_EXTERNAL -> drive * context.external * context.close
                VehicleEngineSoundLayer.DRIVE_INTERNAL -> drive * context.internal
                VehicleEngineSoundLayer.RELEASE_EXTERNAL -> release * context.external * context.close
                VehicleEngineSoundLayer.RELEASE_INTERNAL -> release * context.internal
                VehicleEngineSoundLayer.DISTANCE -> context.far * (0.85f + 0.5f * load) * 1.8f
                else -> 0f
            }
        }

        private fun helicopterVolume(client: Minecraft): Float {
            val context = listenerContext(client)
            val rotorSpeed = Mth.clamp(vehicle.synchedPropellerRot / 0.075f, 0f, 1f)
            val load = Mth.clamp(abs(vehicle.power) / 0.12f, 0f, 1f)
            val rotor = baseVolume() * rotorSpeed
            val turbine = baseVolume() * rotorSpeed * (0.65f + 0.45f * load)

            return when (layer) {
                VehicleEngineSoundLayer.ROTOR_EXTERNAL -> rotor * context.external * context.close
                VehicleEngineSoundLayer.ROTOR_INTERNAL -> rotor * context.internal
                VehicleEngineSoundLayer.ROTOR_DISTANCE -> rotor * context.far * 1.85f
                VehicleEngineSoundLayer.TURBINE_EXTERNAL -> turbine * context.external * context.close
                VehicleEngineSoundLayer.TURBINE_INTERNAL -> turbine * context.internal
                VehicleEngineSoundLayer.TURBINE_DISTANCE -> turbine * context.far * 1.65f
                else -> 0f
            }
        }

        private fun aircraftVolume(client: Minecraft): Float {
            val context = listenerContext(client)
            val load = Mth.clamp(abs(vehicle.power), 0f, 1f)
            val thrust = smoothstep(0.02f, 0.12f, load)
            val idle = 1f - thrust
            val base = baseVolume()

            val cameraPos = client.gameRenderer.mainCamera.position
            val toListener = cameraPos.subtract(vehicle.position()).safeNormalize()
            val direction = vehicle.getViewVector(1f).safeNormalize().dot(toListener).toFloat()
            val front = smoothstep(-0.05f, 0.8f, direction)
            val rear = smoothstep(-0.05f, 0.8f, -direction)
            val middle = 1f - 0.72f * smoothstep(0.12f, 0.88f, abs(direction))
            val distant = context.far * base * (0.75f + 0.85f * load) * 2.15f

            return when (layer) {
                VehicleEngineSoundLayer.IDLE_EXTERNAL -> base * idle * context.external * context.close
                VehicleEngineSoundLayer.IDLE_INTERNAL -> base * idle * context.internal
                VehicleEngineSoundLayer.DRIVE_EXTERNAL -> base * thrust * context.external * context.close
                VehicleEngineSoundLayer.DRIVE_INTERNAL -> base * thrust * context.internal
                VehicleEngineSoundLayer.DISTANCE_FRONT -> distant * front
                VehicleEngineSoundLayer.DISTANCE_MIDDLE -> distant * middle
                VehicleEngineSoundLayer.DISTANCE_REAR -> distant * rear
                else -> 0f
            }
        }

        private fun targetPitch(): Float {
            val load = Mth.clamp(abs(vehicle.power), 0f, 1f)
            return when (kind) {
                Kind.GROUND -> 0.94f + 0.1f * groundLoad()
                Kind.HELICOPTER -> 0.97f + 0.05f * load
                Kind.AIRCRAFT -> 0.96f + 0.08f * load
            }
        }

        private fun groundLoad(): Float {
            val turningLoad = if (vehicle.engineInfo is EngineInfo.Track) abs(1.4f * vehicle.deltaRot) else 0f
            return Mth.clamp(max(abs(vehicle.power), turningLoad), 0f, 1f)
        }

        private fun baseVolume(): Float = max(vehicle.engineInfo?.engineSoundVolume ?: 0.6f, 0.15f)

        private fun listenerContext(client: Minecraft): ListenerContext {
            val cameraPos = client.gameRenderer.mainCamera.position
            val distance = cameraPos.distanceTo(vehicle.position()).toFloat()
            val internal = if (client.player?.vehicle === vehicle && client.options.cameraType == CameraType.FIRST_PERSON) 1f else 0f
            val external = 1f - internal
            return ListenerContext(
                internal = internal,
                external = external,
                close = external * (1f - smoothstep(28f, 72f, distance)),
                far = external * smoothstep(28f, 72f, distance)
            )
        }
    }

    private class TransientSound(
        sound: SoundEvent,
        private val vehicle: VehicleEntity,
        private val internal: Boolean
    ) : AbstractTickableSoundInstance(sound, SoundSource.AMBIENT, vehicle.random) {
        init {
            looping = false
            delay = 0
            volume = 0f
            pitch = 1f
            x = vehicle.x
            y = vehicle.y
            z = vehicle.z
        }

        override fun canStartSilent(): Boolean = true

        override fun tick() {
            val client = Minecraft.getInstance()
            if (vehicle.isRemoved || client.player == null) {
                stop()
                return
            }

            x = vehicle.x
            y = vehicle.y
            z = vehicle.z

            val internalView = client.player?.vehicle === vehicle &&
                    client.options.cameraType == CameraType.FIRST_PERSON
            val target = if (internal == internalView) max(vehicle.engineInfo?.engineSoundVolume ?: 0.6f, 0.6f) else 0f
            volume = target
        }
    }

    private data class ListenerContext(
        val internal: Float,
        val external: Float,
        val close: Float,
        val far: Float
    )

    private fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
        val x = Mth.clamp((value - edge0) / (edge1 - edge0), 0f, 1f)
        return x * x * (3f - 2f * x)
    }

    private fun Vec3.safeNormalize(): Vec3 = if (lengthSqr() < 1.0e-8) Vec3.ZERO else normalize()
}
