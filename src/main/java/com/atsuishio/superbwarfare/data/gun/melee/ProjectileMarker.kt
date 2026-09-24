package com.atsuishio.superbwarfare.data.gun.melee

/**
 * `Projectile` 字段的引擎保留标记。
 *
 * `Projectile` 的取值有两种性质——**引擎保留标记**与**资源 id**，裸词无法从字面区分。
 * 统一加 `@` 前缀后只需记一条规则：**`@` 开头 = 引擎语义**（与已有的 `"@Ammo"`/`"@GunDefault"`/
 * `"@ShotgunAmmo"` 同一套约定）。
 *
 * 归一化走 [normalizeProjectileMarker]：`trim().lowercase().removePrefix("@")`，
 * 所以旧数据里的裸写法（`"empty"`/`"ray"`）继续有效，作为兼容别名。
 */
object ProjectileMarker {
    /** 不发射任何东西（`"Projectile": "@empty"`） */
    const val EMPTY: String = "empty"

    /** 射线类武器（`"Projectile": "@ray"`），走 `GunItem.shootRay` */
    const val RAY: String = "ray"

    /**
     * 近战专属枪械（`"Projectile": "@melee"`）：不发射弹丸，左键直通近战输入。
     */
    const val MELEE: String = "melee"

    /** 写入数据时的标准写法（带 `@`） */
    fun canonical(marker: String): String = "@" + marker.normalizeProjectileMarker()
}

/**
 * 把 `Projectile` 的取值归一化：去空白、转小写、去掉 `@` 前缀。
 *
 * `"@ray"`、`"ray"`、`" RAY "` 都归一到 `"ray"`。
 */
fun String.normalizeProjectileMarker(): String = trim().lowercase().removePrefix("@")

/** 是否是「不发射」的引擎标记（`@empty` / `@ray` / `@melee`） */
fun String.isEngineProjectileMarker(): Boolean {
    return when (normalizeProjectileMarker()) {
        ProjectileMarker.EMPTY, ProjectileMarker.RAY, ProjectileMarker.MELEE -> true
        else -> false
    }
}

/** 是否是近战专属标记（`@melee`，兼容裸写 `melee`） */
fun String.isMeleeProjectileMarker(): Boolean =
    normalizeProjectileMarker() == ProjectileMarker.MELEE
