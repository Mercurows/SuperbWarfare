package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.inventory.menu.VehicleAssemblingMenu
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB

open class VehicleAssemblingTableBlockEntity(pPos: BlockPos, pBlockState: BlockState) :
    BlockEntity(ModBlockEntities.VEHICLE_ASSEMBLING_TABLE.get(), pPos, pBlockState), MenuProvider {

    override fun getDisplayName(): Component {
        return Component.empty()
    }

    override fun createMenu(pContainerId: Int, pPlayerInventory: Inventory, pPlayer: Player): AbstractContainerMenu {
        return VehicleAssemblingMenu(
            pContainerId,
            pPlayerInventory,
            ContainerLevelAccess.create(pPlayer.level(), this.worldPosition)
        )
    }

    override fun getRenderBoundingBox(): AABB {
        val expansion = 2.0
        return AABB(
            (worldPosition.x - 1).toDouble(),
            worldPosition.y.toDouble(),
            (worldPosition.z - 1).toDouble(),
            (worldPosition.x + 2).toDouble(),
            worldPosition.y + expansion,
            (worldPosition.z + 2).toDouble()
        )
    }
}
