package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.mixin.ModTeam;
import com.atsuishio.superbwarfare.network.message.receive.ClientTeamSyncMessage;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerTeam.class)
public abstract class PlayerTeamMixin implements ModTeam {

    @Shadow
    @Final
    private Scoreboard scoreboard;

    @Shadow
    public abstract String getName();

    @Unique
    public boolean superbWarfare$deathMatchEnabled = false;

    @Override
    public void superbWarfare$setDeathMatch(boolean deathMatch) {
        this.superbWarfare$deathMatchEnabled = deathMatch;
        Mod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new ClientTeamSyncMessage(this.getName(), deathMatch));
        this.scoreboard.onTeamChanged((PlayerTeam) (Object) this);
    }

    @Override
    public boolean superbWarfare$isDeathMatchEnabled() {
        return this.superbWarfare$deathMatchEnabled;
    }
}
