package com.atsuishio.superbwarfare.script

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache

/**
 * 供 JS 脚本使用的、按对象身份分别记忆的数值槽。
 *
 * 脚本顶层写 `let` / `const` 看起来像"这条脚本自己的变量"，实际上每个**枪械 id** 只会编译出一个
 * 作用域（[com.atsuishio.superbwarfare.resource.gun.DefaultGunResource.getScript] 把它缓存在
 * `scriptCache` 上），所以顶层变量是全世界所有同型号枪共有的一份。需要"每把枪自己的记忆"时
 * ——最典型的是把某个角度插值到目标值，必须记住上一帧算到哪——就得用这里。
 *
 * 键是**对象身份**而不是相等性：Guava 的 `weakKeys()` 用 `==` 比较，键被回收时条目自动消失。
 * 这正好对上 [net.minecraft.world.item.ItemStack]（不重写 `equals` / `hashCode`）与实体，
 * 也是 [com.atsuishio.superbwarfare.data.gun.GunData.Companion.DATA_CACHE] 用 `weakKeys()` 的理由：
 * 枪会被反复创建销毁，不能把它们永久钉在缓存里。反过来说，**不要把 `String` / `Double` 这类
 * 按值相等的对象当键**，那样两个内容相同的字面量会是两个不同的槽。
 *
 * 脚本里看到的名字是 `JsState`（在 `DefaultGunResource.getScript()` 里与 `JsMath` 一起注入）。
 * 键参数声明成 `Any` 而不是 `ItemStack`，这样载具脚本也能拿实体当键。
 */
object ScriptState {

    /**
     * 每个键一份的命名槽。
     *
     * 值的类型是并发 map 而不是普通 map：脚本目前只在渲染线程跑，但这里没有任何东西保证这一点，
     * 而换成并发容器不花什么代价，能让误用在别的线程上时不至于静默丢数据。
     */
    private val slots: LoadingCache<Any, MutableMap<String, Double>> = CacheBuilder.newBuilder()
        .weakKeys()
        .build(object : CacheLoader<Any, MutableMap<String, Double>>() {
            override fun load(key: Any): MutableMap<String, Double> = java.util.concurrent.ConcurrentHashMap()
        })

    /** 读取 [key] 的 [slot]，没写过时返回 [fallback]。 */
    fun get(key: Any, slot: String, fallback: Double): Double {
        return slots.getUnchecked(key)[slot] ?: fallback
    }

    /** 写入 [key] 的 [slot]。 */
    fun set(key: Any, slot: String, value: Double) {
        slots.getUnchecked(key)[slot] = value
    }

    /**
     * 把 [key] 的 [slot] 以 [rate] 的比例向 [target] 靠近，记住结果并返回。
     *
     * [epsilon] 是吸附阈值：与目标差距小于它时直接跳到 [target]。没有这一步插值只会无限逼近而
     * 永远到不了终点，脚本里就得再写一次"接近了就吸附"的判断，而且还得把吸附后的值写回去。
     *
     * [key] 的 [slot] **没有历史时直接以 [target] 作为起点**，也就是第一次看到这把枪时不会做一次
     * 从 0 到目标值的补间。这跟"值是 0"的直觉相反，但是想要的：玩家装备好配件后第一次渲染，
     * 或者一把已经装着瞄准镜的枪第一次进入视野，都不该先表演一遍折叠动画。
     * 需要从别的值起步就先调 [set]。
     */
    @JvmOverloads
    fun smooth(key: Any, slot: String, target: Double, rate: Double, epsilon: Double = 0.0): Double {
        // 非有限的目标值会污染整个槽（之后每一帧都是从 NaN 插值），直接原样返回并不写回。
        if (!target.isFinite()) return target

        val current = get(key, slot, target)
        // rate 夹到 [0, 1]：脚本传进来的可能是 delta 乘出来的数，越界会让插值冲过目标来回震荡。
        val clamped = if (rate.isFinite()) rate.coerceIn(0.0, 1.0) else 1.0
        var next = ScriptMath.lerp(clamped, current, target)

        if (epsilon > 0.0 && Math.abs(next - target) < epsilon) {
            next = target
        }
        set(key, slot, next)
        return next
    }
}
