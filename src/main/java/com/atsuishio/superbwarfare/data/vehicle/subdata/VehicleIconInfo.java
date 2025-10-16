package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public record VehicleIconInfo(
        @SerializedName("VehicleIcon")
        String vehicleIcon,

        @SerializedName("ContainerIcon")
        @Nullable
        String containerIcon
) {
}
