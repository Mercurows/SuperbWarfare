package com.atsuishio.superbwarfare.init

import net.minecraft.world.level.GameRules

object ModGameRules {
    val MOD_RULE_DO_GENERATE_LOOTS: GameRules.Key<GameRules.BooleanValue> =
        GameRules.register(
            "sbwDoGenerateLoots",
            GameRules.Category.SPAWNING,
            GameRules.BooleanValue.create(true)
        )

    fun bootstrap() {
    }
}
