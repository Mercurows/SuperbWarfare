package com.atsuishio.superbwarfare.config.server;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class VehicleConfig {

    public static ModConfigSpec.BooleanValue COLLISION_DESTROY_BLOCKS;
    public static ModConfigSpec.BooleanValue COLLISION_DESTROY_HARD_BLOCKS;
    public static ModConfigSpec.BooleanValue COLLISION_DESTROY_BLOCKS_BEASTLY;
    public static ModConfigSpec.BooleanValue VEHICLE_ITEM_PICKUP;

    public static ModConfigSpec.ConfigValue<List<? extends String>> COLLISION_ENTITY_WHITELIST;

    public static final List<? extends String> DEFAULT_COLLISION_ENTITY_WHITELIST = List.of();

    public static ModConfigSpec.IntValue REPAIR_COOLDOWN;
    public static ModConfigSpec.DoubleValue REPAIR_AMOUNT;

    public static ModConfigSpec.IntValue MK42_HP;
    public static ModConfigSpec.IntValue MK42_AP_DAMAGE;
    public static ModConfigSpec.IntValue MK42_AP_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue MK42_AP_EXPLOSION_RADIUS;
    public static ModConfigSpec.IntValue MK42_HE_DAMAGE;
    public static ModConfigSpec.IntValue MK42_HE_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue MK42_HE_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue MLE1934_HP;
    public static ModConfigSpec.IntValue MLE1934_AP_DAMAGE;
    public static ModConfigSpec.IntValue MLE1934_AP_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue MLE1934_AP_EXPLOSION_RADIUS;
    public static ModConfigSpec.IntValue MLE1934_HE_DAMAGE;
    public static ModConfigSpec.IntValue MLE1934_HE_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue MLE1934_HE_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue HEAVY_MACHINE_GUN_DAMAGE;

    public static ModConfigSpec.IntValue ANNIHILATOR_HP;
    public static ModConfigSpec.IntValue ANNIHILATOR_SHOOT_COST;
    public static ModConfigSpec.IntValue ANNIHILATOR_MAX_ENERGY;

    public static ModConfigSpec.IntValue LASER_TOWER_HP;
    public static ModConfigSpec.IntValue LASER_TOWER_COOLDOWN;
    public static ModConfigSpec.IntValue LASER_TOWER_DAMAGE;
    public static ModConfigSpec.IntValue LASER_TOWER_SHOOT_COST;
    public static ModConfigSpec.IntValue LASER_TOWER_MAX_ENERGY;

    public static ModConfigSpec.IntValue SPEEDBOAT_HP;
    public static ModConfigSpec.IntValue SPEEDBOAT_ENERGY_COST;
    public static ModConfigSpec.IntValue SPEEDBOAT_MAX_ENERGY;

    public static ModConfigSpec.IntValue WHEELCHAIR_HP;
    public static ModConfigSpec.IntValue WHEELCHAIR_JUMP_ENERGY_COST;
    public static ModConfigSpec.IntValue WHEELCHAIR_MOVE_ENERGY_COST;
    public static ModConfigSpec.IntValue WHEELCHAIR_MAX_ENERGY;

    public static ModConfigSpec.IntValue AH_6_HP;
    public static ModConfigSpec.IntValue AH_6_MIN_ENERGY_COST;
    public static ModConfigSpec.IntValue AH_6_MAX_ENERGY_COST;
    public static ModConfigSpec.IntValue AH_6_MAX_ENERGY;
    public static ModConfigSpec.IntValue AH_6_CANNON_DAMAGE;
    public static ModConfigSpec.IntValue AH_6_ROCKET_DAMAGE;
    public static ModConfigSpec.IntValue AH_6_ROCKET_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue AH_6_ROCKET_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue LAV_150_HP;
    public static ModConfigSpec.IntValue LAV_150_ENERGY_COST;
    public static ModConfigSpec.IntValue LAV_150_MAX_ENERGY;
    public static ModConfigSpec.IntValue LAV_150_CANNON_DAMAGE;
    public static ModConfigSpec.IntValue LAV_150_CANNON_EXPLOSION_DAMAGE;
    public static ModConfigSpec.DoubleValue LAV_150_CANNON_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue TOM_6_HP;
    public static ModConfigSpec.IntValue TOM_6_MAX_ENERGY;
    public static ModConfigSpec.IntValue TOM_6_ENERGY_COST;
    public static ModConfigSpec.IntValue TOM_6_BOMB_EXPLOSION_DAMAGE;
    public static ModConfigSpec.DoubleValue TOM_6_BOMB_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue BMP_2_HP;
    public static ModConfigSpec.IntValue BMP_2_ENERGY_COST;
    public static ModConfigSpec.IntValue BMP_2_MAX_ENERGY;
    public static ModConfigSpec.IntValue BMP_2_CANNON_DAMAGE;
    public static ModConfigSpec.IntValue BMP_2_CANNON_EXPLOSION_DAMAGE;
    public static ModConfigSpec.DoubleValue BMP_2_CANNON_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue YX_100_HP;
    public static ModConfigSpec.IntValue YX_100_SHOOT_COST;
    public static ModConfigSpec.IntValue YX_100_ENERGY_COST;
    public static ModConfigSpec.IntValue YX_100_MAX_ENERGY;
    public static ModConfigSpec.IntValue YX_100_AP_CANNON_DAMAGE;
    public static ModConfigSpec.IntValue YX_100_AP_CANNON_EXPLOSION_DAMAGE;
    public static ModConfigSpec.DoubleValue YX_100_AP_CANNON_EXPLOSION_RADIUS;
    public static ModConfigSpec.IntValue YX_100_HE_CANNON_DAMAGE;
    public static ModConfigSpec.IntValue YX_100_HE_CANNON_EXPLOSION_DAMAGE;
    public static ModConfigSpec.DoubleValue YX_100_HE_CANNON_EXPLOSION_RADIUS;
    public static ModConfigSpec.IntValue YX_100_SWARM_EXPLOSION_DAMAGE;
    public static ModConfigSpec.DoubleValue YX_100_SWARM_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue PRISM_TANK_HP;
    public static ModConfigSpec.IntValue PRISM_TANK_MAX_ENERGY;
    public static ModConfigSpec.IntValue PRISM_TANK_ENERGY_COST;
    public static ModConfigSpec.IntValue PRISM_TANK_DAMAGE_MODE_1;
    public static ModConfigSpec.IntValue PRISM_TANK_SHOOT_COST_MODE_1;
    public static ModConfigSpec.IntValue PRISM_TANK_AOE_DAMAGE;
    public static ModConfigSpec.IntValue PRISM_TANK_AOE_RADIUS;
    public static ModConfigSpec.IntValue PRISM_TANK_DAMAGE_MODE_2;
    public static ModConfigSpec.IntValue PRISM_TANK_SHOOT_COST_MODE_2;

    public static ModConfigSpec.IntValue HPJ11_HP;
    public static ModConfigSpec.DoubleValue HPJ11_DAMAGE;
    public static ModConfigSpec.DoubleValue HPJ11_EXPLOSION_DAMAGE;
    public static ModConfigSpec.DoubleValue HPJ11_EXPLOSION_RADIUS;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("vehicle");

        builder.comment("Allows vehicles to destroy blocks via collision");
        COLLISION_DESTROY_BLOCKS = builder.define("collision_destroy_blocks", false);

        builder.comment("Allows vehicles to destroy hard blocks via collision");
        COLLISION_DESTROY_HARD_BLOCKS = builder.define("collision_destroy_hard_blocks", false);

        builder.comment("Allows vehicles to destroy blocks via collision like a beast");
        COLLISION_DESTROY_BLOCKS_BEASTLY = builder.define("collision_destroy_blocks_beastly", false);

        builder.comment("Allow vehicles to pick up items");
        VEHICLE_ITEM_PICKUP = builder.define("vehicle_item_pickup", true);

        builder.comment("List of entities that can be damaged by collision");
        COLLISION_ENTITY_WHITELIST = builder.defineList("collision_entity_whitelist",
                DEFAULT_COLLISION_ENTITY_WHITELIST,
                e -> e instanceof String);

        builder.push("repair");

        builder.comment("The cooldown of vehicle repair. Set a negative value to disable vehicle repair");
        REPAIR_COOLDOWN = builder.defineInRange("repair_cooldown", 200, -1, 10000000);

        builder.comment("The amount of health restored per tick when a vehicle is self-repairing");
        REPAIR_AMOUNT = builder.defineInRange("repair_amount", 0.05d, 0, 10000000);

        builder.pop();

        builder.push("mk_42");

        builder.comment("The health of MK-42");
        MK42_HP = builder.defineInRange("mk_42_hp", 350, 1, 10000000);

        builder.comment("The AP shell damage of MK-42");
        MK42_AP_DAMAGE = builder.defineInRange("mk_42_ap_damage", 450, 1, 10000000);

        builder.comment("The AP shell explosion damage of MK-42");
        MK42_AP_EXPLOSION_DAMAGE = builder.defineInRange("mk_42_ap_explosion_damage", 120, 1, 10000000);

        builder.comment("The AP shell explosion radius of MK-42");
        MK42_AP_EXPLOSION_RADIUS = builder.defineInRange("mk_42_ap_explosion_radius", 3, 1, 50);

        builder.comment("The HE shell damage of MK-42");
        MK42_HE_DAMAGE = builder.defineInRange("mk_42_he_damage", 150, 1, 10000000);

        builder.comment("The HE shell explosion damage of MK-42");
        MK42_HE_EXPLOSION_DAMAGE = builder.defineInRange("mk_42_he_explosion_damage", 200, 1, 10000000);

        builder.comment("The HE shell explosion radius of MK-42");
        MK42_HE_EXPLOSION_RADIUS = builder.defineInRange("mk_42_he_explosion_radius", 10, 1, 50);

        builder.pop();

        builder.push("mle_1934");

        builder.comment("The health of MLE-1934");
        MLE1934_HP = builder.defineInRange("mle_1934_hp", 350, 1, 10000000);

        builder.comment("The AP shell damage of MLE-1934");
        MLE1934_AP_DAMAGE = builder.defineInRange("mle_1934_ap_damage", 500, 1, 10000000);

        builder.comment("The AP shell explosion damage of MLE-1934");
        MLE1934_AP_EXPLOSION_DAMAGE = builder.defineInRange("mle_1934_ap_explosion_damage", 150, 1, 10000000);

        builder.comment("The AP shell explosion radius of MLE-1934");
        MLE1934_AP_EXPLOSION_RADIUS = builder.defineInRange("mle_1934_ap_explosion_radius", 4, 1, 50);

        builder.comment("The HE shell damage of MLE-1934");
        MLE1934_HE_DAMAGE = builder.defineInRange("mle_1934_he_damage", 180, 1, 10000000);

        builder.comment("The HE shell explosion damage of MLE-1934");
        MLE1934_HE_EXPLOSION_DAMAGE = builder.defineInRange("mle_1934_he_explosion_damage", 240, 1, 10000000);

        builder.comment("The HE shell explosion radius of MLE-1934");
        MLE1934_HE_EXPLOSION_RADIUS = builder.defineInRange("mle_1934_he_explosion_radius", 12, 1, 50);

        builder.pop();

        builder.push("HeavyMachineGun");

        builder.comment("The gun damage of 12.7mm HMG");
        HEAVY_MACHINE_GUN_DAMAGE = builder.defineInRange("heavy_machine_gun_damage", 25, 1, 10000000);

        builder.pop();

        builder.push("annihilator");

        builder.comment("The health of Annihilator");
        ANNIHILATOR_HP = builder.defineInRange("annihilator_hp", 1200, 1, 10000000);

        builder.comment("The energy cost of Annihilator per shoot");
        ANNIHILATOR_SHOOT_COST = builder.defineInRange("annihilator_shoot_cost", 2000000, 0, 2147483647);

        builder.comment("The max energy storage of Annihilator");
        ANNIHILATOR_MAX_ENERGY = builder.defineInRange("annihilator_max_energy", 20000000, 0, 2147483647);

        builder.pop();

        builder.push("laser_tower");

        builder.comment("The health of Laser Tower");
        LASER_TOWER_HP = builder.defineInRange("laser_tower_hp", 100, 1, 10000000);

        builder.comment("The damage of Laser Tower");
        LASER_TOWER_DAMAGE = builder.defineInRange("laser_tower_damage", 15, 1, 10000000);

        builder.comment("The cooldown time(ticks) of Laser Tower");
        LASER_TOWER_COOLDOWN = builder.defineInRange("laser_tower_cooldown", 40, 15, 10000000);

        builder.comment("The energy cost of Laser Tower per shoot");
        LASER_TOWER_SHOOT_COST = builder.defineInRange("laser_tower_shoot_cost", 5000, 0, 2147483647);

        builder.comment("The max energy storage of Laser Tower");
        LASER_TOWER_MAX_ENERGY = builder.defineInRange("laser_tower_max_energy", 500000, 0, 2147483647);

        builder.pop();

        builder.push("speedboat");

        builder.comment("The health of Speedboat");
        SPEEDBOAT_HP = builder.defineInRange("speedboat_hp", 200, 1, 10000000);

        builder.comment("The energy cost of Speedboat per tick");
        SPEEDBOAT_ENERGY_COST = builder.defineInRange("speedboat_energy_cost", 16, 0, 2147483647);

        builder.comment("The max energy storage of Speedboat");
        SPEEDBOAT_MAX_ENERGY = builder.defineInRange("speedboat_max_energy", 500000, 0, 2147483647);

        builder.pop();

        builder.push("wheelchair");

        builder.comment("The health of the wheelchair");
        WHEELCHAIR_HP = builder.defineInRange("wheelchair_hp", 30, 1, 10000000);

        builder.comment("The jump energy cost of the wheelchair");
        WHEELCHAIR_JUMP_ENERGY_COST = builder.defineInRange("wheelchair_jump_energy_cost", 400, 0, 2147483647);

        builder.comment("The move energy cost of the wheelchair");
        WHEELCHAIR_MOVE_ENERGY_COST = builder.defineInRange("wheelchair_move_energy_cost", 1, 0, 2147483647);

        builder.comment("The max energy storage of the wheelchair");
        WHEELCHAIR_MAX_ENERGY = builder.defineInRange("wheelchair_max_energy", 24000, 0, 2147483647);

        builder.pop();

        builder.push("ah_6");

        builder.comment("The health of AH-6");
        AH_6_HP = builder.defineInRange("ah_6_hp", 250, 1, 10000000);

        builder.comment("The min energy cost of AH-6 per tick");
        AH_6_MIN_ENERGY_COST = builder.defineInRange("ah_6_min_energy_cost", 64, 0, 2147483647);

        builder.comment("The max energy cost of AH-6 per tick");
        AH_6_MAX_ENERGY_COST = builder.defineInRange("ah_6_max_energy_cost", 128, 0, 2147483647);

        builder.comment("The max energy storage of AH-6");
        AH_6_MAX_ENERGY = builder.defineInRange("ah_6_max_energy", 5000000, 0, 2147483647);

        builder.comment("The cannon damage of AH-6");
        AH_6_CANNON_DAMAGE = builder.defineInRange("ah_6_cannon_damage", 20, 1, 10000000);

        builder.comment("The rocket damage of AH-6");
        AH_6_ROCKET_DAMAGE = builder.defineInRange("ah_6_rocket_damage", 80, 1, 10000000);

        builder.comment("The rocket explosion damage of AH-6");
        AH_6_ROCKET_EXPLOSION_DAMAGE = builder.defineInRange("ah_6_rocket_explosion_damage", 40, 1, 10000000);

        builder.comment("The rocket explosion radius of AH-6");
        AH_6_ROCKET_EXPLOSION_RADIUS = builder.defineInRange("ah_6_rocket_explosion_radius", 5, 1, 10000000);

        builder.pop();

        builder.push("lav_150");

        builder.comment("The health of Lav_150");
        LAV_150_HP = builder.defineInRange("lav_150_hp", 250, 1, 10000000);

        builder.comment("The energy cost of Lav_150 per tick");
        LAV_150_ENERGY_COST = builder.defineInRange("lav_150_energy_cost", 64, 0, 2147483647);

        builder.comment("The max energy storage of Lav_150");
        LAV_150_MAX_ENERGY = builder.defineInRange("lav_150_max_energy", 5000000, 0, 2147483647);

        builder.comment("The cannon damage of Lav_150");
        LAV_150_CANNON_DAMAGE = builder.defineInRange("lav_150_cannon_damage", 45, 1, 10000000);

        builder.comment("The rocket explosion damage of Lav_150");
        LAV_150_CANNON_EXPLOSION_DAMAGE = builder.defineInRange("lav_150_cannon_explosion_damage", 12, 1, 10000000);

        builder.comment("The rocket explosion radius of Lav_150");
        LAV_150_CANNON_EXPLOSION_RADIUS = builder.defineInRange("lav_150_cannon_explosion_radius", 3d, 1d, 10000000d);

        builder.pop();

        builder.push("tom_6");

        builder.comment("The health of Tom_6");
        TOM_6_HP = builder.defineInRange("tom_6_hp", 40, 1, 10000000);

        builder.comment("The energy cost of Tom_6 per tick");
        TOM_6_ENERGY_COST = builder.defineInRange("tom_6_energy_cost", 16, 0, 2147483647);

        builder.comment("The max energy storage of Tom_6");
        TOM_6_MAX_ENERGY = builder.defineInRange("tom_6_max_energy", 100000, 0, 2147483647);

        builder.comment("The Melon Bomb explosion damage of Tom_6");
        TOM_6_BOMB_EXPLOSION_DAMAGE = builder.defineInRange("tom_6_bomb_explosion_damage", 500, 1, 10000000);

        builder.comment("The Melon Bomb explosion radius of Tom_6");
        TOM_6_BOMB_EXPLOSION_RADIUS = builder.defineInRange("tom_6_bomb_explosion_radius", 10d, 1d, 10000000d);

        builder.pop();

        builder.push("bmp_2");

        builder.comment("The health of Bmp_2");
        BMP_2_HP = builder.defineInRange("bmp_2_hp", 300, 1, 10000000);

        builder.comment("The energy cost of Bmp_2 per tick");
        BMP_2_ENERGY_COST = builder.defineInRange("bmp_2_energy_cost", 64, 0, 2147483647);

        builder.comment("The max energy storage of Bmp_2");
        BMP_2_MAX_ENERGY = builder.defineInRange("bmp_2_max_energy", 5000000, 0, 2147483647);

        builder.comment("The cannon damage of Bmp_2");
        BMP_2_CANNON_DAMAGE = builder.defineInRange("bmp_2_cannon_damage", 55, 1, 10000000);

        builder.comment("The cannon explosion damage of Bmp_2");
        BMP_2_CANNON_EXPLOSION_DAMAGE = builder.defineInRange("bmp_2_cannon_explosion_damage", 15, 1, 10000000);

        builder.comment("The cannon explosion radius of Bmp_2");
        BMP_2_CANNON_EXPLOSION_RADIUS = builder.defineInRange("bmp_2_cannon_explosion_radius", 3.2d, 1d, 10000000d);

        builder.pop();

        builder.push("yx_100");

        builder.comment("The health of Yx_100");
        YX_100_HP = builder.defineInRange("yx_100_hp", 500, 1, 10000000);

        builder.comment("The energy cost of Yx_100 per tick");
        YX_100_ENERGY_COST = builder.defineInRange("yx_100_energy_cost", 128, 0, 2147483647);

        builder.comment("The energy cost of Yx_100 per shoot");
        YX_100_SHOOT_COST = builder.defineInRange("yx_100_shoot_cost", 24000, 0, 2147483647);

        builder.comment("The max energy storage of Yx_100");
        YX_100_MAX_ENERGY = builder.defineInRange("yx_100_max_energy", 20000000, 0, 2147483647);

        builder.comment("The cannon damage of Yx_100");
        YX_100_AP_CANNON_DAMAGE = builder.defineInRange("yx_100_ap_cannon_damage", 500, 1, 10000000);

        builder.comment("The cannon explosion damage of Yx_100");
        YX_100_AP_CANNON_EXPLOSION_DAMAGE = builder.defineInRange("yx_100_ap_cannon_explosion_damage", 100, 1, 10000000);

        builder.comment("The cannon explosion radius of Yx_100");
        YX_100_AP_CANNON_EXPLOSION_RADIUS = builder.defineInRange("yx_100_ap_cannon_explosion_radius", 4d, 1d, 10000000d);

        builder.comment("The cannon damage of Yx_100");
        YX_100_HE_CANNON_DAMAGE = builder.defineInRange("yx_100_he_cannon_damage", 150, 1, 10000000);

        builder.comment("The cannon explosion damage of Yx_100");
        YX_100_HE_CANNON_EXPLOSION_DAMAGE = builder.defineInRange("yx_100_he_cannon_explosion_damage", 150, 1, 10000000);

        builder.comment("The cannon explosion radius of Yx_100");
        YX_100_HE_CANNON_EXPLOSION_RADIUS = builder.defineInRange("yx_100_he_cannon_explosion_radius", 10d, 1d, 10000000d);

        builder.comment("The swarm drone explosion damage of Yx_100");
        YX_100_SWARM_EXPLOSION_DAMAGE = builder.defineInRange("yx_100_swarm_drone_explosion_damage", 80, 1, 10000000);

        builder.comment("The swarm drone explosion radius of Yx_100");
        YX_100_SWARM_EXPLOSION_RADIUS = builder.defineInRange("yx_100_swarm_drone_explosion_radius", 5d, 1d, 10000000d);

        builder.pop();

        builder.push("prism_tank");

        builder.comment("The health of PrismTank");
        PRISM_TANK_HP = builder.defineInRange("prism_tank_hp", 400, 1, 10000000);

        builder.comment("The max energy storage of PrismTank");
        PRISM_TANK_MAX_ENERGY = builder.defineInRange("prism_tank_energy", 20000000, 0, 2147483647);

        builder.comment("The energy cost of PrismTank per tick");
        PRISM_TANK_ENERGY_COST = builder.defineInRange("prism_tank_energy_cost", 96, 0, 2147483647);

        builder.comment("The Laser Damage of PrismTank Mode 1");
        PRISM_TANK_DAMAGE_MODE_1 = builder.defineInRange("prism_tank_damage_mode_1", 350, 0, 2147483647);

        builder.comment("The energy cost of PrismTank Mode 1");
        PRISM_TANK_SHOOT_COST_MODE_1 = builder.defineInRange("prism_tank_shoot_cost_mode_1", 100000, 1, 10000000);

        builder.comment("The laser AOE damage of PrismTank");
        PRISM_TANK_AOE_DAMAGE = builder.defineInRange("prism_tank_aoe_damage", 72, 1, 10000000);

        builder.comment("The laser AOE radius of PrismTank");
        PRISM_TANK_AOE_RADIUS = builder.defineInRange("prism_tank_aoe_radius", 12, 1, 10000000);

        builder.comment("The Laser Damage of PrismTank Mode 2 per tick");
        PRISM_TANK_DAMAGE_MODE_2 = builder.defineInRange("prism_tank_damage_mode_2", 15, 1, 10000000);

        builder.comment("The energy cost of PrismTank Mode 2 per tick");
        PRISM_TANK_SHOOT_COST_MODE_2 = builder.defineInRange("prism_tank_shoot_cost_mode_2", 5000, 1, 10000000);

        builder.pop();

        builder.push("hpj_11");

        builder.comment("The health of HPJ_11");
        HPJ11_HP = builder.defineInRange("hpj_11_hp", 350, 1, 10000000);

        builder.comment("The damage of HPJ_11");
        HPJ11_DAMAGE = builder.defineInRange("hpj_11_damage", 20d, 1, 10000000);

        builder.comment("The explosion damage of HPJ_11");
        HPJ11_EXPLOSION_DAMAGE = builder.defineInRange("hpj_11_explosion_damage", 7d, 1, 10000000);

        builder.comment("The explosion radius of HPJ_11");
        HPJ11_EXPLOSION_RADIUS = builder.defineInRange("hpj_11_explosion_radius", 4d, 1, 50);

        builder.pop();

        builder.pop();
    }
}
