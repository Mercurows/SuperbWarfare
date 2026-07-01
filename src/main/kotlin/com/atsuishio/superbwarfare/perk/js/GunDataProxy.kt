package com.atsuishio.superbwarfare.perk.js

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import net.minecraftforge.common.capabilities.ForgeCapabilities
import kotlin.jvm.optionals.getOrElse

/**
 * Proxy that exposes GunData properties and methods to JS perk scripts.
 */
class GunDataProxy(private val data: GunData) {

    // ── Gun Properties ──
    fun getBypassesArmor(): Double = data.get(GunProp.BYPASSES_ARMOR)

    fun getProjectileAmount(): Int = data.get(GunProp.PROJECTILE_AMOUNT)

    // ── Magazine ──
    fun getMagazine(): Int = data.get(GunProp.MAGAZINE)

    // ── Ammo ──
    fun getAmmo(): Int = data.ammo.get()

    fun setAmmo(value: Int) {
        data.ammo.set(value)
        data.save()
    }

    // ── Virtual Ammo ──
    fun getVirtualAmmo(): Int = data.virtualAmmo.get()

    fun addVirtualAmmo(value: Int) {
        data.virtualAmmo.add(value)
        data.save()
    }

    // ── Gun Type ──
    fun getGunType(): String = data.get(GunProp.GUN_TYPE).name

    // ── Backup Ammo ──
    fun countBackupAmmo(entityProxy: EntityProxy?): Int {
        val entity = entityProxy?.entity
        return data.countBackupAmmo(entity)
    }

    fun consumeBackupAmmo(entityProxy: EntityProxy?, amount: Int) {
        val entity = entityProxy?.entity
        data.consumeBackupAmmo(entity, amount)
    }

    fun hasInfiniteBackupAmmo(entityProxy: EntityProxy?): Boolean {
        val entity = entityProxy?.entity
        return data.hasInfiniteBackupAmmo(entity)
    }

    // ── Zoom / Aiming ──
    fun isZooming(): Boolean = data.zooming.get()

    // ── Energy (for Regeneration) ──
    fun getMaxEnergyStored(): Int {
        return data.stack.getCapability(ForgeCapabilities.ENERGY).map { it.maxEnergyStored }.getOrElse { 0 }
    }

    fun receiveEnergy(amount: Int): Int {
        return data.stack.getCapability(ForgeCapabilities.ENERGY)
            .map { it.receiveEnergy(amount, false) }
            .getOrElse { 0 }
    }
}
