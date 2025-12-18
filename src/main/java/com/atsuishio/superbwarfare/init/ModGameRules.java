package com.atsuishio.superbwarfare.init;

import net.minecraft.world.level.GameRules;

public class ModGameRules {

    public static final GameRules.Key<GameRules.BooleanValue> MOD_RULE_DO_GENERATE_LOOTS =
            GameRules.register("sbwDoGenerateLoots", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));

    public static void bootstrap() {
    }
}
