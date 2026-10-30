package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.command.builder.buildCommand
import com.atsuishio.superbwarfare.command.builder.entityArg
import com.atsuishio.superbwarfare.command.builder.stringArg
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun_skin.GunSkin
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.gun.GunItem
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

val SKIN_COMMAND = buildCommand("skin") {
    requirePermission(2)

    "gun" {
        entityArg("holder") {
            "set" {
                stringArg("skinId") {
                    execute {
                        val data = mainHandSkinGun(entity)
                            ?: fail { Component.translatable("commands.superbwarfare.skin.fail.gun") }
                        val skin = GunSkin.from(data).getSkin(stringArg)
                            ?.takeIf { it.texture != null }
                            ?: fail { Component.translatable("commands.superbwarfare.skin.fail.gun_skin", stringArg) }
                        data.skinId.set(skin.id)
                        success { Component.translatable("commands.superbwarfare.skin.success.gun.set", entity.displayName, skin.id) }
                    }
                }
            }
            "clear" {
                execute {
                    val data = mainHandSkinGun(entity)
                        ?: fail { Component.translatable("commands.superbwarfare.skin.fail.gun") }
                    data.skinId.set("")
                    success { Component.translatable("commands.superbwarfare.skin.success.gun.clear", entity.displayName) }
                }
            }
        }
    }

    entityArg("vehicle") {
        "set" {
            stringArg("skinId") {
                execute {
                    val (res, msg) = setSkin(entity, stringArg)
                    if (res) success { msg } else fail { msg }
                }
            }
        }

        "clear" {
            execute {
                val vehicle = entity as? VehicleEntity
                    ?: fail { Component.translatable("commands.superbwarfare.skin.fail.vehicle") }
                vehicle.skinId = ""
                success { Component.translatable("commands.superbwarfare.skin.success.clear", entity.displayName) }
            }
        }
    }
}

private fun mainHandSkinGun(entity: Entity): GunData? {
    val stack = (entity as? LivingEntity)?.mainHandItem ?: return null
    return if (stack.item is GunItem) GunData.from(stack) else null
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
