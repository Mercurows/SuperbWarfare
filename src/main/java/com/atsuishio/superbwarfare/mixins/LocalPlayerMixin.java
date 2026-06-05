package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends LivingEntity {
    @Shadow
    private boolean flashOnSetHealth;

    protected LocalPlayerMixin(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "hurtTo", at = @At("HEAD"), cancellable = true)
    public void hurtTo(float pHealth, CallbackInfo ci) {
        if (this.flashOnSetHealth) {
            LocalPlayer player = (LocalPlayer) (Object) this;
            if (player.getHealth() > pHealth && player.getLastDamageSource() != null && player.getLastDamageSource().is(ModTags.DamageTypes.NO_HURT_EFFECT)) {
                ci.cancel();
                this.lastHurt = player.getHealth() - pHealth;
                this.invulnerableTime = 0;
                this.setHealth(pHealth);
                this.hurtDuration = 0;
                this.hurtTime = 0;
            }
        }
    }
}
