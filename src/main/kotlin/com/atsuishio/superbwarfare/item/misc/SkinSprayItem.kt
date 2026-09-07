package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.client.renderer.item.SkinSprayRenderer
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.IVehicleInteract
import com.atsuishio.superbwarfare.network.message.receive.OpenVehicleSkinScreenMessage
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.sendPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

@RegistryName("skin_spray")
object SkinSprayItem : Item(Properties().stacksTo(1)), IVehicleInteract {
    override fun onInteractVehicle(
        vehicle: VehicleEntity,
        stack: ItemStack,
        player: Player,
        hand: InteractionHand
    ): InteractionResult {
        val level = player.level()
        if (!level.isClientSide) {
            player.sendPacket(OpenVehicleSkinScreenMessage(vehicle.id))
        }
        return InteractionResult.CONSUME
    }

    override fun initializeClient(consumer: Consumer<IClientItemExtensions?>) {
        super.initializeClient(consumer)
        consumer.accept(object : IClientItemExtensions {
            private val renderer by lazy { SkinSprayRenderer(mc.blockEntityRenderDispatcher, mc.entityModels) }

            override fun getCustomRenderer() = renderer
        })
    }
}
