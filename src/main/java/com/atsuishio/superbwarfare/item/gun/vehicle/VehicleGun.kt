package com.atsuishio.superbwarfare.item.gun.vehicle

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.PrismTankEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.world.phys.EntityResult
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.IEnergyStorage

open class VehicleGun : GunItem(Properties()) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        if (modifier[GunProp.AUTO_RELOAD] == null) {
            modifier[GunProp.AUTO_RELOAD] = true
        }
        if (modifier[GunProp.SHOOT_SHAKE] == null) {
            modifier[GunProp.SHOOT_SHAKE] = Vec3(5.0, 6.0, 9.0)
        }
    }

    override fun init(data: GunData) {}

    override fun isInitialized(data: GunData) = true

    override fun enableShootTimer() = true

    override fun canShoot(data: GunData, shooter: Entity?): Boolean {
        if (shooter !is VehicleEntity) return false

        return data.get(GunProp.PROJECTILE_AMOUNT) > 0
                && !data.overHeat.get()
                && data.get(GunProp.HEAT_PER_SHOOT) <= (100 + data.get(GunProp.HEAT_PER_SHOOT) - data.heat.get())
                && !data.reloading()
                && !data.charging()
                && !data.bolt.needed.get()
                && shooter.getAmmo(data) >= data.get(GunProp.AMMO_COST_PER_SHOOT)
    }

    override fun getEnergyProvider(data: GunData, ammoSupplier: Entity?): LazyOptional<IEnergyStorage> {
        return ammoSupplier?.getCapability(ForgeCapabilities.ENERGY, null) ?: return super.getEnergyProvider(data, null)
    }

    override fun appendHoverText(
        stack: ItemStack,
        pLevel: Level?,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.vehicle_gun").withStyle(ChatFormatting.RED))
    }

    // TODO 去掉特判
    override fun onRayHitEntity(
        shooter: Entity?,
        level: ServerLevel,
        data: GunData,
        result: EntityResult,
        shootPosition: Vec3?,
        shootDirection: Vec3?
    ) {
        super.onRayHitEntity(shooter, level, data, result, shootPosition, shootDirection)

        val prismTank = shooter?.vehicle as? PrismTankEntity ?: return

        val root = prismTank.getShootPos(shooter, 1f)
        prismTank.laserLength = root.distanceTo(result.hitVec).toFloat()
        prismTank.laserScale = data.get(GunProp.SHOOT_ANIMATION_TIME).toFloat()
        prismTank.hitEntity(result.hitVec, data, shooter)
    }

    override fun onRayHitBlock(
        shooter: Entity?,
        level: ServerLevel,
        target: Entity?,
        data: GunData,
        shootDirection: Vec3?,
        result: BlockHitResult,
        pos: Vec3
    ) {
        super.onRayHitBlock(shooter, level, target, data, shootDirection, result, pos)

        val prismTank = shooter?.vehicle as? PrismTankEntity ?: return

        val root = prismTank.getShootPos(shooter, 1f)
        prismTank.laserLength = root.distanceTo(result.getLocation()).toFloat()
        prismTank.laserScale = data.get(GunProp.SHOOT_ANIMATION_TIME).toFloat()
        prismTank.hitBlock(result.getLocation(), data, shooter)
    }

    override fun playFireSounds(data: GunData, shooter: Entity?, zoom: Boolean) {
    }
}