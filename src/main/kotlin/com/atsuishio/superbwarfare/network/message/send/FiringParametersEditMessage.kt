package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.misc.ArtilleryIndicatorItem
import com.atsuishio.superbwarfare.item.misc.FiringParametersItem
import com.atsuishio.superbwarfare.item.misc.firingParameters
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.`is`
import kotlinx.serialization.Serializable
import net.minecraft.core.BlockPos

@Serializable
data class FiringParametersEditMessage(
    val x: Int, val y: Int, val z: Int,
    val radius: Int, val isDepressed: Boolean, val mainHand: Boolean
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val stack = if (mainHand) player.mainHandItem else player.offhandItem
        if (!stack.`is`(ModItems.FIRING_PARAMETERS, ModItems.ARTILLERY_INDICATOR)) return

        stack.firingParameters = FiringParametersItem.Parameters(
            BlockPos(x, y, z),
            radius,
            isDepressed
        )

        val item = stack.item
        if (item is ArtilleryIndicatorItem) {
            item.setTarget(stack, player)
        }
    }
}
