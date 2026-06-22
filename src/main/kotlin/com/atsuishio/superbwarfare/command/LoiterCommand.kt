package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.network.chat.Component
import org.joml.Quaternionf

val LOITER_COMMAND = buildCommand("loiter") {
    // 所有乘客均可使用，无需额外权限
    requirePermission(0)

    // 子命令：loiter true/false 开关盘旋
    boolArg("enabled") {
        execute {
            val player = source.player
            if (player == null) {
                fail(Component.translatable("commands.superbwarfare.loiter.player_only"))
                return@execute 0
            }

            val vehicle = player.vehicle as? VehicleEntity
            if (vehicle == null || vehicle.computed().engineType != EngineType.AIRCRAFT) {
                fail(Component.translatable("commands.superbwarfare.loiter.not_aircraft"))
                return@execute 0
            }

            val enabled = boolArg

            if (enabled) {
                // 以载具当前位置为盘旋中心，保持已有半径（无则使用默认500）
                val pos = vehicle.position()
                val currentRadius = vehicle.loiterRadius
                val radius = if (currentRadius > 0) currentRadius else 500.0
                vehicle.loiterParams = Quaternionf(
                    pos.x.toFloat(),
                    pos.y.toFloat(),
                    pos.z.toFloat(),
                    radius.toFloat()
                )
            }

            vehicle.loiterActive = enabled

            success {
                if (enabled) {
                    Component.translatable(
                        "commands.superbwarfare.loiter.enabled",
                        vehicle.loiterCenterX.toInt(),
                        vehicle.loiterCenterY.toInt(),
                        vehicle.loiterCenterZ.toInt(),
                        vehicle.loiterRadius.toInt()
                    )
                } else {
                    Component.translatable("commands.superbwarfare.loiter.disabled")
                }
            }
            return@execute 1
        }
    }

    // 子命令：loiter <x> <y> <z> <radius> —— 设置盘旋参数并自动开启
    intArg("centerX") centerX@{
        intArg("centerY") centerY@{
            intArg("centerZ") centerZ@{
                intArg("radius", min = 200, max = 10000) {
                    execute {
                        val player = source.player
                        if (player == null) {
                            fail(Component.translatable("commands.superbwarfare.loiter.player_only"))
                            return@execute 0
                        }

                        val vehicle = player.vehicle as? VehicleEntity
                        if (vehicle == null || vehicle.computed().engineType != EngineType.AIRCRAFT) {
                            fail(Component.translatable("commands.superbwarfare.loiter.not_aircraft"))
                            return@execute 0
                        }

                        val x = getArg(this@centerX).toFloat()
                        val y = getArg(this@centerY).toFloat()
                        val z = getArg(this@centerZ).toFloat()
                        val r = intArg.toFloat()

                        vehicle.loiterParams = Quaternionf(x, y, z, r)
                        vehicle.loiterActive = true

                        success {
                            Component.translatable(
                                "commands.superbwarfare.loiter.success",
                                x.toInt(), y.toInt(), z.toInt(), r.toInt()
                            )
                        }
                        return@execute 1
                    }
                }
            }
        }
    }
}
