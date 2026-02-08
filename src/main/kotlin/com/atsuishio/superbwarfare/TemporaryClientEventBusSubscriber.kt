package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.client.shader.ThermalShaderHandler
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.menu.EnergyMenu
import com.atsuishio.superbwarfare.menu.FuMO25Menu
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.common.Mod

// TODO 转为kt后移除该方法和文件
fun manuallyRegisterClientEventSubscribers(modBus: IEventBus) {
    arrayOf(
        ModKeyMappings::class.java,
        EnergyMenu::class.java,
        FuMO25Menu::class.java,
        ThermalShaderHandler::class.java,
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