package com.atsuishio.superbwarfare.entity.mixin;

import net.minecraft.world.scores.PlayerTeam;

public interface ModTeam {

    static ModTeam of(PlayerTeam team) {
        return (ModTeam) team;
    }

    static boolean enabledDeathMatch(PlayerTeam team) {
        return of(team).superbWarfare$isDeathMatchEnabled();
    }

    void superbWarfare$setDeathMatch(boolean deathMatch);

    boolean superbWarfare$isDeathMatchEnabled();
}
