package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.world.TDMSavedData
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel

val TDM_COMMAND = buildCommand("tdm") {
    requirePermission(2)

    "add" {
        entitiesArg("entities") {
            execute {
                val level: ServerLevel = getSource().level
                val entities = getEntities("entities")

                val tdm = level.dataStorage.computeIfAbsent(
                    { tag -> TDMSavedData.load(tag) },
                    { TDMSavedData() },
                    TDMSavedData.FILE_ID
                )

                entities.forEach { entity -> tdm.addEntity(entity.getStringUUID()) }
                tdm.sync()

                // TODO 解决显示问题
                getSource().success {
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
        entitiesArg("entities") {
            execute {
                val level = getSource().level
                val entities = getEntities("entities")

                val tdm = level.dataStorage.computeIfAbsent(
                    { tag -> TDMSavedData.load(tag) },
                    { TDMSavedData() },
                    TDMSavedData.FILE_ID
                )

                entities.forEach { entity -> tdm.removeEntity(entity.getStringUUID()) }
                tdm.sync()

                if (entities.size == 1) {
                    getSource().success {
                        Component.translatable("commands.tdm.remove.single", entities.iterator().next())
                    }
                } else {
                    getSource().success { Component.translatable("commands.tdm.remove.multiple", entities.size) }
                }

                return@execute 0
            }
        }
    }
}
