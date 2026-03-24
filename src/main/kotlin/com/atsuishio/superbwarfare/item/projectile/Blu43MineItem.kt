package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.Blu43Entity
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
import org.joml.Math
import kotlin.random.Random

class Blu43MineItem : Item(Properties()), DispenserLaunchable {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)

        if (!level.isClientSide) {
            val randomRot = ((2 * Random.nextDouble() - 1) * 180).coerceIn(-180.0, 180.0).toFloat()
            val entity = Blu43Entity(player, level)
            entity.moveTo(player.x, player.y + 1.1, player.z, randomRot, 0f)
            entity.setYBodyRot(randomRot)
            entity.setYHeadRot(randomRot)
            entity.setDeltaMovement(
                0.5 * player.lookAngle.x,
                0.5 * player.lookAngle.y,
                0.5 * player.lookAngle.z
            )

            level.addFreshEntity(entity)
        }

        player.cooldowns.addCooldown(this, 4)

        if (!player.abilities.instabuild) {
            stack.shrink(1)
        }

        return InteractionResultHolder.success(stack)
    }

    override fun getLaunchBehavior(): DispenseItemBehavior = object : DefaultDispenseItemBehavior() {
        public override fun execute(pSource: BlockSource, pStack: ItemStack): ItemStack {
            val level: Level = pSource.level
            val position = DispenserBlock.getDispensePosition(pSource)
            val direction = pSource.blockState.getValue(DispenserBlock.FACING)

            val blu43 = Blu43Entity(ModEntities.BLU_43.get(), level)
            blu43.setPos(position.x(), position.y(), position.z())
            val randomRot = Mth.clamp((2 * Math.random() - 1) * 180, -180.0, 180.0).toFloat()

            val pX = direction.stepX
            val pY = direction.stepY
            val pZ = direction.stepZ
            blu43.shoot(pX.toDouble(), pY.toDouble(), pZ.toDouble(), 0.4f, 10f)
            blu43.yRot = randomRot
            blu43.yRotO = blu43.yRot

            level.addFreshEntity(blu43)
            pStack.shrink(1)
            return pStack
        }
    }
}