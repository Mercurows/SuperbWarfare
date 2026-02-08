package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem
import com.atsuishio.superbwarfare.procedures.WelcomeProcedure
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.common.Mod

private val classes = arrayOf(
    ContainerBlockItem::class.java,
    WelcomeProcedure::class.java,
)

// TODO 转为kt后移除该方法和文件
fun manuallyRegisterEventSubscribers(modBus: IEventBus) {
    for (it in classes) {
        val busEnum = it.annotations.filterIsInstance<Mod.EventBusSubscriber>().firstOrNull()?.bus ?: continue

        if (busEnum == Mod.EventBusSubscriber.Bus.MOD) {
            // 牛魔的 Bus.MOD这里为什么是写死的FMLJavaModLoadingContext
            modBus.register(it)
        } else {
            busEnum.bus().get().register(it)
        }
    }
}