package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.mixin.ModTeam;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardSaveData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScoreboardSaveData.class)
public class ScoreboardSaveDataMixin {

    @Shadow
    @Final
    private Scoreboard scoreboard;

    @Inject(method = "saveTeams", at = @At("RETURN"))
    private void saveTeams(CallbackInfoReturnable<ListTag> cir) {
        var listTag = cir.getReturnValue();

        for (int i = 0; i < listTag.size(); ++i) {
            CompoundTag compoundtag = listTag.getCompound(i);
            String s = compoundtag.getString("Name");
            PlayerTeam playerteam = this.scoreboard.getPlayerTeam(s);
            if (playerteam == null) continue;
            ModTeam modTeam = ModTeam.of(playerteam);
            compoundtag.putBoolean("SbwDeathMatch", modTeam.superbWarfare$isDeathMatchEnabled());
        }
    }

    @Inject(method = "loadTeams", at = @At("RETURN"))
    private void loadTeams(ListTag pTagList, CallbackInfo ci) {
        for (int i = 0; i < pTagList.size(); ++i) {
            CompoundTag compoundtag = pTagList.getCompound(i);
            String s = compoundtag.getString("Name");
            PlayerTeam playerteam = this.scoreboard.getPlayerTeam(s);
            if (playerteam == null) continue;
            ModTeam modTeam = ModTeam.of(playerteam);
            modTeam.superbWarfare$setDeathMatch(compoundtag.getBoolean("SbwDeathMatch"));
        }
    }
}
