package com.atsuishio.superbwarfare.config.server;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MiscConfig {

    public static ModConfigSpec.BooleanValue ALLOW_TACTICAL_SPRINT;
    public static ModConfigSpec.BooleanValue SEND_KILL_FEEDBACK;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("misc");

        builder.comment("Set true to enable tactical sprint");
        ALLOW_TACTICAL_SPRINT = builder.define("allow_tactical_sprint", true);

        builder.comment("Set true to enable kill feedback sending");
        SEND_KILL_FEEDBACK = builder.define("send_kill_feedback", true);

        builder.pop();
    }
}
