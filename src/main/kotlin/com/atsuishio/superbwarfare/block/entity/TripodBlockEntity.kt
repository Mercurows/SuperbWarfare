package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.resource.model.BlockModelReloadListener
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

open class TripodBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.TRIPOD.get(), pos, state) {

    open val modelInstance = BlockModelReloadListener.getModel(MODEL)?.createInstance()

    companion object {
        val MODEL = loc("models/bedrock/block/tripod.geo.json")
    }
}
