package com.atsuishio.superbwarfare.inventory.menu

import com.atsuishio.superbwarfare.entity.vehicle.VehicleAssemblingTableVehicleEntity
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.ModMenuTypes
import com.atsuishio.superbwarfare.network.message.receive.FinishAssemblingVehicleMessage
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe
import com.atsuishio.superbwarfare.tools.sendPacketTo
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraftforge.common.capabilities.ForgeCapabilities

open class VehicleAssemblingMenu @JvmOverloads constructor(
    pContainerId: Int,
    inventory: Inventory,
    protected val access: ContainerLevelAccess = ContainerLevelAccess.NULL,
    private val isVehicleMenu: Boolean = false
) : AbstractContainerMenu(ModMenuTypes.VEHICLE_ASSEMBLING_MENU.get(), pContainerId) {
    override fun quickMoveStack(pPlayer: Player, pIndex: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun stillValid(pPlayer: Player): Boolean {
        return (pPlayer.isAlive && !this.isVehicleMenu &&
                this.access.evaluate({ level, pos ->
                    level.getBlockState(pos).`is`(ModBlocks.VEHICLE_ASSEMBLING_TABLE.get())
                            && pPlayer.distanceToSqr(
                        pos.x.toDouble() + 0.5,
                        pos.y.toDouble() + 0.5,
                        pos.z.toDouble() + 0.5
                    ) <= 64
                }, true))
                || (this.isVehicleMenu && pPlayer.vehicle is VehicleAssemblingTableVehicleEntity)
    }

    /**
     * Code based on TaC-Z
     */
    fun assembleVehicle(id: ResourceLocation, player: ServerPlayer) {
        val recipe = this.getRecipeById(id, player.level().recipeManager) ?: return
        player.getCapability(ForgeCapabilities.ITEM_HANDLER)
            .ifPresent { handler ->
                if (!player.isCreative) {
                    val recordCount = Int2IntArrayMap()
                    val ingredients = recipe.inputs

                    for (ingredient in ingredients) {
                        var count = 0

                        for (i in 0..<handler.slots) {
                            val stack = handler.getStackInSlot(i)
                            val stackCount = stack.count
                            if (!stack.isEmpty && ingredient.ingredient.test(stack)) {
                                count += stackCount
                                if (count > ingredient.count) {
                                    val remaining = count - ingredient.count
                                    recordCount.put(i, stackCount - remaining)
                                    break
                                }
                                recordCount.put(i, stackCount)
                            }
                        }

                        if (count < ingredient.count) {
                            return@ifPresent
                        }
                    }

                    recordCount.keys.forEach {
                        handler.extractItem(it, recordCount.get(it), false)
                    }
                }
                val level = player.level()
                if (!level.isClientSide) {
                    val itemEntity = ItemEntity(
                        level,
                        player.x,
                        player.y + 0.5,
                        player.z,
                        recipe.getResultItem(player.level().registryAccess()).copy()
                    )
                    itemEntity.setPickUpDelay(0)
                    level.addFreshEntity(itemEntity)
                }

                player.inventoryMenu.broadcastFullState()
                sendPacketTo(player, FinishAssemblingVehicleMessage(this.containerId))
            }
    }

    fun getRecipeById(id: ResourceLocation, recipeManager: RecipeManager): VehicleAssemblingRecipe? {
        val recipe: Recipe<*>? = recipeManager.byKey(id).orElse(null)
        if (recipe is VehicleAssemblingRecipe) {
            return recipe
        }
        return null
    }
}