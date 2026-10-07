package com.atsuishio.superbwarfare.resource.gun

/**
 * 近战动作动画名（`MeleeActions.Animation`）的拼接规则
 */
object GunAnimationNames {

    private const val PREFIX = "animation."

    /**
     * 把候选解析成实际使用的 clip 名。
     *
     * @param candidate 动作表里写的候选：短名（`hit`）或全名（`animation.ak_47.hit`）。
     * @param gunId 宿主的 id，带不带命名空间都可以（`superbwarfare:ak_47` / `ak_47`）。
     */
    @JvmStatic
    fun resolve(candidate: String, gunId: String): String {
        val trimmed = candidate.trim()
        if (trimmed.startsWith(PREFIX)) return trimmed

        val path = gunId.substringAfter(':')
        return "$PREFIX$path.$trimmed"
    }

    /**
     * 按顺序解析 [candidates]，返回**第一个**满足 [exists] 的 clip 名；一个都没有时返回 `null`。
     *
     * 调用方负责在返回 `null` 时给出回退与日志。
     */
    @JvmStatic
    fun resolveFirst(candidates: List<String>, gunId: String, exists: (String) -> Boolean): String? {
        for (candidate in candidates) {
            val name = resolve(candidate, gunId)
            if (exists(name)) return name
        }
        return null
    }
}
