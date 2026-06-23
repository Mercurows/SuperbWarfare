package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.chunk.ChunkStatus
import net.minecraft.world.level.levelgen.Heightmap
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

    // 子命令：loiter set <x> <y> <z> <radius> [force] —— 设置盘旋参数并自动开启
    // 默认会检查 Y 值是否低于地形高度，若低于则自动抬升至地形+50
    // 末尾加 force 可跳过地形检查，强制使用输入的 Y 值
    "set" {
        intArg("centerX") centerX@{
            intArg("centerY") centerY@{
                intArg("centerZ") centerZ@{
                    intArg("radius", min = 200, max = 10000) {
                        // 无 force → 地形安全检查
                        execute {
                            val player = source.player ?: run {
                                fail(Component.translatable("commands.superbwarfare.loiter.player_only"))
                                return@execute 0
                            }
                            val vehicle = player.vehicle as? VehicleEntity
                            if (vehicle == null || vehicle.computed().engineType != EngineType.AIRCRAFT) {
                                fail(Component.translatable("commands.superbwarfare.loiter.not_aircraft"))
                                return@execute 0
                            }

                            val x = getArg(this@centerX).toFloat()
                            val z = getArg(this@centerZ).toFloat()
                            val r = intArg.toFloat()
                            val safeY = resolveSafeY(vehicle, x.toInt(), getArg(this@centerY).toFloat().toInt(), z.toInt())

                            vehicle.loiterParams = Quaternionf(x, safeY, z, r)
                            vehicle.loiterActive = true

                            success {
                                val hint = if (safeY != getArg(this@centerY).toFloat()) " §7(已自动抬升至地形+50)" else ""
                                Component.translatable(
                                    "commands.superbwarfare.loiter.success",
                                    x.toInt(), safeY.toInt(), z.toInt(), r.toInt()
                                ).append(hint)
                            }
                            return@execute 1
                        }

                        // force → 跳过地形检查，强制使用输入值
                        "force" {
                            execute {
                                val player = source.player ?: run {
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
    }

    // 子命令：loiter get —— 查看当前盘旋参数
    "get" {
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
            success {
                Component.translatable(
                    "commands.superbwarfare.loiter.get",
                    vehicle.loiterCenterX.toInt(),
                    vehicle.loiterCenterY.toInt(),
                    vehicle.loiterCenterZ.toInt(),
                    vehicle.loiterRadius.toInt(),
                    if (vehicle.loiterActive) "§aON" else "§cOFF"
                )
            }
            return@execute 1
        }
    }

    // 子命令：loiter edit <X|Y|Z|R> <value> [force] —— 实时调整单个盘旋参数
    // 默认自动检查 Y 值安全性，加 force 跳过检查
    "edit" {
        // loiter edit X <value> [force]
        "X" {
            intArg("value") {
                execute {
                    editLoiterParam(source.player, { fail(it) }, newX = intArg.toFloat())
                }
                "force" {
                    execute {
                        editLoiterParam(source.player, { fail(it) }, newX = intArg.toFloat(), skipTerrain = true)
                    }
                }
            }
        }
        // loiter edit Y <value> [force]
        "Y" {
            intArg("value") {
                execute {
                    editLoiterParam(source.player, { fail(it) }, newY = intArg.toFloat())
                }
                "force" {
                    execute {
                        editLoiterParam(source.player, { fail(it) }, newY = intArg.toFloat(), skipTerrain = true)
                    }
                }
            }
        }
        // loiter edit Z <value> [force]
        "Z" {
            intArg("value") {
                execute {
                    editLoiterParam(source.player, { fail(it) }, newZ = intArg.toFloat())
                }
                "force" {
                    execute {
                        editLoiterParam(source.player, { fail(it) }, newZ = intArg.toFloat(), skipTerrain = true)
                    }
                }
            }
        }
        // loiter edit R <value>（半径无地形检查，无需 force）
        "R" {
            intArg("value", min = 200, max = 10000) {
                execute {
                    editLoiterParam(source.player, { fail(it) }, newR = intArg.toFloat())
                }
            }
        }
    }
}

/**
 * 编辑单个盘旋参数，自动检查 Y 值安全性。
 * 若修改 X/Z 后新位置地形高于当前 Y，或手动修改 Y 低于地形，
 * 均自动抬升至地形+50。
 * 不改变 loiterActive 状态。
 * @param failCb 失败回调，用于发送错误消息
 */
private fun editLoiterParam(
    player: net.minecraft.world.entity.player.Player?,
    failCb: (net.minecraft.network.chat.Component) -> Unit,
    newX: Float? = null,
    newY: Float? = null,
    newZ: Float? = null,
    newR: Float? = null,
    skipTerrain: Boolean = false
): Int {
    if (player == null) {
        failCb(Component.translatable("commands.superbwarfare.loiter.player_only"))
        return 0
    }

    val vehicle = player.vehicle as? VehicleEntity
    if (vehicle == null || vehicle.computed().engineType != EngineType.AIRCRAFT) {
        failCb(Component.translatable("commands.superbwarfare.loiter.not_aircraft"))
        return 0
    }

    val lp = vehicle.loiterParams
    val finalX = newX ?: lp.x()
    val finalZ = newZ ?: lp.z()
    val rawY = newY ?: lp.y()

    val safeY = if (skipTerrain) rawY
                else resolveSafeY(vehicle, finalX.toInt(), rawY.toInt(), finalZ.toInt())

    vehicle.loiterParams = org.joml.Quaternionf(
        finalX, safeY, finalZ,
        newR ?: lp.w()
    )

    val hint = if (safeY != rawY) " §7(Y已自动抬升)" else ""
    player.sendSystemMessage(
        net.minecraft.network.chat.Component.translatable(
            "commands.superbwarfare.loiter.edit",
            vehicle.loiterCenterX.toInt(),
            vehicle.loiterCenterY.toInt(),
            vehicle.loiterCenterZ.toInt(),
            vehicle.loiterRadius.toInt()
        ).append(hint)
    )
    return 1
}

/**
 * 获取安全的盘旋 Y 值。若指定 Y 低于 (X,Z) 处地形高度，
 * 则自动抬升至地形最高点上方 50 格。
 * 若区块未加载则临时强制加载，利用 MC 自然卸载机制释放。
 */
internal fun resolveSafeY(vehicle: VehicleEntity, x: Int, y: Int, z: Int): Float {
    val level = vehicle.level()
    val chunkX = x shr 4
    val chunkZ = z shr 4

    // 区块未加载则临时强制加载以获取地形高度
    if (!level.hasChunk(chunkX, chunkZ)) {
        if (level is ServerLevel) {
            level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true)
        } else {
            return y.toFloat() // 客户端无法强制加载，保持原值
        }
    }

    val terrainY = if (level.hasChunk(chunkX, chunkZ)) {
        level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z).toFloat()
    } else {
        return y.toFloat()
    }

    return if (y < terrainY) terrainY + 50f else y.toFloat()
}
