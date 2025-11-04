package com.atsuishio.superbwarfare.config.server;

import net.minecraftforge.common.ForgeConfigSpec;

public class MiscConfig {

    public static ForgeConfigSpec.BooleanValue ALLOW_TACTICAL_SPRINT;
    public static ForgeConfigSpec.BooleanValue SEND_KILL_FEEDBACK;
    public static ForgeConfigSpec.BooleanValue ALLOW_FORCE_DAMAGE;
    public static ForgeConfigSpec.BooleanValue DROP_AMMO_BOX;
    public static ForgeConfigSpec.IntValue DEFAULT_ARMOR_LEVEL;
    public static ForgeConfigSpec.IntValue MILITARY_ARMOR_LEVEL;
    public static ForgeConfigSpec.IntValue HEAVY_MILITARY_ARMOR_LEVEL;
    public static ForgeConfigSpec.IntValue ARMOR_POINT_PER_LEVEL;
    public static ForgeConfigSpec.IntValue CHARGING_STATION_MAX_ENERGY;
    public static ForgeConfigSpec.IntValue CHARGING_STATION_GENERATE_SPEED;
    public static ForgeConfigSpec.IntValue CHARGING_STATION_TRANSFER_SPEED;
    public static ForgeConfigSpec.IntValue CHARGING_STATION_CHARGE_RADIUS;
    public static ForgeConfigSpec.IntValue CHARGING_STATION_DEFAULT_FUEL_TIME;
    public static ForgeConfigSpec.IntValue ARTILLERY_INDICATOR_LIST_SIZE;
    public static ForgeConfigSpec.BooleanValue MINE_HITBOX_INVISIBLE;
    public static ForgeConfigSpec.BooleanValue SMOKE_HIDE_TARGET;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("misc");

        builder.comment("Set true to enable tactical sprint");
        ALLOW_TACTICAL_SPRINT = builder.define("allow_tactical_sprint", false);

        builder.comment("Set true to enable kill feedback sending");
        SEND_KILL_FEEDBACK = builder.define("send_kill_feedback", true);

        builder.comment("Set true to enable force damage");
        ALLOW_FORCE_DAMAGE = builder.define("allow_force_damage", false);

        builder.comment("Whether to drop an ammo box after the player dies when GameRule KeepInventory is False");
        DROP_AMMO_BOX = builder.define("drop_ammo_box", true);

        builder.comment("The default maximum armor level for normal armors");
        DEFAULT_ARMOR_LEVEL = builder.defineInRange("default_armor_level", 1, 0, 10000000);

        builder.comment("The maximum armor level for armors with superbwarfare:military_armor tag");
        MILITARY_ARMOR_LEVEL = builder.defineInRange("military_armor_level", 2, 0, 10000000);

        builder.comment("The maximum armor level for armors with superbwarfare:military_armor_heavy tag(will override superbwarfare:military_armor tag!)");
        HEAVY_MILITARY_ARMOR_LEVEL = builder.defineInRange("heavy_military_armor_level", 3, 0, 10000000);

        builder.comment("The points per level for armor plate");
        ARMOR_POINT_PER_LEVEL = builder.defineInRange("armor_point_per_level", 15, 0, 10000000);

        builder.comment("Max energy storage of charging station");
        CHARGING_STATION_MAX_ENERGY = builder.defineInRange("charging_station_max_energy", 4000000, 1, Integer.MAX_VALUE);

        builder.comment("How much FE energy can charging station generate per tick");
        CHARGING_STATION_GENERATE_SPEED = builder.defineInRange("charging_station_generate_speed", 128, 1, Integer.MAX_VALUE);

        builder.comment("How much FE energy can charging station transfer per tick");
        CHARGING_STATION_TRANSFER_SPEED = builder.defineInRange("charging_station_transfer_speed", 100000, 1, Integer.MAX_VALUE);

        builder.comment("The charging radius of the charging station");
        CHARGING_STATION_CHARGE_RADIUS = builder.defineInRange("charging_station_charge_radius", 8, 0, 128);

        builder.comment("The default fuel time of the charging station");
        CHARGING_STATION_DEFAULT_FUEL_TIME = builder.defineInRange("charging_station_default_fuel_time", 1600, 1, Integer.MAX_VALUE);

        builder.comment("The max size of artillery indicator binding list");
        ARTILLERY_INDICATOR_LIST_SIZE = builder.defineInRange("artillery_indicator_list_size", 32, 1, Integer.MAX_VALUE);

        builder.comment("Set true to make mine hitbox invisible");
        MINE_HITBOX_INVISIBLE = builder.define("mine_hitbox_invisible", false);

        builder.comment("Set true to allow smoke to prevent entities from being set as target");
        SMOKE_HIDE_TARGET = builder.define("smoke_hide_target", false);

        builder.pop();
    }
}
