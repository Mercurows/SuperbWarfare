package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.client.particle.CannonMuzzleFlareOption
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.client.particle.CustomFlareOption
import com.atsuishio.superbwarfare.client.particle.ExplosionDebrisOption
import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ExplosionParticleMessage
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage
import com.atsuishio.superbwarfare.tools.SoundTool.playDistantSound
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

object ParticleTool {
    @JvmStatic
    fun <T : ParticleOptions> sendParticle(
        level: ServerLevel, particle: T, x: Double, y: Double, z: Double, count: Int,
        xOffset: Double, yOffset: Double, zOffset: Double, speed: Double, force: Boolean
    ) {
        for (serverPlayer in level.players()) {
            sendParticle(level, particle, x, y, z, count, xOffset, yOffset, zOffset, speed, force, serverPlayer)
        }
    }

    @JvmStatic
    fun <T : ParticleOptions> sendParticle(
        level: ServerLevel, particle: T, x: Double, y: Double, z: Double, count: Int,
        xOffset: Double, yOffset: Double, zOffset: Double, speed: Double, force: Boolean, viewer: ServerPlayer
    ) {
        level.sendParticles(viewer, particle, force, x, y, z, count, xOffset, yOffset, zOffset, speed)
    }

    @JvmStatic
    fun spawnExplosionParticles(type: ParticleType?, level: Level, pos: Vec3) {
        var type = type
        if (type == null) {
            type = ParticleType.MINI
        }

        if (level is ServerLevel) {
            // Server side: play sounds and send a single packet to clients for local rendering
            playExplosionSounds(type, level, pos)
            playExplosionShake(type, level, pos)
            ExplosionParticleMessage.sendToNearbyPlayers(level, type, pos)
        } else {
            // Client side: spawn particles directly (called from ExplosionParticleMessage handler)
            spawnExplosionParticlesClient(type, level, pos)
        }
    }

    @JvmStatic
    fun spawnExplosionParticlesClient(type: ParticleType, level: Level, pos: Vec3) {
        when (type) {
            ParticleType.MINI -> spawnMiniExplosionParticlesInternal(level, pos)
            ParticleType.SMALL -> spawnSmallExplosionParticlesInternal(level, pos)
            ParticleType.MEDIUM -> spawnMediumExplosionParticlesInternal(level, pos)
            ParticleType.LARGE -> spawnLargeExplosionParticlesInternal(level, pos)
            ParticleType.HUGE -> spawnHugeExplosionParticlesInternal(level, pos)
            ParticleType.GIANT -> spawnGiantExplosionParticlesInternal(level, pos)
            ParticleType.EPIC -> spawnEpicExplosionParticlesInternal(level, pos)
        }
    }

    private fun playExplosionSounds(type: ParticleType, level: ServerLevel, pos: Vec3) {
        when (type) {
            ParticleType.MINI -> {
                level.playSound(null, BlockPos.containing(pos.x, pos.y + 1, pos.z), ModSounds.MINI_EXPLOSION.get(), SoundSource.BLOCKS, 4f, 1f)
            }
            ParticleType.SMALL -> {
                playDistantSound(level, ModSounds.EXPLOSION_CLOSE.get(), pos, 2f, 1f, null)
                playDistantSound(level, ModSounds.EXPLOSION_FAR.get(), pos, 8f, 1f, null)
                playDistantSound(level, ModSounds.EXPLOSION_VERY_FAR.get(), pos, 32f, 1f, null)
            }
            ParticleType.MEDIUM -> {
                playDistantSound(level, ModSounds.EXPLOSION_CLOSE.get(), pos, 4f, 1f, null)
                playDistantSound(level, ModSounds.EXPLOSION_FAR.get(), pos, 16f, 1f, null)
                playDistantSound(level, ModSounds.EXPLOSION_VERY_FAR.get(), pos, 32f, 1f, null)
            }
            ParticleType.LARGE -> {
                playDistantSound(level, ModSounds.HUGE_EXPLOSION_CLOSE.get(), pos, 6f, 1f, null)
                playDistantSound(level, ModSounds.HUGE_EXPLOSION_FAR.get(), pos, 20f, 1f, null)
                playDistantSound(level, ModSounds.HUGE_EXPLOSION_VERY_FAR.get(), pos, 64f, 1f, null)
            }
            ParticleType.HUGE -> {
                playDistantSound(level, ModSounds.HUGE_EXPLOSION_CLOSE.get(), pos, 8f, 1f, null)
                playDistantSound(level, ModSounds.HUGE_EXPLOSION_FAR.get(), pos, 24f, 1f, null)
                playDistantSound(level, ModSounds.HUGE_EXPLOSION_VERY_FAR.get(), pos, 128f, 1f, null)
            }
            ParticleType.GIANT -> {
                playDistantSound(level, ModSounds.HUGE_EXPLOSION_CLOSE.get(), pos, 12f, 1f, null)
                playDistantSound(level, ModSounds.HUGE_EXPLOSION_FAR.get(), pos, 32f, 1f, null)
                playDistantSound(level, ModSounds.HUGE_EXPLOSION_VERY_FAR.get(), pos, 192f, 1f, null)
            }
            ParticleType.EPIC -> {
                playDistantSound(level, ModSounds.EPIC_EXPLOSION_CLOSE.get(), pos, 24f, 1f, null)
                playDistantSound(level, ModSounds.EPIC_EXPLOSION_FAR.get(), pos, 38f, 1f, null)
                playDistantSound(level, ModSounds.EPIC_EXPLOSION_VERY_FAR.get(), pos, 132f, 1f, null)
            }
        }
    }

