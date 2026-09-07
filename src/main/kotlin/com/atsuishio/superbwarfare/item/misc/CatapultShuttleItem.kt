package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.block.AircraftCatapultBlock
import com.atsuishio.superbwarfare.compat.valkyrienskies.ValkyrienSkiesCompat
import com.atsuishio.superbwarfare.entity.misc.CatapultShuttleEntity
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.RegistryName
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2

@RegistryName("catapult_shuttle")
open class CatapultShuttleItem : AbstractDeployerItem(Properties().rarity(Rarity.COMMON)) {

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

            if (!blockstate.`is`(ModBlocks.AIRCRAFT_CATAPULT.get())) return InteractionResult.PASS

            val pos = if (blockstate.getCollisionShape(level, clickedPos).isEmpty) {
                clickedPos
            } else {
                clickedPos.relative(direction)
            }

            val entity = this.spawnDeployedEntity(level, player)
            entity.setPos(pos.x.toDouble() + 0.5, pos.y.toDouble() - 1, pos.z.toDouble() + 0.5)
            entity.yRot = -getWorldYRot(level, pos, blockstate)
            level.addFreshEntity(entity)

            if (!player.abilities.instabuild) {
                stack.shrink(1)
            }
            level.gameEvent(player, GameEvent.ENTITY_PLACE, clickedPos)

            return InteractionResult.CONSUME
        }
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemstack = player.getItemInHand(hand)
        val hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY)
        if (hitResult.type != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemstack)
        } else if (level !is ServerLevel) {
            return InteractionResultHolder.success(itemstack)
        } else {
            val blockpos = hitResult.blockPos
            val blockstate = level.getBlockState(blockpos)

            if (!blockstate.`is`(ModBlocks.AIRCRAFT_CATAPULT.get())) return InteractionResultHolder.pass(itemstack)

            if (blockstate.block !is LiquidBlock) {
                return InteractionResultHolder.pass(itemstack)
            } else if (level.mayInteract(player, blockpos)
                && player.mayUseItemAt(blockpos, hitResult.direction, itemstack)
            ) {
                val entity = this.spawnDeployedEntity(level, player)
                entity.setPos(
                    blockpos.x.toDouble() + 0.5,
                    blockpos.y.toDouble() - 1,
                    blockpos.z.toDouble() + 0.5
                )
                entity.yRot = -getWorldYRot(level, blockpos, blockstate)
                level.addFreshEntity(entity)

                if (!player.abilities.instabuild) {
                    itemstack.shrink(1)
                }

                player.awardStat(Stats.ITEM_USED.get(this))
                level.gameEvent(player, GameEvent.ENTITY_PLACE, entity.position())
                return InteractionResultHolder.consume(itemstack)
            } else {
                return InteractionResultHolder.fail(itemstack)
            }
        }
    }
    override fun spawnDeployedEntity(
        level: Level,
        player: Player
    ): Entity {
        return CatapultShuttleEntity(level)
    }

    /**
     * 获取弹射器滑块的放置朝向。
     * 当瓦尔基里天空模组加载时，使用船舶局部朝向（因为滑块实体会作为 shipyard entity 固定在船体上）；
     * 否则将局部方向转换为世界方向后计算朝向。
     */
    private fun getWorldYRot(level: Level, pos: BlockPos, blockstate: BlockState): Float {
        val facing = blockstate.getValue(AircraftCatapultBlock.FACING)
        val stepX = facing.stepX.toDouble()
        val stepZ = facing.stepZ.toDouble()

        return if (ValkyrienSkiesCompat.hasMod()) {
            // Shipyard entity 使用船舶局部 yaw
            Math.toDegrees(atan2(stepX, stepZ)).toFloat()
        } else {
            val localDir = Vec3(stepX, 0.0, stepZ)
            Math.toDegrees(atan2(localDir.x, localDir.z)).toFloat()
        }
    }
}
