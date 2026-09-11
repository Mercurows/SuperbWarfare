package com.atsuishio.superbwarfare.item.ammo

import com.atsuishio.superbwarfare.capability.entity.InfiniteAmmoCapability
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.registerToEventBus
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

@RegistryName("creative_ammo_box")
object CreativeAmmoBoxItem : Item(Properties().rarity(Rarity.EPIC).stacksTo(1)) {

    init {
        registerToEventBus(this)
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.creative_ammo_box_1").withStyle(ChatFormatting.GRAY)
        )
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.creative_ammo_box_2").withStyle(ChatFormatting.GRAY)
        )
    }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val item = player.getItemInHand(usedHand)
        if (player.isShiftKeyDown) {
            invertInfiniteAmmo(player, player)
            return InteractionResultHolder.success(item)
        }
        return super.use(level, player, usedHand)
    }

    @SubscribeEvent
    fun onEntityInteract(event: PlayerInteractEvent.EntityInteract) {
        if (!event.itemStack.`is`(this)) return
        val player = event.entity
        if (player.level().isClientSide) return
        val res = invertInfiniteAmmo(player, event.target)
        if (res) {
            event.cancellationResult = InteractionResult.FAIL
            event.isCanceled = true
        }
    }

    private fun invertInfiniteAmmo(player: Player? = null, entity: Entity): Boolean {
        if (entity.level().isClientSide) return false

        val hasInfiniteAmmo = InfiniteAmmoCapability.toggle(entity)

        player?.displayClientMessage(
            Component.translatable(
                "des.superbwarfare.creative_ammo_box.${if (hasInfiniteAmmo) "enabled" else "disabled"}",
                entity.displayName
            ).withStyle(if (hasInfiniteAmmo) ChatFormatting.GREEN else ChatFormatting.RED),
            true
        )

        return true
    }
}
