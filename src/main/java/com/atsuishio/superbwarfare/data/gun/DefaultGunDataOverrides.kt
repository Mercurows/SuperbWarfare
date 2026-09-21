package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.Prop
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3

/**
 * 把 PMC 的差量一次性写回一份**新的** [DefaultGunData]。
 *
 * 全部属性合并进**一次**编译器生成的 `copy(...)`——无反射、无 JSON、与差量里有几项无关。
 *
 * 只处理能忠实写回的属性（`plainProp`：结果类型 == 字段类型）。以下两类会被跳过：
 * - `leveledIntProp`（`Magazine`、`BoltActionTime` 及各种换弹时间）：结果是按弹匣等级解析出的
 *   单个 Int，而字段是等级表，写回会把多等级数据压平；
 * - 尚未列入本函数的属性（例如 `MeleeDamageTime`）。
 *
 * 跳过**不会改变行为**：读取路径是"先查 diff、再查 computed"，没被写回的值依然生效，
 * 只是这份 computed 值还不完整。
 *
 * TODO 目前跳过的属性是静默的；下一步会把"差量里有、本函数没写回"的属性名在开发环境打成日志，
 * 用来把这份清单补全（也顺手把 KSP 生成的版本替换掉本文件）。
 */
fun DefaultGunData.withOverrides(diff: Map<out Prop<*, *, *, *, *>, Any?>): DefaultGunData {
    if (diff.isEmpty()) return this

    fun num(prop: Prop<*, *, *, *, *>, fallback: Double): Double = (diff[prop] as? Number)?.toDouble() ?: fallback
    fun int(prop: Prop<*, *, *, *, *>, fallback: Int): Int = (diff[prop] as? Number)?.toInt() ?: fallback
    fun flt(prop: Prop<*, *, *, *, *>, fallback: Float): Float = (diff[prop] as? Number)?.toFloat() ?: fallback
    fun bool(prop: Prop<*, *, *, *, *>, fallback: Boolean?): Boolean? =
        if (diff.containsKey(prop)) diff[prop] as? Boolean else fallback

    return copy(
        maxDurability = int(GunProp.MAX_DURABILITY, maxDurability),
        durabilityPerShoot = int(GunProp.DURABILITY_PER_SHOOT, durabilityPerShoot),
        maxEnergy = int(GunProp.MAX_ENERGY, maxEnergy),
        maxReceiveEnergy = int(GunProp.MAX_RECEIVE_ENERGY, maxReceiveEnergy),
        maxExtractEnergy = int(GunProp.MAX_EXTRACT_ENERGY, maxExtractEnergy),
        recoilX = num(GunProp.RECOIL_X, recoilX),
        recoilY = num(GunProp.RECOIL_Y, recoilY),
        shootShake = if (diff.containsKey(GunProp.SHOOT_SHAKE)) diff[GunProp.SHOOT_SHAKE] as? SerializedVec3 else shootShake,
        spread = num(GunProp.SPREAD, spread),
        damage = num(GunProp.DAMAGE, damage),
        headshot = num(GunProp.HEADSHOT, headshot),
        velocity = num(GunProp.VELOCITY, velocity),
        meleeDuration = int(GunProp.MELEE_DURATION, meleeDuration),
        meleeAngle = int(GunProp.MELEE_ANGLE, meleeAngle),
        zoomSpreadRate = num(GunProp.ZOOM_SPREAD_RATE, zoomSpreadRate),
        range = int(GunProp.RANGE, range),
        ammoCostPerShoot = int(GunProp.AMMO_COST_PER_SHOOT, ammoCostPerShoot),
        fuelPerAmmo = int(GunProp.FUEL_PER_AMMO, fuelPerAmmo),
        projectileAmount = int(GunProp.PROJECTILE_AMOUNT, projectileAmount),
        spreadPattern = if (diff.containsKey(GunProp.SPREAD_PATTERN)) {
            diff[GunProp.SPREAD_PATTERN] as? ProjectileSpreadPattern
        } else {
            spreadPattern
        },
        weight = num(GunProp.WEIGHT, weight),
        autoReload = bool(GunProp.AUTO_RELOAD, autoReload),
        defaultZoom = num(GunProp.DEFAULT_ZOOM, defaultZoom),
        minZoom = num(GunProp.MIN_ZOOM, minZoom),
        maxZoom = num(GunProp.MAX_ZOOM, maxZoom),
        burstAmount = int(GunProp.BURST_AMOUNT, burstAmount),
        bypassesArmor = num(GunProp.BYPASSES_ARMOR, bypassesArmor),
        soundRadius = num(GunProp.SOUND_RADIUS, soundRadius),
        rpm = int(GunProp.RPM, rpm),
        rpmMultiplier = num(GunProp.RPM_MULTIPLIER, rpmMultiplier),
        rpmAddAfterShoot = int(GunProp.RPM_ADD_AFTER_SHOOT, rpmAddAfterShoot),
        minCustomRpm = int(GunProp.CUSTOM_RPM_MIN, minCustomRpm),
        maxCustomRpm = int(GunProp.CUSTOM_RPM_MAX, maxCustomRpm),
        explosionDamage = num(GunProp.EXPLOSION_DAMAGE, explosionDamage),
        explosionRadius = num(GunProp.EXPLOSION_RADIUS, explosionRadius),
        heatPerShoot = num(GunProp.HEAT_PER_SHOOT, heatPerShoot),
        naturalCooldown = num(GunProp.NATURAL_COOLDOWN, naturalCooldown),
        drawTime = int(GunProp.DRAW_TIME, drawTime),
        zoomTime = int(GunProp.ZOOM_TIME, zoomTime),
        underwaterMotionScale = flt(GunProp.UNDERWATER_MOTION_SCALE, underwaterMotionScale),
        soundInfo = if (diff.containsKey(GunProp.SOUND_INFO)) {
            diff[GunProp.SOUND_INFO] as? SoundInfo ?: soundInfo
        } else {
            soundInfo
        },
    )
}
