package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.weapon.BeastItem.Companion.beastKill
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage.Companion.sendToNearbyPlayers
import com.mojang.datafixers.util.Pair
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.ProtectionEnchantment
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.ExplosionDamageCalculator
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.ForgeEventFactory
import org.joml.Vector3d
import java.util.function.Supplier
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

open class CustomExplosion(
    private val level: Level,
    private val entity: Entity?,
    source: DamageSource?,
    damageCalculator: ExplosionDamageCalculator?,
    private val damage: Float,
    private val x: Double,
    private val y: Double,
    private val z: Double,
    private val radius: Float,
    blockInteraction: BlockInteraction
) : Explosion(
    level,
    entity, source, null,
    x,
    y,
    z,
    radius, false, blockInteraction
) {
    private val damageSource: DamageSource
    private val damageCalculator: ExplosionDamageCalculator
    private var fireTime = 0
    private var beast = false

    init {
        this.damageSource = source ?: level.damageSources().explosion(this)
        this.damageCalculator = damageCalculator ?: ExplosionDamageCalculator()
    }

    constructor(
        pLevel: Level,
        pSource: Entity?,
        damage: Float,
        pToBlowX: Double,
        pToBlowY: Double,
        pToBlowZ: Double,
        pRadius: Float,
        pBlockInteraction: BlockInteraction
    ) : this(pLevel, pSource, null, null, damage, pToBlowX, pToBlowY, pToBlowZ, pRadius, pBlockInteraction)

    constructor(
        pLevel: Level,
        pSource: Entity?,
        source: DamageSource?,
        damage: Float,
        pToBlowX: Double,
        pToBlowY: Double,
        pToBlowZ: Double,
        pRadius: Float,
        pBlockInteraction: BlockInteraction
    ) : this(pLevel, pSource, source, null, damage, pToBlowX, pToBlowY, pToBlowZ, pRadius, pBlockInteraction) {
        sendToNearbyPlayers(
            level,
            pToBlowX,
            pToBlowY,
            pToBlowZ,
            (4 * radius.coerceAtMost(50f)).toDouble(),
            20 + 0.2 * radius.coerceAtMost(50f),
            50 + 0.5 * radius.coerceAtMost(50f)
        )
    }

    constructor(
        pLevel: Level,
        pSource: Entity?,
        source: DamageSource?,
        damage: Float,
        pToBlowX: Double,
        pToBlowY: Double,
        pToBlowZ: Double,
        pRadius: Float
    ) : this(pLevel, pSource, source, null, damage, pToBlowX, pToBlowY, pToBlowZ, pRadius, BlockInteraction.KEEP) {
        sendToNearbyPlayers(level, pToBlowX, pToBlowY, pToBlowZ, radius.coerceAtMost(50f).toDouble(), 5 + 0.2 * radius.coerceAtMost(50f), 2 + 0.02 * radius.coerceAtMost(50f))
    }

    fun setFireTime(fireTime: Int): CustomExplosion {
        this.fireTime = fireTime
        return this
    }

    fun setBeast(flag: Boolean): CustomExplosion {
        this.beast = flag
        return this
    }

    override fun explode() {
        if (ExplosionConfig.EXPLOSION_DESTROY.get()) {
            this.level.gameEvent(this.entity, GameEvent.EXPLODE, Vec3(this.x, this.y, this.z))

            val center = Vec3(this.x, this.y, this.z)
            val random = level.random

            // ================================================================
            // Pre-compute decreasing tier boundaries to keep block count per
            // tick balanced. Outer shells have 4πr² more volume, so they need
            // smaller tier sizes. Tier sizes: 25, 23, 21, …, min 5.
            // ================================================================
            val initialTierSize = 25.0
            val tierDecrease = 2.0
            val minTierSize = 2.0

            val tierBoundaries = mutableListOf(0.0)
            var currentBoundary = 0.0
            var currentSize = initialTierSize
            while (currentBoundary < radius * 2.0) {
                currentBoundary += currentSize
                tierBoundaries.add(currentBoundary)
                currentSize = (currentSize - tierDecrease).coerceAtLeast(minTierSize)
            }

            // ================================================================
            // Compute shared search parameters once (same for all tiers).
            // ================================================================
            val aabb = AABB(
                x - 0.6 * radius,
                y - 0.3 * radius,
                z - 0.6 * radius,
                x + 0.6 * radius,
                y + 0.3 * radius,
                z + 0.6 * radius
            )
            val minPos = BlockPos(
                floor(aabb.minX).toInt(),
                floor(aabb.minY).toInt(),
                floor(aabb.minZ).toInt()
            )
            val maxPos = BlockPos(
                floor(aabb.maxX).toInt(),
                floor(aabb.maxY).toInt(),
                floor(aabb.maxZ).toInt()
            )

            val maxEffectiveRadius = 0.4 * radius + 0.5 * radius * 0.2
            val maxFlattenedRadius = maxEffectiveRadius * 1.2f

            val numRays = 32 + random.nextInt(17)
            val coreRadius = 0.4f * radius
            val flattenedCoreRadius = coreRadius * 1.2f
            val beltHalfHeight = (radius * 0.34).toInt().coerceAtLeast(1)
            val beltYMin = (floor(center.y) - beltHalfHeight).toInt()
            val beltYMax = (floor(center.y) + beltHalfHeight).toInt()

            val numTiers = tierBoundaries.size - 1

            // ================================================================
            // Process each tier: search → filter → destroy → clear toBlow.
            // Tier 0 runs immediately, tier N is delayed by N ticks.
            // Each tier only searches within its own distance ring to avoid
            // scanning the full AABB on a single tick.
            // ================================================================
            for (tier in 0 until numTiers) {
                val minDist = tierBoundaries[tier]
                val maxDist = tierBoundaries[tier + 1]

                val task = Runnable {
                    // ---- Search this tier's distance ring ----
                    val candidates = hashSetOf<BlockPos>()

                    // AABB sweep: only collect blocks whose Euclidean distance is in [minDist, maxDist)
                    BlockPos.betweenClosedStream(minPos, maxPos).forEach { blockpos ->
                        val dx = (blockpos.center.x - center.x).toFloat()
                        val dy = (blockpos.center.y - center.y).toFloat()
                        val dz = (blockpos.center.z - center.z).toFloat()
                        val flattenedDistSqr = (dx * dx + dz * dz) + dy * dy * 3.0f
                        val distSqr = dx * dx + dy * dy + dz * dz

                        if (level.isInWorldBounds(blockpos)
                            && distSqr >= minDist * minDist
                            && distSqr < maxDist * maxDist
                            && flattenedDistSqr <= maxFlattenedRadius * maxFlattenedRadius
                        ) {
                            candidates.add(blockpos.immutable())
                        }
                    }

                    // Radial spikes: only search where distance is in [minDist, maxDist)
                    for (r in 0 until numRays) {
                        val angle = 2.0 * Math.PI * r / numRays + (random.nextDouble() - 0.5) * 0.25
                        val spikeLength = flattenedCoreRadius * (1.0f + random.nextFloat() * 1.1f)

                        val dx = cos(angle)
                        val dz = sin(angle)

                        var dist = (flattenedCoreRadius * 0.35f).coerceAtLeast(minDist.toFloat())
                        while (dist < spikeLength && dist < maxDist) {
                            val bx = floor(center.x + dx * dist).toInt()
                            val bz = floor(center.z + dz * dist).toInt()

                            for (dy in beltYMin..beltYMax) {
                                val blockpos = BlockPos(bx, dy, bz)

                                if (!level.isInWorldBounds(blockpos) || blockpos in candidates) continue

                                val fdx = (blockpos.center.x - center.x).toFloat()
                                val fdy = (blockpos.center.y - center.y).toFloat()
                                val fdz = (blockpos.center.z - center.z).toFloat()
                                val flattenedDistSqr = (fdx * fdx + fdz * fdz) + fdy * fdy * 3.0f
                                if (flattenedDistSqr > spikeLength * spikeLength) continue

                                candidates.add(blockpos.immutable())
                            }

                            dist += 1.2f
                        }
                    }

                    // ---- Filter and destroy ----
                    val qualified = mutableListOf<BlockPos>()

                    for (blockpos in candidates) {
                        var effectiveRadius = 0.4 * radius
                        val dx = (blockpos.center.x - center.x).toFloat()
                        val dy = (blockpos.center.y - center.y).toFloat()
                        val dz = (blockpos.center.z - center.z).toFloat()
                        val flattenedDistSqr = (dx * dx + dz * dz) + dy * dy * 3.0f
                        val distanceSqr = dx * dx + dy * dy + dz * dz
                        var force = this@CustomExplosion.radius * (0.25f + random.nextFloat() * 0.15f) * 0.02f * damage

                        if (distanceSqr > radius * radius * 0.15) {
                            effectiveRadius += (random.nextDouble() - 0.5) * radius * 0.2
                        }
                        val flattenedRadius = effectiveRadius * 1.2f
                        if (flattenedDistSqr > flattenedRadius * flattenedRadius) continue

                        val blockState = this@CustomExplosion.level.getBlockState(blockpos)
                        var resistance = blockState.block.defaultDestroyTime()
                        if (blockState.soundType === SoundType.METAL || blockState.soundType === SoundType.COPPER || blockState.soundType === SoundType.NETHERITE_BLOCK) {
                            resistance *= 3f
                        }
                        force *= ((1f - (flattenedDistSqr / (flattenedRadius * flattenedRadius))).coerceIn(0.0, 1.0)).toFloat()

                        if (resistance != -1f && force > resistance && this@CustomExplosion.damageCalculator.shouldBlockExplode(
                                this@CustomExplosion, this@CustomExplosion.level, blockpos, blockState, force)
                        ) {
                            this@CustomExplosion.toBlow.add(blockpos.immutable())
                            qualified.add(blockpos.immutable())
                        }
                    }

                    // Destroy qualified blocks for this tier
                    processBlockList(qualified)

                    // Clear toBlow so the next tier starts fresh
                    this@CustomExplosion.toBlow.clear()
                }

                if (tier <= 0) {
                    task.run()
                } else {
                    Mod.queueServerWork(tier, task)
                }
            }
        }

        val diameter = this.radius * 2
        val x0 = Mth.floor(this.x - diameter.toDouble() - 1)
        val x1 = Mth.floor(this.x + diameter.toDouble() + 1)
        val y0 = Mth.floor(this.y - diameter.toDouble() - 1)
        val y1 = Mth.floor(this.y + diameter.toDouble() + 1)
        val z0 = Mth.floor(this.z - diameter.toDouble() - 1)
        val z1 = Mth.floor(this.z + diameter.toDouble() + 1)
        val list = this.level.getEntities(
            this.entity,
            AABB(x0.toDouble(), y0.toDouble(), z0.toDouble(), x1.toDouble(), y1.toDouble(), z1.toDouble())
        )
        ForgeEventFactory.onExplosionDetonate(this.level, this, list, diameter.toDouble())
        val position = Vec3(this.x, this.y, this.z)

        var hit = false

        for (entity in list) {
            if (!entity.ignoreExplosion()) {
                val distanceRate = sqrt(entity.distanceToSqr(position)) / diameter.toDouble()
                if (distanceRate <= 1) {
                    val xDistance = entity.x - this.x
                    val yDistance = (if (entity is PrimedTnt) entity.y else entity.eyeY) - this.y
                    val zDistance = entity.z - this.z
                    val distance = sqrt(xDistance * xDistance + yDistance * yDistance + zDistance * zDistance)

                    if (distance != 0.0) {
                        // VehicleEntity handled by LivingEventHandler.onExplosionDetonate
                        if (entity is VehicleEntity) continue

                        val seenPercent =
                            getSeenPercent(position, entity).toDouble().coerceIn(
                                0.01 * ExplosionConfig.EXPLOSION_PENETRATION_RATIO.get(),
                                Double.POSITIVE_INFINITY
                            )
                        val damagePercent = (1 - distanceRate) * seenPercent
                        val damageFinal = (damagePercent * damagePercent + damagePercent) / 2 * damage

                        // Calculate shockwave delay based on distance and speed
                        val shockwaveDelay = (distance / 340 * 20).toInt().coerceAtMost(100)

                        // Set hit flag immediately for player feedback
                        if (entity is LivingEntity || entity is VehicleEntity) {
                            hit = true
                        }

                        // Capture computed values for delayed application
                        val capturedDamageFinal = damageFinal
                        val capturedDamageSource = this.damageSource
                        val capturedFireTime = this.fireTime

                        // Compute knockback force at explosion time
                        val knockbackForce = if (entity is LivingEntity) {
                            var force = damageFinal * 0.015
                            force = ProtectionEnchantment.getExplosionKnockbackAfterDampener(entity, force)
                            val vec31 = position.vectorTo(entity.boundingBox.center).normalize()
                            force to vec31
                        } else {
                            null
                        }

                        val applyShockwaveDamage = Runnable {
                            if (!entity.isRemoved) {
                                entity.forceHurt(capturedDamageSource, capturedDamageFinal.toFloat())

                                if (this.beast && entity != this.damageSource.entity) {
                                    beastKill(this.damageSource.entity, entity)
                                }

                                if (knockbackForce != null && entity is LivingEntity) {
                                    var (force, vec31) = knockbackForce

                                    force = force.coerceAtLeast(0.0)
                                    if (force > 0.0) {
                                        if (entity is Player && !entity.isCreative && !entity.isSpectator) {
                                            entity.deltaMovement = entity.deltaMovement.add(vec31.scale(force))
                                        } else {
                                            entity.deltaMovement = entity.deltaMovement.add(vec31.scale(force))
                                        }
                                    }
                                }

                                entity.invulnerableTime = 1

                                if (capturedFireTime > 0) {
                                    entity.setSecondsOnFire(capturedFireTime)
                                }
                            }
                        }

                        if (shockwaveDelay <= 0) {
                            applyShockwaveDamage.run()
                        } else {
                            Mod.queueServerWork(shockwaveDelay, applyShockwaveDamage)
                        }
                    }
                }
            }
        }

        if (hit) {
            val player = this.damageSource.entity
            if (player is ServerPlayer) {
                SoundTool.playLocalSound(player, ModSounds.INDICATION.get())
                player.sendPacket(ClientIndicatorMessage(0, 5))
            }
        }
    }

    override fun finalizeExplosion(pSpawnParticles: Boolean) {
        if (this.level.isClientSide) {
            this.level.playLocalSound(
                this.x,
                this.y,
                this.z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS,
                4.0f,
                (1.0f + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2f) * 0.7f,
                false
            )
        }

        val flag = this.interactsWithBlocks()
        if (pSpawnParticles) {
            if (!(this.radius < 2.0f) && flag) {
                this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.x, this.y, this.z, 1.0, 0.0, 0.0)
            } else {
                this.level.addParticle(ParticleTypes.EXPLOSION, this.x, this.y, this.z, 1.0, 0.0, 0.0)
            }
        }

        if (flag) {
            val blowList = this.toBlow.stream().filter { !this.level.getBlockState(it).isAir }.toList()
            processBlockList(blowList)
        }
    }

    /**
     * Destroy the given blocks (mark as exploded, spawn drops) and drop their loot.
     * Called both by [finalizeExplosion] for tier-0 blocks and by delayed tasks for outer tiers.
     */
    private fun processBlockList(blocks: Collection<BlockPos>) {
        val flag1 = this.indirectSourceEntity is Player
        val dropList = ObjectArrayList<Pair<ItemStack, BlockPos>>()

        this.level.profiler.push("explosion_blocks")
        for (blockpos in blocks) {
            if (this.level.getBlockState(blockpos).isAir) continue

            val blockstate = this.level.getBlockState(blockpos)
            val blockpos1 = blockpos.immutable()

            if (this.level is ServerLevel) {
                val blockEntity = if (blockstate.hasBlockEntity()) this.level.getBlockEntity(blockpos) else null
                val lootParamsBuilder = LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockpos))
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, this.entity)
                    .withParameter(LootContextParams.EXPLOSION_RADIUS, this.radius)

                blockstate.spawnAfterBreak(level, blockpos, ItemStack.EMPTY, flag1)
                blockstate.getDrops(lootParamsBuilder).forEach {
                    addBlockDrops(dropList, it, blockpos1)
                }
            }

            blockstate.onBlockExploded(this.level, blockpos, this)
        }

        for (pair in dropList) {
            Block.popResource(this.level, pair.getSecond(), pair.getFirst())
        }

        this.level.profiler.pop()
    }

    class Builder(private var directSource: Entity) {
        private val level: Level = directSource.level()
        private var sourceEntity: Entity?
        private var attackerEntity: Entity?
        private var damage = 0f
        private var radius = 0f
        private var particleType: ParticleTool.ParticleType? = null
        private var destroyBlock: Supplier<BlockInteraction> =
            Supplier { if (ExplosionConfig.EXPLOSION_DESTROY.get()) BlockInteraction.DESTROY else BlockInteraction.KEEP }
        private var fireTime = 0
        private var damageSource: DamageSource? = null
        private var particlePosition: Vec3? = null
        private var beast = false
        var position: Vec3

        init {
            this.sourceEntity = directSource
            this.attackerEntity = directSource
            this.position = directSource.boundingBox.center
        }

        fun directSource(directSource: Entity): Builder {
            this.directSource = directSource
            return this
        }

        fun source(source: Entity?): Builder {
            this.sourceEntity = source
            return this
        }

        fun attacker(attacker: Entity?): Builder {
            this.attackerEntity = attacker
            return this
        }

        fun damage(damage: Float): Builder {
            this.damage = damage
            return this
        }

        fun radius(radius: Float): Builder {
            this.radius = radius
            return this
        }

        fun withParticleType(particleType: ParticleTool.ParticleType): Builder {
            this.particleType = particleType
            return this
        }

        fun destroyBlock(destroyBlock: Supplier<BlockInteraction>): Builder {
            this.destroyBlock = destroyBlock
            return this
        }

        fun keepBlock(): Builder {
            this.destroyBlock = Supplier { BlockInteraction.KEEP }
            return this
        }

        fun fireTime(fireTime: Int): Builder {
            this.fireTime = fireTime
            return this
        }

        fun damageSource(damageSource: DamageSource?): Builder {
            this.damageSource = damageSource
            return this
        }

        fun position(position: Vec3): Builder {
            this.position = position
            return this
        }

        fun particlePosition(particlePosition: Vec3?): Builder {
            this.particlePosition = particlePosition
            return this
        }

        fun beast(flag: Boolean): Builder {
            this.beast = flag
            return this
        }

        fun explode() {
            if (level.isClientSide) return

            val source =
                (if (this.damageSource != null) this.damageSource else ModDamageTypes.causeCustomExplosionDamage(
                    level.registryAccess(),
                    sourceEntity,
                    attackerEntity
                ))!!

            val customExplosion = CustomExplosion(
                level, directSource,
                source, damage,
                position.x, position.y, position.z, radius, destroyBlock.get()
            )
                .setFireTime(fireTime)
                .setBeast(beast)

            customExplosion.explode()
            ForgeEventFactory.onExplosionStart(directSource.level(), customExplosion)
            customExplosion.finalizeExplosion(false)

            // Auto-detect particle type from radius if not explicitly set
            val type = particleType ?: ParticleTool.particleTypeForRadius(radius)
            ParticleTool.spawnExplosionParticles(
                type,
                directSource.level(),
                if (particlePosition != null) particlePosition!! else position
            )
        }
    }

    companion object {
        /**
         * Replacement for [Explosion.getSeenPercent] that samples points on the
         * actual OBB surfaces instead of a dense grid over the vanilla AABB.
         *
         * Uses the OBB center + 6 face centres per OBB (7 points each).
         * For AC-130H: 23 OBBs × 7 = 161 raycasts, vs 118 000 vanilla.
         */
        @JvmStatic
        fun getSeenPercentOptimized(level: Level, center: Vec3, entity: Entity): Float {
            if (entity is OBBEntity && !entity.enableAABB()) {
                return getSeenPercentForOBB(level, center, entity)
            }
            return Explosion.getSeenPercent(center, entity)
        }

        /** Sample every OBB centre + 6 face centres, raycast from [center] to each. */
        @JvmStatic
        fun getSeenPercentForOBB(level: Level, center: Vec3, obbEntity: OBBEntity): Float {
            var hits = 0
            var total = 0
            val tmp = Vector3d()

            for (obb in obbEntity.getOBBs()) {
                val axes = obb.getAxes() // fresh copy each iteration
                val e = obb.extents
                val c = obb.center

                // 7 samples per OBB: centre + face centres on ±X, ±Y, ±Z
                for (axisI in 0..2) {
                    for (sign in listOf(0.0, 1.0, -1.0)) {
                        if (sign == 0.0 && axisI > 0) continue // centre only once
                        val offset = axes[axisI].mul(sign * e.get(axisI), tmp)
                        val point = OBB.vector3dToVec3(Vector3d(c).add(offset))
                        if (level.clip(ClipContext(center, point,
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null)
                            ).type == HitResult.Type.MISS
                        ) hits++
                        total++
                    }
                }
            }
            return if (total > 0) hits.toFloat() / total else 0f
        }

        @JvmStatic
        fun addBlockDrops(
            pDropPositionArray: ObjectArrayList<Pair<ItemStack, BlockPos>>,
            pStack: ItemStack,
            pPos: BlockPos
        ) {
            val i = pDropPositionArray.size

            for (j in 0..<i) {
                val pair = pDropPositionArray[j]
                val itemstack = pair.getFirst()
                if (ItemEntity.areMergable(itemstack, pStack)) {
                    val itemstack1 = ItemEntity.merge(itemstack, pStack, 16)
                    pDropPositionArray[j] = Pair.of(itemstack1, pair.getSecond())
                    if (pStack.isEmpty) {
                        return
                    }
                }
            }

            pDropPositionArray.add(Pair.of(pStack, pPos))
        }
    }
}
