package com.atsuishio.superbwarfare.compat.clothconfig.client;

import com.atsuishio.superbwarfare.config.client.VehicleControlConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class VehicleControlClothConfig {


    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory category = root.getOrCreateCategory(Component.translatable("config.superbwarfare.client.vehicle"));

        category.addEntry(entryBuilder
                .startBooleanToggle(Component.translatable("config.superbwarfare.client.vehicle.invert_aircraft_control"), VehicleControlConfig.INVERT_AIRCRAFT_CONTROL.get())
                .setDefaultValue(true)
                .setSaveConsumer(VehicleControlConfig.INVERT_AIRCRAFT_CONTROL::set)
                .setTooltip(Component.translatable("config.superbwarfare.client.vehicle.left_click_reload.des")).build()
        );
    }


}
