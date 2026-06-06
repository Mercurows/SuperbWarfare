package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.atsuishio.superbwarfare.entity.vehicle.SodayoPickUpRocketEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.misc.ArtilleryIndicatorItem
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import net.minecraft.nbt.Tag

object ArtilleryIndicatorFireMessage : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        var stack = player.mainHandItem

        if (player.mainHandItem.`is`(ModItems.MONITOR.get()) && player.offhandItem.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
            stack = player.offhandItem
        }

        if (!stack.`is`(ModItems.ARTILLERY_INDICATOR.get())) return

        val mainTag = stack.getOrCreateTag()
        val tags = mainTag.getList(ArtilleryIndicatorItem.TAG_CANNON, Tag.TAG_COMPOUND.toInt())
        if (tags.isEmpty()) {
            mainTag.remove(ArtilleryIndicatorItem.TAG_TYPE)
            return
        }

        for (i in tags.indices) {
            val tag = tags.getCompound(i)
            val entity = EntityFindUtil.findEntity(player.level(), tag.getString("UUID"))

            if (entity is VehicleEntity && entity.isWreck) continue

            if (entity is ArtilleryEntity) {
                val gunData = entity.getGunData("Main")
                if (gunData != null && (entity is MortarEntity || gunData.ammo.get() > 0)) {
                    queueServerWork(i % 5 + 1) {
                        entity.vehicleShoot(player, "Main", entity.targetPos.center)
                        entity.resetTarget("Main")
                    }
                }
            }

            if (entity is SodayoPickUpRocketEntity) {
                entity.vehicleShoot(player, "Main", entity.targetPos.center)
            }
        }
    }
}