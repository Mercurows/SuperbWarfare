package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.ContainerBlock
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.joml.Math
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.*
import software.bernie.geckolib.util.GeckoLibUtil

open class ContainerBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.CONTAINER.get(), pos, state), GeoBlockEntity {
    @JvmField
    var entityType: EntityType<*>? = null

    @JvmField
    var entityTag: CompoundTag? = null
    var tick: Int = 0
    var opened: Boolean = false

    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    private fun predicate(event: AnimationState<ContainerBlockEntity>): PlayState? {
        if (this.blockState.getValue(ContainerBlock.OPENED)) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.container.open"))
        }
        return PlayState.STOP
    }

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) {
        data.add(
            AnimationController(this, "controller", 0) { this.predicate(it) }
        )
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return this.cache
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        loadFromTag(tag)
    }

    // 保存额外DataComponent以确保正确生成掉落物
    override fun collectImplicitComponents(components: DataComponentMap.Builder) {
        super.collectImplicitComponents(components)
        components.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(saveToTag()))
    }

    private fun saveToTag(): CompoundTag {
        val tag = CompoundTag()
        tag.putString("id", "superbwarfare:container")
        saveDataToTag(tag)
        return tag
    }

    private fun loadFromTag(tag: CompoundTag) {
        if (tag.contains("EntityType")) {
            this.entityType = EntityType.byString(tag.getString("EntityType")).orElse(null)
        }
        if (tag.contains("Entity") && this.entityTag == null && this.entityType != null) {
            this.entityTag = tag.getCompound("Entity")
        }
        this.tick = tag.getInt("Tick")
        this.opened = tag.getBoolean("Opened")
    }

    private fun saveDataToTag(tag: CompoundTag) {
        val entityType = this.entityType
        if (entityType != null) {
            tag.putString("EntityType", EntityType.getKey(entityType).toString())
        }
        val entityTag = this.entityTag
        if (entityTag != null) {
            tag.put("Entity", entityTag)
        }
        tag.putInt("Tick", this.tick)
        tag.putBoolean("Opened", this.opened)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        saveDataToTag(tag)
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return this.saveWithFullMetadata(registries)
    }

    override fun saveToItem(stack: ItemStack, registries: HolderLookup.Provider) {
        super.saveToItem(stack, registries)

        val tag = CompoundTag()
        val entityType = this.entityType
        if (entityType != null) {
            tag.putString("EntityType", EntityType.getKey(entityType).toString())
        }
        BlockItem.setBlockEntityData(stack, this.type, tag)
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
                        SoundEvents.GENERIC_EXPLODE.value(),
                        SoundSource.BLOCKS,
                        4f,
                        (1 + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.2f) * 0.7f
                    )
                }
            } else {
                if (blockEntity.opened) return

                val direction = pState.getValue(ContainerBlock.FACING)

                val entity = blockEntity.entityType!!.create(pLevel) ?: return

                val entityTag = blockEntity.entityTag
                if (entityTag != null) {
                    entity.load(entityTag)
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
