package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.misc.ArtilleryIndicatorItem
import com.atsuishio.superbwarfare.item.misc.FiringParametersItem
import com.atsuishio.superbwarfare.item.misc.firingParameters
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.`is`
import com.atsuishio.superbwarfare.tools.playLocalSound
import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component

@Serializable
data class DroneFireMessage(val pos: SerializedVector3f) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val stack = player.mainHandItem
        val mainTag = stack.getOrCreateTag()

        if (stack.`is`(ModItems.MONITOR.get()) && mainTag.getBoolean("Using") && mainTag.getBoolean("Linked")) {
            val drone = EntityFindUtil.findDrone(player.level(), mainTag.getString("LinkedDrone")) ?: return
            if (player.offhandItem.`is`(ModItems.FIRING_PARAMETERS, ModItems.ARTILLERY_INDICATOR)) {
                val offStack = player.offhandItem

                val (_, radius, isDepressed) = offStack.firingParameters

                offStack.firingParameters = FiringParametersItem.Parameters(
                    BlockPos(
                        pos.x.toInt(),
                        pos.y.toInt(),
                        pos.z.toInt()
                    ), radius, isDepressed
                )

                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.mortar.target_pos")
                        .withStyle(ChatFormatting.GRAY)
                        .append(
                            Component.literal(("[${pos.x()}, ${pos.y() }, ${pos.z()}]"))
                        ), true
                )

                player.playLocalSound(ModSounds.CANNON_ZOOM_IN.get(), 2f, 1f)

                val item = offStack.item
                if (item is ArtilleryIndicatorItem) {
                    item.setTarget(offStack, player)
                }
            } else {
                drone.fire = true
            }
        }
    }
}
