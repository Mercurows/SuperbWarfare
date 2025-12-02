package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.data.gun.Ammo
import net.minecraft.network.chat.Component

// 再见了牛魔Builder
val AMMO_COMMAND = buildCommand("ammo") {
    requirePermission(0)

    "get" {
        playerArg {
            enumArg<Ammo> {
                execute {
                    // 权限不足时，只允许玩家查询自己的弹药数量
                    if (source.isPlayer && !source.hasPermission(2)) {
                        if (source.player != null && source.player?.getUUID() != player.getUUID()) {
                            source.sendFailure(Component.translatable("commands.ammo.no_permission"))
                            return@execute 0
                        }
                    }

                    val type = enumArg
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

        playersArg {
            enumArg<Ammo> {
                intArg {
                    execute {
                        val type = enumArg

                        for (player in players) {
                            type.set(player, intArg)
                        }

                        getSource().success {
                            Component.translatable(
                                "commands.ammo.set",
                                Component.translatable(type.translationKey),
                                intArg,
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

        playersArg {
            enumArg<Ammo> {
                intArg {
                    execute {
                        val type = enumArg

                        for (player in players) {
                            type.add(player, intArg)
                        }

                        getSource().success {
                            Component.translatable(
                                "commands.ammo.add",
                                Component.translatable(type.translationKey),
                                intArg,
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
