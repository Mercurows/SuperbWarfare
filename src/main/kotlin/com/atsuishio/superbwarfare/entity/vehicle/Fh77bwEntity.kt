package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.annotation.ExcludeBvrSync
import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.base.SpArtilleryEntity
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.VectorTool
import com.atsuishio.superbwarfare.tools.angleTo
import com.atsuishio.superbwarfare.tools.toVec3
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

open class Fh77bwEntity(type: EntityType<Fh77bwEntity>, world: Level) : SpArtilleryEntity(type, world) {
    private var wasClose = false
    private var wasOut = false
    private var wasHoeUp = false
    open var close by CLOSE
    open var opened by OPENED
    open var out by OUT
    open var hoeUp by HOE_UP

    override fun baseTick() {
        super.baseTick()

        val tryMove = forwardInputDown || backInputDown

        if (getNthEntity(turretControllerIndex) == null) {
            if ((deltaMovement.horizontalDistanceSqr() > 0.007 || tryMove) && !lockTurret) {
                shootVec = getViewVec(this, 1f).toVector3f()
                if (shootVec.toVec3().angleTo(getShootVec("Main", 1f)) < 0.5) {
                    close = true
                    Mod.queueServerWork(3) {
                        lockTurret = true
                    }
                } else {
                    close = false
                }

                if (shootVec.toVec3().angleTo(getShootVec("Main", 1f).multiply(1.0, 0.0, 1.0)) > 2) {
                    turretXRot = turretXRot.coerceAtMost(-10f * Mth.clamp((25 - Mth.abs(turretYRot)) * 0.15f, 0f, 1f))
                }

                if (shootVec.toVec3().angleTo(getShootVec("Main", 1f)) < 25) {
                    out = false
                }
            }
            opened = false
        } else {
            Mod.queueServerWork(10) {
                lockTurret = false
            }
            close = false
            if (opened) {
                close = true
                out = true
            }
        }

        if (getNthEntity(turretControllerIndex) != null && lookAngle.angleTo(getShootVec("Main", 1f)) > 25) {
            opened = true
        }

        if (deltaMovement.horizontalDistanceSqr() > 0.004) {
            hoeUp = true
        } else {
            hoeUp = false
        }

        if (level().isClientSide) {
            val ctx = anim?.context ?: return
            if (close && !wasClose) {
                ctx.stopAnimation("animation.fh_77bw.open", 80)
            } else if (!close && wasClose) {
                ctx.playAnimation("animation.fh_77bw.open", AnimationPlayType.LOOP, 80)
            }

            if (out && !wasOut) {
                ctx.playAnimation("animation.fh_77bw.out", AnimationPlayType.LOOP, 80)
            } else if (!out && wasOut) {
                ctx.stopAnimation("animation.fh_77bw.out", 80)
            }

            if (hoeUp && !wasHoeUp) {
                ctx.playAnimation("animation.fh_77bw.hoe_up", AnimationPlayType.LOOP, 80)
            } else if (!hoeUp && wasHoeUp) {
                ctx.stopAnimation("animation.fh_77bw.hoe_up", 80)
            }

            wasClose = close
            wasOut = out
            wasHoeUp = hoeUp
        }

        if (!lockTurret) {
            power = 0f
        }
    }

    override fun beforeShoot(living: LivingEntity?, weaponName: String?) {
        val level = living?.level()
        if (level is ServerLevel && weaponName == "Main") {
            ParticleTool.spawnBigCannonMuzzleParticles(getShootVec("Main", 1f), getShootPos("Main", 1f), level, this)
        }
    }

    override val turretTurnXSpeed: Float
        get() = if (lockTurret) 0f else super.turretTurnXSpeed

    override val turretTurnYSpeed: Float
        get() = if (lockTurret) 0f else super.turretTurnYSpeed

    override val customTurretMinPitch: Float
        get() = if (Mth.abs(turretYRot) < 18 && !lockTurret && shootVec != getViewVec(this, 1f).toVector3f()) 10f * Mth.clamp((18 - Mth.abs(turretYRot)) * 0.4f, 0f, 1f) else 0f

    override fun canShoot(living: LivingEntity?): Boolean {
        if (living == getNthEntity(1)) {
            if (VectorTool.calculateAngle(getUpVec(1f), Vec3(0.0, 1.0, 0.0)) > 1) {
                if (living is Player) {
                    living.displayClientMessage(
                        Component.translatable("tips.superbwarfare.fh77bw.body_tilted").withStyle(ChatFormatting.RED),
                        true
                    )
                }
                return false
            }
            if (deltaMovement.lengthSqr() > 0.001) {
                if (living is Player) {
                    living.displayClientMessage(
                        Component.translatable("tips.superbwarfare.fh77bw.not_stopped").withStyle(ChatFormatting.RED),
                        true
                    )
                }
                return false
            }
        }
        return super.canShoot(living)
    }

    companion object {
        @JvmField
        @ExcludeBvrSync("Close")
        val CLOSE: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(Fh77bwEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        @ExcludeBvrSync("Opened")
        val OPENED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(Fh77bwEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        @ExcludeBvrSync("Out")
        val OUT: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(Fh77bwEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        @ExcludeBvrSync("HoeUp")
        val HOE_UP: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(Fh77bwEntity::class.java, EntityDataSerializers.BOOLEAN)
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        with(entityData) {
            define(CLOSE, true)
            define(OPENED, false)
            define(OUT, false)
            define(HOE_UP, false)
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putBoolean("Close", close)
        compound.putBoolean("Opened", opened)
        compound.putBoolean("Out", out)
        compound.putBoolean("HoeUp", hoeUp)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("Close")) {
            close = compound.getBoolean("Close")
        }
        if (compound.contains("Opened")) {
            opened = compound.getBoolean("Opened")
        }
        if (compound.contains("Out")) {
            out = compound.getBoolean("Out")
        }
        if (compound.contains("HoeUp")) {
            hoeUp = compound.getBoolean("HoeUp")
        }
    }
}
