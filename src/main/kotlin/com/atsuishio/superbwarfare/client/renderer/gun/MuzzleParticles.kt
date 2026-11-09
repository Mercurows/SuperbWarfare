package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.particle.MuzzleSmokeParticle
import com.atsuishio.superbwarfare.resource.gun.DefaultGunResource
import com.atsuishio.superbwarfare.tools.BedrockBoneCoordinateTool
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.WorldEmitterManager
import net.minecraft.client.Minecraft
import net.minecraft.client.ParticleStatus
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.roundToInt
import kotlin.random.Random

internal object MuzzleParticles {
    val smoke = Mod.loc("muzzle_smoke")
    val sparks = Mod.loc("muzzle_sparks")
    val tail = Mod.loc("muzzle_smoke_tail")
    const val ANCHOR = "#shot_muzzle"

    /** Override only our two smoke effects; all other SBM particles keep their own delivery path. */
    fun bindSmokeDelivery(emitter: ParticleEmitterInstance, effect: ResourceLocation, firstPerson: Boolean) {
        if (effect != smoke && effect != tail) return
        val mc = Minecraft.getInstance()
        val definition = emitter.definition
        val delivery = ParticleEmitterInstance.ParticleSpawnCallback { particle ->
            val level = mc.level ?: return@ParticleSpawnCallback
            if (firstPerson) {
                val camera = mc.gameRenderer.mainCamera.position
                particle.x += camera.x.toFloat()
                particle.y += camera.y.toFloat()
                particle.z += camera.z.toFloat()
            }
            mc.particleEngine.add(MuzzleSmokeParticle(level, particle, definition, emitter))
        }
        if (firstPerson) emitter.setWorldSpaceParticleCallback(delivery)
        else emitter.setExternalParticleManagement(delivery)
    }

    fun layers(resource: DefaultGunResource, includeSmoke: Boolean, includeSparks: Boolean, distance: Double,
               emit: (ResourceLocation) -> Unit) {
        if (distance > 64.0) return
        val quality = Minecraft.getInstance().options.particles().get()
        val fx = resource.muzzleFx
        if (includeSmoke && (fx?.smoke?.enabled ?: resource.hasSmoke)) {
            if ((fx?.smoke?.nearCount?.toFloat() ?: resource.smoke.count) > 0) emit(smoke)
            if (quality == ParticleStatus.ALL && distance < 32 && (fx?.smoke?.tailCount ?: 0) > 0) emit(tail)
        }
        if (includeSparks && distance < 32 && quality != ParticleStatus.MINIMAL && (fx?.sparks?.maxCount ?: 0) > 0) emit(sparks)
    }

    // Parameters belong to one emitter. Independent samples keep large puffs from always
    // being the fastest, densest and longest lived ones as well.
    fun configure(emitter: ParticleEmitterInstance, resource: DefaultGunResource, effect: ResourceLocation) {
        val base = resource.smoke
        fun jitter(amount: Float) = 1f + (2f * Random.nextFloat() - 1f) * amount.coerceIn(0f, 1f)
        val residual = effect == tail
        val fx = resource.muzzleFx
        val quality = Minecraft.getInstance().options.particles().get()
        val limit = when (quality) {
            ParticleStatus.MINIMAL -> 2
            ParticleStatus.DECREASED -> 4
            else -> 6
        }
        val count = when (effect) {
            sparks -> Random.nextInt(1, (fx?.sparks?.maxCount ?: 1).coerceIn(1, 2) + 1)
            tail -> (fx?.smoke?.tailCount ?: 1).coerceIn(0, 2)
            else -> ((fx?.smoke?.nearCount?.toFloat() ?: base.count) * jitter(base.randomCount)).roundToInt()
        }
        emitter.setVariable("smoke_count", count.coerceIn(0, limit).toFloat())
        // 火星定义只读 smoke_count。下面这些 smoke_* 会被粒子定义「每 tick 重新求值」
        //（尺寸公式 / 颜色 alpha / 阻尼），而第一人称整套 FirstPersonParticleSystem 共用
        // 同一个 ParticleMolangEnvironment：后 tick 的发射器会把自己的 override 写进同一份存储，
        // 屏幕上还活着的旧烟团下一 tick 就会读到它的值 —— 一发火星就能把在场所有烟团的大小、
        // 透明度整批换掉（火星是否触发还带概率，观感上就是随机抽动）。所以火星到此为止。
        // 动画时间轴里的其它粒子（如 ql_1031 的 cherry_blossom）仍按原来的参数配置。
        if (effect == sparks) return
        emitter.setVariable("smoke_size", (base.size * jitter(base.randomSize)).coerceIn(0f, 4f) * if (residual) 0.62f else 0.8f)
        emitter.setVariable("smoke_growth", (base.growth * jitter(base.randomGrowth)).coerceIn(0f, 5f))
        emitter.setVariable("smoke_speed", (base.speed * jitter(base.randomSpeed)).coerceIn(0f, 10f) * if (residual) 0.4f else 1.4f)
        emitter.setVariable("smoke_lifetime", (base.lifetime * jitter(base.randomLifetime)).coerceIn(0.05f, 2f) * if (residual) 1.55f else 0.85f)
        emitter.setVariable("smoke_opacity", (base.opacity * jitter(base.randomOpacity)).coerceIn(0f, 1f) * if (residual) 0.4f else 1f)
        emitter.setVariable("smoke_drag", base.drag.coerceIn(0f, 30f))
    }

    fun spawnWorld(view: Matrix4f, resource: DefaultGunResource, includeSmoke: Boolean, includeSparks: Boolean) {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val camera = mc.gameRenderer.mainCamera
        val origin = BedrockBoneCoordinateTool.viewPointToWorld(Vec3(view.m30().toDouble(), view.m31().toDouble(), view.m32().toDouble()), camera)
        val axis = view.transformDirection(0f, 0f, -1f, Vector3f())
        if (axis.lengthSquared() < 1e-10f) return
        val direction = BedrockBoneCoordinateTool.viewPointToWorldOffset(Vec3(axis.x.toDouble(), axis.y.toDouble(), axis.z.toDouble()), camera).normalize()
        val anchor = Matrix4f().translation(origin.x.toFloat(), origin.y.toFloat(), origin.z.toFloat())
            .rotate(Quaternionf().rotationTo(0f, 0f, -1f, direction.x.toFloat(), direction.y.toFloat(), direction.z.toFloat()))
        val manager = WorldEmitterManager.getInstance()
        layers(resource, includeSmoke, includeSparks, camera.position.distanceTo(origin)) { effect ->
            if (manager.emitterCount >= 96) return@layers
            val definition = ParticleDefinitionLoader.getInstance().getDefinition(effect) ?: return@layers
            // Third argument is emitter velocity, not barrel direction (SBM 2.6.0).
            val emitter = manager.addEmitter(level, origin, Vec3.ZERO, definition) ?: return@layers
            bindSmokeDelivery(emitter, effect, false)
            manager.updateEmitterAnchor(emitter, anchor)
            configure(emitter, resource, effect)
        }
    }
}
