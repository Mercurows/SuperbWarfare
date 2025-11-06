package com.atsuishio.superbwarfare.item.gun.vehicle;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VehicleGun extends GunItem {

    public VehicleGun() {
        super(new Properties());

        setProperty(GunProp.AUTO_RELOAD, v -> true);
    }

    @Override
    public void init(GunData data) {
    }

    @Override
    public boolean isInitialized(GunData data) {
        return true;
    }

    public boolean canShoot(GunData data, @Nullable Entity shooter) {
        return data.get(GunProp.PROJECTILE_AMOUNT) > 0
                && !data.overHeat.get()
                && data.get(GunProp.HEAT_PER_SHOOT) <= (100 + data.get(GunProp.HEAT_PER_SHOOT) - data.heat.get())
                && !data.reloading()
                && !data.charging()
                && !data.bolt.needed.get()
                // TODO 能否优化这个判断
                && (data.useBackpackAmmo() ? data.backupAmmoCount.get() : data.ammo.get()) >= data.get(GunProp.AMMO_COST_PER_SHOOT);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("des.superbwarfare.vehicle_gun").withStyle(ChatFormatting.RED));
    }
}