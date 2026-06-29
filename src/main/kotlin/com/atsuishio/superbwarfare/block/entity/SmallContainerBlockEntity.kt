package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.SmallContainerBlock
import com.atsuishio.superbwarfare.client.animation.block.SmallContainerBlockAnimationInstance
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.Vec3

open class SmallContainerBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.SMALL_CONTAINER.get(), pos, state) {
    var lootTable: ResourceLocation? = null
    var lootTableSeed: Long = 0
    var tick: Int = 0
    var player: Player? = null
    var opened: Boolean = false

    var animationInstance: SmallContainerBlockAnimationInstance? = null

    override fun load(compound: CompoundTag) {
        super.load(compound)
        if (compound.contains("LootTable", 8)) {
            this.lootTable = ResourceLocation(compound.getString("LootTable"))
            this.lootTableSeed = compound.getLong("LootTableSeed")
        }
        this.tick = compound.getInt("Tick")
        this.opened = compound.getBoolean("Opened")
    }

    public override fun saveAdditional(compound: CompoundTag) {
        super.saveAdditional(compound)
        if (this.lootTable != null) {
            compound.putString("LootTable", this.lootTable.toString())
            if (this.lootTableSeed != 0L) {
                compound.putLong("LootTableSeed", this.lootTableSeed)
            }
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
        if (this.lootTable != null) {
            tag.putString("LootTable", this.lootTable.toString())
            if (this.lootTableSeed != 0L) {
                tag.putLong("LootTableSeed", this.lootTableSeed)
            }
        }
        BlockItem.setBlockEntityData(pStack, this.type, tag)
    }

    fun setLootTable(pLootTable: ResourceLocation?, pLootTableSeed: Long) {
        this.lootTable = pLootTable
        this.lootTableSeed = pLootTableSeed
    }

    fun unpackLootTable(pPlayer: Player?): MutableList<ItemStack> {
        if (this.lootTable != null && this.level != null && this.level!!.server != null) {
            val table = this.level!!.server!!.lootData.getLootTable(this.lootTable!!)
            if (pPlayer is ServerPlayer) {
                CriteriaTriggers.GENERATE_LOOT.trigger(pPlayer, this.lootTable!!)
            }

            this.lootTable = null
            val builder = (LootParams.Builder(this.level as ServerLevel))
                .withParameter<Vec3?>(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition))
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
                        SoundEvents.GENERIC_EXPLODE,
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
