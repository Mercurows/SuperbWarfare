package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.tools.VectorTool
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

abstract class BounceProjectile : FastThrowableProjectile {
    constructor(type: EntityType<out ThrowableItemProjectile>, level: Level) : super(type, level)
    constructor(type: EntityType<out ThrowableItemProjectile>, x: Double, y: Double, z: Double, level: Level) : super(type, x, y, z, level)
    constructor(type: EntityType<out ThrowableItemProjectile>, shooter: Entity?, level: Level) : super(type, shooter, level)

    override fun projectileMove(level: Level) {
        // 客户端位置由 ClientMotionSyncMessage 每 tick 同步，不再自行推算

        val vec = this.deltaMovement

        // 更新朝向（在 deltaMovement 可能被 onHit/反弹 修改之后）
        if (vec.length() >= STOP_SPEED) {
            this.updateRotation()
        }

        // 5. 对当前 deltaMovement（已包含反弹等修改）施加摩擦力和重力
        val friction = if (this.isInWater) 0.8 else 1.0
        this.deltaMovement = vec.scale(friction)

        this.deltaMovement = this.deltaMovement.add(0.0, -this.getCustomGravity().toDouble(), 0.0)

        if (level is ServerLevel && this.canPassThroughFluid() && VectorTool.isInLiquid(level, position())) {
            this.deltaMovement = this.deltaMovement.scale(this.underwaterMotionScaleValue.toDouble().coerceIn(0.0, 1.0))
        }

        var f = 0.98f

        if (this.onGround()) {
            val pos = this.blockPosBelowThatAffectsMyMovement
            f = level.getBlockState(pos).getFriction(level, pos, this) * 0.98f
        }

        this.deltaMovement = deltaMovement.multiply(f.toDouble(), 0.98, f.toDouble())

        this.move(MoverType.SELF, deltaMovement)
    }

    open fun bounce(direction: Direction) {
        val speed = this.deltaMovement.length()
        if (speed < STOP_SPEED) {
            this.deltaMovement = Vec3.ZERO
            return
        }

        when (direction.axis) {
            Direction.Axis.X -> this.deltaMovement = this.deltaMovement.multiply(-0.6, 0.8, 0.8)
            Direction.Axis.Y -> {
                this.deltaMovement = this.deltaMovement.multiply(0.8, -0.5, 0.8)
                if (this.deltaMovement.y() < this.getCustomGravity()) {
                    this.deltaMovement = this.deltaMovement.multiply(1.0, 0.0, 1.0)
                }
            }

            Direction.Axis.Z -> this.deltaMovement = this.deltaMovement.multiply(0.8, 0.8, -0.6)
        }
    }

    companion object {
        private const val STOP_SPEED = 0.15
    }
}
