package com.atsuishio.superbwarfare.perk

import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.ShootParameters
import com.atsuishio.superbwarfare.data.gun.melee.ProjectileMarker
import com.atsuishio.superbwarfare.data.gun.melee.normalizeProjectileMarker
import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.HitboxHelper
import com.atsuishio.superbwarfare.tools.RangeTool
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.VectorTool
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.cos

object AutoAimHelper {
    private val MIN_ALIGNMENT = cos(Math.toRadians(20.0))

    @JvmStatic
    fun compensatedDirection(shot: ShootParameters): Vec3? {
        val shooter = shot.shooter as? ServerPlayer ?: return null
        val targetId = shot.targetEntityUUID ?: return null
        if (!shot.zoom || shooter.isShiftKeyDown || shot.ammoSupplier !== shooter || shot.targetPos != null) return null
        val rewind = HitboxHelper.rewindTicks(shooter)
        if (rewind == 0) return null

        val data = shot.data
        if (!data.get(GunProp.AUTO_AIM)) return null
        val target = shot.level.getEntity(targetId) ?: return null
        val range = data.get(GunProp.AUTO_AIM_RANGE)
        if (target === shooter || target.vehicle != null || shooter.distanceToSqr(target) > range * range) return null
        if (!SeekTool.BASIC_FILTER.test(target) || SeekTool.IS_INVULNERABLE.test(target)) return null
        if (SeekTool.IS_FRIENDLY.test(shooter, target) || SeekTool.IS_OWNER.test(shooter, target)) return null
        if (target is Player && !shooter.canHarmPlayer(target)) return null

        val projectileId = data.get(GunProp.PROJECTILE).itemId
        val projectileType = EntityType.byString(projectileId).orElse(null)
        val isBeam = projectileType?.`is`(ModTags.EntityTypes.BEAM) == true
        val isInstant = isBeam || projectileId.normalizeProjectileMarker() == ProjectileMarker.RAY
        val usesCompensatedBox = (target !is OBBEntity || target.enableAABB()) &&
                (data.item.isBulletProjectile(data) || (isBeam && target is ServerPlayer))
        val eye = target.eyePosition
        val aimPoint = if (usesCompensatedBox) {
            val box = HitboxHelper.getCompensatedBoundingBox(target, shooter)
            val eyeY = if (target is ServerPlayer) HitboxHelper.getCompensatedEyeY(target, shooter)
            else box.minY + target.eyeHeight
            Vec3(box.center.x, eyeY.coerceIn(box.minY, box.maxY), box.center.z)
        } else {
            eye
        }
        val origin = shot.shootPosition
        if (origin.distanceToSqr(aimPoint) > range * range) return null
        val direction = origin.vectorTo(aimPoint).normalize()
        val look = shooter.lookAngle
        if (direction.dot(look) < MIN_ALIGNMENT) return null

        if (!data.get(GunProp.PROJECTILE_PENETRATING)) {
            if (!SeekTool.NOT_IN_SMOKE.test(target) || !isVisible(shooter, origin, eye)) return null
            if (aimPoint != eye && !isVisible(shooter, origin, aimPoint)) return null
        }

        val corrected = if (isInstant) {
            direction
        } else {
            val muzzleVelocity = data.get(GunProp.VELOCITY) * shot.power.coerceAtLeast(0.0)
            if (muzzleVelocity <= 0.0 || !muzzleVelocity.isFinite()) return null
            val velocity = if (VectorTool.isInLiquid(shot.level, origin)) 2.0 + 0.05 * muzzleVelocity else muzzleVelocity
            val targetVelocity = if (target is ServerPlayer) {
                HitboxHelper.getVelocity(target, if (usesCompensatedBox) rewind else 0)
            } else {
                Vec3(target.x - target.xOld, target.y - target.yOld, target.z - target.zOld)
            }
            val gravity = if (data.perk.getLevel(ModPerks.MICRO_MISSILE) > 0) 0.0 else data.get(GunProp.GRAVITY)
            RangeTool.calculateFiringSolution(origin, aimPoint, targetVelocity, velocity, gravity).normalize()
        }
        return corrected.takeIf { it.dot(look) >= MIN_ALIGNMENT && it.dot(shot.shootDirection) >= MIN_ALIGNMENT }
    }

    private fun isVisible(shooter: ServerPlayer, origin: Vec3, target: Vec3): Boolean =
        shooter.level().clip(ClipContext(origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter))
            .type != HitResult.Type.BLOCK
}
