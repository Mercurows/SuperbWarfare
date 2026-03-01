package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.client.renderer.item.Ptkm1rItemRenderer
import com.atsuishio.superbwarfare.entity.Ptkm1rEntity
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.Shapes
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

open class Ptkm1rItem : Item(Properties().rarity(Rarity.RARE).stacksTo(2)), GeoItem {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun initializeClient(consumer: Consumer<IClientItemExtensions>) {
        super.initializeClient(consumer)
        consumer.accept(object : IClientItemExtensions {
            private val renderer: BlockEntityWithoutLevelRenderer = Ptkm1rItemRenderer()

            override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                return renderer
            }
        })
    }

    override fun registerControllers(data: ControllerRegistrar?) {
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return this.cache
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        if (level !is ServerLevel) {
            return InteractionResult.SUCCESS
        } else {
            val stack = context.itemInHand
            val clickedPos = context.clickedPos
            val direction = context.clickedFace
            val player = context.player ?: return InteractionResult.PASS

            val blockstate = level.getBlockState(clickedPos)
            val pos = if (blockstate.getCollisionShape(level, clickedPos).isEmpty) {
                clickedPos
            } else {
                clickedPos.relative(direction)
            }

            val ptkm1rEntity = Ptkm1rEntity(player, level)
            ptkm1rEntity.setPos(pos.x.toDouble() + 0.5, (pos.y + 1).toDouble(), pos.z.toDouble() + 0.5)
            val yOffset = this.getYOffset(
                level,
                pos,
                clickedPos != pos && direction == Direction.UP,
                ptkm1rEntity.boundingBox
            )
            ptkm1rEntity.moveTo(pos.x.toDouble() + 0.5, pos.y + yOffset, pos.z.toDouble() + 0.5)
            level.addFreshEntity(ptkm1rEntity)

            if (!player.abilities.instabuild) {
                stack.shrink(1)
            }
            level.gameEvent(player, GameEvent.ENTITY_PLACE, clickedPos)

            return InteractionResult.CONSUME
        }
    }

    fun getYOffset(pLevel: LevelReader, pPos: BlockPos, pShouldOffsetYMore: Boolean, pBox: AABB): Double {
        var aabb = AABB(pPos)
        if (pShouldOffsetYMore) {
            aabb = aabb.expandTowards(0.0, -1.0, 0.0)
        }

        val iterable = pLevel.getCollisions(null, aabb)
        return 1 + Shapes.collide(
            Direction.Axis.Y, pBox, iterable,
            (if (pShouldOffsetYMore) -2 else -1).toDouble()
        )
    }

    override fun use(pLevel: Level, pPlayer: Player, pHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = pPlayer.getItemInHand(pHand)
        val hitResult = getPlayerPOVHitResult(pLevel, pPlayer, ClipContext.Fluid.SOURCE_ONLY)
        if (hitResult.type != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack)
        } else if (pLevel !is ServerLevel) {
            return InteractionResultHolder.success(stack)
        } else {
            val blockpos = hitResult.blockPos
            if (pLevel.getBlockState(blockpos).block !is LiquidBlock) {
                return InteractionResultHolder.pass(stack)
            } else if (pLevel.mayInteract(pPlayer, blockpos)
                && pPlayer.mayUseItemAt(blockpos, hitResult.direction, stack)
            ) {
                val ptkm1rEntity = Ptkm1rEntity(pPlayer, pLevel)
                ptkm1rEntity.setPos(
                    blockpos.x.toDouble() + 0.5,
                    blockpos.y.toDouble(),
                    blockpos.z.toDouble() + 0.5
                )
                pLevel.addFreshEntity(ptkm1rEntity)

                if (!pPlayer.abilities.instabuild) {
                    stack.shrink(1)
                }

                pPlayer.awardStat(Stats.ITEM_USED.get(this))
                pLevel.gameEvent(pPlayer, GameEvent.ENTITY_PLACE, ptkm1rEntity.position())
                return InteractionResultHolder.consume(stack)
            } else {
                return InteractionResultHolder.fail(stack)
            }
        }
    }
}
