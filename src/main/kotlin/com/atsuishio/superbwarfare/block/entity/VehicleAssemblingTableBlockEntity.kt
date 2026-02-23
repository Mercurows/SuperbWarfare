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
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

open class VehicleAssemblingTableBlockEntity(pPos: BlockPos, pBlockState: BlockState) :
    BlockEntity(ModBlockEntities.VEHICLE_ASSEMBLING_TABLE.get(), pPos, pBlockState), MenuProvider, GeoBlockEntity {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

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

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) {}

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return this.cache
    }
}
