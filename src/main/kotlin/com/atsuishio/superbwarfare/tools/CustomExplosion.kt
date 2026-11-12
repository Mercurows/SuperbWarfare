package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.compat.valkyrienskies.ValkyrienSkiesCompat
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
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.Clearable
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
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.ForgeEventFactory
import net.minecraftforge.registries.ForgeRegistries
import org.joml.Vector3d
import java.util.*
import java.util.function.Supplier
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

open class CustomExplosion(
    private val level: Level,
    private val entity: Entity?,
    source: DamageSource?,
    damageCalculator: ExplosionDamageCalculator?,
    val damage: Float,
    val x: Double,
    val y: Double,
    val z: Double,
    val radius: Float,
    private val blockInteraction: BlockInteraction
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
    private var excludeDirectSource = true
    private var sphericalBlockDestruction = false
    private var dropBlocks = true
    private var blockDestructionRadius: Float? = null
    private var blockDestructionDurationTicks: Int? = null
    private var deferredBlockDrops: ObjectArrayList<Pair<ItemStack, BlockPos>>? = null

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
        sendToNearbyPlayers(
            level,
            pToBlowX,
            pToBlowY,
            pToBlowZ,
            radius.coerceAtMost(50f).toDouble(),
            5 + 0.2 * radius.coerceAtMost(50f),
            2 + 0.02 * radius.coerceAtMost(50f)
        )
    }

    fun setFireTime(fireTime: Int): CustomExplosion {
        this.fireTime = fireTime
        return this
    }

    fun setBeast(flag: Boolean): CustomExplosion {
        this.beast = flag
        return this
    }

    /** 取伤害目标时是否排除 [entity]（默认排除） */
    fun setExcludeDirectSource(flag: Boolean): CustomExplosion {
        this.excludeDirectSource = flag
        return this
    }

    /** Use the same block destruction reach vertically and horizontally instead of a flattened crater. */
    fun setSphericalBlockDestruction(flag: Boolean): CustomExplosion {
        this.sphericalBlockDestruction = flag
        return this
    }

    /** Controls block loot, container contents and block experience; destruction still runs normally. */
    fun setDropBlocks(flag: Boolean): CustomExplosion {
        this.dropBlocks = flag
        return this
    }

    /** Explicit horizontal block reach, independent of the legacy crater scale and entity-damage radius. */
    fun setBlockDestructionRadius(radius: Float): CustomExplosion {
        require(radius.isFinite() && radius > 0f)
        this.blockDestructionRadius = radius
        return this
    }

    /** Wave-front travel time, not a deadline that discards unfinished block work. */
    fun setBlockDestructionDuration(ticks: Int): CustomExplosion {
        require(ticks > 0)
        blockDestructionDurationTicks = ticks
        return this
    }

    override fun explode() {
        this.level.gameEvent(this.entity, GameEvent.EXPLODE, Vec3(this.x, this.y, this.z))
        val drops = ObjectArrayList<Pair<ItemStack, BlockPos>>()
        deferredBlockDrops = drops
        try {
            if (ExplosionConfig.EXPLOSION_DESTROY.get() && this.interactsWithBlocks()) {
                explodeBlocks()
            }
            damageEntities()
        } finally {
            // Do not let this explosion's entity damage destroy the loot it just generated.
            deferredBlockDrops = null
            for (pair in drops) {
                Block.popResource(this.level, pair.second, pair.first)
            }
        }
    }

    /**
     * 分层搜索并清除范围内的方块。
     *
     * 从 [explode] 拆出来是为了让"是否允许破坏方块"只在一处判断：
     * 调用方已经确认过 [interactsWithBlocks]。
     */
    open fun explodeBlocks() {
        if (!radius.isFinite() || radius <= 0f) return
        val serverLevel = level as? ServerLevel ?: return
        val verticalScale = if (sphericalBlockDestruction) 1.0 else 3.0
        val reach = blockDestructionRadius?.toDouble() ?: (0.6 * radius)
        val verticalReach = if (blockDestructionRadius != null || sphericalBlockDestruction)
            reach / sqrt(verticalScale) else 0.3 * radius
        val cursor = ExplosionBlockCursor(
            x, y, z, reach, 1.0,
            maxOf(level.minBuildHeight, ceil(y - verticalReach - 0.5 - 1e-9).toInt()),
            minOf(level.maxBuildHeight - 1, floor(y + verticalReach - 0.5 + 1e-9).toInt())
        )
        val aabb = AABB(x - reach, y - verticalReach, z - reach, x + reach, y + verticalReach, z + reach)
        // Resolve physics-body transforms on the worker's first tick, not on the detonation call stack.
        val ships by lazy {
            if (ValkyrienSkiesCompat.hasMod()) ValkyrienSkiesCompat.ShipTransformCache.create(level, aabb) else null
        }
        val random = level.random
        val startTick = level.gameTime
        val travelTicks = blockDestructionDurationTicks ?: ceil(reach).toInt().coerceAtLeast(1)
        val task = object : Runnable {
            private var lastTick = Long.MIN_VALUE

            override fun run() {
                if (serverLevel.server.isStopped) return
                // A continuation may never consume another batch in the same world tick.
                if (lastTick == level.gameTime) {
                    Mod.queueServerWork(1, this)
                    return
                }
                lastTick = level.gameTime
                val started = System.nanoTime()
                val budgetNanos = if (blockDestructionDurationTicks != null) {
                    // Use spare server tick time, without turning a slow tick into an unbounded catch-up burst.
                    ((40.0 - serverLevel.server.averageTickTime).coerceIn(1.0, 6.0) * 1_000_000).toLong()
                } else 3_000_000L
                var destroyed = 0
                var budgetChecks = 0
                val batch = ArrayList<BlockPos>(8)
                var chunkX = Int.MIN_VALUE
                var chunkZ = Int.MIN_VALUE
                var chunk: LevelChunk? = null
                fun flush() {
                    if (batch.isNotEmpty()) {
                        processBlockList(batch)
                        batch.clear()
                    }
                }
                val elapsed = (level.gameTime - startTick + 1).coerceAtLeast(1)
                val frontier = (reach * elapsed / travelTicks).coerceAtMost(reach)
                val maxShell = (ceil(frontier).toInt() - 1).coerceAtLeast(0)
                val complete = cursor.visit(
                    maxSteps = 65536,
                    maxShell = maxShell,
                    shouldYield = {
                        destroyed >= 4096 || (++budgetChecks and 15 == 0 && System.nanoTime() - started >= budgetNanos)
                    }
                ) { sample ->
                    val dy = sample.y + 0.5 - y
                    val blockDistanceSquared = sample.distanceSquared + (verticalScale - 1.0) * dy * dy
                    val effectiveRadius = blockDestructionRadius?.toDouble() ?: run {
                        var legacy = 0.4 * radius
                        if (sample.distanceSquared > radius * radius * 0.15) {
                            legacy += (random.nextDouble() - 0.5) * radius * 0.2
                        }
                        legacy * 1.2
                    }
                    if (blockDistanceSquared >= effectiveRadius * effectiveRadius) return@visit
                    val worldPos = BlockPos(sample.x, sample.y, sample.z)
                    if (!level.isInWorldBounds(worldPos)) return@visit
                    val actualPos = ships?.toShipSpace(worldPos) ?: worldPos
                    if (level.isOutsideBuildHeight(actualPos)) return@visit
                    // An explosion must not synchronously generate/load every chunk in a huge radius.
                    val cx = actualPos.x shr 4
                    val cz = actualPos.z shr 4
                    if (cx != chunkX || cz != chunkZ) {
                        chunkX = cx
                        chunkZ = cz
                        chunk = serverLevel.chunkSource.getChunkNow(cx, cz)
                    }
                    val loadedChunk = chunk ?: return@visit
                    if (actualPos == worldPos && loadedChunk.getSection(loadedChunk.getSectionIndex(actualPos.y)).hasOnlyAir()) {
                        cursor.skipZSection()
                        return@visit
                    }
                    val state = loadedChunk.getBlockState(actualPos)
                    if (state.isAir) return@visit
                    var resistance = state.block.defaultDestroyTime()
                    if (resistance < 0f) return@visit
                    if (state.soundType === SoundType.METAL || state.soundType === SoundType.COPPER ||
                        state.soundType === SoundType.NETHERITE_BLOCK) resistance *= 3f
                    val falloff = (1.0 - blockDistanceSquared / (effectiveRadius * effectiveRadius)).coerceIn(0.0, 1.0)
                    val force = (radius * (0.25f + random.nextFloat() * 0.15f) * 0.02f * damage * falloff).toFloat()
                    if (force > resistance && damageCalculator.shouldBlockExplode(
                            this@CustomExplosion, level, actualPos, state, force
                        )) {
                        batch.add(actualPos.immutable())
                        destroyed++
                        if (batch.size >= 8) flush()
                    }
                }
                flush()
                if (!complete) Mod.queueServerWork(1, this)
            }
        }
        // Even the innermost shell is deferred: visuals and damage do not wait for a volume scan.
        Mod.queueServerWork(1, task)
    }

    /** 范围内的实体伤害与冲击波；与是否破坏方块无关。 */
    open fun damageEntities() {
        val diameter = this.radius * 2
        val x0 = Mth.floor(this.x - diameter.toDouble() - 1)
        val x1 = Mth.floor(this.x + diameter.toDouble() + 1)
        val y0 = Mth.floor(this.y - diameter.toDouble() - 1)
        val y1 = Mth.floor(this.y + diameter.toDouble() + 1)
        val z0 = Mth.floor(this.z - diameter.toDouble() - 1)
        val z1 = Mth.floor(this.z + diameter.toDouble() + 1)
        val list = this.level.getEntities(
            if (this.excludeDirectSource) this.entity else null,
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
                        if (entity is LivingEntity) {
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
     * Called by the budgeted shell worker and by [finalizeExplosion] for explicitly supplied blocks.
     */
    private fun processBlockList(blocks: Collection<BlockPos>) {
        val flag1 = dropBlocks && this.indirectSourceEntity is Player
        val dropList = if (dropBlocks) ObjectArrayList<Pair<ItemStack, BlockPos>>() else null

        this.level.profiler.push("explosion_blocks")
        for (blockpos in blocks) {
            val blockstate = this.level.getBlockState(blockpos)
            if (blockstate.isAir) continue
            val blockpos1 = blockpos.immutable()

            if (dropList != null && this.level is ServerLevel && blockstate.canDropFromExplosion(this.level, blockpos, this)) {
                val blockEntity = if (blockstate.hasBlockEntity()) this.level.getBlockEntity(blockpos) else null
                val lootParamsBuilder = LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockpos))
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, this.entity)

                // This parameter enables loot decay; ordinary DESTROY explosions keep normal drops.
                if (blockInteraction == BlockInteraction.DESTROY_WITH_DECAY) {
                    lootParamsBuilder.withParameter(LootContextParams.EXPLOSION_RADIUS, this.radius)
                }

                blockstate.spawnAfterBreak(level, blockpos, ItemStack.EMPTY, flag1)
                blockstate.getDrops(lootParamsBuilder).forEach {
                    addBlockDrops(dropList, it, blockpos1)
                }
            }

            if (!dropBlocks && blockstate.hasBlockEntity()) {
                // Container onRemove hooks scatter their contents independently of the block loot table.
                Clearable.tryClear(this.level.getBlockEntity(blockpos))
            }
            if (!dropBlocks && blockDestructionRadius != null && canRemoveTerrainQuickly(blockpos, blockstate)) {
                // Keep chunk saving, heightmaps, lighting and section-batched client updates in Level.setBlock.
                // Only skip neighbour/shape recursion when all adjacent blocks have no such behaviour.
                level.setBlock(blockpos, Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS or Block.UPDATE_KNOWN_SHAPE or Block.UPDATE_SUPPRESS_DROPS)
                blockstate.block.wasExploded(level, blockpos, this)
            } else {
                blockstate.onBlockExploded(this.level, blockpos, this)
            }
        }

        val deferred = deferredBlockDrops
        if (deferred != null && dropList != null) {
            deferred.addAll(dropList)
        } else if (dropList != null) {
            for (pair in dropList) {
                Block.popResource(this.level, pair.getSecond(), pair.getFirst())
            }
        }

        this.level.profiler.pop()
    }

    private fun canRemoveTerrainQuickly(pos: BlockPos, state: BlockState): Boolean {
        if (!isPlainTerrain(state)) return false
        val serverLevel = level as? ServerLevel ?: return false
        val chunk = serverLevel.chunkSource.getChunkNow(pos.x shr 4, pos.z shr 4) ?: return false
        for (direction in BLOCK_DIRECTIONS) {
            val adjacent = pos.relative(direction)
            if (!level.isInWorldBounds(adjacent)) continue
            val neighbourChunk = if (adjacent.x shr 4 == pos.x shr 4 && adjacent.z shr 4 == pos.z shr 4) chunk
                else serverLevel.chunkSource.getChunkNow(adjacent.x shr 4, adjacent.z shr 4) ?: return false
            val neighbour = neighbourChunk.getBlockState(adjacent)
            if (!neighbour.isAir && !isPlainTerrain(neighbour)) return false
        }
        return true
    }

    private fun isPlainTerrain(state: BlockState): Boolean = PLAIN_TERRAIN.getOrPut(state.block) {
        state.block.javaClass == Block::class.java && ForgeRegistries.BLOCKS.getKey(state.block)?.namespace == "minecraft"
    }

    /** Resumable, one-block-thick ellipsoidal shells. No full-volume scan or position list is built up front. */
    private class ExplosionBlockCursor(
        private val centerX: Double,
        private val centerY: Double,
        private val centerZ: Double,
        private val radius: Double,
        private val verticalScale: Double,
        private val minHeight: Int,
        private val maxHeight: Int
    ) {
        data class Sample(val x: Int, val y: Int, val z: Int, val distanceSquared: Double)

        var shellIndex = 0
            private set
        var complete = !radius.isFinite() || radius <= 0.0 || minHeight > maxHeight
            private set
        private var inner = 0.0
        private var outer = minOf(1.0, radius)
        private var minX = 0
        private var maxX = -1
        private var minY = 0
        private var maxY = -1
        private var columnX = 0
        private var columnY = 0
        private var sampleX = 0
        private var sampleY = 0
        private var baseDistanceSquared = 0.0
        private var nextZ = 0
        private var lastZ = -1
        private var secondZ = 0
        private var secondLastZ = -1

        init {
            require(verticalScale.isFinite() && verticalScale > 0.0)
            if (!complete) beginShell()
        }

        /** Continue through all currently reached shells while there is work/time budget left. */
        fun visit(maxSteps: Int, maxShell: Int, shouldYield: () -> Boolean, visit: (Sample) -> Unit): Boolean {
            var steps = 0
            while (!complete && shellIndex <= maxShell && steps < maxSteps && !shouldYield()) {
                step()?.let(visit)
                steps++
            }
            return complete
        }

        /** The caller has verified that this entire chunk section contains only air. */
        fun skipZSection() {
            val lastSectionZ = (nextZ - 1) or 15
            nextZ = minOf(lastZ + 1, lastSectionZ + 1)
        }

        private fun beginShell() {
            minX = ceil(centerX - 0.5 - outer - 1e-9).toInt()
            maxX = floor(centerX - 0.5 + outer + 1e-9).toInt()
            val verticalReach = outer / sqrt(verticalScale)
            minY = maxOf(minHeight, ceil(centerY - 0.5 - verticalReach - 1e-9).toInt())
            maxY = minOf(maxHeight, floor(centerY - 0.5 + verticalReach + 1e-9).toInt())
            columnX = minX
            columnY = minY
            nextZ = 0
            lastZ = -1
            secondZ = 0
            secondLastZ = -1
        }

        private fun step(): Sample? {
            if (nextZ <= lastZ) {
                val z = nextZ++
                val dz = z + 0.5 - centerZ
                val distanceSquared = baseDistanceSquared + dz * dz
                if (distanceSquared > outer * outer || shellIndex > 0 && distanceSquared <= inner * inner) return null
                return Sample(sampleX, sampleY, z, distanceSquared)
            }
            if (secondZ <= secondLastZ) {
                nextZ = secondZ
                lastZ = secondLastZ
                secondLastZ = secondZ - 1
                return null
            }
            if (columnX > maxX || minY > maxY) {
                if (outer >= radius) {
                    complete = true
                } else {
                    inner = outer
                    outer = minOf(radius, outer + 1.0)
                    shellIndex++
                    beginShell()
                }
                return null
            }

            sampleX = columnX
            sampleY = columnY
            if (++columnY > maxY) {
                columnY = minY
                columnX++
            }
            val dx = sampleX + 0.5 - centerX
            val dy = sampleY + 0.5 - centerY
            baseDistanceSquared = dx * dx + verticalScale * dy * dy
            val outerSquared = outer * outer - baseDistanceSquared
            if (outerSquared < 0.0) return null
            val extent = sqrt(outerSquared)
            nextZ = ceil(centerZ - 0.5 - extent - 1e-9).toInt()
            lastZ = floor(centerZ - 0.5 + extent + 1e-9).toInt()

            val innerSquared = inner * inner - baseDistanceSquared
            if (shellIndex > 0 && innerSquared >= 0.0) {
                // Skip the inner interval instead of rescanning the entire shell's enclosing cube.
                val hole = sqrt(innerSquared)
                val holeMin = ceil(centerZ - 0.5 - hole + 1e-9).toInt()
                val holeMax = floor(centerZ - 0.5 + hole - 1e-9).toInt()
                if (holeMin <= holeMax) {
                    secondZ = holeMax + 1
                    secondLastZ = lastZ
                    lastZ = holeMin - 1
                }
            }
            return null
        }
    }

    class Builder(private var directSource: Entity) {
        private val level: Level = directSource.level()
        private var sourceEntity: Entity?
        private var attackerEntity: Entity?
        private var damage = 0f
        private var radius = 0f
        private var particleType: ParticleTool.ParticleType? = null
        private var destroyBlock: Boolean = true
        private var dropBlocks = true
        private var fireTime = 0
        private var damageSource: DamageSource? = null
        private var particlePosition: Vec3? = null
        private var beast = false
        private var includeDirectSource = false
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

        fun destroyBlock(destroyBlock: Boolean): Builder {
            this.destroyBlock = destroyBlock
            return this
        }

        fun keepBlock(): Builder {
            this.destroyBlock = false
            return this
        }

        fun dropBlocks(flag: Boolean): Builder {
            this.dropBlocks = flag
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

        /** 让 [directSource] 也吃这次爆炸的伤害（默认被排除） */
        fun includeDirectSource(): Builder {
            this.includeDirectSource = true
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

            val interaction =
                Supplier {
                    if (ExplosionConfig.EXPLOSION_DESTROY.get() && this.destroyBlock) BlockInteraction.DESTROY else BlockInteraction.KEEP
                }

            val customExplosion = CustomExplosion(
                level, directSource,
                source, damage,
                position.x, position.y, position.z, radius, interaction.get()
            )
                .setFireTime(fireTime)
                .setBeast(beast)
                .setExcludeDirectSource(!includeDirectSource)
                .setDropBlocks(dropBlocks)

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
        private val PLAIN_TERRAIN = IdentityHashMap<Block, Boolean>()
        private val BLOCK_DIRECTIONS = Direction.entries.toTypedArray()
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
            return getSeenPercent(center, entity)
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
                        if (level.clip(
                                ClipContext(
                                    center, point,
                                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null
                                )
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
