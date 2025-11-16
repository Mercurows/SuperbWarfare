package com.atsuishio.superbwarfare.config.server;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ExplosionConfig {

    public static ModConfigSpec.IntValue EXPLOSION_PENETRATION_RATIO;
    public static ModConfigSpec.BooleanValue EXPLOSION_DESTROY;
    public static ModConfigSpec.BooleanValue EXTRA_EXPLOSION_EFFECT;

    public static ModConfigSpec.IntValue RGO_GRENADE_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue RGO_GRENADE_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue M67_GRENADE_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue M67_GRENADE_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue C4_EXPLOSION_COUNTDOWN;
    public static ModConfigSpec.IntValue C4_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue C4_EXPLOSION_RADIUS;

    public static ModConfigSpec.IntValue CLAYMORE_EXPLOSION_DAMAGE;
    public static ModConfigSpec.IntValue CLAYMORE_EXPLOSION_RADIUS;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("explosion");

        builder.comment("The percentage of explosion damage you take behind cover");
        EXPLOSION_PENETRATION_RATIO = builder.defineInRange("explosion_penetration_ratio", 15, 0, 100);

        builder.comment("Set true to allow Explosion to destroy blocks");
        EXPLOSION_DESTROY = builder.define("explosion_destroy", true);

        builder.comment("Set true to enable extra explosion effect. For example, C4 and RPG will destroy blocks before explosion");
        EXTRA_EXPLOSION_EFFECT = builder.define("extra_explosion_effect", true);

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


        builder.push("C4");

        builder.comment("The explosion damage of C4");
        C4_EXPLOSION_DAMAGE = builder.defineInRange("c4_explosion_damage", 300, 1, Integer.MAX_VALUE);

        builder.comment("The explosion countdown of C4");
        C4_EXPLOSION_COUNTDOWN = builder.defineInRange("c4_explosion_countdown", 514, 1, Integer.MAX_VALUE);

        builder.comment("The explosion radius of C4");
        C4_EXPLOSION_RADIUS = builder.defineInRange("c4_explosion_radius", 10, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.push("Claymore");

        builder.comment("The explosion damage of Claymore");
        CLAYMORE_EXPLOSION_DAMAGE = builder.defineInRange("claymore_explosion_damage", 140, 1, Integer.MAX_VALUE);

        builder.comment("The explosion radius of Claymore");
        CLAYMORE_EXPLOSION_RADIUS = builder.defineInRange("claymore_explosion_radius", 4, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.pop();
    }
}
