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
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

@EventBusSubscriber
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
    val PTKM_1R_MA = registerCommonModelAndAnimation("projectile/ptkm_1r")

    // armor models
    val GE_HELMET_M_35_MODEL = registerArmorModel("armor/ge_helmet_m_35")
    val RU_CHEST_6B43_MODEL = registerArmorModel("armor/ru_chest_6b43")
    val RU_HELMET_6B47_MODEL = registerArmorModel("armor/ru_helmet_6b47")
    val US_CHEST_IOTV_MODEL = registerArmorModel("armor/us_chest_iotv")
    val US_HELMET_PASGT_MODEL = registerArmorModel("armor/us_helmet_pasgt")

    // models
    val STEEL_COIL_MODEL = registerCommonModel("entity/steel_coil")

    val PROJECTILE_MODEL = registerCommonModel("projectile/projectile")
    val HAND_GRENADE_MODEL = registerCommonModel("projectile/hand_grenade")
    val RGO_GRENADE_MODEL = registerCommonModel("projectile/rgo_grenade")
    val M18_SMOKE_GRENADE_MODEL = registerCommonModel("projectile/m18_smoke_grenade")
    val SC_50_MODEL = registerCommonModel("projectile/sc_50")
    val SC_250_MODEL = registerCommonModel("projectile/sc_250")
    val SMALL_CANNON_SHELL_MODEL = registerCommonModel("projectile/small_cannon_shell")
    val PTKM_PROJECTILE_MODEL = registerCommonModel("projectile/ptkm_projectile")
    val AGM_65_MODEL = registerCommonModel("projectile/agm_65")
    val GUN_GRENADE_MODEL = registerCommonModel("projectile/gun_grenade")
    val CANNON_SHELL_MODEL = registerCommonModel("projectile/cannon_shell")
    val RPG_ROCKET_STANDARD_MODEL = registerCommonModel("projectile/rpg_rocket_standard")
    val RPG_ROCKET_TBG_MODEL = registerCommonModel("projectile/rpg_rocket_tbg")
    val IGLA_9K38_MISSILE_MODEL = registerCommonModel("projectile/igla_9k38_missile")
    val RU_9M336_MISSILE_MODEL = registerCommonModel("projectile/ru_9m336_missile")
    val WIRE_GUIDE_MISSILE_MODEL = registerCommonModel("projectile/wire_guide_missile")
    val KH_39_MODEL = registerCommonModel("projectile/kh_39")
    val JAVELIN_MISSILE_MODEL = registerCommonModel("projectile/javelin_missile")
    val SMALL_ROCKET_MODEL = registerCommonModel("projectile/small_rocket")
    val MEDIUM_ROCKET_MODEL = registerCommonModel("projectile/medium_rocket")
    val MORTAR_SHELL_MODEL = registerCommonModel("projectile/mortar_shell")
    val TASER_BULLET_MODEL = registerCommonModel("projectile/taser_bullet")
    val CLAYMORE_MODEL = registerCommonModel("projectile/claymore")
    val MEDICAL_KIT_MODEL = registerCommonModel("projectile/medical_kit")
    val BLU_43_MODEL = registerCommonModel("projectile/blu_43")
    val C4_MODEL = registerCommonModel("projectile/c4")
    val TM_62_MODEL = registerCommonModel("projectile/tm_62")
    val EDD_MODEL = registerCommonModel("projectile/edd")

    val BLUEPRINT_RESEARCH_TABLE_MODEL = registerCommonModel("block/blueprint_research_table")
    val FUMO_25_MODEL = registerCommonModel("block/fumo_25")

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