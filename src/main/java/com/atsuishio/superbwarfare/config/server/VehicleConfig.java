package com.atsuishio.superbwarfare.config.server;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class VehicleConfig {

    public static ModConfigSpec.BooleanValue COLLISION_DESTROY_SOFT_BLOCKS;
    public static ModConfigSpec.BooleanValue COLLISION_DESTROY_NORMAL_BLOCKS;
    public static ModConfigSpec.BooleanValue COLLISION_DESTROY_HARD_BLOCKS;
    public static ModConfigSpec.BooleanValue COLLISION_DESTROY_BLOCKS_BEASTLY;
    public static ModConfigSpec.BooleanValue VEHICLE_ITEM_PICKUP;
    public static ModConfigSpec.BooleanValue COLLECT_DROPS_BY_CRASHING;

    public static ModConfigSpec.ConfigValue<List<? extends String>> COLLISION_ENTITY_WHITELIST;

    public static final List<? extends String> DEFAULT_COLLISION_ENTITY_WHITELIST = List.of();

    public static ModConfigSpec.IntValue REPAIR_COOLDOWN;
    public static ModConfigSpec.DoubleValue REPAIR_AMOUNT;

    public static ModConfigSpec.IntValue SELF_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue SELF_EXPLOSION_COUNT;
    public static ModConfigSpec.IntValue AIR_CRASH_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue AIR_CRASH_EXPLOSION_COUNT;

    public static ModConfigSpec.IntValue VEHICLE_INFO_DISPLAY_DISTANCE;

    public static ModConfigSpec.IntValue ANNIHILATOR_SHOOT_COST;


    public static ModConfigSpec.IntValue WHEELCHAIR_JUMP_ENERGY_COST;
    public static ModConfigSpec.IntValue WHEELCHAIR_MOVE_ENERGY_COST;

    public static ModConfigSpec.IntValue TOM_6_ENERGY_COST;
    public static ModConfigSpec.IntValue TOM_6_BOMB_EXPLOSION_DAMAGE;
    public static ModConfigSpec.DoubleValue TOM_6_BOMB_EXPLOSION_RADIUS;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("vehicle");

        builder.push("collision");

        builder.comment("Allows vehicles to destroy soft blocks via collision");
        COLLISION_DESTROY_SOFT_BLOCKS = builder.define("collision_destroy_soft_blocks", false);

        builder.comment("Allows vehicles to destroy normal blocks via collision");
        COLLISION_DESTROY_NORMAL_BLOCKS = builder.define("collision_destroy_normal_blocks", false);

        builder.comment("Allows vehicles to destroy hard blocks via collision");
        COLLISION_DESTROY_HARD_BLOCKS = builder.define("collision_destroy_hard_blocks", false);

        builder.comment("Allows vehicles to destroy blocks via collision like a beast");
        COLLISION_DESTROY_BLOCKS_BEASTLY = builder.define("collision_destroy_blocks_beastly", false);

        builder.comment("List of entities that can be damaged by collision");
        COLLISION_ENTITY_WHITELIST = builder.defineList("collision_entity_whitelist",
                DEFAULT_COLLISION_ENTITY_WHITELIST,
                () -> "",
                e -> e instanceof String);

        builder.pop();

        builder.comment("Allow vehicles to pick up items");
        VEHICLE_ITEM_PICKUP = builder.define("vehicle_item_pickup", true);

        builder.comment("Allow vehicles to collect drops after killing other entities by crashing");
        COLLECT_DROPS_BY_CRASHING = builder.define("collect_drops_by_crashing", true);

        builder.comment("Within this distance, the vehicle info will be displayed at client side");
        VEHICLE_INFO_DISPLAY_DISTANCE = builder.defineInRange("vehicle_info_display_distance", 512, 0, 1024);

        builder.comment("The damage of self explosion when a vehicle is destroyed");
        SELF_EXPLOSION_DAMAGE = builder.defineInRange("self_explosion_damage", 114514, 0, Integer.MAX_VALUE);

        builder.comment("The damage count of self explosion when a vehicle is destroyed");
        SELF_EXPLOSION_COUNT = builder.defineInRange("self_explosion_count", 5, 0, 100);

        builder.comment("The air crash damage when an aircraft is destroyed");
        AIR_CRASH_EXPLOSION_DAMAGE = builder.defineInRange("air_crash_explosion_damage", 114514, 0, Integer.MAX_VALUE);

        builder.comment("The air crash damage count when an aircraft is destroyed");
        AIR_CRASH_EXPLOSION_COUNT = builder.defineInRange("air_crash_explosion_count", 5, 0, 100);

        builder.push("repair");

        builder.comment("The default cooldown of vehicle repair. Set a negative value to disable vehicle repair");
        REPAIR_COOLDOWN = builder.defineInRange("repair_cooldown", 200, -1, 100000000);

        builder.comment("The default amount of health restored per tick when a vehicle is self-repairing");
        REPAIR_AMOUNT = builder.defineInRange("repair_amount", 0.05d, -100000000, 100000000);

        builder.pop();

        builder.push("Annihilator");

        builder.comment("The energy cost of Annihilator per shoot");
        ANNIHILATOR_SHOOT_COST = builder.defineInRange("annihilator_shoot_cost", 2000000, 0, 2147483647);

        builder.pop();

        builder.push("Wheelchair");

        builder.comment("The jump energy cost of the wheelchair");
        WHEELCHAIR_JUMP_ENERGY_COST = builder.defineInRange("wheelchair_jump_energy_cost", 400, 0, 2147483647);

        builder.comment("The move energy cost of the wheelchair");
        WHEELCHAIR_MOVE_ENERGY_COST = builder.defineInRange("wheelchair_move_energy_cost", 1, 0, 2147483647);

        builder.pop();

        builder.push("Tom-6");

        builder.comment("The energy cost of Tom-6 per tick");
        TOM_6_ENERGY_COST = builder.defineInRange("tom_6_energy_cost", 16, 0, 2147483647);

        builder.comment("The Melon Bomb explosion damage of Tom-6");
        TOM_6_BOMB_EXPLOSION_DAMAGE = builder.defineInRange("tom_6_bomb_explosion_damage", 500, 1, 10000000);

        builder.comment("The Melon Bomb explosion radius of Tom-6");
        TOM_6_BOMB_EXPLOSION_RADIUS = builder.defineInRange("tom_6_bomb_explosion_radius", 10d, 1d, 10000000d);

        builder.pop();

        builder.pop();
    }
}
