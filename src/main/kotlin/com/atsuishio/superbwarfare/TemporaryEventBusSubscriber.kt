package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.compat.CompatHolder
import com.atsuishio.superbwarfare.data.container.ContainerDataManager
import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity
import com.atsuishio.superbwarfare.entity.SenpaiEntity
import com.atsuishio.superbwarfare.entity.TargetEntity
import com.atsuishio.superbwarfare.init.ModAttributes
import com.atsuishio.superbwarfare.init.ModLootModifier
import com.atsuishio.superbwarfare.init.ModTabs
import com.atsuishio.superbwarfare.init.ModVillagers
import com.atsuishio.superbwarfare.item.Hammer
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem
import com.atsuishio.superbwarfare.procedures.WelcomeProcedure
import com.atsuishio.superbwarfare.recipe.ModPotionRecipes
import com.atsuishio.superbwarfare.world.TDMSavedData
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.common.Mod

private val classes = arrayOf(
    CompatHolder::class.java,
    ContainerDataManager::class.java,
    DPSGeneratorEntity::class.java,
    SenpaiEntity::class.java,
    TargetEntity::class.java,
    ModAttributes::class.java,
    ModLootModifier::class.java,
    ModTabs::class.java,
    ModVillagers::class.java,
    Hammer::class.java,
    ContainerBlockItem::class.java,
    WelcomeProcedure::class.java,
    ModPotionRecipes::class.java,
    TDMSavedData::class.java,
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