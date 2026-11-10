package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.BoobyTrapBlockEntity
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.event.EventHooks

open class BoobyTrapBlock : BaseEntityBlock(Properties.of().strength(2f, 3f).sound(SoundType.METAL).noOcclusion()) {
    override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

    init {
        registerDefaultState(stateDefinition.any().setValue(CAMOUFLAGED, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(CAMOUFLAGED)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState) = BoobyTrapBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState) =
        if (state.getValue(CAMOUFLAGED)) RenderShape.INVISIBLE else RenderShape.MODEL

    override fun canConnectRedstone(state: BlockState, level: BlockGetter, pos: BlockPos, side: Direction?) =
        side != null

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): ItemInteractionResult {
        val entity = level.getBlockEntity(pos) as? BoobyTrapBlockEntity
            ?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        if (stack.`is`(ModTags.Items.TOOLS_CROWBAR)) {
            if (!player.mayUseItemAt(pos, hit.direction, stack)) return ItemInteractionResult.FAIL
            if (!state.getValue(CAMOUFLAGED) || entity.camouflage == null) return ItemInteractionResult.CONSUME
            if (!level.isClientSide) entity.applyCamouflage(null)
            return ItemInteractionResult.sidedSuccess(level.isClientSide)
        }
        val item = stack.item as? BlockItem ?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        if (!player.mayUseItemAt(pos, hit.direction, stack)) return ItemInteractionResult.FAIL
        val material = item.block.defaultBlockState()
        if (item.block === this || material.renderShape != RenderShape.MODEL ||
            !material.isCollisionShapeFullBlock(level, pos) || !Block.isShapeFullBlock(material.getShape(level, pos))
        ) return ItemInteractionResult.CONSUME
        if (!level.isClientSide) entity.applyCamouflage(material)
        return ItemInteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, moving: Boolean) {
        super.onPlace(state, level, pos, oldState, moving)
        if (oldState.block !== this && !level.isClientSide) {
            // onPlace runs before LevelChunk finishes installing the block entity.
            level.scheduleTick(pos, this, 1)
        }
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        checkPower(level, pos)
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        moving: Boolean
    ) {
        checkPower(level, pos)
    }

    private fun checkPower(level: Level, pos: BlockPos) {
        if (level.isClientSide || level.getBlockState(pos).block !== this || !level.hasNeighborSignal(pos)) return
        val entity = level.getBlockEntity(pos) as? BoobyTrapBlockEntity ?: return
        if (entity.detonating) return
        // Guard event callbacks and neighbor updates against reentrant detonation.
        entity.detonating = true
        try {
            val radius = ExplosionConfig.BOOBY_TRAP_EXPLOSION_RADIUS.get().toFloat()
            val interaction =
                if (ExplosionConfig.EXPLOSION_DESTROY.get()) Explosion.BlockInteraction.DESTROY else Explosion.BlockInteraction.KEEP
            val explosion = CustomExplosion(
                level,
                null,
                null,
                ExplosionConfig.BOOBY_TRAP_EXPLOSION_DAMAGE.get().toFloat(),
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5,
                radius,
                interaction
            )
            if (EventHooks.onExplosionStart(level, explosion)) return
            // Respect cancellation, then remove before the blast can trigger further neighbor updates.
            if (level.getBlockEntity(pos) !== entity || !level.removeBlock(pos, false)) return
            explosion.explode()
            explosion.finalizeExplosion(false)
            ParticleTool.spawnExplosionParticles(ParticleTool.particleTypeForRadius(radius), level, Vec3.atCenterOf(pos))
        } finally {
            entity.detonating = false
        }
    }

    companion object {
        private val CODEC: MapCodec<BoobyTrapBlock> = simpleCodec { _ -> BoobyTrapBlock() }

        @JvmField
        val CAMOUFLAGED: BooleanProperty = BooleanProperty.create("camouflaged")
    }
}
