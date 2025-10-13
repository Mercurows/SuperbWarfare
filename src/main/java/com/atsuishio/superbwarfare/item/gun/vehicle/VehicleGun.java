package com.atsuishio.superbwarfare.item.gun.vehicle;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.vehicle.VehicleProp;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

// TODO 实现基于VehicleGun的开火控制
public class VehicleGun extends GunItem {

    public VehicleGun() {
        super(new Properties());
    }

    @Nullable
    public static GunData fromVehicle(VehicleEntity vehicle, int seatIndex) {
        var seats = vehicle.data().get(VehicleProp.SEATS);
        if (seatIndex < 0 || seatIndex >= seats.size()) return null;

        var seat = seats.get(seatIndex);
        if (seat.weaponData == null) return null;

        // TODO 正确读取和存储VehicleGun ItemStack
        var data = GunData.from(new ItemStack(ModItems.VEHICLE_GUN.get()));
        data.defaultDataSupplier = () -> seat.weaponData;

        return data;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.vehicle_gun").withStyle(ChatFormatting.RED));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}