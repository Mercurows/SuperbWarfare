package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntitySetPosRawMixin {

    @Redirect(
            method = "setPosRaw(DDD)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"
            )
    )
    private LevelChunk skipForcedChunkLoadForEntity(Level level, int chunkX, int chunkZ) {
        if ((Entity) (Object) this instanceof FastThrowableProjectile) {
            // 无视区块加载飞行的弹射物：不强制加载所在区块（返回值被丢弃，无副作用）
            return null;
        }
        return level.getChunk(chunkX, chunkZ);
    }
}
