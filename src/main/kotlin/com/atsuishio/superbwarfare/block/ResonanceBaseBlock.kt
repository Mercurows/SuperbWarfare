package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.ResonanceBaseBlockEntity
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument

@Suppress("OVERRIDE_DEPRECATION")
class ResonanceBaseBlock : BaseEntityBlock(
    Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL)
        .strength(4f, 6f).requiresCorrectToolForDrops().lightLevel { 5 }.noOcclusion()
) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState) = ResonanceBaseBlockEntity(pos, state)


    val codec: MapCodec<ResonanceBaseBlock> = simpleCodec { _ -> ResonanceBaseBlock() }

    override fun codec(): MapCodec<out BaseEntityBlock> = codec

    override fun getRenderShape(state: BlockState) = RenderShape.ENTITYBLOCK_ANIMATED
}
