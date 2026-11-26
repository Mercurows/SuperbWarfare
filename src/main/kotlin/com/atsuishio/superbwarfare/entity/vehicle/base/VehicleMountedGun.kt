package com.atsuishio.superbwarfare.entity.vehicle.base

import com.atsuishio.superbwarfare.data.gun.FireMode
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.ShootParameters
import com.atsuishio.superbwarfare.data.vehicle.VehicleMountSlot
import com.atsuishio.superbwarfare.event.GunEventHandler
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.tools.ActionTickClock
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

open class VehicleMountedGun(val installed: VehicleMounts.Installed) {
    val data = GunData.from(installed.stack)
    private var operator: Player? = null
    private var pressed = false
    private var pending = false
    private var nextShot = Double.NEGATIVE_INFINITY
    private var triggerExpires = Long.MIN_VALUE

    init {
        GunEventHandler.init(null, data)
        cancelAction()
    }

    open fun trigger(player: Player, down: Boolean, renew: Boolean) {
        val tick = player.level().gameTime
        if (renew) {
            if (down && operator === player && pressed && tick < triggerExpires) triggerExpires = tick + 12
            return
        }
        if (!down && operator !== player) return
        if (down && operator !== player) {
            stop()
            operator = player
        }
        if (down && !pressed) pending = true
        pressed = down
        if (down) triggerExpires = tick + 12
    }

    open fun reload(player: Player) {
        operator = player
        pressed = false
        pending = false
        GunEventHandler.tryStartReload(player, data)
    }

    open fun stop() {
        if (operator == null) return
        operator = null
        pressed = false
        pending = false
        cancelAction()
    }

    private fun cancelAction() {
        data.batch {
            val needsBolt = data.bolt.needed.get()
            data.resetStatus()
            data.bolt.needed.set(needsBolt)
            data.burstAmount.reset()
            data.reload.stage3Starter.finish()
        }
    }

    open fun tick(vehicle: VehicleEntity, slot: VehicleMountSlot, origin: Vec3, player: Player?) {
        val level = vehicle.level() as? ServerLevel ?: return
        val environmentRate = GunEventHandler.computeEnvironmentRate(vehicle, data)
        if (player == null || !player.isAlive || player.isSpectator) {
            stop()
            data.batch {
                GunEventHandler.handleCooldown(vehicle, data, environmentRate, ActionTickClock.budget)
                data.cooldown.tick(ActionTickClock.budget)
            }
            return
        }
        if (operator !== player) {
            stop()
            operator = player
        }

        if (data.get(GunProp.BOLT_ACTION_TIME) > 0 && data.bolt.needed.get() && data.bolt.actionTimer.get() == 0 && !data.reloading()) {
            data.startBolt()
        }
        GunEventHandler.gunTick(player, data, true, environmentRate)
        if (player.hasEffect(ModMobEffects.SHOCK.get()) || vehicle.hasWeapon(slot.controlSeat)) {
            stop()
            return
        }

        val mode = data.selectedFireModeInfo().mode
        if (level.gameTime >= triggerExpires) pressed = false
        if (level.gameTime < nextShot) return
        val wantsShot = pending || when (mode) {
            FireMode.AUTO -> pressed
            FireMode.BURST -> data.burstAmount.get() > 0
            else -> false
        }
        pending = false
        if (!wantsShot || !data.canShoot(player)) return
        if (mode != FireMode.SEMI && mode != FireMode.AUTO && mode != FireMode.BURST) return

        val direction = vehicle.getVectorFromString(slot.transform, 1f)
        val currentTick = level.gameTime.toDouble()
        val scheduledTick = if (currentTick - nextShot < 1.0) nextShot else currentTick
        nextShot = scheduledTick + (1200.0 / data.get(GunProp.RPM).coerceAtLeast(1)).coerceAtLeast(1.0)
        data.shoot(ShootParameters(player, player, level, origin, direction, data,
            data.get(GunProp.SPREAD), false, null, null, mounted = true))
    }
}
