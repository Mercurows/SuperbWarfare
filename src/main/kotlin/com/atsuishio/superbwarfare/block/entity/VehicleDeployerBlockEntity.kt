package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.VehicleDeployerBlock
import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.joml.Math
import java.util.*

open class VehicleDeployerBlockEntity(pPos: BlockPos, pBlockState: BlockState) :
    BlockEntity(ModBlockEntities.VEHICLE_DEPLOYER.get(), pPos, pBlockState) {
    @JvmField
    var entityData: CompoundTag = CompoundTag()

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)

        if (this.entityData.contains("EntityType")) {
            tag.putString("EntityType", this.entityData.getString("EntityType"))
        }
        if (this.entityData.contains("Entity")) {
            tag.put("Entity", this.entityData.getCompound("Entity"))
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        this.entityData = tag.copy()
    }

    fun deploy(state: BlockState) {
        val level = this.level ?: return

        if (this.entityData.contains("EntityType")) {
            val entityType = EntityType.byString(entityData.getString("EntityType")).orElse(null) ?: return

            val entity = entityType.create(level) ?: return

            if (entityData.contains("Entity")) {
                val entityTag = entityData.getCompound("Entity").copy()
                entityTag.remove("UUID")
                entity.load(entityTag)
            }

            val direction = state.getValue(VehicleDeployerBlock.FACING)

            entity.setUUID(UUID.randomUUID())
            entity.setPos(
                this.blockPos.x + 0.5 + (2 * Math.random() - 1) * 0.1f,
                this.blockPos.y + 1.5 + (2 * Math.random() - 1) * 0.1f,
                this.blockPos.z + 0.5 + (2 * Math.random() - 1) * 0.1f
            )
            entity.yRot = direction.toYRot()
            level.addFreshEntity(entity)
        }
    }

    fun writeEntityInfo(stack: ItemStack) {
        val tag = stack.get(DataComponents.BLOCK_ENTITY_DATA) ?: return

        this.entityData = tag.copyTag()
    }
}
