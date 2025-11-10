package com.atsuishio.superbwarfare.item.gun.vehicle;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VehicleGun extends GunItem {

    public VehicleGun() {
        super(new Properties());
    }

    @Override
    public void init(GunData data) {
    }

    @Override
    public boolean isInitialized(GunData data) {
        return true;
    }

    public boolean canShoot(GunData data, @Nullable Entity shooter) {
        return data.compute().projectileAmount > 0
                && !data.overHeat.get()
                && data.compute().heatPerShoot <= (100 + data.compute().heatPerShoot - data.heat.get())
                && !data.reloading()
                && !data.charging()
                && !data.bolt.needed.get()
                && (data.useBackpackAmmo() ? data.backupAmmoCount.get() : data.ammo.get()) >= data.compute().ammoCostPerShoot;
    }

    @Override
    public LazyOptional<IEnergyStorage> getEnergyProvider(@NotNull GunData data, @Nullable Entity ammoSupplier) {
        if (ammoSupplier != null) {
            return ammoSupplier.getCapability(ForgeCapabilities.ENERGY, null);
        }

        return super.getEnergyProvider(data, null);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("des.superbwarfare.vehicle_gun").withStyle(ChatFormatting.RED));
    }
}