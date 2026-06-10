package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.client.particle.CannonMuzzleFlareOption
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.client.particle.CustomFlareOption
import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage.Companion.sendToNearbyPlayers
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
import net.minecraft.world.level.block.Blocks
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

        when (type) {
            ParticleType.MINI -> spawnMiniExplosionParticles(level, pos)
            ParticleType.SMALL -> spawnSmallExplosionParticles(level, pos)
            ParticleType.MEDIUM -> spawnMediumExplosionParticles(level, pos)
            ParticleType.LARGE -> spawnLargeExplosionParticles(level, pos)
            ParticleType.HUGE -> spawnHugeExplosionParticles(level, pos)
            ParticleType.GIANT -> spawnGiantExplosionParticles(level, pos)
            ParticleType.EPIC -> spawnEpicExplosionParticles(level, pos)
        }
    }

    //@formatter:off
    @JvmStatic
    fun spawnMiniExplosionParticles(level: Level, pos: Vec3) {
        val x = pos.x
        val y = pos.y
        val z = pos.z

        if (level is ServerLevel) {
            level.playSound(null, BlockPos.containing(x, y + 1, z), ModSounds.MINI_EXPLOSION.get(), SoundSource.BLOCKS, 4f, 1f)
            sendParticle(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 2, 0.1, 0.1, 0.1, 0.02, true)
            sendParticle(level, ParticleTypes.EXPLOSION, x, y, z, 2, 0.05, 0.05, 0.05, 1.0, true)
            sendParticle(level, ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.2, 0.2, 0.2, 0.02, true)
            sendParticle(level, ModParticleTypes.FIRE_STAR.get(), x, y, z, 7, 0.0, 0.0, 0.0, 0.4, true)
            sendParticle(level, ParticleTypes.FLASH, x, y, z, 1, 0.0, 0.0, 0.0, 20.0, true)
        }
    }

    @JvmStatic
    fun spawnSmallExplosionParticles(level: Level?, pos: Vec3) {
        val x = pos.x
        val y = pos.y
        val z = pos.z

        if (level is ServerLevel) {
            playDistantSound(level, ModSounds.EXPLOSION_CLOSE.get(), pos, 2f, 1f, null)
            playDistantSound(level, ModSounds.EXPLOSION_FAR.get(), pos, 8f, 1f, null)
            playDistantSound(level, ModSounds.EXPLOSION_VERY_FAR.get(), pos, 32f, 1f, null)

            sendParticle(level, ParticleTypes.EXPLOSION, x, y, z, 2, 0.05, 0.05, 0.05, 1.0, true)
            sendParticle(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 3, 0.1, 0.1, 0.1, 0.02, true)
            sendParticle(level, ParticleTypes.LARGE_SMOKE, x, y, z, 4, 0.2, 0.2, 0.2, 0.02, true)
            sendParticle(level, ModParticleTypes.FIRE_STAR.get(), x, y, z, 12, 0.0, 0.0, 0.0, 0.6, true)
            sendParticle(level, ParticleTypes.FLASH, x, y, z, 3, 0.1, 0.1, 0.1, 20.0, true)
        }
    }

    @JvmStatic
    fun spawnMediumExplosionParticles(level: Level?, pos: Vec3) {
        val x = pos.x
        val y = pos.y
        val z = pos.z

        if (level is ServerLevel) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).block === Blocks.WATER) {
                sendParticle(level, ParticleTypes.CLOUD, x, y + 3, z, 20, 1.0, 3.0, 1.0, 0.01, true)
                sendParticle(level, ParticleTypes.CLOUD, x, y + 3, z, 30, 2.0, 1.0, 2.0, 0.01, true)
                sendParticle(level, ParticleTypes.FALLING_WATER, x, y + 3, z, 50, 1.5, 4.0, 1.5, 1.0, true)
                sendParticle(level, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 60, 3.0, 0.5, 3.0, 0.1, true)
            }

            playDistantSound(level, ModSounds.EXPLOSION_CLOSE.get(), pos, 4f, 1f, null)
            playDistantSound(level, ModSounds.EXPLOSION_FAR.get(), pos, 16f, 1f, null)
            playDistantSound(level, ModSounds.EXPLOSION_VERY_FAR.get(), pos, 32f, 1f, null)

            sendParticle(level, ParticleTypes.EXPLOSION, x, y + 1, z, 5, 0.7, 0.7, 0.7, 1.0, true)
            sendParticle(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 20, 0.2, 1.0, 0.2, 0.02, true)
            sendParticle(level, ParticleTypes.LARGE_SMOKE, x, y + 1, z, 10, 0.4, 1.0, 0.4, 0.02, true)
            sendParticle(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.25, z, 40, 2.0, 0.001, 2.0, 0.01, true)
            sendParticle(level, ModParticleTypes.FIRE_STAR.get(), x, y + 0.2, z, 50, 0.0, 0.0, 0.0, 0.8, true)
            sendParticle(level, ParticleTypes.FLASH, x, y + 0.5, z, 50, 0.2, 0.2, 0.2, 20.0, true)
            sendParticle(level, CustomFlareOption(0.75f, 0.375f, 0f, 60, 0.8f, 1, 0.2f), x, y + 0.5, z, 50, 0.75, 1.2, 0.75, 0.05, true)
            sendParticle(level, CustomFlareOption(0.25f, 0.125f, 0f, 100, 0.91f, 3, 0.25f), x, y + 0.5, z, 50, 0.75, 1.2, 0.75, 0.05, true)
        }
    }

    @JvmStatic
    fun spawnLargeExplosionParticles(level: Level?, pos: Vec3) {
        val x = pos.x
        val y = pos.y
        val z = pos.z

        if (level is ServerLevel) {
            if ((level.getBlockState(BlockPos.containing(x, y, z))).block === Blocks.WATER) {
                sendParticle(level, ParticleTypes.CLOUD, x, y + 3, z, 100, 2.0, 6.0, 2.0, 0.01, true)
                sendParticle(level, ParticleTypes.CLOUD, x, y + 3, z, 200, 4.0, 2.0, 4.0, 0.01, true)
                sendParticle(level, ParticleTypes.FALLING_WATER, x, y + 3, z, 500, 3.0, 8.0, 3.0, 1.0, true)
                sendParticle(level, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 350, 6.0, 1.0, 6.0, 0.1, true)
            }

            playDistantSound(level, ModSounds.HUGE_EXPLOSION_CLOSE.get(), pos, 6f, 1f, null)
            playDistantSound(level, ModSounds.HUGE_EXPLOSION_FAR.get(), pos, 20f, 1f, null)
            playDistantSound(level, ModSounds.HUGE_EXPLOSION_VERY_FAR.get(), pos, 64f, 1f, null)

            sendParticle(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 60, 0.5, 2.0, 0.5, 0.02, true)
            sendParticle(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.25, z, 120, 5.0, 0.001, 5.0, 0.01, true)
            sendParticle(level, ModParticleTypes.FIRE_STAR.get(), x, y + 0.2, z, 100, 0.0, 0.0, 0.0, 1.2, true)
            sendParticle(level, ParticleTypes.EXPLOSION, x, y + 1, z, 35, 1.5, 1.5, 1.5, 1.0, true)
            sendParticle(level, ParticleTypes.FLASH, x, y + 1, z, 120, 3.0, 3.0, 3.0, 20.0, true)
            sendParticle(level, CustomFlareOption(0.75f, 0.375f, 0f, 60, 0.8f, 1, 0.2f), x, y + 1, z, 150, 1.3, 1.7, 1.3, 0.05, true)
            sendParticle(level, CustomFlareOption(0.25f, 0.125f, 0f, 100, 0.91f, 3, 0.25f), x, y + 1, z, 80, 1.3, 1.7, 1.3, 0.05, true)

            for (h in 0..1) {
                for (i in 0..149) {
                    val v = Vec3(1.0, 0.0, 0.0).yRot((i * Math.random()).toFloat())
                    sendParticle(
                        level, CustomCloudOption(0xFFFFFF, 25, 2f, 0f, cooldown = false, light = false), x, y + 0.2, z,
                        0, v.x, v.y, v.z, (140 - 2 * h).toDouble(), true
                    )
                }
            }
        }
    }

    @JvmStatic
    fun spawnHugeExplosionParticles(level: Level?, pos: Vec3) {
        val x = pos.x
        val y = pos.y
        val z = pos.z

        if (level is ServerLevel) {
            playDistantSound(level, ModSounds.HUGE_EXPLOSION_CLOSE.get(), pos, 8f, 1f, null)
            playDistantSound(level, ModSounds.HUGE_EXPLOSION_FAR.get(), pos, 24f, 1f, null)
            playDistantSound(level, ModSounds.HUGE_EXPLOSION_VERY_FAR.get(), pos, 128f, 1f, null)

            if ((level.getBlockState(BlockPos.containing(x, y, z))).block === Blocks.WATER) {
                sendParticle(level, ParticleTypes.CLOUD, x, y + 3, z, 100, 2.0, 6.0, 2.0, 0.01, true)
                sendParticle(level, ParticleTypes.CLOUD, x, y + 3, z, 200, 4.0, 2.0, 4.0, 0.01, true)
                sendParticle(level, ParticleTypes.FALLING_WATER, x, y + 3, z, 500, 3.0, 8.0, 3.0, 1.0, true)
                sendParticle(level, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 350, 6.0, 1.0, 6.0, 0.1, true)
            }

            for (h in 0..3) {
                for (i in 0..199) {
                    val v = Vec3(1.0, 0.0, 0.0).yRot((i * Math.random()).toFloat())
                    sendParticle(
                        level, CustomCloudOption(0xFFFFFF, 25, 4f, 0f, cooldown = false, light = false), x, y + 0.5, z,
                        0, v.x, v.y, v.z, (200 - 2 * h).toDouble(), true
                    )
                }
            }

            sendParticle(level, ParticleTypes.EXPLOSION, x, y + 3, z, 75, 2.5, 2.5, 2.5, 1.0, true)
            sendParticle(level, ParticleTypes.FLASH, x, y + 3, z, 200, 5.0, 5.0, 5.0, 20.0, true)
            sendParticle(level, ModParticleTypes.FIRE_STAR.get(), x, y + 1, z, 400, 0.0, 0.0, 0.0, 1.5, true)
            sendParticle(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 3, z, 75, 2.0, 3.0, 2.0, 0.005, true)
            sendParticle(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 150, 7.0, 0.1, 7.0, 0.005, true)
            sendParticle(level, CustomFlareOption(0.75f, 0.375f, 0f, 60, 0.8f, 1, 0.2f), x, y + 1, z, 200, 1.5, 2.0, 1.5, 0.05, true)
            sendParticle(level, CustomFlareOption(0.25f, 0.125f, 0f, 100, 0.91f, 3, 0.25f), x, y + 1, z, 100, 1.5, 2.0, 1.5, 0.05, true)

            sendToNearbyPlayers(level, x, y, z, 192.0, 30.0, 12.0)
        }
    }

    @JvmStatic
    fun spawnGiantExplosionParticles(level: Level?, pos: Vec3) {
        val x = pos.x
        val y = pos.y
        val z = pos.z

        if (level is ServerLevel) {
            playDistantSound(level, ModSounds.HUGE_EXPLOSION_CLOSE.get(), pos, 12f, 1f, null)
            playDistantSound(level, ModSounds.HUGE_EXPLOSION_FAR.get(), pos, 32f, 1f, null)
            playDistantSound(level, ModSounds.HUGE_EXPLOSION_VERY_FAR.get(), pos, 192f, 1f, null)

            if ((level.getBlockState(BlockPos.containing(x, y, z))).block === Blocks.WATER) {
                sendParticle(level, ParticleTypes.CLOUD, x, y + 3, z, 100, 2.0, 6.0, 2.0, 0.01, true)
                sendParticle(level, ParticleTypes.CLOUD, x, y + 3, z, 200, 4.0, 2.0, 4.0, 0.01, true)
                sendParticle(level, ParticleTypes.FALLING_WATER, x, y + 3, z, 500, 3.0, 8.0, 3.0, 1.0, true)
                sendParticle(level, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 350, 6.0, 1.0, 6.0, 0.1, true)
            }

            sendParticle(level, ParticleTypes.EXPLOSION, x, y + 6, z, 100, 6.0, 6.0, 6.0, 1.0, true)
            sendParticle(level, ParticleTypes.FLASH, x, y + 7, z, 200, 7.0, 7.0, 7.0, 1.0, true)
            sendParticle(level, ModParticleTypes.FIRE_STAR.get(), x, y + 3, z, 800, 0.0, 0.0, 0.0, 2.0, true)
            sendParticle(level, CustomFlareOption(1f, 1f, 1f, 10, 0.75f, 1, 2f), x, y + 3, z, 1000, 10.0, 10.0, 10.0, 0.005, true)

            for (h in 0..4) {
                for (i in 0..199) {
                    val v = Vec3(1.0, 0.0, 0.0).yRot((i * Math.random()).toFloat())
                    sendParticle(
                        level, CustomCloudOption(1f, 1f, 1f, 25, 4f, 0f, cooldown = false, light = false), x, y + 1, z,
                        0, v.x, v.y, v.z, (1000 - 3 * h).toDouble(), true
                    )
                }
            }

            for (i in 0..23) {
                queueServerWork(i) {
                    if (i < 12) {
                        sendParticle(level, CustomFlareOption(0.6f - (i.toFloat() / 24), 0.3f - (i.toFloat() / 48), 0f, 140, 0.95f, 4, 0.075f), x, y + 2 * i, z, 35, 2 - 0.1 * i, 0.5, 2 - 0.1 * i, 0.005, true)
                        sendParticle(level, CustomFlareOption(0.4f - (i.toFloat() / 48), 0.2f - (i.toFloat() / 96), 0f, 180, 0.96f, 4, 0.075f), x, y + 0.5, z, 55, 3 + 0.5 * i, 0.3, 3 + 0.5 * i, 0.005, true)
                    }
                    if (i in 8..<16) {
                        val k = i - 8
                        sendParticle(level, CustomFlareOption(0.25f, 0.125f, 0f, 100, 0.95f, 2, 0.075f), x, y + 20, z, 20 * k, 1 + 0.5 * k, 1 + 0.2 * k, 1 + 0.5 * k, 0.005, true)
                        sendParticle(level, CustomFlareOption(0.125f, 0.0625f, 0f, 150, 0.95f, 3, 0.01f), x, y + 20, z, 10 * k, 1 + 0.5 * k, 1 + 0.2 * k, 1 + 0.5 * k, 0.005, true)
                        sendParticle(level, CustomFlareOption(0.0625f, 0.03125f, 0f, 200, 0.95f, 4, 0.15f), x, y + 20, z, 10 * k, 1 + 0.5 * k, 1 + 0.2 * k, 1 + 0.5 * k, 0.005, true)
                    }
                    sendParticle(level, CustomFlareOption(0.667f, 0.631f, 0.592f, 100, 0.97f, 10, 0.06f), x, y - 1, z, 6 * i, i.toDouble(), 0.05, i.toDouble(), 0.0003 * i, true)
                }
            }
            sendToNearbyPlayers(level, x, y, z, 384.0, 30.0, 16.0)
        }
    }

    @JvmStatic
    fun spawnEpicExplosionParticles(level: Level?, pos: Vec3) {
        val x = pos.x
        val y = pos.y
        val z = pos.z

        if (level is ServerLevel) {
            playDistantSound(level, ModSounds.EPIC_EXPLOSION_CLOSE.get(), pos, 24f, 1f, null)
            playDistantSound(level, ModSounds.EPIC_EXPLOSION_FAR.get(), pos, 38f, 1f, null)
            playDistantSound(level, ModSounds.EPIC_EXPLOSION_VERY_FAR.get(), pos, 132f, 1f, null)

            if ((level.getBlockState(BlockPos.containing(x, y, z))).block === Blocks.WATER) {
                sendParticle(level, ParticleTypes.CLOUD, x, y + 3, z, 100, 2.0, 6.0, 2.0, 0.01, true)
                sendParticle(level, ParticleTypes.CLOUD, x, y + 3, z, 200, 4.0, 2.0, 4.0, 0.01, true)
                sendParticle(level, ParticleTypes.FALLING_WATER, x, y + 3, z, 500, 3.0, 8.0, 3.0, 1.0, true)
                sendParticle(level, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 350, 6.0, 1.0, 6.0, 0.1, true)
            }

            sendParticle(level, ParticleTypes.EXPLOSION, x, y + 6, z, 450, 12.0, 12.0, 12.0, 1.0, true)
            sendParticle(level, ParticleTypes.FLASH, x, y + 7, z, 600, 13.0, 13.0, 13.0, 1.0, true)
            sendParticle(level, ModParticleTypes.FIRE_STAR.get(), x, y + 8, z, 1800, 0.0, 0.0, 0.0, 4.0, true)
            sendParticle(level, CustomFlareOption(1f, 1f, 1f, 10, 0.75f, 1, 2f), x, y + 3, z, 2500, 18.0, 18.0, 18.0, 0.005, true)

            for (h in 0..4) {
                for (i in 0..450) {
                    val v = Vec3(1.0, 0.0, 0.0).yRot((i * Math.random()).toFloat())
                    sendParticle(
                        level, CustomCloudOption(1f, 1f, 1f, 45, 9f, 0f, cooldown = false, light = false), x, y + 1, z,
                        0, v.x, v.y, v.z, (900 - 3 * h).toDouble(), true
                    )
                }
            }

            for (i in 0..23) {
                queueServerWork(i) {
                    if (i < 12) {
                        sendParticle(level, CustomFlareOption(0.6f - (i.toFloat() / 24), 0.3f - (i.toFloat() / 48), 0f, 140, 0.95f, 4, 0.075f), x, y + 5 * i, z, 60, 4.5 - 0.15 * i, 1.2, 4.5 - 0.15 * i, 0.005, true)
                        sendParticle(level, CustomFlareOption(0.4f - (i.toFloat() / 48), 0.2f - (i.toFloat() / 96), 0f, 180, 0.96f, 4, 0.075f), x, y + 0.5, z, 80, 6 + 0.9 * i, 1.2, 6 + 0.9 * i, 0.005, true)
                    }
                    if (i in 8..<16) {
                        val k = i - 8
                        sendParticle(level, CustomFlareOption(0.25f, 0.125f, 0f, 100, 0.95f, 2, 0.075f), x, y + 50, z, 60 * k, 3 + 1.3 * k, 2.2 + 0.5 * k, 3 + 1.3 * k, 0.005, true)
                        sendParticle(level, CustomFlareOption(0.125f, 0.0625f, 0f, 150, 0.95f, 3, 0.01f), x, y + 50, z, 30 * k, 3 + 1.3 * k, 2.2 + 0.5 * k, 3 + 1.3 * k, 0.005, true)
                        sendParticle(level, CustomFlareOption(0.0625f, 0.03125f, 0f, 200, 0.95f, 4, 0.15f), x, y + 50, z, 30 * k, 3 + 1.3 * k, 2.2 + 0.5 * k, 3 + 1.3 * k, 0.005, true)
                    }
                    sendParticle(level, CustomFlareOption(0.667f, 0.631f, 0.592f, 100, 0.97f, 10, 0.06f), x, y - 1, z, 18 * i, i.toDouble() * 2.5, 0.05, i.toDouble() * 2.5, 0.0003 * i, true)
                }
            }
            sendToNearbyPlayers(level, x, y, z, 768.0, 54.0, 9.0)
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
