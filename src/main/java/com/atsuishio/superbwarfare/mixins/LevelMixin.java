package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Shadow
    protected abstract LevelEntityGetter<Entity> getEntities();

    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("RETURN"))
    public void getEntities(Entity pEntity, AABB pBoundingBox, Predicate<? super Entity> pPredicate, CallbackInfoReturnable<List<Entity>> cir) {
        if (pEntity instanceof ProjectileEntity) {
            this.getEntities().get(pBoundingBox.inflate(3), entity -> {
                if (entity instanceof OBBEntity obbEntity) {
                    for (OBB obb : obbEntity.getOBBs()) {
                        if (OBB.isColliding(obb, pBoundingBox)) {
                            if (!cir.getReturnValue().contains(entity)) {
                                cir.getReturnValue().add(entity);
                            }
                        }
                    }
                }
            });
        } else {
            this.getEntities().get(pBoundingBox, entity -> {
                if (entity instanceof OBBEntity obbEntity) {
                    for (OBB obb : obbEntity.getOBBs()) {
                        if (OBB.isColliding(obb, pBoundingBox)) {
                            if (!cir.getReturnValue().contains(entity)) {
                                cir.getReturnValue().add(entity);
                            }
                        }
                    }
                }
            });
        }
    }
}
