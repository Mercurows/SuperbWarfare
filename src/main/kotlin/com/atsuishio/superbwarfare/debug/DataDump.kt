package com.atsuishio.superbwarfare.debug

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.DataCodec
import com.atsuishio.superbwarfare.data.DataLoader
import com.atsuishio.superbwarfare.debug.DataDump.DIR_NAME
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import net.neoforged.fml.loading.FMLPaths
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 把 [DataLoader] 里**已经加载完**的数据集导出成磁盘上的 JSON，供调试时用编辑器 / `diff` 查看。
 *
 * 为什么不直接打在聊天栏里：一个数据集动辄几十个条目、几百个字段，聊天栏既刷屏又没法搜索与对比。
 * 落到文件之后可以直接和 `src/main/resources` 下的源数据 diff —— 导出的内容是
 * **反序列化后的对象**（`CustomData` 里注册的 `DefaultXxxData` / `DefaultXxxResource`），
 * 所以源文件里没写的键会以数据类默认值补齐，「游戏里实际生效的值」一眼可见。
 *
 * 布局（`<gameDir>/superbwarfare/debug_data/`）：
 * ```
 * debug_data/
 *   _index.json                     # 本次导出的总索引（数据集清单 + 条目数）
 *   data/sbw/guns/_index.json       # 每个数据集一份索引（条目 id -> 文件名）
 *   data/sbw/guns/ak_47.json        # 每个条目一个文件，文件名取 id 的 path 部分
 *   resource/sbw/guns/ak_47.json
 * ```
 *
 * 数据集带**来源前缀**区分：`data/sbw/guns`（`CustomData.GUN_DATA`）与
 * `resource/sbw/guns`（`CustomData.GUN_RESOURCE`）目录同名但内容不同。
 * 只导出了一个数据集时不再多套一层目录，直接落在该数据集自己的目录里，
 * 于是重复导出同一数据集就是覆盖同名文件，可以持续 diff。
 *
 * 命令入口见 [com.atsuishio.superbwarfare.command.DATA_COMMAND]。
 */
object DataDump {

    /** 数据集来源：服务端数据包数据 / 客户端资源包数据 */
    enum class Kind { DATA, RESOURCE }

    /** 导出根目录名，固定放在 `superbwarfare/` 下，避免误删别的目录 */
    private const val DIR_NAME = "debug_data"

