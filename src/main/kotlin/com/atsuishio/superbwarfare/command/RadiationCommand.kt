package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.capability.living.RadiationCapability
import com.atsuishio.superbwarfare.command.builder.buildCommand
import com.atsuishio.superbwarfare.command.builder.entityArg
import com.atsuishio.superbwarfare.command.builder.intArg
import com.atsuishio.superbwarfare.mobeffect.RadiationMobEffect
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity

val RADIATION_COMMAND = buildCommand("radiation") {
    requirePermission(2)

    entityArg {
        "get" {
            execute {
                val target = entity as? LivingEntity
                    ?: fail { Component.translatable("commands.superbwarfare.radiation.invalid") }
                val capability = RadiationCapability.getOrNull(target)
                    ?: fail { Component.translatable("commands.superbwarfare.radiation.invalid") }

                success {
                    Component.translatable(
                        "commands.superbwarfare.radiation.get",
                        target.displayName,
                        capability.dosage.toInt()
                    )
                }
            }
        }

        "clear" {
            execute {
                val target = entity as? LivingEntity
                    ?: fail { Component.translatable("commands.superbwarfare.radiation.invalid") }
                RadiationCapability.getOrNull(target)
                    ?: fail { Component.translatable("commands.superbwarfare.radiation.invalid") }

                RadiationMobEffect.clear(target)
                success {
                    Component.translatable("commands.superbwarfare.radiation.clear", target.displayName)
                }
            }
        }

        "set" {
            intArg(min = 0, argName = "dosage") {
                execute {
                    val target = entity as? LivingEntity
                        ?: fail { Component.translatable("commands.superbwarfare.radiation.invalid") }
                    RadiationCapability.getOrNull(target)
                        ?: fail { Component.translatable("commands.superbwarfare.radiation.invalid") }

                    val dosage = RadiationCapability.setDosage(target, intArg.toFloat())

                    success {
                        Component.translatable(
                            "commands.superbwarfare.radiation.set",
                            target.displayName,
                            dosage.toInt()
                        )
                    }
                }
            }
        }
    }
}
