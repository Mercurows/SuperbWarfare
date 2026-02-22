package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.ContainerBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.ceil

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class ContainerBlock :
    BaseEntityBlock(Properties.of().sound(SoundType.METAL).strength(3.0f).noOcclusion().requiresCorrectToolForDrops()) {
    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPENED, false)
        )
    }

    override fun use(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pPlayer: Player,
        pHand: InteractionHand,
        pHit: BlockHitResult
    ): InteractionResult {
        val blockEntity = pLevel.getBlockEntity(pPos)
        if (pLevel.isClientSide
            || pState.getValue(OPENED)
            || blockEntity !is ContainerBlockEntity
        ) return InteractionResult.PASS

        val stack = pPlayer.getItemInHand(pHand)
        if (!stack.`is`(ModTags.Items.TOOLS_CROWBAR)) {
            pPlayer.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.crowbar"), true)
            return InteractionResult.PASS
        }

        if (!hasEntity(pLevel, pPos)) {
            pPlayer.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.empty"), true)
            return InteractionResult.PASS
        }

        if (canOpen(pLevel, pPos, blockEntity.entityType, blockEntity.entityTag)) {
            pLevel.setBlockAndUpdate(pPos, pState.setValue(OPENED, true))
            pLevel.playSound(
                null,
                BlockPos.containing(pPos.x.toDouble(), pPos.y.toDouble(), pPos.z.toDouble()),
                ModSounds.OPEN.get(),
                SoundSource.BLOCKS,
                1f,
                1f
            )

            return InteractionResult.SUCCESS
        } else {
            pPlayer.displayClientMessage(Component.translatable("des.superbwarfare.container.fail.open"), true)
            return InteractionResult.PASS
        }
    }

    fun hasEntity(pLevel: Level, pPos: BlockPos): Boolean {
        val blockEntity = pLevel.getBlockEntity(pPos)
        if (blockEntity !is ContainerBlockEntity) return false
        return blockEntity.entityTag != null || blockEntity.entityType != null
    }

    override fun <T : BlockEntity?> getTicker(
        pLevel: Level,
        pState: BlockState,
        pBlockEntityType: BlockEntityType<T?>
    ): BlockEntityTicker<T?>? {
        if (!pLevel.isClientSide) {
            return createTickerHelper<ContainerBlockEntity?, T?>(
                pBlockEntityType,
                ModBlockEntities.CONTAINER.get()
            ) { pLevel, pPos, pState, blockEntity ->
                ContainerBlockEntity.serverTick(
                    pLevel,
                    pPos,
                    pState,
                    blockEntity
                )
            }
        }
        return null
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: BlockGetter?,
        pTooltip: MutableList<Component?>,
        pFlag: TooltipFlag
    ) {
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag)
        val tag = BlockItem.getBlockEntityData(pStack)
        if (tag != null && tag.contains("EntityType")) {
            val type = tag.getString("EntityType")
            val location = ResourceLocation.tryParse(type) ?: return

            val info =
                Component.translatableWithFallback("info.${location.namespace}.${location.path}", "")
            val hasDescription = !info.string.isEmpty()

            if (Screen.hasShiftDown() && hasDescription) {
                // 详细描述
                pTooltip.add(info.withStyle(ChatFormatting.GRAY))
                pTooltip.add(Component.empty())
                pTooltip.add(
                    Component.translatableWithFallback(
                        "info.${location.namespace}.mod_id",
                        location.namespace
                    ).withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.AQUA)
                )
            } else {
                // 实体名称
                val entityTranslationKey = getEntityTranslationKey(type)
                pTooltip.add(
                    Component.translatable(entityTranslationKey ?: "des.superbwarfare.container.empty")
                        .withStyle(ChatFormatting.GRAY)
                )
            }

            val entityType = EntityType.byString(type).orElse(null)
            if (entityType != null) {
                var w = 0f
                var h = 0

                // N * N * N
                if (pLevel is Level && tag.contains("Entity")) {
                    val entity: Entity? = entityType.create(pLevel)
                    if (entity != null) {
                        entity.load(tag.getCompound("Entity"))
                        w = ceil((entity.type.dimensions.width / 2).toDouble()).toFloat()
                        h = (entity.type.dimensions.height + 1).toInt()
                    }
                } else {
                    w = ceil((entityType.dimensions.width / 2).toDouble()).toFloat()
                    h = (entityType.dimensions.height + 1).toInt()
                }
                if (w != 0f && h != 0) {
                    w *= 2f
                    if (w.toInt() % 2 == 0) w++
                    pTooltip.add(
                        Component.literal(w.toInt().toString() + " x " + w.toInt() + " x " + h)
                            .withStyle(ChatFormatting.YELLOW)
                    )
                }
            }
        }
    }

    override fun getShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return if (state.getValue(OPENED)) box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0)
        else box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0)
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }

    override fun newBlockEntity(blockPos: BlockPos, blockState: BlockState): BlockEntity? {
        return ContainerBlockEntity(blockPos, blockState)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING).add(OPENED)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        return this.defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
            .setValue(OPENED, false)
    }

    override fun getCloneItemStack(pLevel: BlockGetter, pPos: BlockPos, pState: BlockState): ItemStack {
        val itemstack = super.getCloneItemStack(pLevel, pPos, pState)
        pLevel.getBlockEntity(pPos, ModBlockEntities.CONTAINER.get())
            .ifPresent { it.saveToItem(itemstack) }
        return itemstack
    }

    companion object {
        @JvmField
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING

        @JvmField
        val OPENED: BooleanProperty = BooleanProperty.create("opened")

        @JvmStatic
        fun canOpen(pLevel: Level, pPos: BlockPos, entityType: EntityType<*>?, tag: CompoundTag?): Boolean {
            if (entityType == null) return false

            val entity: Entity? = entityType.create(pLevel)
            if (entity != null && tag != null) {
                entity.load(tag)
            }

            var flag = true

            var w = (entityType.dimensions.width / 2 + 1).toInt()
            var h = (entityType.dimensions.height + 1).toInt()

            if (entity != null) {
                w = (entity.type.dimensions.width / 2 + 1).toInt()
                h = (entity.type.dimensions.height + 1).toInt()
            }

            for (i in -w..<w + 1) {
                for (j in 0..<h) {
                    for (k in -w..<w + 1) {
                        if (i == 0 && j == 0 && k == 0) {
                            continue
                        }

                        val state = pLevel.getBlockState(pPos.offset(i, j, k))
                        if (state.canOcclude() && !state.`is`(Blocks.SNOW)) {
                            flag = false
                        }
                    }
                }
            }

            return flag
        }

        @JvmStatic
        fun getEntityTranslationKey(path: String): String? {
            val parts = path.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return if (parts.size > 1) {
                "entity.${parts[0]}.${parts[1]}"
            } else {
                null
            }
        }
    }
}

