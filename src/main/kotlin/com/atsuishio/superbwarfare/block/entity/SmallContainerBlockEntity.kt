package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.SmallContainerBlock
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.SeededContainerLoot
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.*
import software.bernie.geckolib.util.GeckoLibUtil

open class SmallContainerBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.SMALL_CONTAINER.get(), pos, state), GeoBlockEntity {
    var lootTable: ResourceKey<LootTable>? = null
    var lootTableSeed: Long = 0
    var tick: Int = 0
    var player: Player? = null
    var opened: Boolean = false

    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    private fun predicate(event: AnimationState<SmallContainerBlockEntity>): PlayState? {
        return if (this.blockState.getValue(SmallContainerBlock.OPENED)) {
            event.setAndContinue(RawAnimation.begin().thenPlay("animation.container.open"))
        } else {
            PlayState.STOP
        }
    }

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) {
        data.add(
            AnimationController(this, "controller", 0) { this.predicate(it) }
        )
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return this.cache
    }

    override fun applyImplicitComponents(componentInput: DataComponentInput) {
        super.applyImplicitComponents(componentInput)

        val loot = componentInput.get(DataComponents.CONTAINER_LOOT)
        if (loot != null) {
            this.lootTable = loot.lootTable()
            this.lootTableSeed = loot.seed()
        }
    }

    override fun collectImplicitComponents(components: DataComponentMap.Builder) {
        super.collectImplicitComponents(components)

        if (this.lootTable != null) {
            components.set(DataComponents.CONTAINER_LOOT, SeededContainerLoot(this.lootTable!!, this.lootTableSeed))
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        if (tag.contains("LootTable", 8)) {
            this.lootTable = ResourceKey.create(
                Registries.LOOT_TABLE,
                ResourceLocation.parse(tag.getString("LootTable"))
            )
            this.lootTableSeed = tag.getLong("LootTableSeed")
        }
        this.tick = tag.getInt("Tick")
        this.opened = tag.getBoolean("Opened")
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)

        val lootTable = this.lootTable
        if (lootTable != null) {
            tag.putString("LootTable", lootTable.location().toString())
            if (this.lootTableSeed != 0L) {
                tag.putLong("LootTableSeed", this.lootTableSeed)
            }
        }
        tag.putInt("Tick", this.tick)
        tag.putBoolean("Opened", this.opened)
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return this.saveWithFullMetadata(registries)
    }

    override fun saveToItem(stack: ItemStack, registries: HolderLookup.Provider) {
        val tag = CompoundTag()
        if (this.lootTable != null) {
            tag.putString("LootTable", this.lootTable.toString())
            if (this.lootTableSeed != 0L) {
                tag.putLong("LootTableSeed", this.lootTableSeed)
            }
        }
        BlockItem.setBlockEntityData(stack, this.type, tag)
    }

    fun setLootTable(pLootTable: ResourceKey<LootTable>, pLootTableSeed: Long) {
        this.lootTable = pLootTable
        this.lootTableSeed = pLootTableSeed
    }

    fun unpackLootTable(pPlayer: Player?): MutableList<ItemStack> {
        val level = this.level
        val lootTable = this.lootTable
        val serverLevel = level?.server
        if (lootTable != null && level != null && serverLevel != null) {
            val table = serverLevel.reloadableRegistries().getLootTable(lootTable)
            if (pPlayer is ServerPlayer) {
                CriteriaTriggers.GENERATE_LOOT.trigger(pPlayer, lootTable)
            }

            this.lootTable = null
            val builder = (LootParams.Builder(level as ServerLevel))
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition))
            if (pPlayer != null) {
                builder.withLuck(pPlayer.luck).withParameter(LootContextParams.THIS_ENTITY, pPlayer)
            }

            return table.getRandomItems(builder.create(LootContextParamSets.CHEST), this.lootTableSeed).stream()
                .toList()
        }
        return mutableListOf()
    }

    companion object {
        fun serverTick(pLevel: Level, pPos: BlockPos, pState: BlockState, blockEntity: SmallContainerBlockEntity) {
            if (!pState.getValue(SmallContainerBlock.OPENED)) {
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
                        (1f + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.2f) * 0.7f
                    )
                }
            } else {
                if (blockEntity.opened) return

                val items = blockEntity.unpackLootTable(blockEntity.player)
                if (!items.isEmpty()) {
                    for (item in items) {
                        val entity = ItemEntity(pLevel, pPos.x + 0.5, pPos.y + 0.85, pPos.z + 0.5, item)
                        entity.deltaMovement = Vec3(
                            pLevel.random.nextDouble() * 0.1,
                            0.1,
                            pLevel.random.nextDouble() * 0.1
                        )
                        pLevel.addFreshEntity(entity)
                    }
                }

                blockEntity.opened = true
                blockEntity.setChanged()
                pLevel.setBlockAndUpdate(pPos, Blocks.AIR.defaultBlockState())
            }
        }
    }
}
