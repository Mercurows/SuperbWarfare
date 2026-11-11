package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.entity.misc.HighEnergyExplosivesEntity
import com.atsuishio.superbwarfare.init.ModEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.TntBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import kotlin.math.cos
import kotlin.math.sin

open class HighEnergyExplosivesBlock : TntBlock(Properties.copy(Blocks.TNT)) {
    override fun getFlammability(state: BlockState, level: BlockGetter, pos: BlockPos, face: Direction) = 100

    override fun getFireSpreadSpeed(state: BlockState, level: BlockGetter, pos: BlockPos, face: Direction) = 15

    override fun onCaughtFire(state: BlockState, level: Level, pos: BlockPos, face: Direction?, igniter: LivingEntity?) {
        if (level.isClientSide) return
        val entity = prime(level, pos, igniter)
        level.playSound(null, entity.x, entity.y, entity.z, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1f, 1f)
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos)
    }

    override fun wasExploded(level: Level, pos: BlockPos, explosion: Explosion) {
        if (!level.isClientSide) prime(level, pos, explosion.indirectSourceEntity, true)
    }

    private fun prime(level: Level, pos: BlockPos, igniter: LivingEntity?, chain: Boolean = false): HighEnergyExplosivesEntity {
        val entity = HighEnergyExplosivesEntity(ModEntities.HIGH_ENERGY_EXPLOSIVES.get(), level)
        entity.setPos(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        val angle = level.random.nextDouble() * Math.PI * 2
        entity.setDeltaMovement(-sin(angle) * 0.02, 0.2, -cos(angle) * 0.02)
        entity.xo = entity.x
        entity.yo = entity.y
        entity.zo = entity.z
        entity.igniterUUID = igniter?.uuid
        if (chain) entity.fuse = level.random.nextInt(entity.fuse / 4) + entity.fuse / 8
        level.addFreshEntity(entity)
        return entity
    }
}
