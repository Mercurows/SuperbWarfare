package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.network.chat.Component
import org.joml.Quaternionf

val LOITER_COMMAND = buildCommand("loiter") {
    // 所有乘客均可使用，无需额外权限
    requirePermission(0)

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
