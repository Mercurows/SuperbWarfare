package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.clientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.concurrent.ThreadLocalRandom

/**
 * Calculates and spawns muzzle flashlight sources for small arms.
 *
 * Features randomized muzzle offsets, per-shot intensity scaling, and realistic suppressor attenuation.
 *
 * @author paralax034
 * @since 0.8.9.1
 */
@OnlyIn(Dist.CLIENT)
object MuzzleFlashHelper {

    // -----------------------------
    // Data
    // -----------------------------

    /**
     * Parameters for a single muzzle flash event.
     *
     * @param maxLevel  peak light level (1–15)
     * @param minLevel  minimum level at expiry (≥1)
     * @param duration  lifetime in client ticks
     */
    data class FlashParams(val maxLevel: Int, val minLevel: Int, val duration: Int)

    /** Items that produce no muzzle flash at all. */
    private val NO_FLASH_ITEMS = setOf(
        ModItems.BOCEK
    )

    // -----------------------------
    // Public API
    // -----------------------------

    /**
     * Spawns a single block-light node in front of the muzzle along the barrel axis.
     *
     * Applies subtle spatial offset variance (0.35m..0.45m) and level variance per shot for visual uniqueness.
     *
     * @param origin    world-space muzzle tip position
     * @param direction barrel direction vector
     * @param params    flash intensity and duration
     */
    @JvmStatic
    fun spawnFlashCone(origin: Vec3, direction: Vec3, params: FlashParams) {
        if (!DisplayConfig.ENABLE_FIRE_FLASH_LIGHT.get()) return
        if (params.maxLevel <= 0) return

        val level = clientLevel ?: return
        val engine = level.lightEngine
        val dir = direction.normalize()

        val random = ThreadLocalRandom.current()

        val muzzleOffset = 0.35 + random.nextDouble() * 0.10
        val muzzlePoint = origin.add(dir.scale(muzzleOffset))
        val muzzleBp = BlockPos.containing(muzzlePoint.x, muzzlePoint.y, muzzlePoint.z)

        val juicedMax = (params.maxLevel + random.nextInt(3) - 1).coerceIn(1, 15)
        val juicedMin = (params.minLevel + random.nextInt(2) - 1).coerceIn(1, juicedMax)

        if (juicedMax >= 10) {
            LightPositionRegistry.putSparkRadius(muzzleBp, juicedMax, juicedMin, params.duration, radius = 1)
        } else {
            LightPositionRegistry.putSpark(muzzleBp.asLong(), juicedMax, juicedMin, params.duration)
        }
        engine.checkBlock(muzzleBp)
    }

    /**
     * Spawns flash lighting for raycast tools (such as RepairTool or Taser) that do not use standard projectiles.
     *
     * @param player the player holding the tool
     * @param stack  the tool item stack
     */
    @JvmStatic
    fun spawnToolFlash(player: Player, stack: ItemStack) {
        if (!stack.`is`(ModItems.REPAIR_TOOL.get()) && !stack.`is`(ModItems.TASER.get())) return
        val params = calculateFromStack(stack) ?: return
        spawnFlashCone(player.eyePosition, player.lookAngle, params)
    }

    // -----------------------------
    // Flash parameter calculation
    // -----------------------------

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
            isFlashHider = barrelType == 1,
            projectileAmount = data.get(GunProp.PROJECTILE_AMOUNT),
            stack = data.stack
        )
    }

    /**
     * Derives [FlashParams] from raw weapon statistics.
     *
     * Calibrated Tiers:
     * Multi-projectile (Shotguns/Mech): Level 9..10, 4 ticks (increased for chunk meshing).
     * Suppressed: Level 6..7, 2 ticks.
     * Flash Hider: Level 8..9, 2 ticks.
     * Assault Rifles: Level 9..10, 3-4 ticks.
     * High RPM: Floor level increased to prevent flickering.
     *
     * @param damage           base damage per shot
     * @param rpm              rounds per minute
     * @param boltAction       bolt-action delay ticks (> 0 = bolt-action weapon)
     * @param isSilenced       true if a suppressor is attached
     * @param isFlashHider     true if a flash hider is attached
     * @param projectileAmount number of projectiles fired per shot
     * @param stack            optional item stack for special tool checks
     * @return computed flash parameters
     */
    @JvmStatic
    fun calculateFromStats(
        damage: Double,
        rpm: Int,
        boltAction: Int,
        isSilenced: Boolean,
        isFlashHider: Boolean,
        projectileAmount: Int = 1,
        stack: ItemStack = ItemStack.EMPTY
    ): FlashParams {
        if (!stack.isEmpty) {
            if (stack.`is`(ModItems.REPAIR_TOOL.get())) {
                return FlashParams(8, 3, 4) // Bright welding arc spark
            }
            if (stack.`is`(ModItems.TASER.get())) {
                return FlashParams(7, 3, 2) // Electric arc pop
            }
        }

        var maxLevel: Int
        var minLevel = 1
        var duration: Int
        
        // Dynamic detection for shotguns and multi-barrel vehicle systems
        val isMultiProjectile = projectileAmount > 1

        when {
            isMultiProjectile -> {
                maxLevel = 9; duration = 4
            }

            damage >= 30.0 -> {
                maxLevel = 11; duration = 4
            }

            damage >= 15.0 -> {
                maxLevel = 7; duration = 4
            }

            damage >= 8.0 -> {
                maxLevel = 8; duration = 3
            }

            else -> {
                maxLevel = 6; duration = 3
            }
        }

        // Bolt action weapons have slightly reduced flash intensity to balance their high damage profile
        if (boltAction > 0 && !isMultiProjectile) {
            maxLevel = (maxLevel * 4 / 5).coerceAtLeast(6)
            duration = 4
        }

        // Attachments overrides
        when {
            isSilenced -> {
                maxLevel = 4; minLevel = 1; duration = 2
            }

            isFlashHider -> {
                maxLevel = (maxLevel * 6 / 10).coerceAtLeast(3)
                minLevel = 1; duration = 2
            }
        }

        // High RPM weapon logic (e.g., Miniguns, fast ARs)
        // Extremely fast firing rates cause harsh light flickering if the duration is too short 
        // or if it drops to 0 too fast. By increasing duration and raising the `minLevel` floor, 
        // we create a continuous "glowing barrel" effect instead of aggressive strobing.
        if (rpm > 600 && !isSilenced) {
            duration = 4
            if (rpm >= 800) {
                maxLevel = (maxLevel + 1).coerceAtMost(10)
                // Retain at least a level 3 base glow between ticks for smooth visual blending
                minLevel = (maxLevel / 2).coerceAtLeast(3)
            }
        }

        return FlashParams(maxLevel, minLevel, duration)
    }

    /**
     * Calculates [FlashParams] from an [ItemStack]'s weapon data.
     *
     * @param stack the held weapon stack
     * @return flash parameters, or null if this item produces no flash
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
     * @return flash parameters, or null if no flash should be produced
     */
    @JvmStatic
    fun calculateFromOwner(owner: LivingEntity): FlashParams? = calculateFromStack(owner.mainHandItem)
}