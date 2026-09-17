package com.atsuishio.superbwarfare.item.gun.launcher

import com.atsuishio.superbwarfare.data.gun.ShootParameters
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2
import com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.item.Rarity

@RegistryName("rpg")
object RpgItem : GeoGunItemV2(Properties().rarity(Rarity.RARE)) {
    override fun shootBullet(parameters: ShootParameters): Boolean {
        if (!super.shootBullet(parameters)) return false

        val shooter = parameters.shooter
        val level = parameters.level

        if (shooter != null) {
            sendParticle(
                level, ParticleTypes.CLOUD, shooter.x + 1.8 * shooter.lookAngle.x,
                shooter.y + shooter.bbHeight - 0.1 + 1.8 * shooter.lookAngle.y,
                shooter.z + 1.8 * shooter.lookAngle.z,
                30, 0.4, 0.4, 0.4, 0.005, true
            )
        }

        return true
    }
}
