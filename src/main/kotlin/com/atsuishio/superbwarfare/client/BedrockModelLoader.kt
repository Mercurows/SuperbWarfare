package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.GsonUtil
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockAnimationEvent
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockModelEvent
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.BedrockAnimationResourceSet
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.BedrockModelResourceSet
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.RawResourceLoader
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object BedrockModelLoader {
    // models
    val SENPAI_MODEL = loc("entity/senpai.geo")
    val TARGET_MODEL = loc("entity/target.geo")
    val DPS_GENERATOR_MODEL = loc("entity/dps_generator.geo")
    val PROJECTILE_MODEL = loc("entity/projectile.geo")

    // animations
    val SENPAI_ANI = loc("senpai.animation")
    val TARGET_ANI = loc("target.animation")
    val DPS_GENERATOR_ANI = loc("dps_generator.animation")

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

    @JvmStatic
    fun getModel(location: ResourceLocation): BedrockModel? {
        return BedrockModelResourceSet.getInstance().getModel(location)
    }

    @JvmStatic
    fun getAnimations(location: ResourceLocation): MutableList<BedrockAnimation>? {
        return BedrockAnimationResourceSet.getInstance().getAnimations(location)
    }
}