    /** 与 [DataLoader.JSON] 同配置，只是打开缩进 —— 导出的文件要保持可读、可 diff */
    @OptIn(ExperimentalSerializationApi::class)
    private val DEBUG_JSON: Json = Json(DataLoader.JSON) {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    private val TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /** 不能进文件名的字符（Windows 上 `:` 非法，`/` 会变成子目录） */
    private val ILLEGAL_NAME_CHARS = Regex("[^A-Za-z0-9._-]")

    data class Entry(val id: String, val file: String)

    data class Dataset(
        val kind: Kind,
        val path: String,
        val typeName: String,
        val entries: List<Entry>
    ) {
        /** `data/sbw/guns` / `resource/sbw/guns`，同时是 `_index.json` 里的稳定标识 */
        val key: String get() = "${kind.name.lowercase()}/$path"

        /**
         * 命令行里写的目录名：取路径最后一段（`sbw/guns` -> `guns`）。
         *
         * 做成纯字母数字下划线是**故意**的：Brigadier 的非引号字符串不接受 `/`、`:`，
         * 只有这样才能保证 `dump data guns` 一定解析得动。见 [com.atsuishio.superbwarfare.command.DATA_COMMAND]。
         */
        val shortName: String get() = path.substringAfterLast('/')

        /** 聊天栏/清单里的显示名：`data guns` */
        val displayName: String get() = "${kind.name.lowercase()} $shortName"
    }

    data class Failure(val dataset: String, val id: String, val reason: String)

    data class Result(
        val directory: File,
        val datasetCount: Int,
        val entryCount: Int,
        val failures: List<Failure>,
        val elapsedMillis: Long
    )

    /**
     * 读取数据集时用的锁。
     *
     * 命令跑在服务端线程，而客户端资源包 reload（`GUN_RESOURCE` / `VEHICLE_RESOURCE`）跑在客户端线程，
     * 集成服务器里两者同进程，`ComplexJsonResourceReloadListener` 又是边读边往 map 里塞的，
     * 所以导出前要先在同一把锁下把「数据集 -> 条目表」快照出来，再慢慢写文件。
     */
    private val LOCK = Any()

    /** 本次导出要用的快照：数据集结构 + 每个条目的实际对象 */
    private class Snapshot(
        val datasets: List<Dataset>,
        val values: Map<String, Map<String, Any>>
    )

    private fun snapshot(): Snapshot = synchronized(LOCK) {
        val datasets = buildList {
            addAll(datasetsOf(Kind.DATA, DataLoader.LOADED_DATA))
            addAll(datasetsOf(Kind.RESOURCE, DataLoader.LOADED_RESOURCE))
        }

        // key 与 Dataset.key 一致：`data/sbw/guns` / `resource/sbw/guns`
        val values = buildMap {
            DataLoader.LOADED_DATA.forEach { (path, general) -> put("data/$path", general.dataMap.toMap()) }
            DataLoader.LOADED_RESOURCE.forEach { (path, general) -> put("resource/$path", general.dataMap.toMap()) }
        }

        Snapshot(datasets, values)
    }

    /** 当前进程内全部可导出的数据集（服务端数据 + 客户端资源），按来源、目录排序 */
    fun datasets(): List<Dataset> = snapshot().datasets

    private fun datasetsOf(kind: Kind, source: Map<String, DataLoader.GeneralData<*>>): List<Dataset> =
        source.entries
            .sortedBy { it.key }
            .map { (path, general) -> toDataset(kind, path, general) }

    private fun toDataset(kind: Kind, path: String, general: DataLoader.GeneralData<*>): Dataset {
        val entries = general.dataMap.keys.sorted().map { entry(it) }
        return Dataset(kind, path, general.type.simpleName, entries)
    }

    /**
     * `superbwarfare:ak_47` -> `ak_47.json`。
     *
     * 同一目录下条目 id 唯一，所以去掉命名空间不会撞名；真撞了（数据包写了不同命名空间但同 path）
     * 才退回完整 id。
     */
    private fun entry(id: String): Entry {
        val short = id.substringAfter(':')
        return if (short.isEmpty() || short == id) {
            Entry(id, "${id.replace(ILLEGAL_NAME_CHARS, "_")}.json")
        } else {
            Entry(id, "${short.replace(ILLEGAL_NAME_CHARS, "_")}.json")
        }
    }

    /**
     * 按名字解析数据集。接受的写法（从最方便到最精确）：
     * - `guns` —— 目录名（[Dataset.shortName]），`data/sbw/guns` 与 `resource/sbw/guns` 会同时命中
     * - `sbw/guns` —— 完整目录
     * - `data/sbw/guns` / `resource/sbw/guns` —— 带来源前缀，唯一命中
     *
     * 注意前两种写法里有 `/`，**只能从代码里调用**：命令行的非引号参数字符集不允许斜杠，
     * 所以命令侧只走 [Dataset.shortName]。
     *
     * @return 命中的数据集；名字不存在时为空列表
     */
    fun resolve(name: String): List<Dataset> {
        val trimmed = name.trim().trim('/')
        if (trimmed.isEmpty()) return emptyList()

        val all = datasets()
        all.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.let { return listOf(it) }
        all.filter { it.path.equals(trimmed, ignoreCase = true) }.let { if (it.isNotEmpty()) return it }

        // 退回目录名：`guns` 也能命中 `data/sbw/guns` 与 `resource/sbw/guns`
        return all.filter { it.shortName.equals(trimmed, ignoreCase = true) }
    }

    /**
     * 把 [datasets] 导出到磁盘。
     *
     * @param datasets 要导出的数据集
     * @param entryIds 只导出这些条目 id；为空表示导出数据集里的全部条目
     * @param clean 导出前清空整个 `debug_data/`，让目录里只剩本次导出的内容
     */
    @JvmOverloads
    fun dump(datasets: List<Dataset>, entryIds: Set<String> = emptySet(), clean: Boolean = false): Result {
        val startedAt = System.nanoTime()
        val snapshot = snapshot()
        val root = dumpDirectory()

        if (clean) {
            deleteContents(root)
        }

        val failures = mutableListOf<Failure>()
        var entryCount = 0
        var datasetCount = 0

        // 单个数据集时不再多套一层目录，直接落在它自己的目录里（增量导出同一个数据集就是覆盖同名文件）
        val scope = if (datasets.size == 1) {
            root.resolve(datasets[0].key.replace('/', File.separatorChar))
        } else {
            root
        }

        for (dataset in datasets) {
            val selected = dataset.entries.filter { entryIds.isEmpty() || it.id in entryIds }
            if (selected.isEmpty()) continue

            val values = snapshot.values[dataset.key]

            if (values == null) {
                failures += Failure(dataset.key, "*", "dataset is not loaded in this environment")
                continue
            }

            val dir = scope.resolve(dataset.key.replace('/', File.separatorChar))
            val written = linkedMapOf<String, String>()
            for (entry in selected) {
                val value = values[entry.id]
                if (value == null) {
                    failures += Failure(dataset.key, entry.id, "entry is no longer loaded")
                    continue
                }

                try {
                    writeJson(File(dir, entry.file), DataCodec.serializerOf(value), value)
                    written[entry.id] = entry.file
                    entryCount++
                } catch (exception: Exception) {
                    Mod.LOGGER.warn("[DataDump] failed to export {}/{}", dataset.key, entry.id, exception)
                    failures += Failure(dataset.key, entry.id, exception.message ?: exception.toString())
                }
            }

            if (written.isEmpty()) continue

            writeText(File(dir, "_index.json"), indexJson(dataset, written, failures))
            datasetCount++
        }

        writeText(File(root, "_index.json"), rootIndexJson(datasets, datasetCount, entryCount, failures))

        return Result(
            directory = root,
            datasetCount = datasetCount,
            entryCount = entryCount,
            failures = failures,
            elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000
        )
    }

    /** 每个数据集的索引：条目 id -> 文件名，外加来源、类型与导出时间 */
    private fun indexJson(dataset: Dataset, written: Map<String, String>, failures: List<Failure>): String {
        val failed = failures.filter { it.dataset == dataset.key }

        return DEBUG_JSON.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("dataset", JsonPrimitive(dataset.key))
                put("kind", JsonPrimitive(dataset.kind.name.lowercase()))
                put("path", JsonPrimitive(dataset.path))
                put("type", JsonPrimitive(dataset.typeName))
                put("exportedAt", JsonPrimitive(now()))
                put("entryCount", JsonPrimitive(written.size))
                put(
                    "entries",
                    JsonObject(
                        written.entries.sortedBy { it.key }
                            .associate { (id, file) -> id to JsonPrimitive(file) }
                    )
                )
                if (failed.isNotEmpty()) {
                    put(
                        "failures",
                        JsonArray(failed.map { failure ->
                            buildJsonObject {
                                put(
                                    failure.id,
                                    JsonPrimitive(failure.reason)
                                )
                            }
                        })
                    )
                }
            }
        )
    }

    /** 本次导出的总索引：都导了哪些数据集、各多少条 */
    private fun rootIndexJson(
        datasets: List<Dataset>,
        datasetCount: Int,
        entryCount: Int,
        failures: List<Failure>
    ): String = DEBUG_JSON.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("exportedAt", JsonPrimitive(now()))
            put("datasetCount", JsonPrimitive(datasetCount))
            put("entryCount", JsonPrimitive(entryCount))
            put(
                "datasets",
                JsonArray(
                    datasets.map { dataset ->
                        buildJsonObject {
                            put("dataset", JsonPrimitive(dataset.key))
                            put("type", JsonPrimitive(dataset.typeName))
                            put("entryCount", JsonPrimitive(dataset.entries.size))
                        }
                    }
                )
            )
            if (failures.isNotEmpty()) {
                put("failureCount", JsonPrimitive(failures.size))
            }
        }
    )

    private fun now(): String = LocalDateTime.now().format(TIMESTAMP)

    /** 导出根目录（需要时才创建） */
    fun dumpDirectory(): File = FMLPaths.GAMEDIR.get().resolve("superbwarfare").resolve(DIR_NAME).toFile()

    /**
     * 写成带缩进的 JSON。非对象（理论上不会发生，所有数据集都是数据类）退回紧凑输出而不是失败。
     */
    private fun <T> writeJson(file: File, serializer: KSerializer<T>, value: T) {
        val element: JsonElement = DEBUG_JSON.encodeToJsonElement(serializer, value)
        writeText(file, DEBUG_JSON.encodeToString(JsonElement.serializer(), element))
    }

    private fun writeText(file: File, text: String) {
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    /**
     * 只清空 [dir] 的**内容**，不删目录本身。
     *
     * 双保险：目录名不是 [DIR_NAME] 就拒绝递归删除，避免上层传错路径时把游戏目录清掉。
     */
    private fun deleteContents(dir: File) {
        if (!dir.isDirectory) return
        if (dir.name != DIR_NAME) {
            Mod.LOGGER.error("[DataDump] refusing to clean unexpected directory {}", dir)
            return
        }

        dir.listFiles()?.forEach { it.deleteRecursively() }
    }
}