    private fun playExplosionShake(type: ParticleType, level: ServerLevel, pos: Vec3) {
        when (type) {
            ParticleType.HUGE -> ShakeClientMessage.sendToNearbyPlayers(level, pos.x, pos.y, pos.z, 192.0, 30.0, 12.0)
            ParticleType.GIANT -> ShakeClientMessage.sendToNearbyPlayers(level, pos.x, pos.y, pos.z, 384.0, 30.0, 16.0)
            ParticleType.EPIC -> ShakeClientMessage.sendToNearbyPlayers(level, pos.x, pos.y, pos.z, 768.0, 54.0, 9.0)
            else -> {}
        }
    }

    /**
     * Spawns particles on the client side, exactly mimicking the vanilla ClientboundLevelParticlesPacket handler.
     * Position offsets and velocity are INDEPENDENT random values, matching vanilla behavior.
     * This is critical for particles like FIRE_STAR and ExplosionDebrisOption which have
     * xDist/yDist/zDist=0 but non-zero maxSpeed — they should burst outward from the center.
     */
    private fun sendParticleClient(
        level: Level, particle: ParticleOptions, x: Double, y: Double, z: Double,
        count: Int, xDist: Double, yDist: Double, zDist: Double, maxSpeed: Double
    ) {
        val random = level.random
        repeat(count) {
            val dx = random.nextGaussian() * xDist
            val dy = random.nextGaussian() * yDist
            val dz = random.nextGaussian() * zDist
            val speedX = random.nextGaussian() * maxSpeed
            val speedY = random.nextGaussian() * maxSpeed
            val speedZ = random.nextGaussian() * maxSpeed
            level.addParticle(particle, true, x + dx, y + dy, z + dz, speedX, speedY, speedZ)
        }
    }

    /**
     * Spawns a single directional particle on the client side, mimicking vanilla count=0 behavior.
     */
    private fun sendDirectionalParticleClient(
        level: Level, particle: ParticleOptions, x: Double, y: Double, z: Double,
        xSpeed: Double, ySpeed: Double, zSpeed: Double
    ) {
        level.addParticle(particle, true, x, y, z, xSpeed, ySpeed, zSpeed)
    }

    //@formatter:off

    // ---- Public backward-compatible wrappers ----

    @JvmStatic
    fun spawnMiniExplosionParticles(level: Level, pos: Vec3) {
        spawnExplosionParticles(ParticleType.MINI, level, pos)
    }

    @JvmStatic
    fun spawnSmallExplosionParticles(level: Level, pos: Vec3) {
        spawnExplosionParticles(ParticleType.SMALL, level, pos)
    }

    @JvmStatic
    fun spawnMediumExplosionParticles(level: Level, pos: Vec3) {
        spawnExplosionParticles(ParticleType.MEDIUM, level, pos)
    }

    @JvmStatic
    fun spawnLargeExplosionParticles(level: Level, pos: Vec3) {
        spawnExplosionParticles(ParticleType.LARGE, level, pos)
    }

    @JvmStatic
    fun spawnHugeExplosionParticles(level: Level, pos: Vec3) {
        spawnExplosionParticles(ParticleType.HUGE, level, pos)
    }

    @JvmStatic
    fun spawnGiantExplosionParticles(level: Level, pos: Vec3) {
        spawnExplosionParticles(ParticleType.GIANT, level, pos)
    }

    @JvmStatic
    fun spawnEpicExplosionParticles(level: Level, pos: Vec3) {
        spawnExplosionParticles(ParticleType.EPIC, level, pos)
    }

    // ---- Internal client-side particle spawning ----

