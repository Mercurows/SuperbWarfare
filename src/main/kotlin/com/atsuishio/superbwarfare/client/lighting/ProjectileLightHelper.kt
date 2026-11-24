package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.entity.projectile.*
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

/**
 * 炮弹曳光与爆炸闪光。
 *
 * 这些光源以前通过已删除的方块光注入链路往客户端区块光照引擎里写"假方块光"，
 * 现在统一走 [WorldLightHandler] → [GunMuzzleLightHandler.Light] → [GunMuzzleLightRenderer]
 * 的屏幕空间光 pass，与枪口焰/手电一致：
 * - 曳光：每 tick 用 projectile 实体作 key 刷新位置，光随弹丸移动；
 * - 爆炸：按爆炸半径注册一次性大半径点光。
 */
@OnlyIn(Dist.CLIENT)
object ProjectileLightHelper {
    private const val TRAIL_CULL_SQ = 128.0 * 128.0
    private const val EXPLODE_CULL_SQ = 192.0 * 192.0

    /**
     * @param level     peak light level per trail point
     * @param minLevel  minimum before expiry（保留字段，屏幕空间光按 level 计算强度）
     * @param ttl       lifetime in client ticks
     * @param radial    emit ambient glow around the projectile (rocket engines)
     */
    data class TrailLight(
        val level: Int,
        val minLevel: Int,
        val ttl: Int,
        val radial: Boolean = false
    )

    // -----------------------------------------------------------------
    // Trail classification — only things that visibly glow in flight
    // -----------------------------------------------------------------

    /**
     * Returns trail parameters for projectiles with a visible light source
     * (rocket exhaust, superheated metal).  Returns null for bullets,
     * grenades, mines, and other non-luminous projectiles.
     */
    @JvmStatic
    fun getTrailLight(entity: Entity): TrailLight? = when (entity) {
        // Guided missiles — rocket engine, brightest
        is MissileProjectile -> TrailLight(15, 12, 10, radial = true)

        // Unguided rockets — strong engine glow
        is MediumRocketEntity,
        is RpgRocketStandardEntity,
        is RpgRocketTBGEntity -> TrailLight(15, 11, 9, radial = true)

        // Small rockets
        is SmallRocketEntity -> TrailLight(14, 9, 7, radial = true)

        // Large cannon shells — superheated metal
        is CannonShellEntity -> TrailLight(13, 9, 6)

        // Small cannon shells
        is SmallCannonShellEntity -> TrailLight(11, 7, 5)

        // Mortar shells — visible hot trajectory
        is MortarShellEntity -> TrailLight(12, 8, 6)

        is GrapeshotEntity -> TrailLight(8, 6, 3)

        // Bullets, grenades, mines — no visible glow in flight
        else -> null
    }

    /**
     * Launch backblast parameters for rockets and large shells.
     */
    @JvmStatic
    fun getLaunchFlash(entity: Entity): MuzzleFlashHelper.FlashParams? = when (entity) {
        is MissileProjectile -> MuzzleFlashHelper.FlashParams(15, 12, 7)
        is MediumRocketEntity,
        is RpgRocketStandardEntity,
        is RpgRocketTBGEntity -> MuzzleFlashHelper.FlashParams(15, 11, 6)

        is SmallRocketEntity -> MuzzleFlashHelper.FlashParams(14, 9, 5)
        is CannonShellEntity -> MuzzleFlashHelper.FlashParams(15, 11, 5)
        is MortarShellEntity -> MuzzleFlashHelper.FlashParams(14, 9, 4)
        is GrapeshotEntity -> MuzzleFlashHelper.FlashParams(13, 10, 3)
        else -> null
    }

    // -----------------------------------------------------------------
    // Trail emission
    // -----------------------------------------------------------------

    /** 曳光光源附加的尾焰 key（与实体主光区分，方便一起刷新/移除）。 */
    private fun engineKey(entity: Entity): String = "projectile_engine_${entity.id}"

    /**
     * Emits a moving trail light attached to the projectile.
     * The light position is refreshed every client tick with the entity as key,
     * so it follows the projectile without touching the block light engine.
     */
    @JvmStatic
    fun emitTrailLight(entity: Entity) {
        val trail = getTrailLight(entity) ?: return
        val level = mc.level ?: return
        val player = mc.player ?: return

        val currentPos = entity.position()
        if (player.distanceToSqr(currentPos.x, currentPos.y, currentPos.z) > TRAIL_CULL_SQ) return
        if (entity.level() !== level) return

        val center = currentPos.add(0.0, entity.bbHeight * 0.5, 0.0)
        val movement = entity.deltaMovement
        val direction = if (movement.lengthSqr() > 1e-6) movement.normalize() else player.lookAngle.normalize()

        val strength = (trail.level / 15f).coerceIn(0.08f, 1f)
        val radius = 6f + trail.level * 0.25f + if (trail.radial) 1.25f else 0f
        // 跟随实体的插值位置（getPosition(partialTick)）：客户端 tick 只有 20Hz，直接每 tick 更新会一跳一跳的。
        val centerOffset = Vec3(0.0, entity.bbHeight * 0.5, 0.0)
        WorldLightHandler.put(
            entity, center, direction, strength, radius,
            // 短 TTL + 每 tick 刷新：弹丸消失后光会在 2 tick 内自然熄灭。
            durationTicks = 2,
            color = WorldLightHandler.warmColor(),
            follow = entity,
            followOffset = centerOffset,
        )

        if (trail.radial) {
            val enginePos = center.subtract(direction.scale(0.55))
            WorldLightHandler.put(
                engineKey(entity), enginePos, direction,
                (strength * 0.85f).coerceAtLeast(0.05f), radius * 1.35f,
                durationTicks = 2, color = WorldLightHandler.warmColor(),
                follow = entity,
                followOffset = centerOffset.subtract(direction.scale(0.55)),
            )
        } else {
            WorldLightHandler.remove(engineKey(entity))
        }
    }

    // -----------------------------------------------------------------
    // Explosion flash — all explosive events
    // -----------------------------------------------------------------

    /**
     * 注册一次性的爆炸点光：半径按爆炸半径放大，持续 4~6 tick 后由 [WorldLightHandler] 自动过期。
     *
     * 以爆炸中心所在方块为 key 去重：同一次爆炸可能同时来自爆炸粒子包、弹丸移除和载具摧毁三条路径，
     * 用同一个 key 覆盖可以避免叠加成多个屏幕空间光。
     */
    @JvmStatic
    fun emitExplosionFlashDirect(level: Level, center: Vec3, radius: Float) {
        if (radius <= 0f) return
        val player = localPlayer ?: return
        if (player.distanceToSqr(center.x, center.y, center.z) > EXPLODE_CULL_SQ) return

        val lightRadius = (radius * 2.4f).coerceIn(6f, 80f)
        val duration = if (radius >= 5f) 6 else 4
        // 光源略高于爆炸中心，避免爆炸点嵌在地面里时被屏幕空间深度判定整片压暗。
        // 阴影步进厚度沿用枪口焰默认值（WorldLightHandler 默认 0.65）。
        WorldLightHandler.put(
            BlockPos.containing(center.x, center.y, center.z), center.add(0.0, 0.5, 0.0), Vec3.ZERO,
            strength = 1f,
            radius = lightRadius,
            durationTicks = duration,
            color = WorldLightHandler.explosionColor(),
        )
    }

    @JvmStatic
    fun clear(entity: Entity) {
        WorldLightHandler.remove(entity)
        WorldLightHandler.remove(engineKey(entity))
    }
}
