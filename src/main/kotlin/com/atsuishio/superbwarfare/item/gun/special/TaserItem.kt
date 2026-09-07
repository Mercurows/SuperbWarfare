package com.atsuishio.superbwarfare.item.gun.special

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.ShootParameters
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2
import net.minecraft.world.entity.Entity
import net.minecraftforge.common.capabilities.ForgeCapabilities

@RegistryName("taser")
object TaserItem : GeoGunItemV2(Properties()) {

    override fun afterShoot(parameters: ShootParameters) {
        super.afterShoot(parameters)

        val data = parameters.data
        val stack = data.stack
        stack.getCapability(ForgeCapabilities.ENERGY)
            .ifPresent { it.extractEnergy(400, false) }
    }

    override fun canShoot(data: GunData, shooter: Entity?): Boolean {
        val stack = data.stack
        val hasEnoughEnergy = stack.getCapability(ForgeCapabilities.ENERGY)
            .map { it.energyStored >= 400 }
            .orElseGet { false }

        if (!hasEnoughEnergy) return false

        return super.canShoot(data, shooter)
    }
}
