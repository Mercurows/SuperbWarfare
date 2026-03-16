package com.atsuishio.superbwarfare.resource

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.BedrockArmorModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.GsonUtil
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockAnimationEvent
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockModelEvent
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockModelReloadListenerEvent
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.BedrockAnimationResourceSet
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.BedrockModelResourceSet
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.RawResourceLoader
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
object BedrockModelLoader {
    // models
    val SENPAI_MODEL = loc("entity/senpai.geo")
    val TARGET_MODEL = loc("entity/target.geo")
    val DPS_GENERATOR_MODEL = loc("entity/dps_generator.geo")
    val PROJECTILE_MODEL = loc("entity/projectile.geo")

    val GE_HELMET_M_35_MODEL = loc("armor/ge_helmet_m_35.geo")
    val RU_CHEST_6B43_MODEL = loc("armor/ru_chest_6b43.geo")
    val RU_HELMET_6B47_MODEL = loc("armor/ru_helmet_6b47.geo")
    val US_CHEST_IOTV_MODEL = loc("armor/us_chest_iotv.geo")
    val US_HELMET_PASGT_MODEL = loc("armor/us_helmet_pasgt.geo")

    val HAND_GRENADE_MODEL = loc("projectile/hand_grenade.geo")
    val M18_SMOKE_GRENADE_MODEL = loc("projectile/m18_smoke_grenade.geo")

    // animations
    val SENPAI_ANI = loc("senpai.animation")
    val TARGET_ANI = loc("target.animation")
    val DPS_GENERATOR_ANI = loc("dps_generator.animation")

    // textures
    val GE_HELMET_M_35_TEXTURE = loc("textures/armor/ge_helmet_m_35.png")
    val RU_CHEST_6B43_TEXTURE = loc("textures/armor/ru_chest_6b43.png")
    val RU_HELMET_6B47_TEXTURE = loc("textures/armor/ru_helmet_6b47.png")
    val US_CHEST_IOTV_TEXTURE = loc("textures/armor/us_chest_iotv.png")
    val US_HELMET_PASGT_TEXTURE = loc("textures/armor/us_helmet_pasgt.png")

    lateinit var geHelmetM35Model: BedrockArmorModel
    lateinit var ruChest6b43Model: BedrockArmorModel
    lateinit var ruHelmet6b47Model: BedrockArmorModel
    lateinit var usChestIotvModel: BedrockArmorModel
    lateinit var usHelmetPasgtModel: BedrockArmorModel

    val COMMON_LOADER: RawResourceLoader = object : RawResourceLoader {
        override fun <T> load(inputStream: InputStream, clazz: Class<T>): T {
            try {
                InputStreamReader(inputStream).use { reader ->
                    return GsonUtil.CLIENT_GSON.fromJson<T>(reader, clazz)
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    @SubscribeEvent
    fun onRegisterBedrockModels(event: RegisterBedrockModelEvent) {
        with(event) {
            register(SENPAI_MODEL, COMMON_LOADER)
            register(TARGET_MODEL, COMMON_LOADER)
            register(DPS_GENERATOR_MODEL, COMMON_LOADER)
            register(PROJECTILE_MODEL, COMMON_LOADER)
            register(GE_HELMET_M_35_MODEL, COMMON_LOADER, ::BedrockArmorModel)
            register(RU_CHEST_6B43_MODEL, COMMON_LOADER, ::BedrockArmorModel)
            register(RU_HELMET_6B47_MODEL, COMMON_LOADER, ::BedrockArmorModel)
            register(US_CHEST_IOTV_MODEL, COMMON_LOADER, ::BedrockArmorModel)
            register(US_HELMET_PASGT_MODEL, COMMON_LOADER, ::BedrockArmorModel)
            register(HAND_GRENADE_MODEL, COMMON_LOADER)
            register(M18_SMOKE_GRENADE_MODEL, COMMON_LOADER)
        }
    }

    @SubscribeEvent
    fun onRegisterBedrockAnimations(event: RegisterBedrockAnimationEvent) {
        with(event) {
            register(SENPAI_ANI, SENPAI_MODEL, COMMON_LOADER)
            register(TARGET_ANI, TARGET_MODEL, COMMON_LOADER)
            register(DPS_GENERATOR_ANI, DPS_GENERATOR_MODEL, COMMON_LOADER)
        }
    }

    @SubscribeEvent
    fun onModelLoaded(event: RegisterBedrockModelReloadListenerEvent) {
        event.register {
            geHelmetM35Model = it[GE_HELMET_M_35_MODEL] as BedrockArmorModel
            ruChest6b43Model = it[RU_CHEST_6B43_MODEL] as BedrockArmorModel
            ruHelmet6b47Model = it[RU_HELMET_6B47_MODEL] as BedrockArmorModel
            usChestIotvModel = it[US_CHEST_IOTV_MODEL] as BedrockArmorModel
            usHelmetPasgtModel = it[US_HELMET_PASGT_MODEL] as BedrockArmorModel
        }
    }

    @JvmStatic
    fun getModel(location: ResourceLocation): BedrockModel? {
        return BedrockModelResourceSet.getInstance().getModel(location)
    }

    @JvmStatic
    fun getAnimations(location: ResourceLocation): MutableList<BedrockAnimation>? {
        return BedrockAnimationResourceSet.getInstance().getAnimations(location)
    }
}