package com.atsuishio.superbwarfare.config.common;

import net.neoforged.neoforge.common.ModConfigSpec;

public class GameplayConfig {

    public static ModConfigSpec.BooleanValue RESPAWN_RELOAD;
    public static ModConfigSpec.BooleanValue GLOBAL_INDICATION;
    public static ModConfigSpec.BooleanValue RESPAWN_AUTO_ARMOR;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("gameplay");

        builder.comment("Set true if you want to reload all your guns when respawn");
        RESPAWN_RELOAD = builder.define("respawn_reload", true);

        builder.comment("Set false if you want to show kill indication ONLY while killing an entity with a gun");
        GLOBAL_INDICATION = builder.define("global_indication", true);

        builder.comment("Set true if you want to refill your armor plate when respawn");
        RESPAWN_AUTO_ARMOR = builder.define("respawn_auto_armor", true);

        builder.pop();
    }

}
