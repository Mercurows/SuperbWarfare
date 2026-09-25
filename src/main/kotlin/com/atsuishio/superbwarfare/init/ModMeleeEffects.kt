package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.melee.MeleeEffectBehavior
import com.atsuishio.superbwarfare.melee.MeleeEffectBehaviors

/**
 * 近战额外效果的**行为注册表**。
 *
 * 数据侧只写行为 id（`"Type": "superbwarfare:explosion"`），
 * 真正的执行体在这里登记；id 会被归一化成"小写、去掉命名空间"的形式，
 * 所以 `superbwarfare:explosion` 与 `explosion` 等价。
 *
 * 与 `CustomData.MELEE_EFFECTS`（`sbw/melee_effects` 目录下的预设）的分工：
 * - 这里回答"这个 id 是什么行为"；
 * - 预设回答"这套参数叫什么名字"，两者通过 `MeleeEffectSpec.resolve()` 合并。
 */
object ModMeleeEffects {

    private val REGISTRY = LinkedHashMap<String, MeleeEffectBehavior>()

    init {
        MeleeEffectBehaviors.ALL.forEach { register(it) }
    }

    /**
     * 把数据里的 id 归一化成注册表的键：去空白、转小写、去掉 `superbwarfare:` 前缀。
     *
     * 空字符串与 `null` 都返回 `null`。
     */
    @JvmStatic
    fun normalize(id: String?): String? {
        val trimmed = id?.trim()?.lowercase().orEmpty()
        if (trimmed.isEmpty()) return null
        return trimmed.removePrefix("${Mod.MODID}:")
    }

    /** 登记一个行为；重复登记会覆盖并打一条 warning（新增行为忘了改 id 时能立刻发现） */
    @JvmStatic
    fun register(behavior: MeleeEffectBehavior) {
        val key = normalize(behavior.id) ?: run {
            Mod.LOGGER.warn("[MeleeEffect] ignoring a behavior with a blank id: {}", behavior.javaClass.name)
            return
        }
        val previous = REGISTRY.put(key, behavior)
        if (previous != null) {
            Mod.LOGGER.warn("[MeleeEffect] behavior '{}' was already registered, overwriting", key)
        }
    }

    /** 取行为；id 未知时返回 `null`（调用方自行决定是否打日志） */
    @JvmStatic
    fun get(id: String?): MeleeEffectBehavior? = normalize(id)?.let { REGISTRY[it] }

    @JvmStatic
    fun contains(id: String?): Boolean = get(id) != null

    /** 全部已登记的行为 id（`DataValidator` 报错文案与调试命令用） */
    @JvmStatic
    fun ids(): List<String> = REGISTRY.keys.toList()
}
