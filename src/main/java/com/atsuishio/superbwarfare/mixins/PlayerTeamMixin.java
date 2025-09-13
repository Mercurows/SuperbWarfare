package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.mixin.ModTeam;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerTeam.class)
public class PlayerTeamMixin implements ModTeam {

    @Shadow
    @Final
    private Scoreboard scoreboard;
    @Unique
    public boolean superbWarfare$deathMatchEnabled = false;

    @Override
    public void superbWarfare$setDeathMatch(boolean deathMatch) {
        this.superbWarfare$deathMatchEnabled = deathMatch;
        this.scoreboard.onTeamChanged((PlayerTeam) (Object) this);
    }

    @Override
    public boolean superbWarfare$isDeathMatchEnabled() {
        return this.superbWarfare$deathMatchEnabled;
    }
}
