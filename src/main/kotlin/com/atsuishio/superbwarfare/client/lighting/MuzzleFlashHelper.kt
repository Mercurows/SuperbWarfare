package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.clientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/**
 * Calculates and spawns muzzle flashlight sources.
 *
 * Uses exactly two block-light nodes per shot:
 *
 * - A bright node at the muzzle tip (0.3 blocks forward).</li>
 * - A dimmer node further along the barrel (2.0 blocks forward).</li>
 *
 * Both nodes share the same TTL so neither outlasts the other —
 * eliminating the "leftover light fragment" artifact caused by
 * mismatched lifetimes.  No rings, no chains, no gradients.
 *
 * @author paralax034
 * @since 0.8.9.1
 */
@OnlyIn(Dist.CLIENT)
object MuzzleFlashHelper {

    // -------------------------------------------------------------------------
    // Data
    // -------------------------------------------------------------------------

    /**
     * Parameters for a single muzzle flash event.
     *
     * @param maxLevel  peak light level (1–15)
     * @param minLevel  minimum level at expiry (≥1)
     * @param duration  lifetime in client ticks (shared by both nodes)
     */
    data class FlashParams(val maxLevel: Int, val minLevel: Int, val duration: Int)

    /**
     * Forward offset of the bright near-node along the barrel direction (blocks).
     * Placed just past the muzzle so it illuminates the area directly in front.
     */
    private const val NEAR_OFFSET = 0.3

    /**
     * Forward offset of the dim far-node (blocks).
     * Provides a hint of depth without creating visible light fragments.
     */
    private const val FAR_OFFSET = 2.0

    /**
     * Intensity multiplier for the far node relative to [FlashParams.maxLevel].
     * Kept low (0.35) so the far node is barely visible — its only job is to
     * slightly extend the perceived cone; it must not outlast the near node.
     */
    private const val FAR_MULT = 0.35

    /** Items that produce no muzzle flash at all. */
    private val NO_FLASH_ITEMS = setOf(
        ModItems.BOCEK,
        ModItems.TASER,
        ModItems.REPAIR_TOOL
    )

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Spawns two block-light nodes in front of the muzzle along the barrel axis.
     *
     * <p>Both nodes expire on the same tick, preventing staggered-fade artifacts.
     * Suppressed weapons (maxLevel &lt; 5) emit only the near node to avoid
     * any visible flash.
     *
     * @param origin    world-space muzzle tip position
     * @param direction barrel direction vector (does not need to be normalized)
     * @param params    flash intensity and duration
     */
    @JvmStatic
    fun spawnFlashCone(origin: Vec3, direction: Vec3, params: FlashParams) {
        if (!DisplayConfig.ENABLE_FIRE_FLASH_LIGHT.get()) return
        if (params.maxLevel <= 0) return

        val level = clientLevel ?: return
        val engine = level.lightEngine
        val dir = direction.normalize()

        // --- Node 1: near (bright) ---
        val nearPoint = origin.add(dir.scale(NEAR_OFFSET))
        val nearBp = BlockPos.containing(nearPoint.x, nearPoint.y, nearPoint.z)
        LightPositionRegistry.putSpark(nearBp.asLong(), params.maxLevel, params.minLevel, params.duration)
        engine.checkBlock(nearBp)

        // Skip far node for silenced / very dim weapons
        if (params.maxLevel < 5) return

        // --- Node 2: far (dim) ---
        val farPoint = origin.add(dir.scale(FAR_OFFSET))
        val farBp = BlockPos.containing(farPoint.x, farPoint.y, farPoint.z)
        val farMax = (params.maxLevel * FAR_MULT).toInt().coerceAtLeast(1)
        val farMin = (params.minLevel * FAR_MULT).toInt().coerceAtLeast(1)
        // Same TTL as near node — they expire together, no stray fragments
        LightPositionRegistry.putSpark(farBp.asLong(), farMax, farMin, params.duration)
        engine.checkBlock(farBp)
    }

    // -------------------------------------------------------------------------
    // Flash parameter calculation
    // -------------------------------------------------------------------------

    /**
     * Derives [FlashParams] directly from a weapon's live [GunData].
     *
     * @param data live GunData for the currently held weapon
     * @return computed flash parameters
     */
    @JvmStatic
    fun calculateFromGunData(data: GunData): FlashParams {
        val barrelType = data.attachment.get(AttachmentType.BARREL)
        return calculateFromStats(
            damage = data.get(GunProp.DAMAGE),
            rpm = data.get(GunProp.RPM),
            boltAction = data.get(GunProp.BOLT_ACTION_TIME),
            isSilenced = barrelType == 2,
            isFlashHider = barrelType == 1
        )
    }

    /**
     * Derives [FlashParams] from raw weapon statistics.
     *
     * @param damage       base damage per shot
     * @param rpm          rounds per minute
     * @param boltAction   bolt-action delay ticks (> 0 = bolt-action weapon)
     * @param isSilenced   true if a suppressor is attached
     * @param isFlashHider true if a flash hider is attached
     * @return computed flash parameters
     */
    @JvmStatic
    fun calculateFromStats(
        damage: Double,
        rpm: Int,
        boltAction: Int,
        isSilenced: Boolean,
        isFlashHider: Boolean
    ): FlashParams {
        var maxLevel: Int
        var minLevel: Int
        var duration: Int

        // Base tiers derived from damage
        when {
            damage >= 30 -> {
                maxLevel = 15; minLevel = 9; duration = 4
            }

            damage >= 15 -> {
                maxLevel = 12; minLevel = 7; duration = 3
            }

            damage >= 8 -> {
                maxLevel = 9; minLevel = 4; duration = 3
            }

            else -> {
                maxLevel = 7; minLevel = 3; duration = 2
            }
        }

        // Bolt-action: slower cycle → slightly shorter flash
        if (boltAction > 0) {
            maxLevel = maxLevel.coerceAtMost(11)
            minLevel = minLevel.coerceAtMost(6)
            duration = (duration - 1).coerceAtLeast(2)
        }

        // Barrel attachments
        when {
            isSilenced -> {
                maxLevel = maxLevel.coerceAtMost(4); minLevel = 1; duration = 1
            }

            isFlashHider -> {
                maxLevel = (maxLevel * 2 / 3).coerceAtLeast(3)
                minLevel = (minLevel / 2).coerceAtLeast(1)
            }
        }

        // High RPM: shorten per-shot flash to reduce stacking at full auto
        if (rpm > 600 && !isSilenced) {
            duration = (duration - 1).coerceAtLeast(1)
        }

        return FlashParams(maxLevel, minLevel, duration)
    }

    /**
     * Calculates [FlashParams] from an [ItemStack]'s weapon data.
     *
     * @param stack the held weapon stack
     * @return flash parameters, or {@code null} if this item produces no flash
     */
    @JvmStatic
    fun calculateFromStack(stack: ItemStack): FlashParams? {
        if (stack.item !is GunItem) return null
        if (NO_FLASH_ITEMS.any { stack.`is`(it.get()) }) return null
        return calculateFromGunData(GunData.from(stack))
    }

    /**
     * Convenience overload — derives [FlashParams] from an entity's main-hand item.
     *
     * @param owner the entity holding the weapon
     * @return flash parameters, or {@code null} if no flash should be produced
     */
    @JvmStatic
    fun calculateFromOwner(owner: LivingEntity): FlashParams? = calculateFromStack(owner.mainHandItem)
}
