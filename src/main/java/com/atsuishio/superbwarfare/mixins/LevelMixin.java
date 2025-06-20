package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
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
import java.util.stream.StreamSupport;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Shadow
    protected abstract LevelEntityGetter<Entity> getEntities();

    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("RETURN"))
    public void getEntities(Entity pEntity, AABB pBoundingBox, Predicate<? super Entity> pPredicate, CallbackInfoReturnable<List<Entity>> cir) {
        // TODO 研究OBB碰撞的时候把这个删了
        if (!(pEntity instanceof Projectile)) return;

        StreamSupport.stream(this.getEntities().getAll().spliterator(), false).filter(e -> e instanceof OBBEntity && pPredicate.test(e))
                .forEach(entity -> {
                            for (OBB obb : ((OBBEntity) entity).getOBBs()) {
                                if (OBB.isColliding(obb, pBoundingBox) && !cir.getReturnValue().contains(entity)) {
                                    cir.getReturnValue().add(entity);
                                }
                            }
                        }
                );
    }
}
