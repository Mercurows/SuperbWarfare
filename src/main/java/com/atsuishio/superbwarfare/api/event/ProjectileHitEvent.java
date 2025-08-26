package com.atsuishio.superbwarfare.api.event;

import com.atsuishio.superbwarfare.tools.ExtendedEntityRayTraceResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * 子弹等投射物在命中实体或方块时触发的事件
 */
@Cancelable
@ApiStatus.AvailableSince("0.8.7")
public class ProjectileHitEvent extends Event {

    @Nullable
    private final Entity owner;
    private final Projectile projectile;
    private final Vec3 hitVec;

    private ProjectileHitEvent(@Nullable Entity owner, Projectile projectile, Vec3 hitVec) {
        this.owner = owner;
        this.projectile = projectile;
        this.hitVec = hitVec;
    }

    public static class HitEntity extends ProjectileHitEvent {

        private final Entity target;
        private final boolean isHeadshot;
        private final boolean isLegShot;

        public HitEntity(@Nullable Entity owner, Projectile projectile, ExtendedEntityRayTraceResult result) {
            super(owner, projectile, result.getLocation());
            this.target = result.getEntity();
            this.isHeadshot = result.isHeadshot();
            this.isLegShot = result.isLegShot();
        }

        public HitEntity(@Nullable Entity owner, Projectile projectile, Entity target, Vec3 hitVec) {
            super(owner, projectile, hitVec);
            this.target = target;
            this.isHeadshot = false;
            this.isLegShot = false;
        }

        public Entity getTarget() {
            return target;
        }

        public boolean isHeadshot() {
            return isHeadshot;
        }

        public boolean isLegShot() {
            return isLegShot;
        }
    }

    public static class HitBlock extends ProjectileHitEvent {

        private final BlockPos pos;
        private final BlockState state;
        private final Direction face;

        public HitBlock(BlockPos pos, BlockState state, Direction face, @Nullable Entity owner, Projectile projectile, Vec3 hitVec) {
            super(owner, projectile, hitVec);
            this.pos = pos;
            this.state = state;
            this.face = face;
        }

        public BlockPos getPos() {
            return pos;
        }

        public BlockState getState() {
            return state;
        }

        public Direction getFace() {
            return face;
        }
    }

    public Entity getOwner() {
        return owner;
    }

    public Projectile getProjectile() {
        return projectile;
    }

    public Vec3 getHitVec() {
        return hitVec;
    }
}
