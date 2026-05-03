package com.atsuishio.superbwarfare.data

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.ModColor.ModColorAdapter
import com.atsuishio.superbwarfare.data.StringOrVec3.StringOrVec3Adapter
import com.atsuishio.superbwarfare.data.vehicle.subdata.CollisionLevel
import com.atsuishio.superbwarfare.data.vehicle.subdata.CollisionLevel.LimitAdapter
import com.atsuishio.superbwarfare.network.message.receive.DataSyncMessage
import com.atsuishio.superbwarfare.tools.sendPacket
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.event.OnDatapackSyncEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import java.util.function.Consumer

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
object DataLoader {

    @JvmField
    val GSON: Gson = createCommonBuilder().create()

    @OptIn(ExperimentalSerializationApi::class)
    val JSON = Json {
        isLenient = true
        ignoreUnknownKeys = true
        serializersModule = com.atsuishio.superbwarfare.serialization.serializersModule
        allowTrailingComma = true
        allowSpecialFloatingPointValues = true
    }

    @JvmField
    val JSON_OBJECT_CACHE: LoadingCache<Any, JsonObject> = CacheBuilder.newBuilder()
        .weakKeys()
        .build(object : CacheLoader<Any, JsonObject>() {
            override fun load(obj: Any): JsonObject {
                return GSON.toJsonTree(obj).getAsJsonObject()
            }
        })

    val LOADED_DATA = mutableMapOf<String, GeneralData<*>>()
    val LOADED_RESOURCE = mutableMapOf<String, GeneralData<*>>()

    val SERVER_LISTENER: ComplexJsonResourceReloadListener = ComplexJsonResourceReloadListener(LOADED_DATA)
    val CLIENT_LISTENER: ComplexJsonResourceReloadListener = ComplexJsonResourceReloadListener(LOADED_RESOURCE)

    @SubscribeEvent
    fun addDataReloadListener(event: AddReloadListenerEvent) {
        event.addListener(SERVER_LISTENER)
    }

    @Suppress("unchecked_cast")
    @JvmOverloads
    fun <T> createData(
        directory: String,
        clazz: Class<T>,
        synced: Boolean = false,
        isKtData: Boolean = false,
        onReload: Consumer<Map<String, Any>>? = null
    ): DataMap<T> {
        val data = LOADED_DATA[directory]

        if (data != null) {
            return data.proxyMap as DataMap<T>
        } else {
            val proxyMap = DataMap<T>(directory, LOADED_DATA)
            LOADED_DATA[directory] = GeneralData(clazz, proxyMap, HashMap(), synced, isKtData, onReload)
            return proxyMap
        }
    }

    @Suppress("unchecked_cast")
    @JvmOverloads
    fun <T> createResource(
        directory: String,
        clazz: Class<T>,
        isKtData: Boolean = false,
        onReload: Consumer<Map<String, Any>>? = null
    ): DataMap<T> {
        val resource = LOADED_RESOURCE[directory]

        if (resource != null) {
            return resource.proxyMap as DataMap<T>
        } else {
            val proxyMap = DataMap<T>(directory, LOADED_RESOURCE)
            LOADED_RESOURCE[directory] = GeneralData(clazz, proxyMap, HashMap(), false, isKtData, onReload)
            return proxyMap
        }
    }

    // 务必在所有需要序列化GSON数据的地方调用，避免报错
    @JvmStatic
    fun createCommonBuilder(): GsonBuilder {
        return GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .setLenient()
            .serializeSpecialFloatingPointValues()
            .registerTypeAdapter(Vec2::class.java, Vec2Adapter())
            .registerTypeAdapter(Vec3::class.java, Vec3Adapter())
            .registerTypeAdapter(ResourceLocation::class.java, ResourceLocationAdapter())
            .registerTypeAdapter(SoundEvent::class.java, SoundEventAdapter())
            .registerTypeAdapter(ModColor::class.java, ModColorAdapter())
            .registerTypeAdapter(StringOrVec3::class.java, StringOrVec3Adapter())
            .registerTypeAdapter(CollisionLevel.Limit::class.java, LimitAdapter())
            .registerTypeAdapterFactory(ObjectToList.AdapterFactory())
            .registerTypeAdapterFactory(StringToObject.AdapterFactory())
    }


    /**
     * 将StringToObject和ObjectToList转换为原始值
     */
    @JvmStatic
    fun processValue(value: Any?): Any? {
        return when (value) {
            is ObjectToList<*> -> value.list.map { value -> processValue(value) }
            is StringToObject<*> -> processValue(value.value)
            else -> value
        }
    }

    data class GeneralData<T>(
        val type: Class<*>,
        val proxyMap: DataMap<T>,
        val dataMap: HashMap<String, Any>,
        val synced: Boolean,
        val isKtData: Boolean = false,
        val onReload: Consumer<Map<String, Any>>?
    ) {
        val mapType by lazy {
            TypeToken.getParameterized(HashMap::class.java, String::class.java, type)!!
        }

        fun serializeToString(): String {
            return if (isKtData) {
                JSON.encodeToString(serializer(mapType.type), dataMap)
            } else {
                GSON.toJson(dataMap)!!
            }
        }
    }

    @SubscribeEvent
    fun onDataPackSync(event: OnDatapackSyncEvent) {
        val server = event.playerList.server

        LOADED_DATA.filter { it.value.synced }.forEach { (key, data) ->
            val packet = DataSyncMessage(key, data.serializeToString())

            val player = event.player
            for (player in if (player != null) listOf(player) else event.playerList.players) {
                if (server.isSingleplayerOwner(player.gameProfile)) continue

                player.sendPacket(packet)
            }
        }
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(
        modid = Mod.MODID,
        bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD
    )
    internal object ClientReloadListener {
        @SubscribeEvent
        fun addResourceReloadListener(event: RegisterClientReloadListenersEvent) {
            event.registerReloadListener(CLIENT_LISTENER)
        }
    }
}
