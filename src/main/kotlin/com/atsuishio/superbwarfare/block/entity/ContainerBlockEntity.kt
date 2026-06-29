package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.ContainerBlock
import com.atsuishio.superbwarfare.client.animation.block.ContainerBlockAnimationInstance
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.joml.Math

open class ContainerBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.CONTAINER.get(), pos, state) {

    var animationInstance: ContainerBlockAnimationInstance? = null

    @JvmField
    var entityType: EntityType<*>? = null

    @JvmField
    var entityTag: CompoundTag? = null
    var tick: Int = 0
    var opened: Boolean = false

    override fun load(compound: CompoundTag) {
        super.load(compound)
        if (compound.contains("EntityType")) {
            this.entityType = EntityType.byString(compound.getString("EntityType")).orElse(null)
        }
        if (compound.contains("Entity")) {
            this.entityTag = compound.getCompound("Entity")
        }
        this.tick = compound.getInt("Tick")
        this.opened = compound.getBoolean("Opened")
    }

    public override fun saveAdditional(compound: CompoundTag) {
        super.saveAdditional(compound)
        if (this.entityTag != null) {
            compound.put("Entity", this.entityTag!!)
        }
        if (this.entityType != null) {
            compound.putString("EntityType", EntityType.getKey(this.entityType!!).toString())
        }
        compound.putInt("Tick", this.tick)
        compound.putBoolean("Opened", this.opened)
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(): CompoundTag {
        return this.saveWithFullMetadata()
    }

    override fun saveToItem(pStack: ItemStack) {
        val tag = CompoundTag()
        if (this.entityType != null) {
            tag.putString("EntityType", EntityType.getKey(this.entityType!!).toString())
        }
        BlockItem.setBlockEntityData(pStack, this.type, tag)
    }

    companion object {
        fun serverTick(pLevel: Level, pPos: BlockPos, pState: BlockState, blockEntity: ContainerBlockEntity) {
            if (!pState.getValue(ContainerBlock.OPENED)) {
                return
            }

            if (blockEntity.tick < 20) {
                blockEntity.tick++
                blockEntity.setChanged()

                if (blockEntity.tick == 18) {
                    ParticleTool.sendParticle(
                        pLevel as ServerLevel,
                        ParticleTypes.EXPLOSION,
                        pPos.x.toDouble(),
                        (pPos.y + 1).toDouble(),
                        pPos.z.toDouble(),
                        40,
                        1.5,
                        1.5,
                        1.5,
                        1.0,
                        false
                    )
                    pLevel.playSound(
                        null,
                        pPos,
                        SoundEvents.GENERIC_EXPLODE,
                        SoundSource.BLOCKS,
                        4f,
                        (1 + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.2f) * 0.7f
                    )
                }
            } else {
                if (blockEntity.opened) return

                val direction = pState.getValue(ContainerBlock.FACING)

                val entity = blockEntity.entityType!!.create(pLevel) ?: return

                if (blockEntity.entityTag != null) {
                    entity.load(blockEntity.entityTag!!)
                }

                blockEntity.opened = true
                blockEntity.setChanged()

                entity.setPos(
                    pPos.x + 0.5 + (2 * Math.random() - 1) * 0.1f,
                    pPos.y + 0.5 + (2 * Math.random() - 1) * 0.1f,
                    pPos.z + 0.5 + (2 * Math.random() - 1) * 0.1f
                )
                entity.yRot = direction.toYRot()
                if (entity is VehicleEntity) {
                    entity.serverYaw = direction.toYRot()
                }
                pLevel.addFreshEntity(entity)

                pLevel.setBlockAndUpdate(pPos, Blocks.AIR.defaultBlockState())
            }
        }
    }
}
