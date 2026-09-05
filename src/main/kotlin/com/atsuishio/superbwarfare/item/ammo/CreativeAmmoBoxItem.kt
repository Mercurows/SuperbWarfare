package com.atsuishio.superbwarfare.item.ammo

import com.atsuishio.superbwarfare.capability.entity.InfiniteAmmoCapability
import com.atsuishio.superbwarfare.network.message.receive.ClientInfiniteAmmoMessage
import com.atsuishio.superbwarfare.registerToEventBus
import com.atsuishio.superbwarfare.tools.sendPacketTo
import com.atsuishio.superbwarfare.tools.sendPacketToTrackingThis
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

object CreativeAmmoBoxItem : Item(Properties().rarity(Rarity.EPIC).stacksTo(1)) {

    init {
        registerToEventBus(this)
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.creative_ammo_box").withStyle(ChatFormatting.GRAY)
        )
    }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        invertInfinityAmmo(player, player)
        return super.use(level, player, usedHand)
    }

    @SubscribeEvent
    fun onEntityInteract(event: PlayerInteractEvent.EntityInteract) {
        if (!event.itemStack.`is`(this)) return
        val res = invertInfinityAmmo(event.entity, event.target)
        if (res) {
            event.isCanceled = true
        }
    }

    private fun invertInfinityAmmo(player: Player? = null, entity: Entity): Boolean {
        if (entity.level().isClientSide) return false

        var hasInfiniteAmmo = false

        InfiniteAmmoCapability.modify(entity) {
            hasInfiniteAmmo = !it.hasInfinityAmmo
            it.hasInfinityAmmo = hasInfiniteAmmo
        }

        // TODO message
        player?.displayClientMessage(Component.literal(if (hasInfiniteAmmo) "+ infinity" else "- infinity"), true)

        if (entity is Player) {
            sendPacketTo(entity, ClientInfiniteAmmoMessage(entity.id, hasInfiniteAmmo))
        }
        entity.sendPacketToTrackingThis(ClientInfiniteAmmoMessage(entity.id, hasInfiniteAmmo))
        return true
    }
}
