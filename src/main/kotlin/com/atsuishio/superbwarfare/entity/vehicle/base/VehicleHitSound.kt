package com.atsuishio.superbwarfare.entity.vehicle.base

import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.damagesource.DamageSource
import java.util.WeakHashMap

/**
 * PJM: звуки попадания по технике (BattleBit).
 *
 * При попадании пули/снаряда/взрыва в технику:
 *  - снаружи, позиционно (слышат все рядом) — металлический звон [ModSounds.VEHICLE_HIT_METAL];
 *  - пассажирам, от 1-го лица (звук в точке игрока, без затухания) — глухой внутренний удар:
 *    тяжёлая техника → [ModSounds.VEHICLE_HIT_HEAVY], лёгкая → [ModSounds.VEHICLE_HIT_LIGHT].
 *
 * Столкновения (vehicle_strike) обрабатываются штатным кодом — здесь только «попадание».
 */
object VehicleHitSound {

    // ponytail: короткий троттлинг, чтобы очередь пуль (миниган/дробовик) не превращалась в кашу
    private const val COOLDOWN_TICKS = 3L

    private val lastHitTick = WeakHashMap<VehicleEntity, Long>()

    /** Тяжёлая техника (танки/тяжёлая броня/арта) по entity id. Всё остальное — лёгкое. */
    private val HEAVY_HIT = setOf(
        "m_1a_2", "t_90a", "ztz_99a", "bmp_2", "bradley",
        "yx_100", "prism_tank", "lav_150", "lav_25", "lav_ad", "plz_05"
    )

    fun onVehicleHurt(vehicle: VehicleEntity, source: DamageSource) {
        val level = vehicle.level()
        if (level !is ServerLevel) return
        if (!isImpactDamage(source)) return

        val now = level.gameTime
        val last = lastHitTick[vehicle]
        if (last != null && now - last < COOLDOWN_TICKS) return
        lastHitTick[vehicle] = now

        // Внешний металлический звон — позиционно у техники.
        level.playSound(
            null, vehicle, ModSounds.VEHICLE_HIT_METAL.get(), vehicle.soundSource,
            1f, 0.9f + vehicle.random.nextFloat() * 0.2f
        )

        // Внутренний удар — только пассажирам, в точке игрока (эффективно без затухания = «от 1 лица»).
        val id = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.type).path
        val interior = if (id in HEAVY_HIT) ModSounds.VEHICLE_HIT_HEAVY.get() else ModSounds.VEHICLE_HIT_LIGHT.get()
        val holder = Holder.direct(interior)
        for (passenger in vehicle.passengers) {
            if (passenger !is ServerPlayer) continue
            passenger.connection.send(
                ClientboundSoundPacket(
                    holder, SoundSource.PLAYERS,
                    passenger.x, passenger.eyeY, passenger.z,
                    1f, 0.9f + passenger.random.nextFloat() * 0.2f,
                    level.random.nextLong()
                )
            )
        }
    }

    /** «Попадание» = пуля/снаряд/взрыв, но не столкновение (у него свой vehicle_strike). */
    private fun isImpactDamage(source: DamageSource): Boolean {
        if (source.`is`(ModDamageTypes.VEHICLE_STRIKE)) return false
        return DamageTypeTool.isGunDamage(source)
                || source.`is`(ModTags.DamageTypes.PROJECTILE)
                || source.`is`(ModTags.DamageTypes.PROJECTILE_ABSOLUTE)
                || source.`is`(DamageTypeTags.IS_EXPLOSION)
    }
}
