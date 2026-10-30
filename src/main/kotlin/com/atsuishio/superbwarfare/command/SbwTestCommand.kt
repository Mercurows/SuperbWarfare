package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.command.builder.buildCommand
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.physics.VehiclePhysics
import com.atsuishio.superbwarfare.physics.tracked.TrackedTerrainDebugService
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

/**
 * ```
 * /sbwtest bt show
 * /sbwtest bt hide
 * /sbwtest bt status
 * ```
 *
 * `show` draws the installed Bullet mesh, hull OBBs, wheel cylinders and run
 * boxes for every tracked vehicle already loaded in this dimension. It does
 * not require a passenger, and it is a viewer, not a terrain rebuild.
 */
val SBWTEST_COMMAND = buildCommand("sbwtest") {
    requirePermission(2)

    "bt" {
        "show" {
            execute { showBulletTerrain(this) }
        }
        "hide" {
            execute { hideBulletTerrain(this) }
        }
        "status" {
            execute { bulletTerrainStatus(this) }
        }
    }
}

private fun CommandContext<CommandSourceStack>.player(): ServerPlayer? = source.entity as? ServerPlayer

private fun CommandContext<CommandSourceStack>.riddenTracked(): VehicleEntity? {
    val riding = player()?.vehicle as? VehicleEntity ?: return null
    return riding.takeIf { VehiclePhysics.trackedRuntime(it) != null || it.computed().engineType == EngineType.TRACK }
}

private fun showBulletTerrain(context: CommandContext<CommandSourceStack>): Int {
    val player = context.player() ?: return fail(context, "需要由玩家执行")
    val message = TrackedTerrainDebugService.show(player)
    context.source.sendSuccess({ Component.literal(message) }, false)
    return 1
}

private fun hideBulletTerrain(context: CommandContext<CommandSourceStack>): Int {
    val player = context.player() ?: return fail(context, "需要由玩家执行")
    TrackedTerrainDebugService.hide(player)
    context.source.sendSuccess({ Component.literal("已关闭 Bullet 地形线框") }, false)
    return 1
}

private fun bulletTerrainStatus(context: CommandContext<CommandSourceStack>): Int {
    val player = context.player() ?: return fail(context, "需要由玩家执行")
    for (line in TrackedTerrainDebugService.status(player, context.riddenTracked())) {
        context.source.sendSuccess({ Component.literal(line) }, false)
    }
    return 1
}

private fun fail(context: CommandContext<CommandSourceStack>, message: String): Int {
    context.source.sendFailure(Component.literal(message))
    return 0
}
