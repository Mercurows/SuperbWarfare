package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.mixin.BeastEntityKiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = Integer.MAX_VALUE)
public abstract class BeastMixin implements BeastEntityKiller {

    @Unique
    public boolean sbw$beastKilled = false;

    @Override
    public void sbw$kill() {
        this.sbw$beastKilled = true;
    }

    @Inject(method = "isDeadOrDying", at = @At("HEAD"), cancellable = true, order = Integer.MAX_VALUE)
    public void isDeadOrDying(CallbackInfoReturnable<Boolean> cir) {
        if (this.sbw$beastKilled) {
            cir.cancel();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getHealth", at = @At("HEAD"), cancellable = true, order = Integer.MAX_VALUE)
    public void getHealth(CallbackInfoReturnable<Float> cir) {
        if (this.sbw$beastKilled) {
            cir.cancel();
            cir.setReturnValue(0f);
        }
    }

    @Inject(method = "remove", at = @At("RETURN"), order = Integer.MAX_VALUE)
    public void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (this.sbw$beastKilled) {
            ((LivingEntity) (Object) this).setRemoved(reason);
        }
    }
}
