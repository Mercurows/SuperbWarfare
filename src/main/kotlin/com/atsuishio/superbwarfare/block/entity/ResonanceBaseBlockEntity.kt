package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB

/** Rendering anchor only; no ticking, inventory or custom persistent state. */
class ResonanceBaseBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.RESONANCE_BASE.get(), pos, state) {
    override fun getRenderBoundingBox(): AABB = AABB(blockPos).inflate(1.0, 0.0, 1.0)
}
