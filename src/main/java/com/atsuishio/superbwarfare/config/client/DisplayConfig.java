package com.atsuishio.superbwarfare.config.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public class DisplayConfig {

    public static ModConfigSpec.BooleanValue ENABLE_GUN_LOD;
    public static ModConfigSpec.IntValue WEAPON_HUD_X_OFFSET;
    public static ModConfigSpec.IntValue WEAPON_HUD_Y_OFFSET;
    public static ModConfigSpec.BooleanValue KILL_INDICATION;
    public static ModConfigSpec.BooleanValue AMMO_HUD;
    public static ModConfigSpec.BooleanValue VEHICLE_INFO;
    public static ModConfigSpec.BooleanValue FLOAT_CROSS_HAIR;
    public static ModConfigSpec.BooleanValue CAMERA_ROTATE;
    public static ModConfigSpec.BooleanValue ARMOR_PLATE_HUD;
    public static ModConfigSpec.BooleanValue STAMINA_HUD;
    public static ModConfigSpec.BooleanValue DOG_TAG_NAME_VISIBLE;
    public static ModConfigSpec.BooleanValue DOG_TAG_ICON_VISIBLE;
    public static ModConfigSpec.IntValue WEAPON_SCREEN_SHAKE;
    public static ModConfigSpec.IntValue EXPLOSION_SCREEN_SHAKE;
    public static ModConfigSpec.IntValue SHOCK_SCREEN_SHAKE;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("display");

        builder.comment("Set true to enable gun lod");
        ENABLE_GUN_LOD = builder.define("enable_gun_lod", false);

        builder.comment("The x offset of weapon hud");
        WEAPON_HUD_X_OFFSET = builder.defineInRange("weapon_hud_x_offset", 0, -1000, 1000);

        builder.comment("The y offset of weapon hud");
        WEAPON_HUD_Y_OFFSET = builder.defineInRange("weapon_hud_y_offset", 0, -1000, 1000);

        builder.comment("Set true if you want to show kill indication while killing an entity");
        KILL_INDICATION = builder.define("kill_indication", true);

        builder.comment("Set true to show ammo and gun info on HUD");
        AMMO_HUD = builder.define("ammo_hud", true);

        builder.comment("Set true to display vehicle info when aiming at a vehicle");
        VEHICLE_INFO = builder.define("vehicle_info", true);

        builder.comment("Set true to enable float cross hair");
        FLOAT_CROSS_HAIR = builder.define("float_cross_hair", true);

        builder.comment("Set true to enable camera rotate when holding a gun");
        CAMERA_ROTATE = builder.define("camera_rotate", true);

        builder.comment("Set true to enable armor plate hud");
        ARMOR_PLATE_HUD = builder.define("armor_plate_hud", true);

        builder.comment("Set true to enable stamina hud");
        STAMINA_HUD = builder.define("stamina_hud", true);

        builder.comment("Set true to show the name of dog tag in kill messages");
        DOG_TAG_NAME_VISIBLE = builder.define("dog_tag_name_visible", true);

        builder.comment("Set true to show the icon of dog tag in kill messages");
        DOG_TAG_ICON_VISIBLE = builder.define("dog_tag_icon_visible", false);

        builder.comment("The strength of screen shaking while firing with a weapon");
        WEAPON_SCREEN_SHAKE = builder.defineInRange("weapon_screen_shake", 100, 0, 100);

        builder.comment("The strength of screen shaking while exploding");
        EXPLOSION_SCREEN_SHAKE = builder.defineInRange("explosion_screen_shake", 100, 0, 100);

        builder.comment("The strength of screen shaking when shocked");
        SHOCK_SCREEN_SHAKE = builder.defineInRange("shock_screen_shake", 100, 0, 100);

        builder.pop();
    }
}
