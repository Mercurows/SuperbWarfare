package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.tools.NBTTool;
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
        var tag = NBTTool.getTag(stack);

        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
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
                || !Minecraft.getInstance().player.hasEffect(ModMobEffects.SHOCK)
                || Minecraft.getInstance().player.isSpectator()
        ) return;

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
