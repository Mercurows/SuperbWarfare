package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Shadow @Final private ClientLevel.ClientLevelData clientLevelData;

    //TODO 找一个更像人类的方式实现降低世界亮度

    @Inject(method = "setDayTime(J)V",
            at = @At("RETURN"), cancellable = true)
    public void setDayTime(long pTime, CallbackInfo ci) {
        if (ClientEventHandler.activeThermalImaging && ClientEventHandler.thermalImagingMode == 0) {
            ci.cancel();
            this.clientLevelData.setDayTime(18000);
        }
    }
}
