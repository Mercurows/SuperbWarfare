package com.atsuishio.superbwarfare.config.server;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class SeekConfig {

    public static ModConfigSpec.ConfigValue<List<? extends String>> SEEK_BLACKLIST;

    public static final List<? extends String> DEFAULT_SEEK_BLACKLIST = List.of(
            "minecraft:item",
            "minecraft:experience_orb",
            "minecraft:armor_stand",
            "minecraft:area_effect_cloud",
            "superbwarfare:claymore",
            "superbwarfare:c4",
            "touhou_little_maid:power_point",
            "evilcraft:vengeance_spirit",
            "mts:builder_rendering"
    );

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("seek");

        builder.comment("List of entities that can NOT be sought");
        SEEK_BLACKLIST = builder.defineList("seek_blacklist",
                DEFAULT_SEEK_BLACKLIST,
                e -> e instanceof String);

        builder.pop();
    }
}
