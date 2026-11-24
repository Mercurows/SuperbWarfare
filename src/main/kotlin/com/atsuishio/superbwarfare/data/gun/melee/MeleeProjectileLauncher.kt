package com.atsuishio.superbwarfare.data.gun.melee

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties
import com.atsuishio.superbwarfare.entity.projectile.SwordWaveEntity
import com.atsuishio.superbwarfare.perk.Perk
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.projectile.Projectile
import net.minecraftforge.registries.ForgeRegistries

/** Script API for launching a projectile during a validated melee swing. */
open class MeleeProjectileLauncher internal constructor(
    private val player: ServerPlayer,
    private val data: GunData,
    private val roll: Float,
    private val headshot: Float,
) {
    // The script scope persists between calls. Keep a retained launcher from firing on a later swing.
    private var active = true

    open fun launch(type: String, damageMultiplier: Double, lifeTicks: Int): Boolean {
        if (!active || !damageMultiplier.isFinite() || damageMultiplier <= 0 || lifeTicks <= 0) return false
        val damage = (data.get(GunProp.MELEE_DAMAGE) * damageMultiplier).toFloat()
        if (!damage.isFinite() || damage <= 0f) return false
        val id = ResourceLocation.tryParse(type) ?: return false
        val world = player.serverLevel()
        val entity = ForgeRegistries.ENTITY_TYPES.getValue(id)?.create(world) ?: return false
        if (entity !is Projectile || entity !is IBulletProperties) return false

        entity.owner = player
        entity.setDamage(damage)
        entity.setLife(lifeTicks)
        data.perk.get(Perk.Type.AMMO)?.ammoRgb?.let(entity::setRGB)
        val direction = player.lookAngle
        val launchY = if (entity is SwordWaveEntity) player.eyeY else player.eyeY - 0.18
        entity.moveTo(player.x, launchY, player.z, player.yRot, player.xRot)
        entity.shoot(direction.x, direction.y, direction.z, entity.getVelocity(), 0f)
        if (entity is SwordWaveEntity) {
            entity.roll = roll % 360f
            entity.setHeadShot(headshot)
            // Preserve the player's yaw even for a vertical launch, where velocity has no stable yaw.
            entity.yRot = player.yRot
            entity.xRot = player.xRot
        }
        return world.addFreshEntity(entity)
    }

    companion object {
        internal fun canLaunch(data: GunData, action: ResolvedMeleeAction, release: Boolean): Boolean =
            action.projectileRoll(release) != null &&
                data.perk.getInstances(Perk.Type.FUNCTIONAL).any { it.level > 0 && it.perk.launchesMeleeProjectile }

        internal fun dispatch(player: ServerPlayer, data: GunData, action: ResolvedMeleeAction, release: Boolean) {
            val roll = action.projectileRoll(release) ?: return
            val headshot = action.resolvedHeadshot(data.get(GunProp.MELEE_HEADSHOT)).toFloat()
            val launcher = MeleeProjectileLauncher(player, data, roll, headshot)
            try {
                for (instance in data.perk.getInstances(Perk.Type.FUNCTIONAL)) {
                    if (instance.level > 0 && instance.perk.launchesMeleeProjectile) {
                        instance.perk.onMeleeLaunch(instance, launcher)
                    }
                }
            } finally {
                launcher.active = false
            }
        }
    }
}
