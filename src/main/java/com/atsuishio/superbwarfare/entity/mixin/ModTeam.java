package com.atsuishio.superbwarfare.entity.mixin;

import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

public interface ModTeam {

    static ModTeam of(PlayerTeam team) {
        return (ModTeam) team;
    }

    static boolean enabledDeathMatch(Team team) {
        if (!(team instanceof PlayerTeam playerTeam)) return false;
        return of(playerTeam).superbWarfare$isDeathMatchEnabled();
    }

    void superbWarfare$setDeathMatch(boolean deathMatch);

    boolean superbWarfare$isDeathMatchEnabled();
}