    private fun spawnMiniExplosionParticlesInternal(level: Level, pos: Vec3) {
        val x = pos.x; val y = pos.y; val z = pos.z
        sendParticleClient(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 2, 0.1, 0.1, 0.1, 0.02)
        sendParticleClient(level, ParticleTypes.EXPLOSION, x, y, z, 2, 0.05, 0.05, 0.05, 1.0)
        sendParticleClient(level, ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.2, 0.2, 0.2, 0.02)
        sendParticleClient(level, ModParticleTypes.FIRE_STAR.get(), x, y, z, 7, 0.0, 0.0, 0.0, 0.4)
        sendParticleClient(level, ParticleTypes.FLASH, x, y, z, 1, 0.0, 0.0, 0.0, 20.0)
    }

    private fun spawnSmallExplosionParticlesInternal(level: Level, pos: Vec3) {
        val x = pos.x; val y = pos.y; val z = pos.z
        sendParticleClient(level, ParticleTypes.EXPLOSION, x, y, z, 2, 0.05, 0.05, 0.05, 1.0)
        sendParticleClient(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 3, 0.1, 0.1, 0.1, 0.02)
        sendParticleClient(level, ParticleTypes.LARGE_SMOKE, x, y, z, 4, 0.2, 0.2, 0.2, 0.02)
        sendParticleClient(level, ModParticleTypes.FIRE_STAR.get(), x, y, z, 8, 0.0, 0.0, 0.0, 0.6)
        sendParticleClient(level, CustomFlareOption(1f, 1f, 1f, 10, 0.25f, 1, 0.2f, size = 4f), x, y, z, 1, 0.0, 0.0, 0.0, 0.005)
    }

    private fun spawnMediumExplosionParticlesInternal(level: Level, pos: Vec3) {
        val x = pos.x; val y = pos.y; val z = pos.z
        sendParticleClient(level, ParticleTypes.EXPLOSION, x, y + 1, z, 4, 1.0, 1.0, 1.0, 1.0)
        sendParticleClient(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 3, 0.2, 1.0, 0.2, 0.02)
        sendParticleClient(level, ParticleTypes.LARGE_SMOKE, x, y + 1, z, 6, 0.8, 1.0, 0.8, 0.02)
        sendParticleClient(level, ModParticleTypes.FIRE_STAR.get(), x, y + 0.2, z, 8, 0.0, 0.0, 0.0, 0.8)

        sendParticleClient(level, CustomFlareOption(1f, 1f, 1f, 10, 0.25f, 1, 0.2f, size = 6f), x, y, z, 1, 0.0, 0.0, 0.0, 0.005)
        sendParticleClient(level, CustomFlareOption(1f, 0.9f, 0.8f, 10, 0.25f, 1, 0.4f, size = 8f), x, y, z, 1, 0.0, 0.0, 0.0, 0.005)

        sendParticleClient(level, CustomFlareOption(0.5f, 0.25f, 0f, 40, 0.85f, 1, 0.25f), x, y + 0.5, z, 1, 0.75, 1.2, 0.75, 0.05)

        sendParticleClient(level, CustomFlareOption(0.667f, 0.631f, 0.592f, 40, 0.85f, (8 + 16 * Math.random()).toInt(), 0.002f, size = 0.6f + 0.8f * Math.random().toFloat()), x, y, z, 16, 2.4, 0.05, 2.4, 0.02)

        sendParticleClient(level, ExplosionDebrisOption(0.5f, 0.2f, 0.0f, 12, 0.75f, (8 + 8 * Math.random()).toInt(), 0.01f, size = 0.1f), x, y + 0.5, z, 8, 0.0, 0.0, 0.0, 0.8)
    }

