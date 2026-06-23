package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.client.screens.LoiterConfigScreen
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.tools.localPlayer
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/**
 * 监听按键打开自动绕点盘旋配置 GUI（Screen）
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
object LoiterConfigOverlay : CommonOverlay("loiter_config") {

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.START) return

        while (ModKeyMappings.LOITER_CONFIG.consumeClick()) {
            val player = localPlayer ?: continue
            val vehicle = player.vehicle as? VehicleEntity ?: continue
            if (vehicle.computed().engineType != EngineType.AIRCRAFT) continue

            val mc = Minecraft.getInstance()
            if (mc.screen == null) {
                mc.setScreen(LoiterConfigScreen(vehicle))
            }
        }
    }

    override fun shouldRender(): Boolean = false
}
