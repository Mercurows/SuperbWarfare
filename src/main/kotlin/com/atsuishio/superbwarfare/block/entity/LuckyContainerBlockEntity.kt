package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.LuckyContainerBlock
import com.atsuishio.superbwarfare.client.animation.block.LuckyContainerBlockAnimationInstance
import com.atsuishio.superbwarfare.data.container.ContainerDataManager
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.joml.Math

open class LuckyContainerBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.LUCKY_CONTAINER.get(), pos, state) {
    var location: ResourceLocation? = null
    var icon: ResourceLocation? = null
    var tick: Int = 0
    var opened: Boolean = false

    var animationInstance: LuckyContainerBlockAnimationInstance? = null

    fun unpackEntities(): EntityType<*>? {
        if (this.location != null && this.level != null && this.level!!.server != null) {
            val dataManager = ContainerDataManager
            val list = dataManager.getEntityTypes(this.location!!)
            if (!list.isEmpty()) {
                val sum = list.stream().mapToInt { it.second() }.sum()
                if (sum <= 0) return null

                val rand = this.level!!.random.nextInt(sum)

                var cumulativeWeight = 0
                for (entry in list) {
                    cumulativeWeight += entry.second()
                    if (rand < cumulativeWeight) {
                        return EntityType.byString(entry.first()).orElse(null)
                    }
                }
            }
        }
        return null
    }

    override fun load(compound: CompoundTag) {
        super.load(compound)
        if (compound.contains("Location", 8)) {
            this.location = ResourceLocation(compound.getString("Location"))
        }
        if (compound.contains("Icon", 8)) {
            this.icon = ResourceLocation(compound.getString("Icon"))
        }
        this.tick = compound.getInt("Tick")
        this.opened = compound.getBoolean("Opened")
    }

    public override fun saveAdditional(compound: CompoundTag) {
        super.saveAdditional(compound)
        if (this.location != null) {
            compound.putString("Location", this.location.toString())
        }
        if (this.icon != null) {
            compound.putString("Icon", this.icon.toString())
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
        if (this.location != null) {
            tag.putString("Location", this.location.toString())
        }
        if (this.icon != null) {
            tag.putString("Icon", this.icon.toString())
        }
        BlockItem.setBlockEntityData(pStack, this.type, tag)
    }

    companion object {
        fun serverTick(pLevel: Level, pPos: BlockPos, pState: BlockState, blockEntity: LuckyContainerBlockEntity) {
            if (!pState.getValue(LuckyContainerBlock.OPENED)) {
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

                val direction = pState.getValue(LuckyContainerBlock.FACING)
                val type = blockEntity.unpackEntities()

                blockEntity.opened = true
                blockEntity.setChanged()

                if (type != null) {
                    val entity: Entity? = type.create(pLevel)
                    if (entity != null) {
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
                    }
                }

                pLevel.setBlockAndUpdate(pPos, Blocks.AIR.defaultBlockState())
            }
        }
    }
}
