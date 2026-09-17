package com.atsuishio.superbwarfare.data

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.DataLoader.JSON
import com.atsuishio.superbwarfare.network.message.receive.DataSyncMessage
import com.atsuishio.superbwarfare.tools.sendPacket
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
import net.neoforged.neoforge.event.AddReloadListenerEvent
import net.neoforged.neoforge.event.OnDatapackSyncEvent
import java.util.function.Consumer

/**
 * 数据包加载入口。
 *
 * 全部数据集都已经迁到 kotlinx.serialization（不再有 Gson 分支），
 * 所以这里只剩下 [JSON] 一份配置，以及"按目录把 `data/&lt;ns&gt;/&lt;directory&gt;/` 下的 `.json`
 * 解析成 `Map&lt;id, T&gt;`"的通用流程。
 */
@EventBusSubscriber(modid = Mod.MODID)
object DataLoader {

    @OptIn(ExperimentalSerializationApi::class)
    val JSON = Json {
        isLenient = true
        ignoreUnknownKeys = true
        serializersModule = com.atsuishio.superbwarfare.serialization.serializersModule
        allowTrailingComma = true
        allowSpecialFloatingPointValues = true
    }

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
        onReload: Consumer<Map<String, Any>>? = null
    ): DataMap<T> {
        val data = LOADED_DATA[directory]

        if (data != null) {
            return data.proxyMap as DataMap<T>
        } else {
            val proxyMap = DataMap<T>(directory, LOADED_DATA)
            LOADED_DATA[directory] = GeneralData(clazz, proxyMap, HashMap(), synced, onReload)
            return proxyMap
        }
    }

    @Suppress("unchecked_cast")
    @JvmOverloads
    fun <T> createResource(
        directory: String,
        clazz: Class<T>,
        onReload: Consumer<Map<String, Any>>? = null
    ): DataMap<T> {
        val resource = LOADED_RESOURCE[directory]

        if (resource != null) {
            return resource.proxyMap as DataMap<T>
        } else {
            val proxyMap = DataMap<T>(directory, LOADED_RESOURCE)
            LOADED_RESOURCE[directory] = GeneralData(clazz, proxyMap, HashMap(), false, onReload)
            return proxyMap
        }
    }

    /**
     * 将 StringOrObject 和 SingleOrList 转换为原始值
     */
    @JvmStatic
    fun processValue(value: Any?): Any? {
        return when (value) {
            is SingleOrList<*> -> value.list.map { value -> processValue(value) }
            is StringOrObject<*> -> processValue(value.value)
            else -> value
        }
    }

    data class GeneralData<T>(
        val type: Class<*>,
        val proxyMap: DataMap<T>,
        val dataMap: HashMap<String, Any>,
        val synced: Boolean,
        val onReload: Consumer<Map<String, Any>>?
    ) {
        /** `Map<String, T>` 的 serializer，用于数据同步（取代原来的 Gson `TypeToken` + `GSON.toJson`） */
        val mapSerializer: KSerializer<Map<String, Any>> by lazy {
            @Suppress("UNCHECKED_CAST")
            MapSerializer(String.serializer(), JSON.serializersModule.serializer(type) as KSerializer<Any>)
        }

        fun serializeToString(): String {
            return JSON.encodeToString(mapSerializer, dataMap)
        }
    }

    @SubscribeEvent
    fun onDataPackSync(event: OnDatapackSyncEvent) {
        val server = event.playerList.server

        LOADED_DATA.filter { it.value.synced }.forEach { (key, data) ->
            val packet = DataSyncMessage(key, data.serializeToString())

            for (player in event.relevantPlayers) {
                if (server.isSingleplayerOwner(player.gameProfile)) continue

                player.sendPacket(packet)
            }
        }
    }

    @EventBusSubscriber(modid = Mod.MODID)
    internal object ClientReloadListener {
        @SubscribeEvent
        fun addResourceReloadListener(event: RegisterClientReloadListenersEvent) {
            event.registerReloadListener(CLIENT_LISTENER)
        }
    }
}
