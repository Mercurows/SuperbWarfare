package com.atsuishio.superbwarfare.item.curio

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import top.theillusivec4.curios.api.CuriosApi
import top.theillusivec4.curios.api.SlotContext
import top.theillusivec4.curios.api.type.capability.ICurioItem
import javax.annotation.ParametersAreNonnullByDefault

class IffItem : Item(Properties().stacksTo(1)), ICurioItem {
    override fun canEquip(slotContext: SlotContext, stack: ItemStack?): Boolean {
        return CuriosApi.getCuriosInventory(slotContext.entity())
            .flatMap { c -> c.findFirstCurio(this) }
            .isEmpty
    }

    @ParametersAreNonnullByDefault
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.iff_1").withStyle(ChatFormatting.GRAY))
    }

    override fun curioTick(slotContext: SlotContext, stack: ItemStack?) {
        val living = slotContext.entity()
        if (living is Player && living.level() is ServerLevel) {
            val server = living.server
            if (server != null) {
                if (server.tickCount % MiscConfig.SYNC_ENTITY_INTERVAL.get() != 0) return
                for (level in server.allLevels) {
                    val friendlyList = arrayListOf<EntitySyncMessage.SyncedEntity>()
                    for (entity in level.allEntities) {
                        if (!SeekTool.NOT_IN_SMOKE.test(entity)) continue
                        if (entity is VehicleEntity) {
                            val synced = EntitySyncMessage.SyncedEntity(
                                entity.id,
                                BuiltInRegistries.ENTITY_TYPE.getKey(entity.type),
                                entity.position(),
                                entity.deltaMovement,
                                entity.serializeNBT(server.registryAccess())
                            )

                            if (SeekTool.IS_FRIENDLY.test(living, entity)) {
                                friendlyList.add(synced)
                            }
                        }
                    }

                    sendPacketTo(living, EntitySyncMessage(level.dimension().location(), friendlyList, true))
                }
            }
        }
    }
}
