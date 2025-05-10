package com.atsuishio.superbwarfare.config.server;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ExplosionConfig {

    public static ModConfigSpec.IntValue EXPLOSION_PENETRATION_RATIO;
    public static ModConfigSpec.BooleanValue EXPLOSION_DESTROY;

    public static ModConfigSpec.IntValue RGO_GRENADE_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue RGO_GRENADE_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue M67_GRENADE_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue M67_GRENADE_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue MORTAR_SHELL_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue MORTAR_SHELL_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue DRONE_KAMIKAZE_HIT_DAMAGE;
    public static ModConfigSpec.IntValue DRONE_KAMIKAZE_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue DRONE_KAMIKAZE_EXPLOSION_RADIUS;
    public static ModConfigSpec.IntValue DRONE_KAMIKAZE_HIT_DAMAGE_C4;
    public static ModConfigSpec.IntValue DRONE_KAMIKAZE_HIT_DAMAGE_RPG;

    public static ModConfigSpec.IntValue C4_EXPLOSION_COUNTDOWN;
    public static ModConfigSpec.IntValue C4_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue C4_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue RPG_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue RPG_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue WIRE_GUIDE_MISSILE_DAMAGE;
    public static ModConfigSpec.IntValue WIRE_GUIDE_MISSILE_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue WIRE_GUIDE_MISSILE_EXPLOSION_RADIUS;
    
    public static ModConfigSpec.IntValue AGM_65_DAMAGE;
    public static ModConfigSpec.IntValue AGM_65_EXPLOSION_DAMAGE;
    public static ModConfigSpec.DoubleValue AGM_65_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue MK_82_EXPLOSION_DAMAGE;
    public static ModConfigSpec.DoubleValue MK_82_EXPLOSION_RADIUS;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("explosion");

        builder.comment("The percentage of explosion damage you take behind cover");
        EXPLOSION_PENETRATION_RATIO = builder.defineInRange("explosion_penetration_ratio", 15, 0, 100);

        builder.comment("Set true to allow Explosion to destroy blocks");
        EXPLOSION_DESTROY = builder.define("explosion_destroy", true);

        builder.push("RGO Grenade");

        builder.comment("The explosion damage of RGO grenade");
        RGO_GRENADE_EXPLOSION_DAMAGE = builder.defineInRange("rgo_grenade_explosion_damage", 90, 1, 10000000);

        builder.comment("The explosion radius of RGO grenade");
        RGO_GRENADE_EXPLOSION_RADIUS = builder.defineInRange("rgo_grenade_explosion_radius", 5, 1, 50);

        builder.pop();


        builder.push("M67 Grenade");

        builder.comment("The explosion damage of M67 grenade");
        M67_GRENADE_EXPLOSION_DAMAGE = builder.defineInRange("m67_grenade_explosion_damage", 120, 1, 10000000);

        builder.comment("The explosion radius of M67 grenade");
        M67_GRENADE_EXPLOSION_RADIUS = builder.defineInRange("m67_grenade_explosion_radius", 6, 1, 50);

        builder.pop();


        builder.push("Mortar Shell");

        builder.comment("The explosion damage of Mortar shell");
        MORTAR_SHELL_EXPLOSION_DAMAGE = builder.defineInRange("mortar_shell_explosion_damage", 160, 1, 10000000);

        builder.comment("The explosion radius of Mortar shell");
        MORTAR_SHELL_EXPLOSION_RADIUS = builder.defineInRange("mortar_shell_explosion_radius", 9, 1, 50);

        builder.pop();

        builder.push("Drone Kamikaze");

        builder.comment("The hit damage of Drone Kamikaze");
        DRONE_KAMIKAZE_HIT_DAMAGE = builder.defineInRange("drone_kamikaze_hit_damage", 200, 1, 10000000);

        builder.comment("The hit damage of Drone Kamikaze with C4");
        DRONE_KAMIKAZE_HIT_DAMAGE_C4 = builder.defineInRange("drone_kamikaze_hit_damage_c4", 150, 1, 10000000);

        builder.comment("The hit damage of Drone Kamikaze with RPG");
        DRONE_KAMIKAZE_HIT_DAMAGE_RPG = builder.defineInRange("drone_kamikaze_hit_damage_rpg", 270, 1, 10000000);

        builder.comment("The explosion damage of Drone Kamikaze");
        DRONE_KAMIKAZE_EXPLOSION_DAMAGE = builder.defineInRange("drone_kamikaze_explosion_damage", 160, 1, 10000000);

        builder.comment("The explosion radius of Drone Kamikaze");
        DRONE_KAMIKAZE_EXPLOSION_RADIUS = builder.defineInRange("drone_kamikaze_explosion_radius", 9, 1, 50);


        builder.pop();

        builder.push("C4");

        builder.comment("The explosion damage of C4");
        C4_EXPLOSION_DAMAGE = builder.defineInRange("c4_explosion_damage", 300, 1, Integer.MAX_VALUE);

        builder.comment("The explosion countdown of C4");
        C4_EXPLOSION_COUNTDOWN = builder.defineInRange("c4_explosion_countdown", 514, 1, Integer.MAX_VALUE);

        builder.comment("The explosion radius of C4");
        C4_EXPLOSION_RADIUS = builder.defineInRange("c4_explosion_radius", 10, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.push("Wire Guide Missile");

        builder.comment("The damage of wire guide missile");
        WIRE_GUIDE_MISSILE_DAMAGE = builder.defineInRange("wire_guide_missile_damage", 700, 1, Integer.MAX_VALUE);

        builder.comment("The explosion damage of wire guide missile");
        WIRE_GUIDE_MISSILE_EXPLOSION_DAMAGE = builder.defineInRange("wire_guide_missile_explosion_damage", 60, 1, Integer.MAX_VALUE);

        builder.comment("The explosion radius of wire guide missile");
        WIRE_GUIDE_MISSILE_EXPLOSION_RADIUS = builder.defineInRange("wire_guide_missile_explosion_radius", 6, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.push("RPG");

        builder.comment("The explosion damage of RPG in the drone");
        RPG_EXPLOSION_DAMAGE = builder.defineInRange("rpg_explosion_damage", 130, 1, Integer.MAX_VALUE);

        builder.comment("The explosion radius of RPG in the drone");
        RPG_EXPLOSION_RADIUS = builder.defineInRange("rpg_explosion_radius", 10, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.push("AGM-65");

        builder.comment("The damage of AGM-65");
        AGM_65_DAMAGE = builder.defineInRange("agm_65_damage", 1100, 1, Integer.MAX_VALUE);

        builder.comment("The explosion damage of AGM-65");
        AGM_65_EXPLOSION_DAMAGE = builder.defineInRange("agm_65_explosion_damage", 150, 1, Integer.MAX_VALUE);

        builder.comment("The explosion radius of AGM-65");
        AGM_65_EXPLOSION_RADIUS = builder.defineInRange("agm_65_explosion_radius", 9d, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.push("MK-82");

        builder.comment("The explosion damage of MK-82");
        MK_82_EXPLOSION_DAMAGE = builder.defineInRange("mk_82_explosion_damage", 650, 1, Integer.MAX_VALUE);

        builder.comment("The explosion radius of MK-82");
        MK_82_EXPLOSION_RADIUS = builder.defineInRange("mk_82_explosion_radius", 11d, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.pop();
    }
}
