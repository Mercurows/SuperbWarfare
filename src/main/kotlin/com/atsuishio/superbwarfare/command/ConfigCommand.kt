package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.config.server.*
import com.atsuishio.superbwarfare.network.message.receive.ClientTacticalSprintSyncMessage
import com.atsuishio.superbwarfare.tools.sendPacketToAll
import net.minecraft.network.chat.Component
import net.minecraftforge.common.ForgeConfigSpec
import kotlin.reflect.KProperty0

val CONFIG_COMMAND = buildCommand("config") {
    requirePermission(0)

    buildDestroyTypesCommand()

    // TODO 干掉这些翻译字段
    booleanConfig(
        "tacticalSprint",
        MiscConfig.ALLOW_TACTICAL_SPRINT,
        "commands.config.tactical_sprint"
    ) { sendPacketToAll(ClientTacticalSprintSyncMessage(it)) }

    booleanConfig(SpawnConfig::SPAWN_SENPAI)
    booleanConfig(SpawnConfig::SPAWN_MOB_WITH_GUNS)

    booleanConfig(ExplosionConfig::EXPLOSION_DESTROY, "commands.config.explosion_destroy")
    booleanConfig(ExplosionConfig::EXTRA_EXPLOSION_EFFECT)

    booleanConfig("blockDestroy", ProjectileConfig.ALLOW_PROJECTILE_DESTROY_BLOCKS, "commands.config.block_destroy")

    booleanConfig(VehicleConfig::COLLECT_DROPS_BY_CRASHING)
    booleanConfig(VehicleConfig::VEHICLE_ITEM_PICKUP)
    booleanConfig(VehicleConfig::COLLISION_DESTROY_SOFT_BLOCKS)
    booleanConfig(VehicleConfig::COLLISION_DESTROY_NORMAL_BLOCKS)
    booleanConfig(VehicleConfig::COLLISION_DESTROY_HARD_BLOCKS)
    booleanConfig(VehicleConfig::COLLISION_DESTROY_BLOCKS_BEASTLY)

    booleanConfig("forceDamage", MiscConfig.ALLOW_FORCE_DAMAGE, "commands.config.force_damage")
    booleanConfig(MiscConfig::DROP_AMMO_BOX, "commands.config.drop_ammo_box")
    booleanConfig(MiscConfig::SEND_KILL_FEEDBACK)
    booleanConfig(MiscConfig::MINE_HITBOX_INVISIBLE)
    booleanConfig(MiscConfig::DROP_AMMO_BOX)
    booleanConfig(MiscConfig::SMOKE_HIDE_TARGET)
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

                    success { Component.translatable("commands.config.collision_destroy.${type.commandName}") }

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
    prop: KProperty0<ForgeConfigSpec.BooleanValue>,
    msg: String = "",
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

    booleanConfig(name, prop.get(), msg, effect)
}

private fun SingleCommand.booleanConfig(
    name: String,
    config: ForgeConfigSpec.BooleanValue,
    msg: String = "",
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
                    Component.translatable("$msg.${if (value) "enabled" else "disabled"}")
                }

                return@execute 0
            }
        }
    }
}