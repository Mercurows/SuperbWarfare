package com.atsuishio.superbwarfare.item.gun.shotgun

import com.atsuishio.superbwarfare.data.gun.ShootParameters
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2
import com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Rarity

@RegistryName("homemade_shotgun")
object HomemadeShotgunItem : GeoGunItemV2(Properties().rarity(Rarity.COMMON)) {

    override fun beforeShoot(parameters: ShootParameters) {
        super.beforeShoot(parameters)

        val shooter = parameters.shooter
        val level = parameters.level

        if (shooter is ServerPlayer) {
            sendParticle(
                level,
                ParticleTypes.CLOUD,
                shooter.getX() + 1.8 * shooter.getLookAngle().x,
                shooter.getY() + shooter.getBbHeight() - 0.1 + 1.8 * shooter.getLookAngle().y,
                shooter.getZ() + 1.8 * shooter.getLookAngle().z,
                30,
                0.4,
                0.4,
                0.4,
                0.005,
                true,
                shooter
            )
        }
    }
}
