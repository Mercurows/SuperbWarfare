package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.C4Entity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockSource
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.DispenserBlock
import net.minecraft.world.phys.Vec3

open class C4BombItem : Item(Properties()), DispenserLaunchable {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack?> {
        val stack = player.getItemInHand(hand)

        if (!level.isClientSide) {
            val flag = stack.getOrCreateTag().getBoolean(TAG_CONTROL)

            val entity = C4Entity(player, level, flag)
            entity.setPos(
                player.x + 0.25 * player.lookAngle.x,
                player.eyeY - 0.2f + 0.25 * player.lookAngle.y,
                player.z + 0.25 * player.lookAngle.z
            )
            entity.setDeltaMovement(
                0.5 * player.lookAngle.x,
                0.5 * player.lookAngle.y,
                0.5 * player.lookAngle.z
            )
            entity.ownerUUID = player.getUUID()

            level.addFreshEntity(entity)
        }

        if (player is ServerPlayer) {
            player.level().playSound(null, player.onPos, ModSounds.C4_THROW.get(), SoundSource.PLAYERS, 1f, 1f)
        }

        player.cooldowns.addCooldown(this, 20)

        if (!player.abilities.instabuild) {
            stack.shrink(1)
        }

        return InteractionResultHolder.consume<ItemStack?>(stack)
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        if (pStack.getOrCreateTag().getBoolean(TAG_CONTROL)) {
            pTooltipComponents.add(
                Component.translatable("des.superbwarfare.c4_bomb.control").withStyle(ChatFormatting.GRAY)
            )
        } else {
            pTooltipComponents.add(
                Component.translatable("des.superbwarfare.c4_bomb.time").withStyle(ChatFormatting.GRAY)
            )
        }
    }

    override fun getLaunchBehavior(): DispenseItemBehavior {
        return object : DefaultDispenseItemBehavior() {
            public override fun execute(pSource: BlockSource, pStack: ItemStack): ItemStack {
                val level: Level = pSource.level
                val position = DispenserBlock.getDispensePosition(pSource)
                val direction = pSource.blockState.getValue(DispenserBlock.FACING)

                val entity = C4Entity(ModEntities.C4.get(), level)
                entity.setPos(position.x(), position.y(), position.z())

                val pX = direction.stepX
                val pY = direction.stepY + 0.1f
                val pZ = direction.stepZ
                val vec3 = (Vec3(pX.toDouble(), pY.toDouble(), pZ.toDouble())).normalize().scale(0.05)
                entity.deltaMovement = vec3
                val d0 = vec3.horizontalDistance()
                entity.yRot = (Mth.atan2(vec3.x, vec3.z) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
                entity.xRot = (Mth.atan2(vec3.y, d0) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
                entity.yRotO = entity.yRot
                entity.xRotO = entity.xRot

                level.addFreshEntity(entity)
                pStack.shrink(1)
                return pStack
            }
        }
    }

    override fun getBurnTime(itemStack: ItemStack?, recipeType: RecipeType<*>?): Int {
        return 20000
    }

    companion object {
        const val TAG_CONTROL: String = "Control"

        fun makeInstance(): ItemStack {
            val stack = ItemStack(ModItems.C4_BOMB.get())
            stack.getOrCreateTag().putBoolean(TAG_CONTROL, true)
            return stack
        }
    }
}
