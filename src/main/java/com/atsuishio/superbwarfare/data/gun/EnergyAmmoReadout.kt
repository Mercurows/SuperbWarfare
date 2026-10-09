package com.atsuishio.superbwarfare.data.gun

/**
 * 「能量即弹药」的背包型能量武器的弹药读数，见 [GunData.energyAmmoReadout]。
 *
 * 这类枪（`Magazine <= 0` 且主弹种直接是能量，如 `ql_1031`）没有弹匣，能量就是唯一弹源：
 * 开火时按 `AmmoCostPerShoot` 直接从枪内能量扣 FE（见 `EnergyAmmoStrategy` 的背包型分支）。
 * 所以界面上该显示的不是弹匣发数那套数字 —— 它们的 `GunData.ammo` 恒为 `0`，
 * 而 `Magazine <= 0` 又会让弹药条按满算，两者合起来就是「满条 + 0」，什么信息都没有。
 *
 * 与 [GunData.isEnergyMagazine]（有弹匣、能量只在换弹时折算成发数）区分开：那边弹匣发数是真数字，
 * 照常走原有读数即可。
 */
data class EnergyAmmoReadout(
    /** 枪内当前能量（FE） */
    val stored: Int,

    /** 枪内能量上限（FE）。没有能量能力的枪上是 `0` */
    val capacity: Int,

    /** 按当前射击模式的 `AmmoCostPerShoot` 折算，这点能量还够开几发 */
    val shots: Int,
) {
    /** 电量百分比，`0.0` ~ `1.0` —— 正好当作弹药条的进度用 */
    val ratio: Float
        get() = if (capacity <= 0) 0f else (stored.toFloat() / capacity.toFloat()).coerceIn(0f, 1f)
}
