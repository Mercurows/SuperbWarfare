package com.atsuishio.superbwarfare.inventory.menu

import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMenuTypes
import com.atsuishio.superbwarfare.item.misc.FiringParametersItem
import com.atsuishio.superbwarfare.item.misc.firingParameters
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData
import com.atsuishio.superbwarfare.network.dataslot.SimpleEnergyData
import com.atsuishio.superbwarfare.network.message.receive.RadarMenuCloseMessage
import com.atsuishio.superbwarfare.network.message.receive.RadarMenuOpenMessage
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.entity.player.PlayerContainerEvent.Close
import net.minecraftforge.event.entity.player.PlayerContainerEvent.Open
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import java.util.*

open class FuMO25Menu(
    pContainerId: Int,
    inventory: Inventory,
    container: Container,
    access: ContainerLevelAccess,
    containerData: ContainerEnergyData
) : EnergyMenu(ModMenuTypes.FUMO_25_MENU.get(), pContainerId, containerData) {
    protected val container: Container
    protected val access: ContainerLevelAccess
    protected val containerData: ContainerEnergyData

    private var posX = Int.MIN_VALUE
    private var posY = Int.MIN_VALUE
    private var posZ = Int.MIN_VALUE

    constructor(pContainerId: Int, pPlayerInventory: Inventory) : this(
        pContainerId,
        pPlayerInventory,
        SimpleContainer(1),
        ContainerLevelAccess.NULL,
        SimpleEnergyData(FuMO25BlockEntity.MAX_DATA_COUNT)
    )

    constructor(
        pContainerId: Int,
        pPlayerInventory: Inventory,
        access: ContainerLevelAccess,
        containerData: ContainerEnergyData
    ) : this(pContainerId, pPlayerInventory, SimpleContainer(1), access, containerData)

    init {
        checkContainerSize(container, 1)

        this.container = container
        this.access = access
        this.containerData = containerData

        this.addSlot(ParaSlot(container, 0, 278, 60))

        for (i in 0..2) {
            for (j in 0..8) {
                this.addSlot(Slot(inventory, j + i * 9 + 9, 8 + j * 18 + X_OFFSET, 84 + i * 18 + Y_OFFSET))
            }
        }

        for (k in 0..8) {
            this.addSlot(Slot(inventory, k, 8 + k * 18 + X_OFFSET, 142 + Y_OFFSET))
        }
    }

    fun setPos(x: Int, y: Int, z: Int) {
        this.posX = x
        this.posY = y
        this.posZ = z
    }

    fun resetPos() {
        this.posX = Int.MIN_VALUE
        this.posY = Int.MIN_VALUE
        this.posZ = Int.MIN_VALUE
    }

    fun setPosToParameters() {
        if (this.posX != Int.MIN_VALUE && this.posY != Int.MIN_VALUE) {
            val stack = this.container.getItem(0)
            if (stack.isEmpty) return

            val parameters = stack.firingParameters
            val isDepressed = parameters.isDepressed
            val radius = parameters.radius

            stack.firingParameters =
                FiringParametersItem.Parameters(BlockPos(this.posX, this.posY, this.posZ), radius, isDepressed)

            this.resetPos()
            this.container.setChanged()
        }
    }

    val currentPos: BlockPos?
        get() {
            if (this.posX != Int.MIN_VALUE && this.posY != Int.MIN_VALUE && this.posZ != Int.MIN_VALUE) {
                return BlockPos(this.posX, this.posY, this.posZ)
            }
            return null
        }

    val selfPos: Optional<BlockPos>
        get() = this.access.evaluate { _, pos -> pos }

    override fun quickMoveStack(pPlayer: Player, pIndex: Int): ItemStack {
        var itemstack = ItemStack.EMPTY
        val slot = this.slots[pIndex]
        if (slot.hasItem()) {
            val itemstack1 = slot.item
            itemstack = itemstack1.copy()
            if (pIndex != 0) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY
                } else if (pIndex in 1..<28) {
                    if (!this.moveItemStackTo(itemstack1, 28, 37, false)) {
                        return ItemStack.EMPTY
                    }
                } else if (pIndex in 28..<37 && !this.moveItemStackTo(itemstack1, 1, 28, false)) {
                    return ItemStack.EMPTY
                }
            } else if (!this.moveItemStackTo(itemstack1, 1, 37, false)) {
                return ItemStack.EMPTY
            }

            if (itemstack1.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }

            if (itemstack1.count == itemstack.count) {
                return ItemStack.EMPTY
            }

            slot.onTake(pPlayer, itemstack1)
        }

        return itemstack
    }

    override fun stillValid(pPlayer: Player): Boolean {
        return this.access.evaluate({ level, pos ->
            level!!.getBlockState(pos).`is`(ModBlocks.FUMO_25.get())
                    && pPlayer.distanceToSqr(
                pos!!.x.toDouble() + 0.5,
                pos.y.toDouble() + 0.5,
                pos.z.toDouble() + 0.5
            ) <= 64
        }, true)
    }

    override fun removed(pPlayer: Player) {
        super.removed(pPlayer)
        this.access.execute { _, _ ->
            val para = this.container.getItem(0)
            if (!para.isEmpty) {
                pPlayer.inventory.placeItemBackInInventory(para)
            }
            this.container.removeItemNoUpdate(0)
            resetPos()
        }
    }

    val energy: Long
        get() = this.containerData[0]

    var funcType: Long
        set(type) {
            this.containerData[1] = type
        }
        get() = this.containerData[1]

    val isPowered: Boolean
        get() = this.containerData[2] == 1L

    internal class ParaSlot(pContainer: Container, pSlot: Int, pX: Int, pY: Int) : Slot(pContainer, pSlot, pX, pY) {
        override fun mayPlace(pStack: ItemStack): Boolean {
            return pStack.`is`(ModItems.FIRING_PARAMETERS.get())
        }
    }

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE)
    companion object {
        const val X_OFFSET: Int = 164
        const val Y_OFFSET: Int = 0

        @SubscribeEvent
        fun onFuMO25MenuOpened(event: Open) {
            val menu = event.container as? FuMO25Menu ?: return
            val player = event.entity as? ServerPlayer ?: return

            menu.selfPos.ifPresent { sendPacketTo(player, RadarMenuOpenMessage(it)) }
        }

        @SubscribeEvent
        fun onFuMO25MenuClosed(event: Close) {
            if (event.container !is FuMO25Menu) return
            val player = event.entity as? ServerPlayer ?: return

            sendPacketTo(player, RadarMenuCloseMessage)
        }
    }
}
