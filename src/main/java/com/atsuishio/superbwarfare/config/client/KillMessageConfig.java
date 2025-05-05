package com.atsuishio.superbwarfare.config.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public class KillMessageConfig {

    public static ModConfigSpec.BooleanValue SHOW_KILL_MESSAGE;
    public static ModConfigSpec.IntValue KILL_MESSAGE_COUNT;
    public static ModConfigSpec.IntValue KILL_MESSAGE_MARGIN_X;
    public static ModConfigSpec.IntValue KILL_MESSAGE_MARGIN_Y;
    public static ModConfigSpec.EnumValue<KillMessagePosition> KILL_MESSAGE_POSITION;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("kill_message");

        builder.comment("Set true if you want to show kill message");
        SHOW_KILL_MESSAGE = builder.define("show_kill_message", true);

        builder.comment("The max count of kill messages to show concurrently");
        KILL_MESSAGE_COUNT = builder.defineInRange("kill_message_count", 5, 1, 20);

        builder.comment("The position of kill message");
        KILL_MESSAGE_POSITION = builder.defineEnum("kill_message_position", KillMessagePosition.RIGHT_TOP);

        builder.comment("The x margin of kill message");
        KILL_MESSAGE_MARGIN_X = builder.defineInRange("kill_message_margin_x", 0, -1000, 1000);

        builder.comment("The y margin of kill message");
        KILL_MESSAGE_MARGIN_Y = builder.defineInRange("kill_message_margin_y", 5, -1000, 1000);

        builder.pop();
    }

    public enum KillMessagePosition {
        LEFT_TOP, LEFT_BOTTOM, RIGHT_TOP, RIGHT_BOTTOM,
    }
}
