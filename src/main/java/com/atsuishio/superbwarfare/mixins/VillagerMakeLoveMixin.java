package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.CupidLove;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(VillagerMakeLove.class)
public class VillagerMakeLoveMixin {

    @Inject(method = "takeVacantBed(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;)Ljava/util/Optional;",
            at = @At("HEAD"), cancellable = true)
    private void takeVacantBed(ServerLevel pLevel, Villager pVillager, CallbackInfoReturnable<Optional<BlockPos>> cir) {
        CupidLove entity = CupidLove.getInstance(pVillager);
        if (entity.superbwarfare$getCupidLove()) {
            cir.setReturnValue(Optional.of(pVillager.getOnPos()));
        }
    }
}