    private fun spawnLargeExplosionParticlesInternal(level: Level, pos: Vec3) {
        val x = pos.x; val y = pos.y; val z = pos.z
        sendParticleClient(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 10, 0.5, 2.0, 0.5, 0.02)
        sendParticleClient(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.25, z, 20, 5.0, 0.001, 5.0, 0.01)
        sendParticleClient(level, ModParticleTypes.FIRE_STAR.get(), x, y + 0.2, z, 20, 0.0, 0.0, 0.0, 1.2)
        sendParticleClient(level, ParticleTypes.EXPLOSION, x, y + 1, z, 10, 1.5, 1.5, 1.5, 1.0)

        sendParticleClient(level, CustomFlareOption(1f, 1f, 1f, 10, 0.25f, 1, 0.3f, size = 8f), x, y, z, 1, 0.0, 0.0, 0.0, 0.005)
        sendParticleClient(level, CustomFlareOption(1f, 0.9f, 0.8f, 10, 0.25f, 1, 0.6f, size = 12f), x, y, z, 1, 0.0, 0.0, 0.0, 0.005)

        sendParticleClient(level, CustomFlareOption(0.6f, 0.58f, 0.57f, 40, 0.88f, 1, 0.2f), x, y + 1, z, 4, 1.3, 1.7, 1.3, 0.05)
        sendParticleClient(level, CustomFlareOption(0.25f, 0.125f, 0f, 60, 0.88f, 3, 0.25f), x, y + 1, z, 2, 1.3, 1.7, 1.3, 0.05)

        sendParticleClient(level, CustomFlareOption(0.667f, 0.631f, 0.592f, 50, 0.86f, (8 + 16 * Math.random()).toInt(), 0.003f, size = 0.6f + 0.8f * Math.random().toFloat()), x, y, z, 30, 3.2, 0.05, 3.2, 0.04)

        sendParticleClient(level, ExplosionDebrisOption(0.5f, 0.43f, 0.16f, 60, 0.91f, (8 + 8 * Math.random()).toInt(), 0.014f, size = 0.13f), x, y + 0.5, z, 26, 0.0, 0.0, 0.0, 0.8)
        for (i in 0..149) {
            val v = Vec3(1.0, 0.0, 0.0).yRot((i * Math.random()).toFloat())
            sendDirectionalParticleClient(
                level, CustomCloudOption(0xFFFFFF, 3, 2f, 0f, cooldown = false, light = false), x, y + 0.2, z,
                v.x * 12.0, v.y * 12.0, v.z * 12.0
            )
        }

        for (i in 0..5) {
            Mod.queueClientWork(i) {
                sendParticleClient(level, CustomFlareOption(0.5f - (i.toFloat() / 10), 0.25f - (i.toFloat() / 20), 0f, 8 * i, 0.85f, 4, 0.25f, size = 0.7f + 0.08f * i), x, y + 1.3 + 0.8 * i, z, i, 1.1, 0.4, 1.1, 0.1)
            }
        }
    }

    private fun spawnHugeExplosionParticlesInternal(level: Level, pos: Vec3) {
        val x = pos.x; val y = pos.y; val z = pos.z

        sendParticleClient(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 10, 0.75, 2.2, 0.75, 0.02)
        sendParticleClient(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.25, z, 10, 8.0, 0.001, 8.0, 0.01)
        sendParticleClient(level, ModParticleTypes.FIRE_STAR.get(), x, y + 0.2, z, 40, 0.0, 0.0, 0.0, 1.2)
        sendParticleClient(level, ParticleTypes.EXPLOSION, x, y + 1, z, 10, 2.5, 2.5, 2.5, 1.0)

        // 冲击波
        sendParticleClient(level, CustomFlareOption(1f, 1f, 1f, 10, 0.5f, 2, 20f, size = 3f), x, y + 3, z, 1, 0.0, 0.0, 0.0, 0.005)

        // 闪光
        sendParticleClient(level, CustomFlareOption(1f, 1f, 1f, 10, 0.25f, 1, 0.3f, size = 12f), x, y, z, 1, 0.0, 0.0, 0.0, 0.005)
        sendParticleClient(level, CustomFlareOption(1f, 0.9f, 0.8f, 10, 0.25f, 1, 0.6f, size = 17f), x, y, z, 1, 0.0, 0.0, 0.0, 0.005)

        // 火球
        sendParticleClient(level, CustomFlareOption(0.6f, 0.58f, 0.57f, 60, 0.9f, 1, 0.4f), x, y + 1, z, 8, 1.3, 1.7, 1.3, 0.05)
        sendParticleClient(level, CustomFlareOption(0.25f, 0.125f, 0f, 80, 0.9f, 3, 0.5f), x, y + 1, z, 4, 1.3, 1.7, 1.3, 0.05)

        // 地面烟尘
        sendParticleClient(level, CustomFlareOption(0.667f, 0.631f, 0.592f, 50, 0.86f, (8 + 16 * Math.random()).toInt(), 0.003f, size = 0.6f + 0.8f * Math.random().toFloat()), x, y, z, 55, 4.0, 0.05, 4.2, 0.04)

        // 飞溅碎片
        sendParticleClient(level, ExplosionDebrisOption(0.5f, 0.43f, 0.16f, 60, 0.91f, (8 + 8 * Math.random()).toInt(), 0.014f, size = 0.13f), x, y + 0.5, z, 40, 0.0, 0.0, 0.0, 0.8)

        // 火球升腾
        for (i in 0..5) {
            Mod.queueClientWork(i) {
                sendParticleClient(level, CustomFlareOption(0.5f - (i.toFloat() / 10), 0.25f - (i.toFloat() / 20), 0f, 10 * i, 0.9f, 4, 0.35f, size = 1 + 0.1f * i), x, y + 2 + 1.2 * i, z, i, 1.5, 0.5, 1.5, 0.1)
            }
        }

        // 环形冲击波
        for (i in 0..149) {
            val v = Vec3(1.0, 0.0, 0.0).yRot((i * Math.random()).toFloat())
            sendDirectionalParticleClient(
                level, CustomCloudOption(0xFFFFFF, 3, 3f, 0f, cooldown = false, light = false), x, y + 0.2, z,
                v.x * 16.0, v.y * 16.0, v.z * 16.0
            )
        }
    }

