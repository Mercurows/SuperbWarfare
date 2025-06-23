package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class SteelPipeFallMixin {

    @Shadow public abstract boolean onGround();

    @Inject(method = "setOnGroundWithMovement", at = @At("HEAD"))
    public void playSteelPipeDropSound(boolean onGround, Vec3 movement, CallbackInfo ci) {
        if (!onGround || onGround()) return;

        var entity = (Entity) (Object) this;
        if (entity instanceof ItemEntity itemEntity && itemEntity.getItem().getItem() == ModItems.STEEL_PIPE.get()) {
            entity.level().playSound(null, entity.getOnPos(), ModSounds.STEEL_PIPE_DROP.get(), SoundSource.PLAYERS, 2, 1);
        }
    }
}
