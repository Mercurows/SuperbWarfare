package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.data.gun.Ammo
import net.minecraft.network.chat.Component

// 再见了牛魔Builder
val AMMO_COMMAND = buildCommand("ammo") {
    requirePermission(0)

    "get" {
        playerArg("player") {
            enumArg<Ammo>("type") {
                execute {
                    val player = getPlayer("player")
                    val source = getSource()

                    // 权限不足时，只允许玩家查询自己的弹药数量
                    if (source.isPlayer && !source.hasPermission(2)) {
                        if (source.player != null && source.player?.getUUID() != player.getUUID()) {
                            source.sendFailure(Component.translatable("commands.ammo.no_permission"))
                            return@execute 0
                        }
                    }

                    val type = getArgument<Ammo>("type")
                    val value = type.get(player)
                    source.success {
                        Component.translatable(
                            "commands.ammo.get",
                            Component.translatable(type.translationKey),
                            value
                        )
                    }
                    return@execute 0
                }
            }
        }
    }

    "set" {
        requirePermission(2)

        playersArg("players") {
            enumArg<Ammo>("type") {
                intArg("value") {
                    execute {
                        val players = getPlayers("players")
                        val type = getArgument<Ammo>("type")
                        val value = getInt("value")

                        for (player in players) {
                            type.set(player, value)
                        }

                        getSource().success {
                            Component.translatable(
                                "commands.ammo.set",
                                Component.translatable(type.translationKey),
                                value,
                                players.size
                            )
                        }
                        return@execute 0
                    }
                }
            }
        }
    }

    "add" {
        requirePermission(2)

        playersArg("players") {
            enumArg<Ammo>("type") {
                intArg("value") {
                    execute {
                        val players = getPlayers("players")
                        val type = getArgument<Ammo>("type")
                        val value = getInt("value")

                        for (player in players) {
                            type.add(player, value)
                        }

                        getSource().success {
                            Component.translatable(
                                "commands.ammo.add",
                                Component.translatable(type.translationKey),
                                value,
                                players.size
                            )
                        }
                        return@execute 0
                    }
                }
            }
        }
    }
}
