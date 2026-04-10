package com.atsuishio.superbwarfare.item.curio

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage
import com.atsuishio.superbwarfare.network.message.receive.PlayerInfoSyncMessage
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.VectorTool
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
import net.minecraftforge.registries.ForgeRegistries
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
        fun onIFFItemServerTick(event: TickEvent.ServerTickEvent) {
            if (event.phase == TickEvent.Phase.START) return
            if (!MiscConfig.SYNC_ENTITY_OVER_RANGE.get()) return
            val server = event.server
            if (server.tickCount % MiscConfig.SYNC_ENTITY_INTERVAL.get() != 0) return

            for (level in server.allLevels) {
                val entities = level.allEntities
                    .asSequence()
                    .filter { it is VehicleEntity && SeekTool.NOT_IN_SMOKE.test(it) }
                    .toList()

                for (player in server.playerList.players) {
                    if (!player.isAlive) continue
                    CuriosApi.getCuriosInventory(player).ifPresent { c ->
                        c.findFirstCurio(ModItems.IFF.get()).ifPresent {
                            val list = entities.mapNotNull {
                                if (!SeekTool.IS_FRIENDLY.test(player, it)) return@mapNotNull null
                                EntitySyncMessage.SyncedEntity(
                                    it.id,
                                    ForgeRegistries.ENTITY_TYPES.getKey(it.type)!!,
                                    it.position(),
                                    it.deltaMovement,
                                    it.serializeNBT()
                                )
                            }.toList()
                            sendPacketTo(player, EntitySyncMessage(level.dimension().location(), list, true))

                            val playerList = server.playerList.players
                                .asSequence()
                                .mapNotNull {
                                    if (!SeekTool.IS_FRIENDLY.test(player, it)) return@mapNotNull null
                                    if (it.vehicle != null) {
                                        PlayerInfoSyncMessage.SyncedPlayerInfo(
                                            it.uuid,
                                            VectorTool.lerpGetEntityBoundingBoxCenter(it.vehicle!!, 1f),
                                            it.displayName.string,
                                            onVehicle = true,
                                            it == it.vehicle!!.firstPassenger
                                        )
                                    } else {
                                        PlayerInfoSyncMessage.SyncedPlayerInfo(
                                            it.uuid,
                                            it.position(),
                                            it.displayName.string,
                                            onVehicle = false,
                                            isDriver = false
                                        )
                                    }
                                }.toList()
                            sendPacketTo(player, PlayerInfoSyncMessage(level.dimension().location(), playerList))
                        }
                    }
                }
            }
        }
    }
}
