package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.advancement.CriteriaRegister
import com.atsuishio.superbwarfare.capability.CapabilityHandler
import com.atsuishio.superbwarfare.client.ClickHandler
import com.atsuishio.superbwarfare.client.ClientRenderHandler
import com.atsuishio.superbwarfare.client.language.ClientLanguageGetter
import com.atsuishio.superbwarfare.client.renderer.curio.ParachuteRenderer
import com.atsuishio.superbwarfare.client.renderer.special.ContainerBlockPreview
import com.atsuishio.superbwarfare.client.renderer.special.PhosphorusFireRenderer
import com.atsuishio.superbwarfare.client.screens.FuMO25ScreenHelper
import com.atsuishio.superbwarfare.client.screens.modsell.ModSellWarningScreen
import com.atsuishio.superbwarfare.client.shader.ThermalShaderHandler
import com.atsuishio.superbwarfare.command.CommandRegister
import com.atsuishio.superbwarfare.compat.CompatHolder
import com.atsuishio.superbwarfare.data.DataLoader
import com.atsuishio.superbwarfare.data.container.ContainerDataManager
import com.atsuishio.superbwarfare.data.vehicle.VehicleDataTool
import com.atsuishio.superbwarfare.datagen.DataGenerators
import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity
import com.atsuishio.superbwarfare.entity.SenpaiEntity
import com.atsuishio.superbwarfare.entity.TargetEntity
import com.atsuishio.superbwarfare.event.*
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.item.Hammer
import com.atsuishio.superbwarfare.item.common.ammo.PotionMortarShell
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem
import com.atsuishio.superbwarfare.menu.EnergyMenu
import com.atsuishio.superbwarfare.menu.FuMO25Menu
import com.atsuishio.superbwarfare.mobeffect.BurnMobEffect
import com.atsuishio.superbwarfare.mobeffect.PhosphorusFireMobEffect
import com.atsuishio.superbwarfare.mobeffect.ShockMobEffect
import com.atsuishio.superbwarfare.mobeffect.TraumaMobEffect
import com.atsuishio.superbwarfare.perk.functional.PowerfulAttraction
import com.atsuishio.superbwarfare.procedures.WelcomeProcedure
import com.atsuishio.superbwarfare.recipe.ModPotionRecipes
import com.atsuishio.superbwarfare.tools.GunsTool
import com.atsuishio.superbwarfare.tools.ResourceOnceLogger
import com.atsuishio.superbwarfare.tools.VectorUtil
import com.atsuishio.superbwarfare.world.TDMSavedData
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.common.Mod

private val classes = arrayOf(
    CriteriaRegister::class.java,
    CapabilityHandler::class.java,
    ClickHandler::class.java,
    ClientRenderHandler::class.java,
    ClientLanguageGetter::class.java,
    ParachuteRenderer::class.java,
    ContainerBlockPreview::class.java,
    FuMO25ScreenHelper::class.java,
    ModSellWarningScreen::class.java,
    CommandRegister::class.java,
    CompatHolder::class.java,
    DataLoader::class.java,
    DataLoader.ClientReloadListener::class.java,
    ContainerDataManager::class.java,
    VehicleDataTool::class.java,
    VehicleDataTool::class.java,
    DataGenerators::class.java,
    DPSGeneratorEntity::class.java,
    SenpaiEntity::class.java,
    TargetEntity::class.java,
    ClientEventHandler::class.java,
    EntityUseGunEventHandler::class.java,
    GunEventHandler::class.java,
    HitboxHelperEventHandler::class.java,
    KillMessageHandler::class.java,
    LivingEventHandler::class.java,
    ModVersionEventHandler::class.java,
    PlayerEventHandler::class.java,
    ModAttributes::class.java,
    ModEntities::class.java,
    ModEntityRenderers::class.java,
    ModKeyMappings::class.java,
    ModLootModifier::class.java,
    ModParticles::class.java,
    ModPerks::class.java,
    ModProperties::class.java,
    ModScreens::class.java,
    ModTabs::class.java,
    ModVillagers::class.java,
    Hammer::class.java,
    PotionMortarShell::class.java,
    ContainerBlockItem::class.java,
    EnergyMenu::class.java,
    FuMO25Menu::class.java,
    BurnMobEffect::class.java,
    ShockMobEffect::class.java,
    TraumaMobEffect::class.java,
    PhosphorusFireMobEffect::class.java,
    PowerfulAttraction::class.java,
    WelcomeProcedure::class.java,
    ModPotionRecipes::class.java,
    GunsTool::class.java,
    ResourceOnceLogger::class.java,
    VectorUtil::class.java,
    TDMSavedData::class.java,
    ClientMouseHandler::class.java,
    ThermalShaderHandler::class.java,
    PhosphorusFireRenderer::class.java,
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