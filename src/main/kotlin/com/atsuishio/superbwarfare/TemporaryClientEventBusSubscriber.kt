package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.client.ClickHandler
import com.atsuishio.superbwarfare.client.ClientRenderHandler
import com.atsuishio.superbwarfare.client.renderer.curio.ParachuteRenderer
import com.atsuishio.superbwarfare.client.renderer.special.ContainerBlockPreview
import com.atsuishio.superbwarfare.client.renderer.special.PhosphorusFireRenderer
import com.atsuishio.superbwarfare.client.screens.modsell.ModSellWarningScreen
import com.atsuishio.superbwarfare.client.shader.ThermalShaderHandler
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.item.common.ammo.PotionMortarShell
import com.atsuishio.superbwarfare.menu.EnergyMenu
import com.atsuishio.superbwarfare.menu.FuMO25Menu
import com.atsuishio.superbwarfare.tools.VectorUtil
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.common.Mod

// TODO 转为kt后移除该方法和文件
fun manuallyRegisterClientEventSubscribers(modBus: IEventBus) {
    arrayOf(
        ClickHandler::class.java,
        ClientRenderHandler::class.java,
        ParachuteRenderer::class.java,
        ContainerBlockPreview::class.java,
        ModSellWarningScreen::class.java,
        ClientEventHandler::class.java,
        ModEntityRenderers::class.java,
        ModKeyMappings::class.java,
        ModParticles::class.java,
        ModProperties::class.java,
        ModScreens::class.java,
        PotionMortarShell::class.java,
        EnergyMenu::class.java,
        FuMO25Menu::class.java,
        VectorUtil::class.java,
        ThermalShaderHandler::class.java,
        PhosphorusFireRenderer::class.java,
    ).forEach {
        run {
            val busEnum = it.annotations.filterIsInstance<Mod.EventBusSubscriber>().firstOrNull()?.bus ?: return@run
            if (busEnum == Mod.EventBusSubscriber.Bus.MOD) {
                // 牛魔的 Bus.MOD这里为什么是写死的FMLJavaModLoadingContext
                modBus.register(it)
            } else {
                busEnum.bus().get().register(it)
            }
        }
    }
}