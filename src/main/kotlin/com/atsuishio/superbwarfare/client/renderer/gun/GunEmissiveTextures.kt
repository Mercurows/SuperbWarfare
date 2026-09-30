package com.atsuishio.superbwarfare.client.renderer.gun

import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller

/**
 * 枪械贴图的**自发光层**查找表。
 *
 * 约定：一张枪械贴图 `textures/bedrock/gun/<名字>.png` 若在**同一个目录**下还存在一张同名的
 * `textures/bedrock/gun/<名字>_e.png`，后者就当作它的发光遮罩 —— [GeoGunRenderer] 会用
 * `RenderType.eyes` 把同一份几何、同一份骨骼姿态照着它**再画一遍**，于是只有 `_e` 上画了东西的地方发光。
 * `_e` 不存在（绝大多数枪都是这样）就返回 `null`，那把枪照旧只画一遍，渲染结果与加这个功能之前逐像素一致。
 *
 * 出图时注意：`RenderType.eyes` 是**加法混合**（`blendFunc(ONE, ONE)`），而 `rendertype_eyes` 着色器
 * 只输出 `贴图 × 顶点色 × ColorModulator × 雾`，既不乘光照也**不按 alpha 混合**。所以
 * - "不发光"必须画成**黑色**（RGB 为 0）——alpha 完全不参与混合，`(255,255,255, a=0)` 这种
 *   "透明但白"的像素会照样全亮，透明底上留白等于没有遮罩；
 * - 把基础贴图另存一份当 `_e` 用不是"发光"而是**整体亮度翻倍**，必须另画一张黑底遮罩。
 *
 * 另外 `_illuminated` 结尾的骨骼本来就已经被 `GeoGunModel.markIlluminatedBones` 按满亮度画过一遍，
 * 遮罩里再给那些部位上色就会叠两次，出图时心里有数就行。
 *
 * 这个表只回答"这张 `_e` 在不在"，并把答案缓存到下一次资源重载为止 —— 它在运行期不会变（只有换资源包才会变），
 * 而 `ResourceManager.getResource` 每次都要挨个翻包，逐帧逐枪地问太亏。贴图本身仍然由
 * `RenderType` 在绘制时绑定，这里不碰它。
 */
object GunEmissiveTextures : SimplePreparableReloadListener<Unit>() {

    /** 插在扩展名**之前**的自发光后缀：`glock_17.png` → `glock_17_e.png`。 */
    private const val SUFFIX = "_e"

    private const val EXTENSION = ".png"

    /** 值存 `null` 表示"查过了，这张贴图没有配套的 `_e`"，免得每帧都去资源管理器里翻一遍。 */
    private val cache = HashMap<ResourceLocation, ResourceLocation?>()

    /**
     * [texture] 对应的自发光贴图；没有就返回 `null`。
     *
     * 只认 `.png`：别的扩展名（包括没有扩展名）无从推导，一律判无。
     */
    fun get(texture: ResourceLocation): ResourceLocation? {
        val cached = cache[texture]
        if (cached != null) return cached
        if (cache.containsKey(texture)) return null

        // 客户端还没把资源加载起来时 resourceManager 是 null；这一帧查不到就先不缓存，下次进来重新查。
        val resourceManager = Minecraft.getInstance().resourceManager ?: return null

        val emissive = resolve(texture, resourceManager)
        cache[texture] = emissive
        return emissive
    }

    private fun resolve(texture: ResourceLocation, resourceManager: ResourceManager): ResourceLocation? {
        val path = texture.path
        if (!path.endsWith(EXTENSION)) return null

        val emissive = ResourceLocation.fromNamespaceAndPath(
            texture.namespace,
            path.substring(0, path.length - EXTENSION.length) + SUFFIX + EXTENSION
        )
        return if (resourceManager.getResource(emissive).isPresent) emissive else null
    }

    override fun prepare(resourceManager: ResourceManager, profiler: ProfilerFiller) {
        // 没有要在后台线程上算的东西，清缓存留到主线程的 apply 里做。
    }

    override fun apply(unit: Unit, resourceManager: ResourceManager, profiler: ProfilerFiller) {
        cache.clear()
    }
}