    private fun spawnGiantExplosionParticlesInternal(level: Level, pos: Vec3) {
        val x = pos.x; val y = pos.y; val z = pos.z

        sendParticleClient(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 20, 0.75, 2.2, 0.75, 0.02)
        sendParticleClient(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.25, z, 20, 12.0, 0.001, 12.0, 0.01)
        sendParticleClient(level, ModParticleTypes.FIRE_STAR.get(), x, y + 0.2, z, 100, 0.0, 0.0, 0.0, 2.0)
        sendParticleClient(level, ParticleTypes.EXPLOSION, x, y + 1, z, 30, 5.0, 5.0, 5.0, 1.0)

        // 冲击波
        sendParticleClient(level, CustomFlareOption(1f, 1f, 1f, 10, 0.5f, 2, 30f, size = 4f), x, y + 3, z, 1, 0.0, 0.0, 0.0, 0.005)

        // 闪光
        sendParticleClient(level, CustomFlareOption(1f, 1f, 1f, 15, 0.4f, 1, 0.35f, size = 18f), x, y, z, 1, 0.0, 0.0, 0.0, 0.005)
        sendParticleClient(level, CustomFlareOption(1f, 0.9f, 0.8f, 15, 0.4f, 1, 0.7f, size = 26f), x, y, z, 1, 0.0, 0.0, 0.0, 0.005)

        // 火球
        sendParticleClient(level, CustomFlareOption(0.6f, 0.58f, 0.57f, 100, 0.89f, 1, 0.6f, size = 2f), x, y + 1, z, 20, 5.2, 1.5, 5.2, 0.05)
        sendParticleClient(level, CustomFlareOption(0.25f, 0.125f, 0f, 100, 0.89f, 2, 0.8f, size = 2f), x, y + 1, z, 12, 5.2, 1.5, 5.2, 0.05)

        // 地面烟尘
        sendParticleClient(level, CustomFlareOption(0.667f, 0.631f, 0.592f, 50, 0.86f, (8 + 16 * Math.random()).toInt(), 0.003f, size = 0.6f + 0.8f * Math.random().toFloat()), x, y, z, 75, 10.0, 0.05, 10.2, 0.04)

        // 飞溅碎片
        sendParticleClient(level, ExplosionDebrisOption(0.5f, 0.43f, 0.16f, 60, 0.91f, (8 + 8 * Math.random()).toInt(), 0.017f, size = 0.4f), x, y + 0.5, z, 80, 0.0, 0.0, 0.0, 1.6)

        // 火球升腾
        for (i in 0..10) {
            Mod.queueClientWork(i) {
                sendParticleClient(level, CustomFlareOption(0.5f - (i.toFloat() / 20), 0.25f - (i.toFloat() / 40), 0f, 4 * i, 0.87f, 2, 0.3f + 0.04f * i, size = 2.4f + 0.03f * i * i), x, y + 1 + 2.2 * i - 0.05 * i * i, z, i.coerceAtMost(5), 3 + 0.01 * i * i, 0.5, 3 + 0.01 * i * i, 0.15)
            // 冲击波扬尘
                sendParticleClient(level, CustomFlareOption(0.667f, 0.631f, 0.592f, 80, 0.93f, (4 + 12 * Math.random()).toInt(), 0.02f, size = 0.8f + 1.4f * Math.random().toFloat()), x, y - 0.5, z, 10 * i, i.toDouble() + 10, 0.05, i.toDouble() + 10, 0.0)
            }
        }

        // 环形冲击波
        for (i in 0..149) {
            val v = Vec3(1.0, 0.0, 0.0).yRot((i * Math.random()).toFloat())
            sendDirectionalParticleClient(
                level, CustomCloudOption(0xFFFFFF, 5, 6f, 0f, cooldown = false, light = false), x, y + 0.2, z,
                v.x * 20.0, v.y * 20.0, v.z * 20.0
            )
        }
    }

