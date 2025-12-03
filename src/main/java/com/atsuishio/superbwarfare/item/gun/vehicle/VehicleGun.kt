package com.atsuishio.superbwarfare.item.gun.vehicle

import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.entity.vehicle.PrismTankEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.world.phys.EntityResult
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage
import javax.annotation.ParametersAreNonnullByDefault

class VehicleGun : GunItem(Properties()) {

    override fun computeProperties(gunData: GunData, rawData: DefaultGunData): DefaultGunData {
        if (rawData.autoReload == null) {
            rawData.autoReload = true
        }
        // TODO 如何处理真的想设置null的情况
        if (rawData.shootShake == null) {
            rawData.shootShake = Vec3(5.0, 6.0, 9.0)
        }

        return rawData
    }

    override fun init(data: GunData) {}
    override fun isInitialized(data: GunData) = true
    override fun enableShootTimer() = true

    override fun canShoot(data: GunData, shooter: Entity?): Boolean {
        if (shooter !is VehicleEntity) return false

        return data.compute().projectileAmount > 0
                && !data.overHeat.get()
                && data.compute().heatPerShoot <= (100 + data.compute().heatPerShoot - data.heat.get())
                && !data.reloading()
                && !data.charging()
                && !data.bolt.needed.get()
                && shooter.getAmmo(data) >= data.compute().ammoCostPerShoot
    }

    override fun getEnergyProvider(data: GunData, ammoSupplier: Entity?): IEnergyStorage? {
        return if (ammoSupplier != null) {
            ammoSupplier.getCapability(Capabilities.EnergyStorage.ENTITY, Direction.UP)
        } else {
            super.getEnergyProvider(data, null)
        }
    }

    @ParametersAreNonnullByDefault
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
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
        prismTank.getEntityData().set(VehicleEntity.LASER_LENGTH, root.distanceTo(result.hitPos).toFloat())
        prismTank.hitEntity(result.hitPos, data, shooter)
        prismTank.getEntityData().set(VehicleEntity.LASER_SCALE, data.compute().shootAnimationTime.toFloat())
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
        prismTank.getEntityData().set(VehicleEntity.LASER_LENGTH, root.distanceTo(result.getLocation()).toFloat())
        prismTank.hitBlock(result.getLocation(), data, shooter)
        prismTank.getEntityData().set(VehicleEntity.LASER_SCALE, data.compute().shootAnimationTime.toFloat())
    }
}