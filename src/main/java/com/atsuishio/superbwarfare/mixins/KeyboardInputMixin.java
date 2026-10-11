package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.event.DeployedWeaponHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    // 按键修改mixin
    @Inject(method = "tick", at = @At("RETURN"))
    public void tick(boolean pIsSneaking, float pSneakingSpeedMultiplier, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        ItemStack stack = player.getMainHandItem();

        if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked")) {
            this.up = false;
            this.down = false;
            this.left = false;
            this.right = false;
            this.shiftKeyDown = false;
            this.forwardImpulse = 0;
            this.leftImpulse = 0;
            this.jumping = false;
        }

        if (DeployedWeaponHandler.isDeployed(player)) {
            this.up = false;
            this.down = false;
            this.left = false;
            this.right = false;
            this.shiftKeyDown = false;
            this.forwardImpulse = 0;
            this.leftImpulse = 0;
            this.jumping = false;
        }

        if (Minecraft.getInstance().player == null
                || !Minecraft.getInstance().player.hasEffect(ModMobEffects.SHOCK.get())
                || Minecraft.getInstance().player.isSpectator()) {
            return;
        }

        this.up = false;
        this.down = false;
        this.left = false;
        this.right = false;
        this.shiftKeyDown = false;
        this.forwardImpulse = 0;
        this.leftImpulse = 0;
        this.jumping = false;
    }
}
