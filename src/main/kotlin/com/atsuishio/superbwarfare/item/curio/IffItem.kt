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
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.ServerTickEvent
import top.theillusivec4.curios.api.CuriosApi
import top.theillusivec4.curios.api.SlotContext
import top.theillusivec4.curios.api.type.capability.ICurioItem

open class IffItem : Item(Properties().stacksTo(1)), ICurioItem {
    override fun canEquip(slotContext: SlotContext, stack: ItemStack?): Boolean {
        return CuriosApi.getCuriosInventory(slotContext.entity())
            .flatMap { c -> c.findFirstCurio(this) }
            .isEmpty
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.iff_1").withStyle(ChatFormatting.GRAY))
    }

    @EventBusSubscriber
    companion object {
        @SubscribeEvent
        fun onIFFItemServerTick(event: ServerTickEvent.Post) {
            if (!MiscConfig.SYNC_ENTITY_OVER_RANGE.get()) return
            val server = event.server
            if (server.tickCount % MiscConfig.SYNC_ENTITY_INTERVAL.get() != 0) return

            for (level in server.allLevels) {
                val entities = level.allEntities
                    .asSequence()
                    .filter { it is VehicleEntity && SeekTool.NOT_IN_SMOKE.test(it) }
                    .toList()

                val players = server.playerList.players
                for (player in players) {
                    if (!player.isAlive) continue
                    CuriosApi.getCuriosInventory(player)
                        .flatMap { c -> c.findFirstCurio(ModItems.IFF.get()) }
                        .ifPresent { _ ->
                            val list = entities.mapNotNull {
                                if (!SeekTool.IS_FRIENDLY.test(player, it)) return@mapNotNull null
                                EntitySyncMessage.SyncedEntity(
                                    it.id,
                                    BuiltInRegistries.ENTITY_TYPE.getKey(it.type),
                                    it.position(),
                                    it.deltaMovement,
                                    CompoundTag().also { tag -> it.saveWithoutId(tag) }
                                )
                            }.toList()
                            sendPacketTo(player, EntitySyncMessage(level.dimension().location(), list, true))

                            val playerList = players
                                .asSequence()
                                .mapNotNull {
                                    if (level != it.level()) return@mapNotNull null
                                    if (!SeekTool.IS_FRIENDLY.test(player, it)) return@mapNotNull null
                                    val vehicle = it.vehicle
                                    if (vehicle != null) {
                                        PlayerInfoSyncMessage.SyncedPlayerInfo(
                                            it.uuid,
                                            if (vehicle is VehicleEntity)
                                                VectorTool.lerpGetEntityBoundingBoxCenter(vehicle, 1f)
                                            else it.position(),
                                            it.displayName?.string ?: "",
                                            onVehicle = true,
                                            it == vehicle.firstPassenger
                                        )
                                    } else {
                                        PlayerInfoSyncMessage.SyncedPlayerInfo(
                                            it.uuid,
                                            it.position(),
                                            it.displayName?.string ?: "",
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
