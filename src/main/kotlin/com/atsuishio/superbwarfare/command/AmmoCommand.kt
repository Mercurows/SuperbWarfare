package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.capability.entity.InfiniteAmmoCapability
import com.atsuishio.superbwarfare.command.builder.*
import com.atsuishio.superbwarfare.config.server.AmmoConfig
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
                        if (source.player != null && source.player?.getUUID() != playerArg.getUUID()) {
                            fail { Component.translatable("commands.superbwarfare.ammo.no_permission") }
                        }
                    }

                    val type = enumArg
                    val value = type.get(playerArg)
                    success {
                        Component.translatable(
                            "commands.superbwarfare.ammo.get",
                            Component.translatable(type.translationKey),
                            value
                        )
                    }
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

                        for (player in playersArg) {
                            type.set(player, intArg)
                        }

                        success {
                            Component.translatable(
                                "commands.superbwarfare.ammo.set",
                                Component.translatable(type.translationKey),
                                intArg,
                                playersArg.size
                            )
                        }
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

                        for (player in playersArg) {
                            type.add(player, intArg)
                        }

                        success {
                            Component.translatable(
                                "commands.superbwarfare.ammo.add",
                                Component.translatable(type.translationKey),
                                intArg,
                                playersArg.size
                            )
                        }
                    }
                }
            }
        }
    }

    "limit" {
        "ammo" {
            buildAmmoLimitCommand(false)
        }
        "ammoBox" {
            buildAmmoLimitCommand(true)
        }
    }

    "infinite" {
        "set" {
            entitiesArg {
                boolArg {
                    execute {
                        for (entity in entities) {
                            InfiniteAmmoCapability.modify(entity) {
                                it.hasInfinityAmmo = boolArg
                            }
                        }

                        if (entities.size == 1) {
                            success {
                                Component.translatable(
                                    "commands.superbwarfare.ammo.infinite.set.${if (boolArg) "enabled" else "disabled"}.single",
                                    entities.iterator().next().displayName
                                )
                            }
                        } else {
                            success {
                                Component.translatable(
                                    "commands.superbwarfare.ammo.infinite.set.${if (boolArg) "enabled" else "disabled"}.multiple",
                                    entities.size
                                )
                            }
                        }
                    }
                }
            }
        }

        "get" {
            entityArg {
                execute {
                    val res = InfiniteAmmoCapability.get(entity).hasInfinityAmmo

                    success {
                        Component.translatable(
                            "commands.superbwarfare.ammo.infinite.get", entity.displayName, res
                        )
                    }
                }
            }
        }
    }
}

private fun CommandNode.buildAmmoLimitCommand(isAmmoBox: Boolean) {
    "get" {
        enumArg<Ammo> {
            execute {
                val type = enumArg
                val limit = if (isAmmoBox) type.ammoBoxLimit else type.limit

                success {
                    Component.translatable(
                        "commands.superbwarfare.ammo.limit.get",
                        Component.translatable(type.translationKey),
                        limit
                    )
                }
            }
        }
    }

    "set" {
        requirePermission(2)

        enumArg<Ammo> {
            intArg {
                execute {
                    val type = enumArg
                    val config = (if (isAmmoBox) AmmoConfig.AMMO_BOX_LIMIT[type] else AmmoConfig.AMMO_LIMIT[type])!!
                    config.set(intArg)
                    config.save()

                    success {
                        Component.translatable(
                            "commands.superbwarfare.ammo.limit.set",
                            Component.translatable(type.translationKey),
                            intArg
                        )
                    }
                }
            }
        }
    }
}
