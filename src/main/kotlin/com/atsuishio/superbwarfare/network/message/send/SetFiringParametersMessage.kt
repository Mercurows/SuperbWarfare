package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.misc.ArtilleryIndicatorItem
import com.atsuishio.superbwarfare.item.misc.FiringParametersItem
import com.atsuishio.superbwarfare.item.misc.firingParameters
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.SoundTool
import com.atsuishio.superbwarfare.tools.TraceTool
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.level.ClipContext

object SetFiringParametersMessage : ServerPacketPayload() {

    override fun PayloadContext.handler() {
        val player = sender()
        val stack = player.offhandItem
        val mainStack = player.mainHandItem
        var lookAtEntity = false
        val lookingEntity = TraceTool.findLookingEntity(player, 520.0)

        val result = player.level().clip(
            ClipContext(
                player.eyePosition, player.eyePosition.add(player.getViewVector(1f).scale(512.0)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
            )
        )
        val hitPos = result.blockPos

        if (lookingEntity != null && !player.isShiftKeyDown) {
            lookAtEntity = true
        }
        if (stack.`is`(ModItems.FIRING_PARAMETERS.get())) {
            val parameters = stack.firingParameters
            val isDepressed = parameters.isDepressed
            val radius = parameters.radius

            if (lookAtEntity) {
                stack.firingParameters = FiringParametersItem.Parameters(lookingEntity!!.blockPosition(), radius, isDepressed)
            } else {
                stack.firingParameters = FiringParametersItem.Parameters(hitPos, radius, isDepressed)
            }

            val pos = stack.firingParameters.pos

            player.displayClientMessage(
                Component.translatable("tips.superbwarfare.mortar.target_pos")
                    .withStyle(ChatFormatting.GRAY)
                    .append(
                        Component.literal(("[" + pos.x + "," + pos.y + "," + pos.z + "]"))
                    ), true
            )
        }

        val item = mainStack.item
        if (item is ArtilleryIndicatorItem) {
            val pos = if (lookAtEntity) {
                BlockPos.containing(lookingEntity!!.boundingBox.center)
            } else {
                hitPos
            }
            val parameters = mainStack.firingParameters
            val isDepressed = parameters.isDepressed
            val radius = parameters.radius

            mainStack.firingParameters = FiringParametersItem.Parameters(pos, radius, isDepressed)

            player.displayClientMessage(
                Component.translatable("tips.superbwarfare.mortar.target_pos")
                    .withStyle(ChatFormatting.GRAY)
                    .append(
                        Component.literal(("[" + pos.x + "," + pos.y + "," + pos.z + "]"))
                    ), true
            )
            SoundTool.playLocalSound(player, ModSounds.CANNON_ZOOM_IN.get(), 2f, 1f)

            item.setTarget(mainStack, player)
        }
    }
}
