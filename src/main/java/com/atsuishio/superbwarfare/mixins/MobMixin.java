package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Mob.class)
public class MobMixin {

    @Shadow
    @Nullable
    private LivingEntity target;

    @Inject(method = "getTarget", at = @At("RETURN"), cancellable = true)
    public void getTarget(CallbackInfoReturnable<LivingEntity> cir) {
        if (!MiscConfig.SMOKE_HIDE_TARGET.get()) return;

        var target = this.target;
        if (target != null && !SeekTool.NOT_IN_SMOKE_WITH_RANGE.test(target, 1d)) {
            cir.setReturnValue(null);
        }
    }
}
