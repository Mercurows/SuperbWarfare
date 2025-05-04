package com.atsuishio.superbwarfare.config.server;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MiscConfig {

    public static ModConfigSpec.BooleanValue ALLOW_TACTICAL_SPRINT;
    public static ModConfigSpec.BooleanValue SEND_KILL_FEEDBACK;
    public static ModConfigSpec.IntValue DEFAULT_ARMOR_LEVEL;
    public static ModConfigSpec.IntValue MILITARY_ARMOR_LEVEL;
    public static ModConfigSpec.IntValue HEAVY_MILITARY_ARMOR_LEVEL;
    public static ModConfigSpec.IntValue ARMOR_PONT_PER_LEVEL;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("misc");

        builder.comment("Set true to enable tactical sprint");
        ALLOW_TACTICAL_SPRINT = builder.define("allow_tactical_sprint", false);

        builder.comment("Set true to enable kill feedback sending");
        SEND_KILL_FEEDBACK = builder.define("send_kill_feedback", true);

        builder.comment("The default maximum armor level for normal armors");
        DEFAULT_ARMOR_LEVEL = builder.defineInRange("default_armor_level", 1, 0, 10000000);

        builder.comment("The maximum armor level for armors with superbwarfare:military_armor tag");
        MILITARY_ARMOR_LEVEL = builder.defineInRange("military_armor_level", 2, 0, 10000000);

        builder.comment("The maximum armor level for armors with superbwarfare:military_armor_heavy tag(will override superbwarfare:military_armor tag!)");
        HEAVY_MILITARY_ARMOR_LEVEL = builder.defineInRange("heavy_military_armor_level", 3, 0, 10000000);

        builder.comment("The points per level for armor plate");
        ARMOR_PONT_PER_LEVEL = builder.defineInRange("armor_point_per_level", 15, 0, 10000000);

        builder.pop();
    }
}
