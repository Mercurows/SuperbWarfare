package com.atsuishio.superbwarfare.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.state.BlockState;
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

    private ProjectileHitEvent(@Nullable Entity owner, Projectile projectile) {
        this.owner = owner;
        this.projectile = projectile;
    }

    public static class HitEntity extends ProjectileHitEvent {

        private final Entity target;

        public HitEntity(Entity target, @Nullable Entity owner, Projectile projectile) {
            super(owner, projectile);
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }

    public static class HitBlock extends ProjectileHitEvent {

        private final BlockPos pos;
        private final BlockState state;
        private final Direction face;

        public HitBlock(BlockPos pos, BlockState state, Direction face, @Nullable Entity owner, Projectile projectile) {
            super(owner, projectile);
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
}
