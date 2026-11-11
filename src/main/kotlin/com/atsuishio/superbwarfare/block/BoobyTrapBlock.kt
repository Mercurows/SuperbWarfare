package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.BoobyTrapBlockEntity
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.item.misc.DefuserItem
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
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
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.neoforged.neoforge.event.EventHooks

open class BoobyTrapBlock : BaseEntityBlock(
    Properties.of().strength(2f, 3f).sound(SoundType.METAL).dynamicShape().pushReaction(PushReaction.BLOCK)
) {
    override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

    init {
        registerDefaultState(stateDefinition.any().setValue(CAMOUFLAGED, false).setValue(OFF, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(CAMOUFLAGED, OFF)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState) = BoobyTrapBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState) =
        if (state.getValue(CAMOUFLAGED) && !state.getValue(OFF)) RenderShape.INVISIBLE else RenderShape.MODEL

    private fun lightMaterial(state: BlockState, world: BlockGetter, pos: BlockPos): BlockState? {
        if (!state.getValue(CAMOUFLAGED) || state.getValue(OFF)) return null
        return (world.getBlockEntity(pos) as? BoobyTrapBlockEntity)?.camouflage
    }

    override fun getLightBlock(state: BlockState, world: BlockGetter, pos: BlockPos): Int =
        lightMaterial(state, world, pos)?.getLightBlock(world, pos) ?: world.maxLightLevel

    override fun getLightEmission(state: BlockState, world: BlockGetter, pos: BlockPos): Int =
        lightMaterial(state, world, pos)?.getLightEmission(world, pos) ?: 0

    override fun propagatesSkylightDown(state: BlockState, world: BlockGetter, pos: BlockPos): Boolean =
        lightMaterial(state, world, pos)?.propagatesSkylightDown(world, pos) ?: false

    override fun getShadeBrightness(state: BlockState, world: BlockGetter, pos: BlockPos): Float =
        lightMaterial(state, world, pos)?.getShadeBrightness(world, pos) ?: 0.2f

    override fun getOcclusionShape(state: BlockState, world: BlockGetter, pos: BlockPos): VoxelShape {
        val material = lightMaterial(state, world, pos) ?: return Shapes.block()
        return if (material.canOcclude()) material.getOcclusionShape(world, pos) else Shapes.empty()
    }

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
        if (stack.item is DefuserItem) {
            if (!player.mayUseItemAt(pos, hit.direction, stack)) return ItemInteractionResult.FAIL
            if (state.getValue(OFF)) return ItemInteractionResult.CONSUME
            if (!level.isClientSide) {
                if (entity.camouflage != null) entity.applyCamouflage(null)
                else {
                    if (!level.setBlock(pos, state.setValue(OFF, true), 3)) return ItemInteractionResult.FAIL
                    stack.hurtAndBreak(1, player,
                        if (hand == InteractionHand.MAIN_HAND) EquipmentSlot.MAINHAND else EquipmentSlot.OFFHAND)
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide)
        }
        if (!state.getValue(OFF) && !entity.isOwnerOrAlly(player)) {
            if (!level.isClientSide) detonate(level, pos)
            return ItemInteractionResult.sidedSuccess(level.isClientSide)
        }
        val item = stack.item as? BlockItem ?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        if (state.getValue(OFF) || item.block === this) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        if (!player.mayUseItemAt(pos, hit.direction, stack)) return ItemInteractionResult.FAIL
        val current = entity.camouflage
        if (current != null) {
            if (item.block !== current.block) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
            val properties = current.block.stateDefinition
            val canRotate = properties.getProperty("axis")?.valueClass == Direction.Axis::class.java ||
                properties.getProperty("facing")?.valueClass == Direction::class.java
            if (!canRotate) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        }
        val material = item.block.defaultBlockState()
        if (material.renderShape != RenderShape.MODEL ||
            !material.isCollisionShapeFullBlock(level, pos) || !Block.isShapeFullBlock(material.getShape(level, pos))
        ) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
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

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        super.setPlacedBy(level, pos, state, placer, stack)
        if (!level.isClientSide && placer != null) {
            (level.getBlockEntity(pos) as? BoobyTrapBlockEntity)?.recordOwner(placer)
        }
    }

    override fun attack(state: BlockState, level: Level, pos: BlockPos, player: Player) {
        if (!level.isClientSide && !canMineSafely(state, level, pos, player)) detonate(level, pos)
    }

    override fun onDestroyedByPlayer(
        state: BlockState, level: Level, pos: BlockPos, player: Player, willHarvest: Boolean, fluid: FluidState
    ): Boolean {
        if (!canMineSafely(state, level, pos, player)) {
            if (!level.isClientSide) detonate(level, pos)
            return false
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid)
    }

    private fun canMineSafely(state: BlockState, level: Level, pos: BlockPos, player: Player): Boolean =
        state.getValue(OFF) || player.isCreative ||
            (level.getBlockEntity(pos) as? BoobyTrapBlockEntity)?.isOwnerOrAlly(player) == true

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
        if (!level.isClientSide && level.hasNeighborSignal(pos)) detonate(level, pos)
    }

    private fun detonate(level: Level, pos: BlockPos) {
        val state = level.getBlockState(pos)
        if (level.isClientSide || state.block !== this || state.getValue(OFF)) return
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
            ).setSphericalBlockDestruction(true)
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

        @JvmField
        val OFF: BooleanProperty = BooleanProperty.create("off")
    }
}
