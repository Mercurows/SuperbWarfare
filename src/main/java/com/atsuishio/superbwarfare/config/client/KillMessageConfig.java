package com.atsuishio.superbwarfare.config.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public class KillMessageConfig {

    public static ModConfigSpec.BooleanValue SHOW_KILL_MESSAGE;
    public static ModConfigSpec.IntValue KILL_MESSAGE_COUNT;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("kill_message");

        builder.comment("Set true if you want to show kill message");
        SHOW_KILL_MESSAGE = builder.define("show_kill_message", true);

        builder.comment("The max count of kill messages to show concurrently");
        KILL_MESSAGE_COUNT = builder.defineInRange("kill_message_count", 5, 1, 20);

        builder.pop();
    }

}
