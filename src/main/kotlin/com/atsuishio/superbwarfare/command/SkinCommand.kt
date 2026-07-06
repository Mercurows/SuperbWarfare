package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity

val SKIN_COMMAND = buildCommand("skin") {
    requirePermission(2)

    entityArg("vehicle") {
        "set" {
            stringArg("skinId") {
                execute {
                    val (res, msg) = setSkin(entity, stringArg)
                    if (res) success { msg } else fail { msg }
                    return@execute 0
                }
            }
        }

        "clear" {
            execute {
                val vehicle = entity as? VehicleEntity
                if (vehicle == null) {
                    fail { Component.translatable("commands.superbwarfare.skin.fail.vehicle") }
                    return@execute 0
                }
                vehicle.skinId = ""
                success { Component.translatable("commands.superbwarfare.skin.success.clear", entity.displayName) }
                return@execute 0
            }
        }
    }
}

private fun setSkin(entity: Entity, skinId: String): Pair<Boolean, Component> {
    val vehicle = entity as? VehicleEntity
        ?: return false to Component.translatable("commands.superbwarfare.skin.fail.vehicle")
    if (skinId.isBlank()) {
        return false to Component.translatable("commands.superbwarfare.skin.fail.empty")
    }
    vehicle.skinId = skinId
    return true to Component.translatable("commands.superbwarfare.skin.success.set", entity.displayName, skinId)
}

// Extension: string argument for the command builder DSL
inline fun SingleCommand.stringArg(
    argName: String,
    builder: StringArgCommand.() -> Unit
) {
    cmd += StringArgCommand(
        RequiredArgumentBuilder.argument(argName, StringArgumentType.string()),
        argName
    ).apply(builder)
}

class StringArgCommand(
    val argBuilder: ArgumentBuilder<CommandSourceStack, *>,
    val argName: String
) : SingleCommand(argBuilder, argName) {

    val CommandContext<CommandSourceStack>.stringArg: String
        get() = StringArgumentType.getString(this, argName)
}
