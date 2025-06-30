package com.atsuishio.superbwarfare.config.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ControlConfig {

    public static ModConfigSpec.BooleanValue INVERT_AIRCRAFT_CONTROL;
    public static ModConfigSpec.IntValue MOUSE_SENSITIVITY;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("control");

        builder.comment("Set true to invert aircraft control");
        INVERT_AIRCRAFT_CONTROL = builder.define("invert_aircraft_control", false);

        builder.comment("Sensitivity of mouse");
        MOUSE_SENSITIVITY = builder.defineInRange("mouse_sensitivity", 100, 10, 200);

        builder.pop();
    }
}
