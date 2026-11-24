package com.atsuishio.superbwarfare.data.gun.melee

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.RightClickAction
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.item.gun.GunItem
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import java.util.*

object MeleeSwingTracker {
    private const val HIT_TOLERANCE_TICKS = 2
    private const val HIT_TIMEOUT_TICKS = 20

    private class Swing(
        val id: Int,
        val item: Item,
        val gunId: UUID,
        val slot: Int,
        val mode: Int,
        val dimension: ResourceKey<Level>,
        val actionIndex: Int,
        val chargeStrike: Boolean,
        val release: Boolean,
        val startedAt: Long,
        val nextStartAt: Long,
        val expiresAt: Long,
        val hitTicks: IntArray,
    ) {
        var nextHit = 0
    }

    data class Hit(val release: Boolean, val first: Boolean)

    private val swings = WeakHashMap<ServerPlayer, Swing>()

    fun start(player: ServerPlayer, data: GunData, id: Int, actionIndex: Int, chargeStrike: Boolean, release: Boolean) {
        if (!canSwing(player, data)) return
        val gunId = data.uuid ?: return
        val actions = data.meleeActions()
        if (actionIndex !in actions.indices || (chargeStrike && release)) return
        if (release && (data.get(GunProp.RIGHT_CLICK_ACTION) != RightClickAction.HEAVY_HOLD ||
                actionIndex != actions.indexOfFirst { it.isHeavy() }.coerceAtLeast(0))) return

        val now = player.server.tickCount.toLong()
        val previous = swings[player]
        if (previous != null && (id - previous.id <= 0 || now + HIT_TOLERANCE_TICKS < previous.nextStartAt)) return
        val startedAt = maxOf(now, previous?.nextStartAt ?: now)
        val timing = data.meleeSwingTiming(player, actionIndex, chargeStrike, release) ?: return
        val chainWindow = data.get(GunProp.MELEE_CHAIN_WINDOW)
        val nextStart = startedAt + maxOf(1, timing.duration - chainWindow, timing.hitTicks.lastOrNull() ?: 0)
        swings[player] = Swing(id, data.item(), gunId, player.inventory.selected, data.selectedFireMode.get(),
            player.level().dimension(), actionIndex, chargeStrike, release, startedAt, nextStart,
            startedAt + timing.duration + HIT_TIMEOUT_TICKS, timing.hitTicks)
    }

    fun acceptHit(player: ServerPlayer, data: GunData, id: Int, actionIndex: Int, chargeStrike: Boolean, hitIndex: Int): Hit? {
        val swing = swings[player] ?: return null
        if (id != swing.id || hitIndex < swing.nextHit || actionIndex != swing.actionIndex || chargeStrike != swing.chargeStrike) return null
        val hitTick = swing.hitTicks.getOrNull(hitIndex) ?: return null
        if (data.item() !== swing.item || data.uuid != swing.gunId || player.inventory.selected != swing.slot ||
            data.selectedFireMode.get() != swing.mode || player.level().dimension() != swing.dimension || !canSwing(player, data)) return null

        val now = player.server.tickCount.toLong()
        if (now > swing.expiresAt || now + HIT_TOLERANCE_TICKS < swing.startedAt + hitTick) return null
        swing.nextHit = hitIndex + 1
        return Hit(swing.release, hitIndex == 0)
    }

    private fun canSwing(player: ServerPlayer, data: GunData): Boolean {
        if (player.isSpectator || !player.isAlive || player.isUsingItem || player.hasEffect(ModMobEffects.SHOCK.get()) ||
            !GunItem.isHeldWeapon(data.stack) || !data.item().hasMeleeAttack(data) || data.item().isBroken(data.stack)) return false
        val vehicle = player.vehicle
        return vehicle !is VehicleEntity || !vehicle.banHand(player)
    }
}
