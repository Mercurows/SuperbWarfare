package com.atsuishio.superbwarfare.entity.vehicle.pjm

import com.atsuishio.superbwarfare.entity.projectile.WireGuideMissileEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.phys.Vec3
import kotlin.math.cos

/**
 * PJM: КОЭП ТШУ-1 «Штора-1» — комплекс оптико-электронного подавления.
 *
 * ИК-прожекторы на передней части башни излучают модулированный сигнал, который ослепляет
 * трассер-датчик подлетающей ПТУР с полуавтоматическим наведением ([WireGuideMissileEntity]).
 * Система управления теряет ракету и плавно уводит её в сторону.
 *
 * Подавление идёт постепенно, пока ракета остаётся в переднем секторе башни
 * ([ARC_DEGREES] в каждую сторону от ствола) и приближается: каждый тик наведение
 * уводится на [DRIFT_PER_TICK] градусов, так что ракета отворачивает по дуге, а не рывком.
 * Ушедшая из сектора ракета остаётся без наведения и летит по инерции мимо.
 */
object ShtoraSystem {
    /** Дальность подавления, блоков */
    private const val RANGE = 40.0

    /** Полусектор подавления по азимуту относительно направления башни, градусов */
    private const val ARC_DEGREES = 30.0

    /** Скорость увода ослеплённой ракеты, градусов за тик */
    private const val DRIFT_PER_TICK = 1.2f

    private val ARC_COS = cos(Math.toRadians(ARC_DEGREES))

    /** Ослепляет все подлетающие ПТУР в переднем секторе башни. Вызывать на сервере каждый тик. */
    fun jamIncomingMissiles(vehicle: VehicleEntity) {
        if (vehicle.level().isClientSide || vehicle.isWreck || !vehicle.engineOn) return

        val front = vehicle.getBarrelVector(1f).flatten() ?: return
        val missiles = vehicle.level().getEntitiesOfClass(
            WireGuideMissileEntity::class.java,
            vehicle.boundingBox.inflate(RANGE)
        )

        for (missile in missiles) {
            val toMissile = vehicle.position().vectorTo(missile.position())
            // ракета удаляется — подавлять нечего
            if (missile.deltaMovement.dot(toMissile) >= 0) continue
            // ракета вне переднего сектора башни
            if (front.dot(toMissile.flatten() ?: continue) < ARC_COS) continue

            dazzle(missile)
        }
    }

    /**
     * Сбивает наведение и уводит ракету по дуге в сторону.
     *
     * Сторона увода берётся из id ракеты, чтобы за все тики подавления она оставалась одной и
     * той же — иначе ракета дёргалась бы на месте вместо плавного отворота.
     */
    private fun dazzle(missile: WireGuideMissileEntity) {
        missile.setLost(true)

        val side = if (missile.id % 2 == 0) 1f else -1f
        missile.yRot += side * DRIFT_PER_TICK
        missile.xRot = (missile.xRot - 0.25f * DRIFT_PER_TICK).coerceIn(-89f, 89f)
        missile.deltaMovement = missile.lookAngle.scale(missile.deltaMovement.length())
    }

    /** Горизонтальная проекция, нормализованная; null если вектор смотрит строго вверх/вниз */
    private fun Vec3.flatten(): Vec3? {
        val flat = Vec3(this.x, 0.0, this.z)
        return if (flat.lengthSqr() < 1.0e-6) null else flat.normalize()
    }
}
