package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.TraceTool
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

object InteractMessage : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val stack = player.mainHandItem
        val tag = stack.getOrCreateTag()

        if (stack.`is`(ModItems.MONITOR.get())
            && tag.getBoolean("Using")
            && tag.getBoolean("Linked")
            && !player.cooldowns.isOnCooldown(stack.item)
        ) {
            val drone = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone")) ?: return

            val looking = Vec3.atLowerCornerOf(
                player.level().clip(
                    ClipContext(
                        drone.eyePosition,
                        drone.eyePosition.add(drone.lookAngle.scale(2.0)),
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        player
                    )
                ).blockPos
            )
            val blockPos = BlockPos.containing(looking.x(), looking.y(), looking.z())
            player.level().getBlockState(blockPos).use(
                player.level(),
                player,
                InteractionHand.MAIN_HAND,
                BlockHitResult.miss(
                    Vec3(
                        blockPos.x.toDouble(),
                        blockPos.y.toDouble(),
                        blockPos.z.toDouble()
                    ), Direction.UP, blockPos
                )
            )

            val lookingEntity = TraceTool.findLookingEntity(drone, 2.0) ?: return

            player.attack(lookingEntity)
            player.cooldowns.addCooldown(stack.item, 13)
        }
    }
}