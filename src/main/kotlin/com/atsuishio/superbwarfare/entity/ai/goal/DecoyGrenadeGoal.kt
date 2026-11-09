package com.atsuishio.superbwarfare.entity.ai.goal

import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.entity.projectile.DecoyGrenadeEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.goal.Goal
import java.util.*

class DecoyGrenadeGoal(private val mob: Mob, private val decoy: DecoyGrenadeEntity) : Goal() {
    private var pathCooldown = 0

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET)
    }

    override fun canUse(): Boolean {
        if (!ProjectileConfig.DECOY_GRENADE_ATTRACT_MOBS.get()) return false
        val radius = ProjectileConfig.DECOY_GRENADE_RADIUS.get().toDouble()
        return mob.isAlive && !mob.isNoAi && decoy.isAlive && decoy.activated && mob.level() === decoy.level() &&
                mob.distanceToSqr(decoy) <= radius * radius
    }

    override fun canContinueToUse() = canUse()

    override fun requiresUpdateEveryTick() = true

    override fun start() {
        mob.target = null
        mob.navigation.stop()
        pathCooldown = 0
    }

    override fun tick() {
        mob.target = null
        mob.lookControl.setLookAt(decoy.x, decoy.y + 0.1, decoy.z)
        if (mob.distanceToSqr(decoy) <= 2.25) mob.navigation.stop()
        else if (--pathCooldown <= 0) {
            mob.navigation.moveTo(decoy.x, decoy.y, decoy.z, 1.1)
            pathCooldown = 10
        }
    }

    override fun stop() {
        mob.navigation.stop()
    }
}