    private fun spawnEpicExplosionParticlesInternal(level: Level, pos: Vec3) {
        val x = pos.x; val y = pos.y; val z = pos.z
        sendParticleClient(level, CustomFlareOption(1f, 1f, 1f, 50, 0.9f, 8, 80f, size = 60f), x, y + 3, z, 1, 0.0, 0.0, 0.0, 0.005)
        sendParticleClient(level, ExplosionDebrisOption(0.5f, 0.43f, 0.16f, 200, 0.96f, (12 + 8 * Math.random()).toInt(), 0.035f, size = 1.1f), x, y + 0.5, z, 400, 0.0, 0.0, 0.0, 3.2)
        for (i in 0..360) {
            val v = Vec3(1.0, 0.0, 0.0).yRot((i * Math.random()).toFloat())
            sendDirectionalParticleClient(
                level, CustomCloudOption(1f, 1f, 1f, 45, 10f, 0f, cooldown = false, light = false), x, y + 1, z,
                v.x * 24.0, v.y * 24.0, v.z * 24.0
            )
        }
        for (i in 0..23) {
            Mod.queueClientWork(i) {
                if (i < 6) {
                    sendParticleClient(level, CustomFlareOption(1f, 1f, 1f, 20, 0.5f, 4, 0.4f, size = 45f), x, y, z, 1, 2.0, 2.0, 2.0, 0.005)
                    sendParticleClient(level, CustomFlareOption(1f, 0.9f, 0.8f, 40, 0.87f, 12, 0.4f, size = 100f), x, y + 3, z, 1, 2.0, 2.0, 2.0, 0.005)
                }
                if (i < 12) {
                    sendParticleClient(level, CustomFlareOption(0.6f - (i.toFloat() / 24), 0.3f - (i.toFloat() / 48), 0f, 200, 0.95f, 14, 0.075f, size = 1.5f), x, y + 5 * i, z, 12, 4.5 - 0.15 * i, 1.2, 4.5 - 0.15 * i, 0.005)
                    sendParticleClient(level, CustomFlareOption(0.4f - (i.toFloat() / 48), 0.2f - (i.toFloat() / 96), 0f, 200, 0.95f, 14, 0.075f, size = 1.2f), x, y + 0.5, z, 12, 6 + 1.2 * i, 0.7, 6 + 1.2 * i, 0.005)
                }
                if (i in 8..<16) {
                    val k = i - 8
                    sendParticleClient(level, CustomFlareOption(0.25f, 0.125f, 0f, 200, 0.95f, 13, 0.025f, size = 4.5f), x, y + 50, z, 8 * k, 3 + 1.1 * k, 3.2 + 0.5 * k, 3 + 1.1 * k, 0.005)
                    sendParticleClient(level, CustomFlareOption(0.125f, 0.0625f, 0f, 200, 0.94f, 14, 0.007f, size = 4.5f), x, y + 54, z, 7 * k, 3 + 1.2 * k, 1.5 + 0.4 * k, 3 + 1.2 * k, 0.005)
                    sendParticleClient(level, CustomFlareOption(0.0625f, 0.03125f, 0f, 200, 0.93f, 15, 0.01f, size = 2.5f), x, y + 44, z, 6 * k, 3 + 1.3 * k, 1.3 + 0.3 * k, 3 + 1.3 * k, 0.005)
                }
                sendParticleClient(level, CustomFlareOption(0.667f, 0.631f, 0.592f, 100, 0.95f, (4 + 12 * Math.random()).toInt(), 0.02f, size = 0.8f + 1.4f * Math.random().toFloat()), x, y - 1, z, 25 * i, i.toDouble() * 5, 0.05, i.toDouble() * 5, 0.0)
            }
        }
    }

    @JvmStatic
    fun spawnBulletHitWaterParticles(level: Level?, pos: Vec3) {
        val x = pos.x
        val y = pos.y
        val z = pos.z

        if (level is ServerLevel) {
            for (i in 0..59) {
                val v = Vec3(1.0, 0.0, 0.0).yRot((i * Math.random()).toFloat())
                sendParticle(
                    level, CustomCloudOption(1f, 1f, 1f, 20, 0.3f, 0f, cooldown = false, light = false), x, y, z,
                    0, v.x, v.y, v.z, 8.0, true
                )
            }
            queueServerWork(8) {
                for (i in 0..44) {
                    val v = Vec3(1.0, 0.0, 0.0).yRot((i * Math.random()).toFloat())
                    sendParticle(
                        level, CustomCloudOption(1f, 1f, 1f, 17, 0.3f, 0f, cooldown = false, light = false), x, y, z,
                        0, v.x, v.y, v.z, 8.0, true
                    )
                }
            }
            queueServerWork(14) {
                for (i in 0..29) {
                    val v = Vec3(1.0, 0.0, 0.0).yRot((i * Math.random()).toFloat())
                    sendParticle(
                        level, CustomCloudOption(1f, 1f, 1f, 15, 0.3f, 0f, cooldown = false, light = false), x, y, z,
                        0, v.x, v.y, v.z, 4.0, true
                    )
                }
            }
            sendParticle(level, ParticleTypes.CLOUD, x, y - 0.2, z, 3, 0.2, 0.0, 0.2, 0.002, false)
            sendParticle(level, ParticleTypes.BUBBLE_COLUMN_UP, x, y - 0.5, z, 5, 0.4, 0.2, 0.4, 0.005, false)
        }
    }

