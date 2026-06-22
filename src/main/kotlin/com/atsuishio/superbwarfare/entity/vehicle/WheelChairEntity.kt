package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.advancement.CriteriaRegister
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.WaterAnimal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Math

open class WheelChairEntity(type: EntityType<out WheelChairEntity>, level: Level) : VehicleEntity(type, level) {
    override fun playerTouch(pPlayer: Player) {
        if (this.position().distanceTo(pPlayer.position()) > 1.4
            || pPlayer === this.getFirstPassenger() && pPlayer.position().y > position().y
        ) return
        if (!this.level().isClientSide && pPlayer.y < this.y + this.bbHeight
            && pPlayer.y + pPlayer.bbHeight > this.y
        ) {
            val entitySize = (pPlayer.bbWidth * pPlayer.bbHeight).toDouble()
            val thisSize = (this.bbWidth * this.bbHeight).toDouble()
            val f = Math.min(entitySize / thisSize, 2.0)
            this.setDeltaMovement(
                this.deltaMovement.add(
                    Vec3(
                        pPlayer.position().vectorTo(this.position()).toVector3f()
                    ).scale(0.5 * f * pPlayer.deltaMovement.length())
                )
            )
            this.yRot = pPlayer.getYHeadRot()
        }
    }

    override fun baseTick() {
        super.baseTick()
        attractEntity()
    }

    open fun hasEnoughSpaceFor(pEntity: Entity): Boolean {
        return pEntity.bbWidth < this.bbWidth
    }

    open fun attractEntity() {
        val list = this.level().getEntities(this, this.boundingBox.inflate(0.2, -0.01, 0.2))
        if (!list.isEmpty()) {
            val flag = !this.level().isClientSide && this.controllingPassenger !is Player

            for (entity in list) {
                if (!entity.hasPassenger(this)
                    && flag
                    && !entity.isPassenger
                    && this.hasEnoughSpaceFor(entity)
                    && (entity is LivingEntity || entity is MortarEntity) && (entity !is WaterAnimal) && (entity !is Player)
                ) {
                    entity.startRiding(this)
                }
            }
        }
    }

    override fun addPassenger(pPassenger: Entity) {
        super.addPassenger(pPassenger)

        if (pPassenger is ServerPlayer
            && (pPassenger.mainHandItem.item == ModItems.ELECTRIC_BATON.get()
                    || pPassenger.offhandItem.item == ModItems.ELECTRIC_BATON.get())
        ) {
            CriteriaRegister.OTTO_SPRINT.trigger(pPassenger)
        }
    }

    override fun bounceHorizontal(direction: Direction) {
        for (entity in getPassengers()) {
            if (entity != null && level() is ServerLevel) {
                entity.stopRiding()
                val speed = deltaMovementO.length()
                if (speed > 0.4) {
                    val dir = deltaMovementO.normalize().add(getUpVec(1f).scale(0.6))
                    queueServerWork(1) {
                        if (entity is Player && entity.level().isClientSide) {
                            entity.deltaMovement = dir.normalize().scale(speed)
                        } else {
                            entity.deltaMovement = dir.normalize().scale(speed)
                        }
                    }
                }
            }
        }
        super.bounceHorizontal(direction)
    }
}
