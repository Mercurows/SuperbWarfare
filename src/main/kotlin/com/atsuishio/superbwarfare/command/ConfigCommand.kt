package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.config.server.*
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.common.ModConfigSpec
import kotlin.reflect.KProperty0

val CONFIG_COMMAND = buildCommand("config") {
    requirePermission(0)

    buildDestroyTypesCommand()

    booleanConfig(SpawnConfig::SPAWN_SENPAI)
    booleanConfig(SpawnConfig::SPAWN_MOB_WITH_GUNS)
    booleanConfig(SpawnConfig::SPAWN_STEEL_COIL)

    booleanConfig(ExplosionConfig::EXPLOSION_DESTROY)
    booleanConfig(ExplosionConfig::EXTRA_EXPLOSION_EFFECT)
    booleanConfig(ExplosionConfig::FRIENDLY_MINES)

    booleanConfig(ProjectileConfig::PROJECTILE_DESTROY_BLOCKS)
    booleanConfig(ProjectileConfig::PROJECTILE_CHUNK_LOADING)

    booleanConfig(VehicleConfig::COLLECT_DROPS_BY_CRASHING)
    booleanConfig(VehicleConfig::VEHICLE_ITEM_PICKUP)
    booleanConfig(VehicleConfig::SAME_TEAM_ENTER_VEHICLE)
    booleanConfig(VehicleConfig::VEHICLE_CHUNK_LOADING)
    booleanConfig(VehicleConfig::COLLISION_DESTROY_SOFT_BLOCKS)
    booleanConfig(VehicleConfig::COLLISION_DESTROY_NORMAL_BLOCKS)
    booleanConfig(VehicleConfig::COLLISION_DESTROY_HARD_BLOCKS)
    booleanConfig(VehicleConfig::COLLISION_DESTROY_BLOCKS_BEASTLY)

    booleanConfig(MiscConfig::FORCE_DAMAGE_MODE)
    booleanConfig(MiscConfig::DROP_AMMO_BOX)
    booleanConfig(MiscConfig::SEND_KILL_FEEDBACK)
    booleanConfig(MiscConfig::MINE_HITBOX_INVISIBLE)
    booleanConfig(MiscConfig::SMOKE_HIDE_TARGET)
    booleanConfig(MiscConfig::THROW_MEDICAL_KIT)
    booleanConfig(MiscConfig::SYNC_ENTITY_OVER_RANGE)
}

private enum class DestroyType(
    val commandName: String,
    val soft: Boolean,
    val normal: Boolean,
    val hard: Boolean,
    val beastly: Boolean
) {
    NONE("none", false, false, false, false),
    SOFT("soft", true, false, false, false),
    NORMAL("normal", true, true, false, false),
    HARD("hard", true, true, true, false),
    BEASTLY("beastly", true, true, true, true)
}

private fun SingleCommand.buildDestroyTypesCommand() {
    "collisionDestroy" {
        requirePermission(2)

        DestroyType.entries.forEach { type ->
            type.commandName {
                execute {
                    VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.set(type.soft)
                    VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.set(type.normal)
                    VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.set(type.hard)
                    VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.set(type.beastly)

                    saveCollisionConfigs()

                    success { Component.translatable("commands.superbwarfare.config.collision_destroy.${type.commandName}") }

                    return@execute 0
                }
            }
        }
    }
}

private fun saveCollisionConfigs() {
    VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.save()
    VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.save()
    VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.save()
    VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.save()
}

private fun SingleCommand.booleanConfig(
    prop: KProperty0<ModConfigSpec.BooleanValue>,
    effect: (Boolean) -> Unit = {}
) {
    val name = buildString {
        val propName = prop.name
        append(propName[0].lowercase())

        var isUpperCase = false
        for (i in 1..<propName.length) {
            val c = propName[i]
            if (c == '_') {
                isUpperCase = true
                continue
            }

            if (isUpperCase) {
                append(c.uppercase())
                isUpperCase = false
            } else {
                append(c.lowercase())
            }
        }
    }

    booleanConfig(name, prop.get(), effect)
}

private fun SingleCommand.booleanConfig(
    name: String,
    config: ModConfigSpec.BooleanValue,
    effect: (Boolean) -> Unit = {}
) {
    name {
        requirePermission(2)

        boolArg {
            execute {
                val value = boolArg
                config.set(value)
                config.save()

                effect(value)

                success {
                    Component.translatable(
                        "commands.superbwarfare.config.${if (value) "enabled" else "disabled"}",
                        name
                    )
                }

                return@execute 0
            }
        }
    }
}