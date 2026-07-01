package com.atsuishio.superbwarfare.item.curio

import com.atsuishio.superbwarfare.network.message.receive.EntityRelationSyncMessage
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.ServerSyncedEntityHandler
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

import top.theillusivec4.curios.api.CuriosApi
import top.theillusivec4.curios.api.SlotContext
import top.theillusivec4.curios.api.type.capability.ICurioItem

open class IffItem : Item(Properties().stacksTo(1)), ICurioItem {
    override fun canEquip(slotContext: SlotContext, stack: ItemStack?): Boolean {
        return CuriosApi.getCuriosInventory(slotContext.entity)
            .map { it.findFirstCurio(this).isEmpty }
            .orElse(false)
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component?>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(Component.translatable("des.superbwarfare.iff_1").withStyle(ChatFormatting.GRAY))
    }

    @Mod.EventBusSubscriber
    companion object {
        @SubscribeEvent
        fun onServerTick(event: TickEvent.ServerTickEvent) {
            if (event.phase == TickEvent.Phase.START) return
            val server = event.server

            for (player in server.playerList.players) {
                if (!player.isAlive) continue
                // 将自己注册到 ServerSyncedEntityHandler，供雷达等系统发现
                ServerSyncedEntityHandler.register(player)

                // 向所有队友同步自身 ID（轻量级，实体状态数据由 BeyondVisualEntitySyncMessage 统一发送）
                val dim = player.level().dimension().location()
                val msg = EntityRelationSyncMessage(dim, friendlyIds = listOf(player.id))
                for (teammate in server.playerList.players) {
                    if (teammate != player && teammate.isAlive
                        && teammate.level().dimension() == player.level().dimension()
                        && SeekTool.IS_FRIENDLY.test(teammate, player)
                    ) {
                        sendPacketTo(teammate, msg)
                    }
                }
            }
        }
    }
}
