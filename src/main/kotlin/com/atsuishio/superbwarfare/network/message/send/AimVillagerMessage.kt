package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.ai.gossip.GossipType
import net.minecraft.world.entity.npc.AbstractVillager
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.schedule.Activity

@Serializable
data class AimVillagerMessage(val villagerId: Int) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val sender = sender()
        val entity = sender.level().getEntity(villagerId) as? AbstractVillager ?: return

        if (entity is Villager) {
            entity.gossips.add(sender.getUUID(), GossipType.MINOR_NEGATIVE, 10)
        }
        entity.getBrain().setActiveActivityIfPossible(Activity.PANIC)
    }
}
