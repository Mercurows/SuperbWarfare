package com.atsuishio.superbwarfare.compat.tacz

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.event.common.EntityHurtByGunEvent
import com.tacz.guns.api.item.IGun
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.loading.LoadingModList
import org.apache.maven.artifact.versioning.DefaultArtifactVersion

object TACZGunEventHandler {
    fun entityHurtByTACZGun(event: EntityHurtByGunEvent.Pre) {
        if (event.getHurtEntity() is VehicleEntity) {
            event.setHeadshot(false)
        }
    }

    fun hasMod(): Boolean {
        return ModList.get().isLoaded("tacz")
    }

    fun compatCondition(): Boolean {
        val modFile = LoadingModList.get().getModFileById("tacz") ?: return false
        val modVersion = DefaultArtifactVersion(modFile.versionString())
        return modVersion >= DefaultArtifactVersion("1.1.4")
    }

    fun getTaczCompatIcon(stack: ItemStack): ResourceLocation? {
        val item = stack.item
        if (item is IGun) {
            val gunId: ResourceLocation = item.getGunId(stack)
            val gunData = TimelessAPI.getClientGunIndex(gunId)
                .map { obj -> obj.gunData }.orElse(null)
            val display = TimelessAPI.getGunDisplay(stack).orElse(null)
            if (gunData != null && display != null) {
                return display.hudTexture
            }
        }
        return null
    }
}
