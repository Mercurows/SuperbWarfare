package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.world.TDMSavedData
import net.minecraft.network.chat.Component
import net.minecraft.world.level.saveddata.SavedData

val TDM_COMMAND = buildCommand("tdm") {
    requirePermission(2)

    "add" {
        entitiesArg {
            execute {
                val tdm = source.level.dataStorage.computeIfAbsent(
                    SavedData.Factory(
                        { TDMSavedData() },
                        { tag, registries -> TDMSavedData.load(tag, registries) },
                        null
                    ), TDMSavedData.FILE_ID
                )

                entities.forEach { entity -> tdm.addEntity(entity.getStringUUID()) }
                tdm.sync()

                // TODO 解决显示问题
                success {
                    if (entities.size == 1) {
                        Component.translatable(
                            "commands.tdm.add.single",
                            entities.iterator().next().displayName
                        )
                    } else {
                        Component.translatable("commands.tdm.add.multiple", entities.size)
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
                    SavedData.Factory(
                        { TDMSavedData() },
                        { tag, registries -> TDMSavedData.load(tag, registries) },
                        null
                    ), TDMSavedData.FILE_ID
                )

                entities.forEach { entity -> tdm.removeEntity(entity.getStringUUID()) }
                tdm.sync()

                if (entities.size == 1) {
                    success {
                        Component.translatable("commands.tdm.remove.single", entities.iterator().next())
                    }
                } else {
                    success { Component.translatable("commands.tdm.remove.multiple", entities.size) }
                }

                return@execute 0
            }
        }
    }
}
