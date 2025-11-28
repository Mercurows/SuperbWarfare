package com.atsuishio.superbwarfare.item.gun.vehicle;

import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.PrismTankEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.world.phys.EntityResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static com.atsuishio.superbwarfare.entity.vehicle.PrismTankEntity.LASER_LENGTH;
import static com.atsuishio.superbwarfare.entity.vehicle.PrismTankEntity.LASER_SCALE;

public class VehicleGun extends GunItem {

    public VehicleGun() {
        super(new Properties());
    }

    @Override
    public DefaultGunData computeProperties(GunData gunData, DefaultGunData rawData) {
        rawData.autoReload = true;
        // TODO 如何处理真的想设置null的情况
        if (rawData.shootShake == null) {
            rawData.shootShake = new Vec3(5, 6, 9);
        }

        return rawData;
    }

    @Override
    public void init(GunData data) {
    }

    @Override
    public boolean isInitialized(GunData data) {
        return true;
    }

    @Override
    public boolean enableShootTimer() {
        return true;
    }

    @Override
    public boolean canShoot(GunData data, @Nullable Entity shooter) {
        if (shooter instanceof VehicleEntity vehicle) {
            return data.compute().projectileAmount > 0
                    && !data.overHeat.get()
                    && data.compute().heatPerShoot <= (100 + data.compute().heatPerShoot - data.heat.get())
                    && !data.reloading()
                    && !data.charging()
                    && !data.bolt.needed.get()
                    && vehicle.getAmmo(data) >= data.compute().ammoCostPerShoot;
        } else {
            return false;
        }
    }

    @Override
    public IEnergyStorage getEnergyProvider(@NotNull GunData data, @Nullable Entity ammoSupplier) {
        if (ammoSupplier != null) {
            return ammoSupplier.getCapability(Capabilities.EnergyStorage.ENTITY, null);
        }

        return super.getEnergyProvider(data, null);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.vehicle_gun").withStyle(ChatFormatting.RED));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    // TODO 去掉特判
    @Override
    public void onRayHitEntity(Entity shooter, ServerLevel level, @NotNull GunData data, EntityResult result, Vec3 shootPosition, Vec3 shootDirection) {
        super.onRayHitEntity(shooter, level, data, result, shootPosition, shootDirection);
        if (shooter.getVehicle() instanceof PrismTankEntity prismTank) {
            Vec3 root = prismTank.getShootPos(shooter, 1);
            prismTank.getEntityData().set(LASER_LENGTH, (float) root.distanceTo(result.getHitPos()));
            prismTank.hitEntity(result.getHitPos(), data, shooter);
            prismTank.getEntityData().set(LASER_SCALE, (float) data.compute().shootAnimationTime);
        }
    }

    @Override
    public void onRayHitBlock(Entity shooter, ServerLevel level, @Nullable Entity target, @NotNull GunData data, Vec3 shootDirection, BlockHitResult result, @NotNull Vec3 pos) {
        super.onRayHitBlock(shooter, level, target, data, shootDirection, result, pos);
        if (shooter.getVehicle() instanceof PrismTankEntity prismTank) {
            Vec3 root = prismTank.getShootPos(shooter, 1);
            prismTank.getEntityData().set(LASER_LENGTH, (float) root.distanceTo(result.getLocation()));
            prismTank.hitBlock(result.getLocation(), data, shooter);
            prismTank.getEntityData().set(LASER_SCALE, (float) data.compute().shootAnimationTime);
        }
    }
}