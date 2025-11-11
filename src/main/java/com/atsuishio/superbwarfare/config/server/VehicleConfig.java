package com.atsuishio.superbwarfare.config.server;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class VehicleConfig {

    public static ForgeConfigSpec.BooleanValue COLLISION_DESTROY_SOFT_BLOCKS;
    public static ForgeConfigSpec.BooleanValue COLLISION_DESTROY_NORMAL_BLOCKS;
    public static ForgeConfigSpec.BooleanValue COLLISION_DESTROY_HARD_BLOCKS;
    public static ForgeConfigSpec.BooleanValue COLLISION_DESTROY_BLOCKS_BEASTLY;
    public static ForgeConfigSpec.BooleanValue VEHICLE_ITEM_PICKUP;
    public static ForgeConfigSpec.BooleanValue COLLECT_DROPS_BY_CRASHING;

    public static ForgeConfigSpec.ConfigValue<List<? extends String>> COLLISION_ENTITY_WHITELIST;

    public static final List<? extends String> DEFAULT_COLLISION_ENTITY_WHITELIST = List.of();

    public static ForgeConfigSpec.IntValue REPAIR_COOLDOWN;
    public static ForgeConfigSpec.DoubleValue REPAIR_AMOUNT;

    public static ForgeConfigSpec.IntValue SELF_EXPLOSION_DAMAGE;
    public static ForgeConfigSpec.IntValue SELF_EXPLOSION_COUNT;
    public static ForgeConfigSpec.IntValue AIR_CRASH_EXPLOSION_DAMAGE;
    public static ForgeConfigSpec.IntValue AIR_CRASH_EXPLOSION_COUNT;

    public static ForgeConfigSpec.IntValue VEHICLE_INFO_DISPLAY_DISTANCE;

    public static ForgeConfigSpec.IntValue ANNIHILATOR_SHOOT_COST;

    public static ForgeConfigSpec.IntValue LASER_TOWER_COOLDOWN;
    public static ForgeConfigSpec.IntValue LASER_TOWER_DAMAGE;
    public static ForgeConfigSpec.IntValue LASER_TOWER_SHOOT_COST;



    public static ForgeConfigSpec.IntValue WHEELCHAIR_JUMP_ENERGY_COST;
    public static ForgeConfigSpec.IntValue WHEELCHAIR_MOVE_ENERGY_COST;

    public static ForgeConfigSpec.IntValue TOM_6_ENERGY_COST;
    public static ForgeConfigSpec.IntValue TOM_6_BOMB_EXPLOSION_DAMAGE;
    public static ForgeConfigSpec.DoubleValue TOM_6_BOMB_EXPLOSION_RADIUS;


    public static ForgeConfigSpec.DoubleValue HPJ11_DAMAGE;
    public static ForgeConfigSpec.DoubleValue HPJ11_EXPLOSION_DAMAGE;
    public static ForgeConfigSpec.DoubleValue HPJ11_EXPLOSION_RADIUS;
    public static ForgeConfigSpec.IntValue HPJ11_SHOOT_COST;
    public static ForgeConfigSpec.IntValue HPJ11_SEEK_COST;

    public static ForgeConfigSpec.IntValue A_10_MAX_ENERGY_COST;
    public static ForgeConfigSpec.IntValue A_10_CANNON_DAMAGE;
    public static ForgeConfigSpec.IntValue A_10_CANNON_EXPLOSION_DAMAGE;
    public static ForgeConfigSpec.DoubleValue A_10_CANNON_EXPLOSION_RADIUS;
    public static ForgeConfigSpec.IntValue A_10_ROCKET_DAMAGE;
    public static ForgeConfigSpec.IntValue A_10_ROCKET_EXPLOSION_DAMAGE;
    public static ForgeConfigSpec.DoubleValue A_10_ROCKET_EXPLOSION_RADIUS;

    public static void init(ForgeConfigSpec.Builder builder) {
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

        builder.push("Laser Tower");

        builder.comment("The damage of Laser Tower");
        LASER_TOWER_DAMAGE = builder.defineInRange("laser_tower_damage", 15, 1, 10000000);

        builder.comment("The cooldown time(ticks) of Laser Tower");
        LASER_TOWER_COOLDOWN = builder.defineInRange("laser_tower_cooldown", 40, 15, 10000000);

        builder.comment("The energy cost of Laser Tower per shoot");
        LASER_TOWER_SHOOT_COST = builder.defineInRange("laser_tower_shoot_cost", 5000, 0, 2147483647);

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

        builder.push("HPJ-11");

        builder.comment("The damage of HPJ-11");
        HPJ11_DAMAGE = builder.defineInRange("hpj_11_damage", 20d, 1, 10000000);

        builder.comment("The explosion damage of HPJ-11");
        HPJ11_EXPLOSION_DAMAGE = builder.defineInRange("hpj_11_explosion_damage", 7d, 1, 10000000);

        builder.comment("The explosion radius of HPJ-11");
        HPJ11_EXPLOSION_RADIUS = builder.defineInRange("hpj_11_explosion_radius", 4d, 1, 50);

        builder.comment("The energy cost of HPJ-11 per shoot");
        HPJ11_SHOOT_COST = builder.defineInRange("hpj_11_shoot_cost", 64, 0, 2147483647);

        builder.comment("The energy cost of HPJ-11 find a new target");
        HPJ11_SEEK_COST = builder.defineInRange("hpj_11_seek_cost", 1024, 0, 2147483647);

        builder.pop();

        builder.push("A-10");

        builder.comment("The max energy cost of A-10 per tick");
        A_10_MAX_ENERGY_COST = builder.defineInRange("A_10_max_energy_cost", 256, 0, 2147483647);

        builder.comment("The cannon damage of A-10");
        A_10_CANNON_DAMAGE = builder.defineInRange("A_10_cannon_damage", 30, 1, 10000000);

        builder.comment("The cannon explosion damage of A-10");
        A_10_CANNON_EXPLOSION_DAMAGE = builder.defineInRange("A_10_cannon_explosion_damage", 10, 1, 10000000);

        builder.comment("The cannon explosion radius of A-10");
        A_10_CANNON_EXPLOSION_RADIUS = builder.defineInRange("A_10_cannon_explosion_radius", 4d, 1, 10000000);

        builder.comment("The rocket damage of A-10");
        A_10_ROCKET_DAMAGE = builder.defineInRange("A_10_rocket_damage", 90, 1, 10000000);

        builder.comment("The rocket explosion damage of A-10");
        A_10_ROCKET_EXPLOSION_DAMAGE = builder.defineInRange("A_10_rocket_explosion_damage", 50, 1, 10000000);

        builder.comment("The rocket explosion radius of A-10");
        A_10_ROCKET_EXPLOSION_RADIUS = builder.defineInRange("A_10_rocket_explosion_radius", 6d, 1, 10000000);

        builder.pop();

        builder.pop();
    }
}
