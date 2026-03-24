package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.client.renderer.item.Tm62ItemRenderer
import com.atsuishio.superbwarfare.entity.projectile.Tm62Entity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
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
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import org.joml.Math
import java.util.function.Consumer

open class Tm62Item : Item(Properties().stacksTo(8)), DispenserLaunchable {
    override fun initializeClient(consumer: Consumer<IClientItemExtensions>) {
        super.initializeClient(consumer)
        consumer.accept(object : IClientItemExtensions {
            private var renderer: BlockEntityWithoutLevelRenderer? = null

            override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                if (renderer == null) {
                    renderer = Tm62ItemRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                }
                return renderer!!
            }
        })
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)

        if (!level.isClientSide) {
            val randomRot = Mth.clamp((2 * Math.random() - 1) * 180, -180.0, 180.0).toFloat()
            val entity = Tm62Entity(player, level, player.isShiftKeyDown)
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

        player.cooldowns.addCooldown(this, 20)

        if (!player.abilities.instabuild) {
            stack.shrink(1)
        }

        return InteractionResultHolder.success(stack)
    }

    override fun getLaunchBehavior(): DispenseItemBehavior {
        return object : DefaultDispenseItemBehavior() {
            public override fun execute(pSource: BlockSource, pStack: ItemStack): ItemStack {
                val level: Level = pSource.level
                val position = DispenserBlock.getDispensePosition(pSource)
                val direction = pSource.blockState.getValue(DispenserBlock.FACING)

                val tm62 = Tm62Entity(ModEntities.TM_62.get(), level)
                tm62.setPos(position.x(), position.y(), position.z())
                val randomRot = ((2 * Math.random() - 1) * 180).coerceIn(-180.0, 180.0).toFloat()

                val pX = direction.stepX
                val pY = direction.stepY
                val pZ = direction.stepZ
                tm62.shoot(pX.toDouble(), pY.toDouble(), pZ.toDouble(), 0.2f, 25f)
                tm62.yRot = randomRot
                tm62.yRotO = tm62.yRot

                level.addFreshEntity(tm62)
                pStack.shrink(1)
                return pStack
            }
        }
    }
}