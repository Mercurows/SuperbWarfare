package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.world.saveddata.TDMSavedData
import net.minecraft.network.chat.Component

val TDM_COMMAND = buildCommand("tdm") {
    requirePermission(2)

    "add" {
        entitiesArg {
            execute {
                val tdm = source.level.dataStorage.computeIfAbsent(
                    { tag -> TDMSavedData.load(tag) },
                    { TDMSavedData() },
                    TDMSavedData.FILE_ID
                )

                entities.forEach { entity -> tdm.addEntity(entity.getStringUUID()) }
                tdm.sync()

                success {
                    if (entities.size == 1) {
                        Component.translatable(
                            "commands.superbwarfare.tdm.add.single",
                            entities.iterator().next().displayName
                        )
                    } else {
                        Component.translatable("commands.superbwarfare.tdm.add.multiple", entities.size)
                    }
                }

                return@execute 0
            }
        }
    }

    "remove" {
        entitiesArg {
            execute {
                val tdm = source.level.dataStorage.computeIfAbsent(
                    { tag -> TDMSavedData.load(tag) },
                    { TDMSavedData() },
                    TDMSavedData.FILE_ID
                )

                entities.forEach { entity -> tdm.removeEntity(entity.getStringUUID()) }
                tdm.sync()

                if (entities.size == 1) {
                    success {
                        Component.translatable("commands.superbwarfare.tdm.remove.single", entities.iterator().next())
                    }
                } else {
                    success { Component.translatable("commands.superbwarfare.tdm.remove.multiple", entities.size) }
                }

                return@execute 0
            }
        }
    }
}
