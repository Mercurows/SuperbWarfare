package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.ClaymoreEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import net.minecraft.core.BlockSource
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.DispenserBlock
import net.minecraft.world.phys.Vec3

open class ClaymoreMineItem : Item(Properties()), DispenserLaunchable {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)

        if (!level.isClientSide) {
            val entity = ClaymoreEntity(player, level)
            entity.moveTo(player.x, player.y + 1.1, player.z, player.yRot, 0f)
            entity.setYBodyRot(player.yRot)
            entity.setYHeadRot(player.yRot)
            entity.setDeltaMovement(
                0.5 * player.lookAngle.x,
                0.5 * player.lookAngle.y,
                0.5 * player.lookAngle.z
            )

            level.addFreshEntity(entity)
        }

        player.cooldowns.addCooldown(this, 20)

        if (!player.abilities.instabuild) {
            stack.shrink(1)
        }

        return InteractionResultHolder.consume(stack)
    }

    override fun getLaunchBehavior(): DispenseItemBehavior {
        return object : DefaultDispenseItemBehavior() {
            public override fun execute(pSource: BlockSource, pStack: ItemStack): ItemStack {
                val level: Level = pSource.level
                val position = DispenserBlock.getDispensePosition(pSource)
                val direction = pSource.blockState.getValue(DispenserBlock.FACING)

                val claymore = ClaymoreEntity(ModEntities.CLAYMORE.get(), level)
                claymore.setPos(position.x(), position.y(), position.z())

                val pX = direction.stepX
                val pY = direction.stepY + 0.1f
                val pZ = direction.stepZ
                val vec3 = (Vec3(pX.toDouble(), pY.toDouble(), pZ.toDouble())).normalize().scale(0.05)
                claymore.deltaMovement = vec3
                val d0 = vec3.horizontalDistance()
                claymore.yRot = (Mth.atan2(vec3.x, vec3.z) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
                claymore.xRot = (Mth.atan2(vec3.y, d0) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
                claymore.yRotO = claymore.yRot
                claymore.xRotO = claymore.xRot

                level.addFreshEntity(claymore)
                pStack.shrink(1)
                return pStack
            }
        }
    }
}
