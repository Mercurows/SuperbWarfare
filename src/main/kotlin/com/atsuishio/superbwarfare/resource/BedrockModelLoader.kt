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
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object BedrockModelLoader {
    private val COMMON_MODELS = mutableListOf<ResourceLocation>()
    private val COMMON_MODELS_AND_ANIMATIONS = hashMapOf<ResourceLocation, ResourceLocation>()
    private val ARMOR_MODELS = mutableListOf<ResourceLocation>()
    private val ARMOR_MODELS_POJO = hashMapOf<ResourceLocation, BedrockArmorModel>()

    // models and animations
    val SENPAI_MA = registerCommonModelAndAnimation("entity/senpai")
    val TARGET_MA = registerCommonModelAndAnimation("entity/target")
    val DPS_GENERATOR_MA = registerCommonModelAndAnimation("entity/dps_generator")
    val MK_82_MA = registerCommonModelAndAnimation("projectile/mk_82")
    val SWARM_DRONE_MA = registerCommonModelAndAnimation("projectile/swarm_drone")
    val AGM_65_MA = registerCommonModelAndAnimation("projectile/agm_65")
    val GUN_GRENADE_MA = registerCommonModelAndAnimation("projectile/gun_grenade")
    val CANNON_SHELL_MA = registerCommonModelAndAnimation("projectile/cannon_shell")
    val RPG_ROCKET_STANDARD_MA = registerCommonModelAndAnimation("projectile/rpg_rocket_standard")
    val RPG_ROCKET_TBG_MA = registerCommonModelAndAnimation("projectile/rpg_rocket_tbg")
    val IGLA_9K38_MISSILE_MA = registerCommonModelAndAnimation("projectile/igla_9k38_missile")
    val RU_9M336_MISSILE_MA = registerCommonModelAndAnimation("projectile/ru_9m336_missile")
    val WIRE_GUIDE_MISSILE_MA = registerCommonModelAndAnimation("projectile/wire_guide_missile")

    // armor models
    val GE_HELMET_M_35_MODEL = registerArmorModel("armor/ge_helmet_m_35")
    val RU_CHEST_6B43_MODEL = registerArmorModel("armor/ru_chest_6b43")
    val RU_HELMET_6B47_MODEL = registerArmorModel("armor/ru_helmet_6b47")
    val US_CHEST_IOTV_MODEL = registerArmorModel("armor/us_chest_iotv")
    val US_HELMET_PASGT_MODEL = registerArmorModel("armor/us_helmet_pasgt")

    // models
    val PROJECTILE_MODEL = registerCommonModel("entity/projectile")
    val HAND_GRENADE_MODEL = registerCommonModel("projectile/hand_grenade")
    val RGO_GRENADE_MODEL = registerCommonModel("projectile/rgo_grenade")
    val M18_SMOKE_GRENADE_MODEL = registerCommonModel("projectile/m18_smoke_grenade")
    val SC_50_MODEL = registerCommonModel("projectile/sc_50")
    val SC_250_MODEL = registerCommonModel("projectile/sc_250")

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

    fun registerCommonModel(path: String): ResourceLocation {
        val rl = loc("$path.geo")
        COMMON_MODELS.add(rl)
        return rl
    }

    fun registerCommonModelAndAnimation(path: String): Pair<ResourceLocation, ResourceLocation> {
        val modelRl = loc("$path.geo")
        val aniRl = loc("$path.animation")
        COMMON_MODELS_AND_ANIMATIONS[modelRl] = aniRl
        return Pair(modelRl, aniRl)
    }

    fun registerArmorModel(path: String): ResourceLocation {
        val rl = loc("$path.geo")
        ARMOR_MODELS.add(rl)
        return rl
    }

    @SubscribeEvent
    fun onRegisterBedrockModels(event: RegisterBedrockModelEvent) {
        with(event) {
            COMMON_MODELS.forEach { register(it, COMMON_LOADER) }
            COMMON_MODELS_AND_ANIMATIONS.forEach { register(it.key, COMMON_LOADER) }
            ARMOR_MODELS.forEach { register(it, COMMON_LOADER, ::BedrockArmorModel) }
        }
    }

    @SubscribeEvent
    fun onRegisterBedrockAnimations(event: RegisterBedrockAnimationEvent) {
        with(event) {
            COMMON_MODELS_AND_ANIMATIONS.forEach { register(it.value, it.key, COMMON_LOADER) }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onModelLoaded(event: RegisterBedrockModelReloadListenerEvent) {
        event.register {
            ARMOR_MODELS.forEach { path ->
                val model = it[path] as? BedrockArmorModel ?: return@forEach
                ARMOR_MODELS_POJO[path] = model
            }
        }
    }

    @JvmStatic
    fun getModel(location: ResourceLocation): BedrockModel? {
        return BedrockModelResourceSet.getInstance().getModel(location)
    }

    @JvmStatic
    fun getArmorModel(location: ResourceLocation): BedrockArmorModel? {
        return ARMOR_MODELS_POJO[location]
    }

    @JvmStatic
    fun getAnimations(location: ResourceLocation): MutableList<BedrockAnimation>? {
        return BedrockAnimationResourceSet.getInstance().getAnimations(location)
    }
}