    @JvmStatic
    fun cannonHitParticles(serverLevel: ServerLevel, pos: Vec3) {
        sendParticle(serverLevel, ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 2, 0.5, 0.5, 0.5, 1.0, true)
        sendParticle(serverLevel, ParticleTypes.FLASH, pos.x, pos.y, pos.z, 2, 0.2, 0.2, 0.2, 10.0, true)
        sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), pos.x, pos.y, pos.z, 15, 0.0, 0.0, 0.0, 1.5, true)
    }

    @JvmStatic
    fun spawnMediumCannonMuzzleParticles(direct: Vec3, pos: Vec3, serverLevel: ServerLevel, entity: Entity?) {
        ParticleTool.spawnDirectionalParticles(4, 0.1, serverLevel, CannonMuzzleFlareOption(1f, 1f, 1f, 8, 0.7f, 1, 0.2f), direct, pos, 0.3)
        ParticleTool.spawnDirectionalParticles(3, 0.06, serverLevel, CannonMuzzleFlareOption(1f, 1f, 1f, 8, 0.72f, 1, 0.15f), direct, pos, 0.2)
        ParticleTool.spawnDirectionalParticles(1, 0.0, serverLevel, CannonMuzzleFlareOption(0.4f, 0.4f, 0.4f, 45, 0.88f, 2, 0.05f), direct, pos, 0.15)
        ParticleTool.spawnDirectionalParticles(1, 0.0, serverLevel, CannonMuzzleFlareOption(0.45f, 0.45f, 0.45f, 47, 0.90f, 2, 0.03f), direct, pos, 0.125)
        ParticleTool.spawnDirectionalParticles(1, 0.0, serverLevel, CannonMuzzleFlareOption(0.5f, 0.5f, 0.5f, 48, 0.92f, 2, 0.01f), direct, pos, 0.1)
    }

    @JvmStatic
    fun spawnBigCannonMuzzleParticles(direct: Vec3, pos: Vec3, serverLevel: ServerLevel, entity: Entity?) {
        ParticleTool.spawnDirectionalParticles(10, 0.1, serverLevel, CannonMuzzleFlareOption(1f, 1f, 1f, 8, 0.7f, 1, 2.2f), direct, pos, 1.0)
        ParticleTool.spawnDirectionalParticles(8, 0.06, serverLevel, CannonMuzzleFlareOption(1f, 1f, 1f, 8, 0.72f, 1, 1.5f), direct, pos, 0.8)
        ParticleTool.spawnDirectionalParticles(1, 0.0, serverLevel, CannonMuzzleFlareOption(0.4f, 0.4f, 0.4f, 36, 0.84f, 2, 1.1f), direct, pos, 0.6)
        ParticleTool.spawnDirectionalParticles(1, 0.0, serverLevel, CannonMuzzleFlareOption(0.4f, 0.4f, 0.4f, 39, 0.87f, 2, 0.9f), direct, pos, 0.5)
        ParticleTool.spawnDirectionalParticles(1, 0.0, serverLevel, CannonMuzzleFlareOption(0.4f, 0.4f, 0.4f, 42, 0.87f, 2, 0.7f), direct, pos, 0.35)
        ParticleTool.spawnDirectionalParticles(1, 0.0, serverLevel, CannonMuzzleFlareOption(0.4f, 0.4f, 0.4f, 45, 0.88f, 2, 0.5f), direct, pos, 0.25)
        ParticleTool.spawnDirectionalParticles(1, 0.0, serverLevel, CannonMuzzleFlareOption(0.45f, 0.45f, 0.45f, 47, 0.90f, 2, 0.3f), direct, pos, 0.17)
        ParticleTool.spawnDirectionalParticles(1, 0.0, serverLevel, CannonMuzzleFlareOption(0.5f, 0.5f, 0.5f, 48, 0.92f, 2, 0.1f), direct, pos, 0.1)
    }

    @JvmStatic
    fun spawnDirectionalParticles(
        count: Int,
        radius: Double,
        level: ServerLevel,
        particle: ParticleOptions,
        direct: Vec3,
        pos: Vec3?,
        speed: Double
    ) {
        if (pos == null) return

        val direction = direct.normalize()
        val random = getRandomPerpendicular(direction)
        val u = random.normalize()
        val v = direction.cross(u).normalize()

        spawnCircularParticles(level, pos, u, v, count, radius, particle, direct, speed)
    }

    @JvmStatic
    fun spawnDirectionalParticles(
        count: Int,
        radius: Double,
        level: Level,
        particle: ParticleOptions,
        direct: Vec3,
        pos: Vec3?,
        speed: Double
    ) {
        if (pos == null) return

        val direction = direct.normalize()
        val random = getRandomPerpendicular(direction)
        val u = random.normalize()
        val v = direction.cross(u).normalize()

        spawnCircularParticles(level, pos, u, v, count, radius, particle, direct, speed)
    }

    private fun getRandomPerpendicular(dir: Vec3): Vec3 {
        val candidate1 = Vec3(dir.y, -dir.x, 0.0) // 在XY平面垂直
        if (candidate1.lengthSqr() > 1e-4) return candidate1
        return Vec3(0.0, dir.z, -dir.y) // 备用垂直向量
    }

    private fun spawnCircularParticles(
        level: ServerLevel,
        center: Vec3,
        u: Vec3,
        v: Vec3,
        count: Int,
        radius: Double,
        particle: ParticleOptions,
        direct: Vec3,
        speed: Double
    ) {
        for (i in 0..<count) {
            val theta = 2 * Math.PI * i / count
            val xOffset = radius * (cos(theta) * u.x + sin(theta) * v.x)
            val yOffset = radius * (cos(theta) * u.y + sin(theta) * v.y)
            val zOffset = radius * (cos(theta) * u.z + sin(theta) * v.z)

            val pos = center.add(xOffset, yOffset, zOffset)
            spawnParticle(level, pos, particle, direct, center, speed)
        }
    }

    private fun spawnCircularParticles(
        level: Level,
        center: Vec3,
        u: Vec3,
        v: Vec3,
        count: Int,
        radius: Double,
        particle: ParticleOptions,
        direct: Vec3,
        speed: Double
    ) {
        for (i in 0..<count) {
            val theta = 2 * Math.PI * i / count
            val xOffset = radius * (cos(theta) * u.x + sin(theta) * v.x)
            val yOffset = radius * (cos(theta) * u.y + sin(theta) * v.y)
            val zOffset = radius * (cos(theta) * u.z + sin(theta) * v.z)

            val pos = center.add(xOffset, yOffset, zOffset)
            spawnParticle(level, pos, particle, direct, center, speed)
        }
    }

    private fun spawnParticle(
        level: ServerLevel,
        pos: Vec3,
        particle: ParticleOptions,
        direct: Vec3,
        originPos: Vec3,
        speed: Double
    ) {
        val v0 = originPos.vectorTo(pos).normalize().add(direct.scale(6.0))
        sendParticle(
            level, particle, pos.x, pos.y, pos.z,
            0, v0.x, v0.y, v0.z, speed, true
        )
    }

    private fun spawnParticle(
        level: Level,
        pos: Vec3,
        particle: ParticleOptions,
        direct: Vec3,
        originPos: Vec3,
        speed: Double
    ) {
        val v0 = originPos.vectorTo(pos).normalize().add(direct.scale(6.0))
        sendParticle(level, particle, pos.x, pos.y, pos.z, v0.x, v0.y, v0.z, speed)
    }

    @JvmStatic
    fun sendParticle(
        level: Level,
        particle: ParticleOptions,
        x: Double,
        y: Double,
        z: Double,
        xOffset: Double,
        yOffset: Double,
        zOffset: Double,
        speed: Double
    ) {
        val vec3 = Vec3(xOffset, yOffset, zOffset).normalize().scale(speed * (0.75 + Math.random() * 0.5))
        level.addAlwaysVisibleParticle(particle, true, x, y, z, vec3.x, vec3.y, vec3.z)
    }

    @JvmStatic
    fun spawnBarrelSmoke(count: Int, level: ServerLevel, v0: Vec3, pos: Vec3) {
        repeat(count) {
            sendParticle(
                level, ModParticleTypes.RISING_SMOKE.get(), pos.x, pos.y, pos.z,
                0, v0.x, v0.y, v0.z, 0.22, true
            )
        }
    }
    //@formatter:on

    /**
     * Unified radius→particleType mapping. The single source of truth for
     * determining which explosion particle effect to use based on radius.
     */
    @JvmStatic
    fun particleTypeForRadius(radius: Float): ParticleType {
        return when {
            radius < 2.0 -> ParticleType.MINI
            radius < 4.0 -> ParticleType.SMALL
            radius < 7.0 -> ParticleType.MEDIUM
            radius < 10.0 -> ParticleType.LARGE
            radius < 20.0 -> ParticleType.HUGE
            radius < 30.0 -> ParticleType.GIANT
            else -> ParticleType.EPIC
        }
    }

    @Serializable
    enum class ParticleType {
        @SerializedName("Mini")
        @SerialName("Mini")
        MINI,

        @SerializedName("Small")
        @SerialName("Small")
        SMALL,

        @SerializedName("Medium")
        @SerialName("Medium")
        MEDIUM,

        @SerializedName("Large")
        @SerialName("Large")
        LARGE,

        @SerializedName("Huge")
        @SerialName("Huge")
        HUGE,

        @SerializedName("Giant")
        @SerialName("Giant")
        GIANT,

        @SerializedName("Epic")
        @SerialName("Epic")
        EPIC
    }
}
