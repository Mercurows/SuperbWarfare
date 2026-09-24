package com.atsuishio.superbwarfare.resource.gun

/**
 * 近战动作动画名（`MeleeActions.Animation`）的拼接规则。
 *
 * 动作表写在**枪械数据**里，而一份数据会被多把枪共用（配件、弹种、开火模式都能覆盖它），
 * 所以动作表里**写不了某一把枪的完整 clip 名**——`animation.ak_47.hit` 里的 `ak_47`
 * 只有运行时才知道（`GunResource` 本来就是按物品注册 id 缓存的）。
 *
 * 于是约定：
 * - 以 `animation.` 开头 → 当成**全名**，原样使用（现有数据全是这种写法，行为不变）；
 * - 其它 → 当成**短名**，拼成 `animation.<枪 id 的 path>.<短名>`。
 *
 * 这样配件就能写 `"Animation": ["hit_bayonet", "hit"]`：哪把枪做了 `hit_bayonet` 就用专属动画，
 * 没做的自动退回它自己的 `hit`，配件数据一个字都不用改。
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
