package com.atsuishio.superbwarfare.world.saveddata

import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.network.message.receive.TDMSyncMessage
import com.atsuishio.superbwarfare.tools.sendPacketTo
import com.atsuishio.superbwarfare.tools.sendPacketToAll
import com.google.common.collect.Sets
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.saveddata.SavedData
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

class TDMSavedData : SavedData {
    val entities: MutableSet<String> = Sets.newHashSet<String>()

    constructor()

    constructor(entities: Collection<String>) {
        this.entities.addAll(entities)
    }

    override fun save(pCompoundTag: CompoundTag): CompoundTag {
        pCompoundTag.put("Entities", this.saveEntities())
        return pCompoundTag
    }

    private fun saveEntities(): ListTag {
        val tags = ListTag()
        for (s in this.entities) {
            tags.add(StringTag.valueOf(s))
        }
        return tags
    }

    private fun loadEntities(pTagList: ListTag) {
        for (i in pTagList.indices) {
            this.entities.add(pTagList.getString(i))
        }
    }

    fun addEntity(entity: String): Boolean {
        return this.entities.add(entity)
    }

    fun removeEntity(entity: String): Boolean {
        return this.entities.remove(entity)
    }

    fun containsEntity(entity: String): Boolean {
        return this.entities.contains(entity)
    }

    fun sync() {
        this.setDirty()
        sendPacketToAll(TDMSyncMessage(this.entities))
    }

    @Mod.EventBusSubscriber
    companion object {
        const val FILE_ID: String = "superbwarfare_tdm"

        fun load(pCompoundTag: CompoundTag): TDMSavedData {
            val tdmSavedData = TDMSavedData()
            if (pCompoundTag.contains("Entities", Tag.TAG_LIST.toInt())) {
                tdmSavedData.loadEntities(pCompoundTag.getList("Entities", Tag.TAG_STRING.toInt()))
            }
            return tdmSavedData
        }

        @JvmStatic
        fun enabledTDM(entity: Entity): Boolean {
            val level = entity.level()
            return if (level is ServerLevel) {
                level.dataStorage
                    .computeIfAbsent(
                        { load(it) },
                        { TDMSavedData() },
                        FILE_ID
                    ).containsEntity(entity.getStringUUID())
            } else {
                ClientEventHandler.tdmSavedData.containsEntity(entity.getStringUUID())
            }
        }

        @SubscribeEvent
        fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
            val player = event.entity as? ServerPlayer ?: return
            val level = player.level()
            if (level !is ServerLevel) return

            val data = level.dataStorage
                .get({ load(it) }, FILE_ID) ?: return
            sendPacketTo(player, TDMSyncMessage(data.entities))
        }
    }
}