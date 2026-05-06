package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.LuckyContainerBlock
import com.atsuishio.superbwarfare.data.container.ContainerDataManager
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
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
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

open class LuckyContainerBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.LUCKY_CONTAINER.get(), pos, state), GeoBlockEntity {
    var location: ResourceLocation? = null
    var icon: ResourceLocation? = null
    var tick: Int = 0
    var opened: Boolean = false

    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

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

    private fun predicate(event: AnimationState<LuckyContainerBlockEntity?>): PlayState? {
        return if (this.blockState.getValue(LuckyContainerBlock.OPENED)) {
            event.setAndContinue(RawAnimation.begin().thenPlay("animation.container.open"))
        } else PlayState.STOP
    }

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) {
        data.add(
            AnimationController(this, "controller", 0) { this.predicate(it) }
        )
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return this.cache
    }

    override fun collectImplicitComponents(components: DataComponentMap.Builder) {
        super.collectImplicitComponents(components)
        components.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(saveToTag()))
    }

    override fun applyImplicitComponents(componentInput: DataComponentInput) {
        super.applyImplicitComponents(componentInput)
        val data = componentInput.get(DataComponents.BLOCK_ENTITY_DATA)
        if (data != null) {
            this.loadFromTag(data.copyTag())
        }
    }

    private fun saveToTag(): CompoundTag {
        val tag = CompoundTag()
        tag.putString("id", "superbwarfare:lucky_container")
        saveDataToTag(tag)
        return tag
    }

    private fun loadFromTag(tag: CompoundTag) {
        if (tag.contains("Location", 8)) {
            this.location = ResourceLocation.parse(tag.getString("Location"))
        }
        if (tag.contains("Icon", 8)) {
            this.icon = ResourceLocation.parse(tag.getString("Icon"))
        }
        this.tick = tag.getInt("Tick")
        this.opened = tag.getBoolean("Opened")
    }

    private fun saveDataToTag(tag: CompoundTag) {
        if (this.location != null) {
            tag.putString("Location", this.location.toString())
        }
        if (this.icon != null) {
            tag.putString("Icon", this.icon.toString())
        }
        tag.putInt("Tick", this.tick)
        tag.putBoolean("Opened", this.opened)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        this.loadFromTag(tag)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        this.saveDataToTag(tag)
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return this.saveWithFullMetadata(registries)
    }

    override fun saveToItem(stack: ItemStack, registries: HolderLookup.Provider) {
        val tag = CompoundTag()
        if (this.location != null) {
            tag.putString("Location", this.location.toString())
        }
        if (this.icon != null) {
            tag.putString("Icon", this.icon.toString())
        }
        BlockItem.setBlockEntityData(stack, this.type, tag)
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
                        SoundEvents.GENERIC_EXPLODE.value(),
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
