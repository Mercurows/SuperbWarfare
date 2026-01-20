package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.advancement.CriteriaRegister
import com.atsuishio.superbwarfare.capability.CapabilityHandler
import com.atsuishio.superbwarfare.command.CommandRegister
import com.atsuishio.superbwarfare.compat.CompatHolder
import com.atsuishio.superbwarfare.data.DataLoader
import com.atsuishio.superbwarfare.data.container.ContainerDataManager
import com.atsuishio.superbwarfare.datagen.DataGenerators
import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity
import com.atsuishio.superbwarfare.entity.SenpaiEntity
import com.atsuishio.superbwarfare.entity.TargetEntity
import com.atsuishio.superbwarfare.event.*
import com.atsuishio.superbwarfare.init.ModAttributes
import com.atsuishio.superbwarfare.init.ModLootModifier
import com.atsuishio.superbwarfare.init.ModTabs
import com.atsuishio.superbwarfare.init.ModVillagers
import com.atsuishio.superbwarfare.item.Hammer
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem
import com.atsuishio.superbwarfare.mobeffect.BurnMobEffect
import com.atsuishio.superbwarfare.mobeffect.PhosphorusFireMobEffect
import com.atsuishio.superbwarfare.mobeffect.ShockMobEffect
import com.atsuishio.superbwarfare.mobeffect.TraumaMobEffect
import com.atsuishio.superbwarfare.perk.functional.PowerfulAttraction
import com.atsuishio.superbwarfare.procedures.WelcomeProcedure
import com.atsuishio.superbwarfare.recipe.ModPotionRecipes
import com.atsuishio.superbwarfare.tools.GunsTool
import com.atsuishio.superbwarfare.tools.ResourceOnceLogger
import com.atsuishio.superbwarfare.world.TDMSavedData
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.common.Mod

private val classes = arrayOf(
    CriteriaRegister::class.java,
    CapabilityHandler::class.java,
    CommandRegister::class.java,
    CompatHolder::class.java,
    DataLoader::class.java,
    DataLoader.ClientReloadListener::class.java,
    ContainerDataManager::class.java,
    DataGenerators::class.java,
    DPSGeneratorEntity::class.java,
    SenpaiEntity::class.java,
    TargetEntity::class.java,
    EntityUseGunEventHandler::class.java,
    GunEventHandler::class.java,
    HitboxHelperEventHandler::class.java,
    LivingEventHandler::class.java,
    PlayerEventHandler::class.java,
    ModAttributes::class.java,
    ModLootModifier::class.java,
    ModTabs::class.java,
    ModVillagers::class.java,
    Hammer::class.java,
    ContainerBlockItem::class.java,
    BurnMobEffect::class.java,
    ShockMobEffect::class.java,
    TraumaMobEffect::class.java,
    PhosphorusFireMobEffect::class.java,
    PowerfulAttraction::class.java,
    WelcomeProcedure::class.java,
    ModPotionRecipes::class.java,
    GunsTool::class.java,
    ResourceOnceLogger::class.java,
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