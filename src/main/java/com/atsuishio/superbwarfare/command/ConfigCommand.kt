package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.network.NetworkRegistry
import com.atsuishio.superbwarfare.network.message.receive.ClientTacticalSprintSyncMessage
import net.minecraft.network.chat.Component
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.network.PacketDistributor

val CONFIG_COMMAND = buildCommand("config") {
    requirePermission(0)

    buildDestroyTypesCommand()

    booleanConfig(
        "tacticalSprint",
        MiscConfig.ALLOW_TACTICAL_SPRINT,
        "commands.config.tactical_sprint"
    ) { NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), ClientTacticalSprintSyncMessage(it)) }

    booleanConfig("explosionDestroy", ExplosionConfig.EXPLOSION_DESTROY, "commands.config.explosion_destroy")
    booleanConfig("blockDestroy", ProjectileConfig.ALLOW_PROJECTILE_DESTROY_BLOCKS, "commands.config.block_destroy")
    booleanConfig("forceDamage", MiscConfig.ALLOW_FORCE_DAMAGE, "commands.config.force_damage")
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

                    source.success { Component.translatable("commands.config.collision_destroy.${type.commandName}") }

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
    name: String,
    config: ForgeConfigSpec.BooleanValue,
    msg: String,
    effect: (Boolean) -> Unit = {}
) {
    name {
        requirePermission(2)

        boolArg("value") {
            execute {
                val value = boolArg
                config.set(value)
                config.save()

                effect(value)

                getSource().success {
                    Component.translatable("$msg.${if (value) "enabled" else "disabled"}")
                }

                return@execute 0
            }
        }
    }
}