package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.entity.misc.CatapultShuttleEntity
import com.atsuishio.superbwarfare.entity.projectile.C4Entity
import com.atsuishio.superbwarfare.entity.projectile.FlareDecoyEntity
import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils.lerpAngle
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.transformPosition
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.tools.OBB
import com.atsuishio.superbwarfare.tools.SpritePixelHelper
import com.atsuishio.superbwarfare.tools.angleTo
import com.atsuishio.superbwarfare.tools.forceHurt
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.tags.BlockTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.entity.vehicle.Minecart
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries
import org.joml.Math
import org.joml.Matrix4d
import org.joml.Vector3d
import kotlin.math.max

/**
 * 处理载具运动相关方法的工具类
 */
object VehicleMotionUtils {
    /** Safety cap for [isSearchBoxProvablyAirborne] column scanning; see its Javadoc. */
    private const val MAX_HEIGHTMAP_PROBE_COLUMNS = 400L

    /**
     * 防止载具堆叠
     *
     * @param vehicle 载具
     */
    @JvmStatic
    fun preventStacking(vehicle: VehicleEntity) {
        val entities = vehicle.level().getEntities(
            EntityTypeTest.forClass(VehicleEntity::class.java),
            vehicle.boundingBox.inflate(6.0)
        ) { entity: VehicleEntity ->
            entity !== vehicle && !vehicle.getPassengers().contains(entity) && entity.vehicle == null
        }

        for (entity in entities) {
            if (entity.boundingBox.intersects(vehicle.boundingBox)) {
                val toVec = vehicle.position()
                    .add(Vec3(1.0, 1.0, 1.0).scale((vehicle.getRandom().nextFloat() * 0.01f + 1f).toDouble()))
                    .vectorTo(entity.position())
                val velAdd = toVec.normalize().scale(
                    Math.max(
                        (vehicle.bbWidth + 2) - vehicle.position().distanceTo(entity.position()),
                        0.0
                    ) * 0.1
                )
                val entitySize = (entity.bbWidth * entity.bbHeight).toDouble()
                val thisSize = (vehicle.bbWidth * vehicle.bbHeight).toDouble()
                val f = Math.min(entitySize / thisSize, 2.0)
                val f1 = Math.min(thisSize / entitySize, 2.0)

                vehicle.pushNew(-f * velAdd.x, -f * velAdd.y, -f * velAdd.z)
                entity.push(f1 * velAdd.x, f1 * velAdd.y, f1 * velAdd.z)
            }
        }
    }

    /**
     * 实体与OBB碰撞箱载具的碰撞交互
     *
     * 基于SAT(分离轴定理)计算OBB与实体AABB之间的MTV(最小平移向量)，
     * 根据MTV方向区分碰撞类型并施加对应的碰撞响应：
     * - 实体在OBB上方 → 支撑站在表面，跟随载具移动
     * - 实体在OBB侧面/下方 → 推出OBB + 动量传递
     *
     * @param vehicle 载具
     */
    @JvmStatic
    fun supportEntities(vehicle: VehicleEntity) {
        if (vehicle.isRemoved) return
        if (vehicle.enableAABB()) return

        val searchBox = calculateCombinedAABBOptimized(vehicle).inflate(0.5)
        val entities = vehicle.level().getEntities(
            EntityTypeTest.forClass(Entity::class.java), searchBox
        ) { entity ->
            entity !== vehicle && entity !== vehicle.getFirstPassenger() && entity.vehicle == null && entity !is C4Entity && entity !is SmokeDecoyEntity && entity !is FlareDecoyEntity && entity !is CatapultShuttleEntity
        }

        for (entity in entities) {
            if (!entity.isAlive) continue
            if (entity is Player && entity.isSpectator) continue

            // 玩家：客户端和服务端都处理（客户端保证响应，服务端保证权威位置不被拉回）
            // 非玩家：仅服务端处理
            if (entity is Player) {
                handleEntityObbCollision(vehicle, entity)
            } else {
                if (!vehicle.level().isClientSide) handleEntityObbCollision(vehicle, entity)
            }
        }
    }

    /**
     * 处理单个实体与载具OBB之间的碰撞交互
     *
     * 关键：位置修正和速度修正分离，防止deltaMovement被加两次。
     * entity.move() 自己会加上deltaMovement，我们不能再加一次。
     *
     * - 阶段A：当前帧已陷入OBB → 从当前位置沿MTV推出（纯位置修正，不含deltaMovement）
     * - 阶段B：deltaMovement会导致穿入 → 只截速度不调位置（交给entity.move()处理）
     */
    private fun handleEntityObbCollision(vehicle: VehicleEntity, entity: Entity) {
        if (entity is Projectile) return
        if (vehicle.enableAABB()) return
        if (entity.noPhysics || vehicle.noPhysics) return

        if (entity is TurretWreckEntity) {
            if (entity.tickCount < 1) return
            entity.supportByVehicle = true
        }

        val vehicleDx = vehicle.x - vehicle.xo
        val vehicleDz = vehicle.z - vehicle.zo
        val movement = entity.deltaMovement
        val minPenetration = 0.01

        // === 阶段A：当前帧已陷入 → 迭代推出，每轮选穿透最深的OBB（避免多OBB间ping-pong） ===
        repeat(4) {
            var bestMtvX = 0.0
            var bestMtvY = 0.0
            var bestMtvZ = 0.0
            var bestLenSq = 0.0
            var bestOnTop = false

            for (obb in vehicle.getOBBs()) {
                if (obb.part == OBB.Part.COLLISION || obb.part == OBB.Part.INTERACTIVE) continue
                val curMtv = OBB.computeObbAabbMtv(obb, entity.boundingBox) ?: continue
                val curLenSq = curMtv.x * curMtv.x + curMtv.y * curMtv.y + curMtv.z * curMtv.z
                if (curLenSq < minPenetration * minPenetration) continue
                if (curLenSq > bestLenSq) {
                    bestLenSq = curLenSq
                    bestMtvX = curMtv.x
                    bestMtvY = curMtv.y
                    bestMtvZ = curMtv.z
                    bestOnTop = -curMtv.y / Math.sqrt(curLenSq) > 0.5
                }
            }

            if (bestLenSq == 0.0) return@repeat  // 没有碰撞

            // 推出方向单位向量，额外加余量防止立刻再陷入
            val bestLen = Math.sqrt(bestLenSq)
            val pushNx = -bestMtvX / bestLen
            val pushNy = -bestMtvY / bestLen
            val pushNz = -bestMtvZ / bestLen
            val extra = 0.02
            val pushX = -bestMtvX + pushNx * extra
            var pushY = -bestMtvY + pushNy * extra
            val pushZ = -bestMtvZ + pushNz * extra
            // 站在地面上时不允许向下推，防止玩家被压进地里
            if (pushY < 0 && entity.onGround()) pushY = 0.0

            if (bestOnTop) {
                entity.setPos(
                    entity.x + pushX + vehicleDx,
                    entity.y + pushY,
                    entity.z + pushZ + vehicleDz
                )
                entity.deltaMovement = Vec3(vehicle.deltaMovement.x, 0.0, vehicle.deltaMovement.z)
                entity.setOnGround(true)
                entity.fallDistance = 0f
                return
            }
            // 推出 + 清零朝向该OBB的速度分量
            entity.setPos(
                entity.x + pushX,
                entity.y + pushY,
                entity.z + pushZ
            )
            val velToward = movement.x * pushNx + movement.y * pushNy + movement.z * pushNz
            if (velToward > 0) {
                entity.deltaMovement = Vec3(
                    movement.x - pushNx * velToward,
                    movement.y - pushNy * velToward,
                    movement.z - pushNz * velToward
                )
            }

            // 玩家潜行时侧面碰撞OBB → 缓慢推车
            if (entity is Player && entity.isCrouching && !vehicle.level().isClientSide) {
                vehicle.pushNew(-pushNx * 0.03, 0.0, -pushNz * 0.03)
            }
        }

        // === 阶段B：deltaMovement会导致穿入 → 对所有OBB同时截速度 ===
        var clampedDx = movement.x
        var clampedDy = movement.y
        var clampedDz = movement.z
        var standingOnObb = false
        var hasCollision = false

        repeat(4) {
            var clippedAny = false
            for (obb in vehicle.getOBBs()) {
                if (obb.part == OBB.Part.COLLISION || obb.part == OBB.Part.INTERACTIVE) continue

                val probeAabb = entity.boundingBox.move(clampedDx, clampedDy, clampedDz)
                val mtv = OBB.computeObbAabbMtv(obb, probeAabb) ?: continue
                val mtvLenSq = mtv.x * mtv.x + mtv.y * mtv.y + mtv.z * mtv.z
                if (mtvLenSq < minPenetration * minPenetration) continue

                val mtvLen = Math.sqrt(mtvLenSq)
                val nx = -mtv.x / mtvLen
                val ny = -mtv.y / mtvLen
                val nz = -mtv.z / mtvLen

                if (mtvLen > 0.05) {
                    val velIntoObb = clampedDx * nx + clampedDy * ny + clampedDz * nz
                    if (velIntoObb > 0) {
                        val oldDy = clampedDy
                        clampedDx -= nx * velIntoObb
                        clampedDy -= ny * velIntoObb
                        clampedDz -= nz * velIntoObb
                        // 不能让截断增加下落速度，否则地面上碰OBB的玩家会陷进地里
                        if (clampedDy < oldDy) clampedDy = oldDy
                        clippedAny = true
                    }
                }

                if (ny > 0.5) standingOnObb = true
                hasCollision = true
                // 不break：本OBB截断后继续检查其他OBB，同一轮内交叉收敛
            }
            if (!clippedAny) return@repeat
        }

        if (!hasCollision) return

        if (standingOnObb) {
            // 站在顶上时速度跟随载具
            entity.deltaMovement = Vec3(vehicle.deltaMovement.x, 0.0, vehicle.deltaMovement.z)
            entity.setOnGround(true)
            entity.fallDistance = 0f
        } else {
            // 侧面碰撞：只截速度不调位置
            entity.deltaMovement = Vec3(clampedDx, clampedDy, clampedDz)
        }
    }


    /**
    * Tests nearby entities for vehicle-crush collisions and applies damage and
    * impulse to entities that are struck.
    *
    * **Performance notes:**
    * - The previous implementation used `stream().filter(...).toList()`, which
    *   allocates a `SpinedNodeBuilder` intermediate buffer, a terminal array, and
    *   a wrapping list — all for a collection that is almost always empty or
    *   contains 1–3 elements.
    * - Replaced with an explicit `ArrayList` pre-sized to `getEntities` result
    *   count and an indexed loop, eliminating all stream-pipeline allocations.
    *
    * @param vehicle the vehicle performing the crush check
    */
    @JvmStatic
    fun crushEntities(vehicle: VehicleEntity) {
        if (!vehicle.canCrushEntities()) return
        if (vehicle.isRemoved) return

        val vec3 = vehicle.deltaMovement

        // Broad-phase: collect raw candidates from the level's entity sections.
        val candidates: List<Entity> = if (!vehicle.enableAABB()) {
            vehicle.level().getEntities(
                EntityTypeTest.forClass(Entity::class.java),
                vehicle.getCombinedAABB()
            ) { entity ->
                entity !== vehicle
                    && entity !== vehicle.getFirstPassenger()
                    && entity.vehicle == null
            }
        } else {
            vehicle.level().getEntities(
                EntityTypeTest.forClass(Entity::class.java),
                vehicle.boundingBox.move(vec3)
            ) { entity ->
                entity !== vehicle
                    && entity !== vehicle.getFirstPassenger()
                    && entity.vehicle == null
            }
        }

        if (candidates.isEmpty()) return

        // Narrow-phase filter: build result list without stream allocation.
        // Pre-size to candidate count — the real result is almost always smaller.
        val entities = ArrayList<Entity>(candidates.size)
        for (entity in candidates) {
            if (!entity.isAlive) continue

            val type = ForgeRegistries.ENTITY_TYPES.getKey(entity.type)
            val inWhitelist = VehicleConfig.COLLISION_ENTITY_WHITELIST.get().contains(type.toString())

            val qualifies = entity is VehicleEntity
                || entity is Boat
                || entity is Minecart
                || (entity is TurretWreckEntity && entity.tickCount > 5)
                || (entity is LivingEntity && !(entity is Player && entity.isSpectator))
                || inWhitelist

            if (!qualifies) continue

            // OBB-mode: additionally require the entity to be inside the OBB volume.
            if (!vehicle.enableAABB() && !vehicle.isInObb(entity, vec3)) continue

            entities.add(entity)
        }

        if (entities.isEmpty()) return

        for (entity in entities) {
            val entitySize = entity.boundingBox.size
            val thisSize   = vehicle.boundingBox.size
            val f: Double
            val f1: Double

            val v0 = vec3.subtract(entity.deltaMovement)
            if (v0.angleTo(vehicle.position().vectorTo(entity.position())) > 90) return

            if (vehicle.deltaMovement.lengthSqr() < 0.09) return

            if (entity is LivingEntity && entity.hasEffect(ModMobEffects.STRIKE_PROTECTION.get())) {
                continue
            }

            if (entity is VehicleEntity) {
                f  = Mth.clamp((entity.mass / vehicle.mass).toDouble(), 0.25, 4.0)
                f1 = Mth.clamp((vehicle.mass / entity.mass).toDouble(), 0.25, 4.0)
            } else {
                f  = Mth.clamp(entitySize / thisSize, 0.25, 4.0)
                f1 = Mth.clamp(thisSize / entitySize, 0.25, 4.0)
            }

            val length = v0.length().toFloat()
            var velAdd = v0.normalize().scale(0.8 * length)

            if (length <= 0.3) continue

            vehicle.level().playSound(
                null, vehicle, ModSounds.VEHICLE_STRIKE.get(), vehicle.soundSource, 1f, 1f
            )

            if (entity is LivingEntity) {
                entity.forceHurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        vehicle.level().registryAccess(), vehicle,
                        vehicle.getFirstPassenger() ?: vehicle
                    ),
                    (f1 * 80 * (Mth.abs(length) - 0.3) * (Mth.abs(length) - 0.3)).toFloat()
                )
            } else {
                entity.hurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        vehicle.level().registryAccess(), vehicle,
                        vehicle.getFirstPassenger() ?: vehicle
                    ),
                    (f1 * 60 * (Mth.abs(length) - 0.3) * (Mth.abs(length) - 0.3)).toFloat()
                )
            }

            if (entity !is TargetEntity) {
                vehicle.pushNew(-0.3f * f * velAdd.x, -0.3f * f * velAdd.y, -0.3f * f * velAdd.z)
            }

            if (entity is VehicleEntity) {
                vehicle.hurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        vehicle.level().registryAccess(), entity,
                        entity.getFirstPassenger() ?: entity
                    ),
                    (f * 40 * (Mth.abs(length) - 0.3) * (Mth.abs(length) - 0.3)).toFloat()
                )

                if (!vehicle.enableAABB() && vehicle.isInObb(entity, Vec3.ZERO)) {
                    var thisPos  = vehicle.position()
                    var otherPos = entity.position()

                    for (obb in vehicle.getOBBs()) {
                        if (!entity.enableAABB()) {
                            for (obb2 in entity.getOBBs()) {
                                if (OBB.isColliding(obb, obb2)) {
                                    thisPos  = OBB.vector3dToVec3(obb.center)
                                    otherPos = OBB.vector3dToVec3(obb2.center)
                                }
                            }
                        } else {
                            if (OBB.isColliding(obb, entity.boundingBox)) {
                                thisPos = OBB.vector3dToVec3(obb.center)
                            }
                        }
                    }

                    val toVec = thisPos
                        .add(Vec3(1.0, 1.0, 1.0).scale((vehicle.getRandom().nextFloat() * 0.01f + 1f).toDouble()))
                        .vectorTo(otherPos)
                    velAdd = toVec.normalize().scale(
                        Math.max(thisPos.distanceTo(otherPos), 0.0) * 0.01
                    )
                    vehicle.pushNew(-f * velAdd.x, -f * velAdd.y, -f * velAdd.z)
                }

                val vec31 = vehicle.deltaMovement.normalize().scale(velAdd.length())
                entity.pushNew(f1 * vec31.x, f1 * vec31.y, f1 * vec31.z)
            } else {
                val vec31 = vehicle.deltaMovement.normalize().scale(velAdd.length())
                entity.push(f1 * vec31.x, f1 * vec31.y, f1 * vec31.z)
            }
        }
    }

    /**
     * 计算载具所有OBB的最小外接AABB（投影法，避免逐顶点计算）
     *
     * 世界坐标轴上的半长 = Σ(|localAxis_i · worldAxis| * extent_i)
     * 对于每个OBB，只需计算一次轴投影即可得到AABB范围，无需遍历8个顶点。
     *
     * 若载具启用了AABB模式则直接返回原版boundingBox。
     *
     * @param vehicle 载具
     * @return 所有OBB的组合外接AABB
     */
    @JvmStatic
    fun calculateCombinedAABBOptimized(vehicle: VehicleEntity): AABB {
        if (vehicle.enableAABB()) return vehicle.boundingBox

        val obbList = vehicle.getOBBs()
        if (obbList.isEmpty()) return vehicle.boundingBox

        val min = Vector3d( Double.MAX_VALUE,  Double.MAX_VALUE,  Double.MAX_VALUE)
        val max = Vector3d(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE)

        // Reuse a single TL axes buffer across the loop — no per-OBB allocation.
        val axes = OBB.AXES_A.get()

        for (obb in obbList) {
            obb.getAxesInto(axes)           // zero-allocation fill
            val c = obb.center
            val e = obb.extents

            val halfX = Math.abs(axes[0].x) * e.x + Math.abs(axes[1].x) * e.y + Math.abs(axes[2].x) * e.z
            val halfY = Math.abs(axes[0].y) * e.x + Math.abs(axes[1].y) * e.y + Math.abs(axes[2].y) * e.z
            val halfZ = Math.abs(axes[0].z) * e.x + Math.abs(axes[1].z) * e.y + Math.abs(axes[2].z) * e.z

            if (c.x - halfX < min.x) min.x = c.x - halfX
            if (c.y - halfY < min.y) min.y = c.y - halfY
            if (c.z - halfZ < min.z) min.z = c.z - halfZ
            if (c.x + halfX > max.x) max.x = c.x + halfX
            if (c.y + halfY > max.y) max.y = c.y + halfY
            if (c.z + halfZ > max.z) max.z = c.z + halfZ
        }

        return AABB(OBB.vector3dToVec3(min), OBB.vector3dToVec3(max))
    }

    /**
     * 根据条件来碰撞方块
     *
     * @param vehicle 载具
     */
    @JvmStatic
    fun collideBlocks(vehicle: VehicleEntity) {
        if (!VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.get()
            && !VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.get()
            && !VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.get()
            && !VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.get()
        ) return

        val collisionLevel = vehicle.computed().collisionLevel
        val limits = collisionLevel.powerLimits

        val power = vehicle.power
        val motion = vehicle.deltaMovement.horizontalDistance()

        val flags = booleanArrayOf(
            VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.get() && collisionLevel.level >= 1,
            VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.get() && collisionLevel.level >= 2,
            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.get() && collisionLevel.level >= 3,
            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.get() && collisionLevel.level >= 4
        )

        var i = 0
        while (i < flags.size && i < limits.size) {
            val limit = limits[i]
            flags[i] =
                flags[i] and if (limit.equals) power >= limit.power || motion >= limit.motion else power > limit.power || motion > limit.motion
            i++
        }

        if (!vehicle.enableAABB()) {
            val aabb = vehicle.getCombinedAABB().inflate(0.25, 0.0, 0.25).move(vehicle.deltaMovement)
                .move(0.0, 0.5, 0.0)
            BlockPos.betweenClosedStream(aabb).forEach { pos ->
                val state = vehicle.level().getBlockState(pos)
                if (vehicle.isInObb(pos, vehicle.deltaMovement)) {
                    if ((flags[0] && state.`is`(ModTags.Blocks.SOFT_COLLISION)) ||
                        (flags[1] && state.`is`(ModTags.Blocks.NORMAL_COLLISION)) ||
                        (flags[2] && state.`is`(ModTags.Blocks.HARD_COLLISION)) ||
                        (flags[3] && (state.block.defaultDestroyTime() > 0 || state.block
                            .defaultDestroyTime() <= 4))
                    ) {
                        vehicle.level().destroyBlock(pos, true)
                    }
                }
            }
        }

        val aabb = vehicle.boundingBox.inflate(0.25, 0.0, 0.25).move(vehicle.deltaMovement).move(0.0, 0.5, 0.0)
        BlockPos.betweenClosedStream(aabb).forEach { pos ->
            val state = vehicle.level().getBlockState(pos)
            if ((flags[0] && state.`is`(ModTags.Blocks.SOFT_COLLISION)) ||
                (flags[1] && state.`is`(ModTags.Blocks.NORMAL_COLLISION)) ||
                (flags[2] && state.`is`(ModTags.Blocks.HARD_COLLISION)) ||
                (flags[3] && (state.block.defaultDestroyTime() > 0 || state.block
                    .defaultDestroyTime() <= 4))
            ) {
                vehicle.level().destroyBlock(pos, true)
            }
        }
    }

    /**
     * Decelerates the vehicle when driving over dragon teeth obstacles.
     *
     * @param vehicle the vehicle entity
     */
    @JvmStatic
    fun handleVehicleMoveOnDragonTeeth(vehicle: VehicleEntity) {
        if (!vehicle.onGround()) return

        val aabb = vehicle.boundingBox
        val y = aabb.minY - 0.01
        val level = vehicle.level()

        // Sample center + 4 corners of the bottom face — O(5) block lookups
        val pos = BlockPos.MutableBlockPos()
        val checkX = doubleArrayOf(vehicle.x, aabb.minX, aabb.maxX, aabb.minX, aabb.maxX)
        val checkZ = doubleArrayOf(vehicle.z, aabb.minZ, aabb.minZ, aabb.maxZ, aabb.maxZ)
        for (i in checkX.indices) {
            pos.set(Mth.floor(checkX[i]), Mth.floor(y), Mth.floor(checkZ[i]))
            if (level.getBlockState(pos).`is`(ModBlocks.DRAGON_TEETH.get())) {
                vehicle.power *= 0.8f
                vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(-0.1, 0.0, -0.1))
                return
            }
        }
    }

    @JvmStatic
    fun bounceHorizontal(vehicle: VehicleEntity, direction: Direction) {
        when (direction.axis) {
            Direction.Axis.X -> vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.8, 0.99, 0.99))
            Direction.Axis.Z -> vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.99, 0.99, 0.8))
            else -> {}
        }
    }

    @JvmStatic
    fun bounceVertical(vehicle: VehicleEntity, direction: Direction) {
        if (!vehicle.level().isClientSide) {
            vehicle.level().playSound(null, vehicle, ModSounds.VEHICLE_STRIKE.get(), vehicle.soundSource, 1f, 1f)
        }
        vehicle.collisionCoolDown = 4
        vehicle.crash = true
        if (direction.axis === Direction.Axis.Y) {
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.9, -0.8, 0.9))
        }
    }

    fun getHeightAboveGround(vehicle: VehicleEntity): Double {
        val level = vehicle.level()
        val chunkX = vehicle.blockX shr 4
        val chunkZ = vehicle.blockZ shr 4
        if (!level.hasChunk(chunkX, chunkZ)) return Double.MAX_VALUE
        val groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, vehicle.blockX, vehicle.blockZ).toDouble()
        return max(0.0, vehicle.y - groundY)
    }

    @JvmStatic
    fun terrainCompact(vehicle: VehicleEntity, positions: MutableList<Vec3>) {
        if (vehicle.vehicleType == VehicleType.AIRSHIP) return

        val level = vehicle.level()
        val chunkX = vehicle.blockX shr 4
        val chunkZ = vehicle.blockZ shr 4
        if (!level.hasChunk(chunkX, chunkZ)) return
        val groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, vehicle.blockX, vehicle.blockZ).toDouble()

        val collisionInfo = vehicle.getCollisionOBBInfo()

        if (collisionInfo == null) {
            terrainCompactAABB(vehicle, positions)
            return
        }

        val maxHalfExtent = run {
            val s = collisionInfo.size
            max(max(s.x, s.y), s.z)
        }

        // 有碰撞OBB时检测整个OBB底部离地高度，无OBB时检测自身AABB底部离地高度
        // 若离地超过阈值则认为悬空，不处理地形贴合
        val heightAboveGround = (vehicle.y + collisionInfo.position.y - collisionInfo.size.y) - groundY
        if (heightAboveGround > maxHalfExtent) {
            if (vehicle.isInFluidType) {
                vehicle.xRot *= 0.9f; vehicle.setZRot(vehicle.roll * 0.9f)
            }
            return
        }

        if (getHeightAboveGround(vehicle) > 4) {
            return
        }

        // 仅含yaw的水平参考系：用它构建采样点的(x,z)世界坐标和搜索窗口中心Y，
        // 使地形采样位置与车身pitch/roll解耦，避免"上一tick的倾角影响这一tick的采样位置"造成的角度自反馈与抖动
        val flatTransform = vehicle.getWheelsTransform(1f)

        // 含pitch/roll的完整参考系：用于计算采样点处OBB底面的实际世界Y坐标，
        // 使heightY能够反映当前车身倾角（如机头抬高时后方采样点Y更低），从而驱动地形贴合修正
        val fullTransform = Matrix4d()
        fullTransform.translate(vehicle.x, vehicle.y, vehicle.z)
        fullTransform.rotate(Axis.YP.rotationDegrees(-vehicle.yRot))
        fullTransform.rotate(Axis.XP.rotationDegrees(vehicle.xRot))
        fullTransform.rotate(Axis.ZP.rotationDegrees(vehicle.roll))

        // 采样列（载具局部坐标，X=右，Z=前）及该列处碰撞OBB底面的局部高度ly
        val sampleLx = ArrayList<Double>()
        val sampleLz = ArrayList<Double>()
        val sampleLy = ArrayList<Double>()

        if (collisionInfo != null) {
            // 用碰撞OBB底面footprint采样：cols列(左右,决定roll) × rows排(前后,决定pitch)
            val cx = collisionInfo.position.x
            val cz = collisionInfo.position.z
            val hx = collisionInfo.size.x
            val hz = collisionInfo.size.z
            val bottomY = collisionInfo.position.y - collisionInfo.size.y
            val cols = 3
            val rows = 5
            for (ci in 0 until cols) {
                val lx = cx - hx + 2.0 * hx * ci / (cols - 1)
                for (ri in 0 until rows) {
                    val lz = cz - hz + 2.0 * hz * ri / (rows - 1)
                    sampleLx.add(lx)
                    sampleLz.add(lz)
                    sampleLy.add(bottomY)
                }
            }
        } else {
            // 回退：无碰撞OBB时用预设接地点（轮位/起落架）
            for (p in positions) {
                sampleLx.add(p.x)
                sampleLz.add(p.z)
                sampleLy.add(p.y)
            }
        }
        val count = sampleLx.size
        if (count == 0) return

        // 容差/坑洞参数（单位：方块）
        val embedTolerance = 0.25    // 横向嵌入容差：地面与OBB底相差不超过此值视为贴合，不产生倾角
        val searchUp = vehicle.stepHeight.toDouble()           // 上坡探测上限：检测高出OBB底的地形（爬坡），同时限制最大抬头幅度
        val searchDown = maxHalfExtent         // 下坡/坑洞探测下限：检测低于OBB底的地形，同时限制最大低头幅度
        val potholeDepth = 0.6       // 采样列地面低于OBB底超过此值视为"坑"
        val potholeIgnoreRatio = 0.4 // 坑采样占比不超过此值时忽略其影响（保持水平，不栽进小坑）

        // 第一遍：对每个采样列做精确AABB探测，求地面相对OBB底的高度差
        // heightY 约定：正=地面在OBB底下方(悬空/坑)，负=地面嵌入OBB(上坡)
        val heightY = DoubleArray(count)
        val isPit = BooleanArray(count)
        for (i in 0 until count) {
            val worldFlat = transformPosition(flatTransform, sampleLx[i], sampleLy[i], sampleLz[i])
            val worldFull = transformPosition(fullTransform, sampleLx[i], sampleLy[i], sampleLz[i])
            // 使用flat投影的(x,z)采样地形列，保证采样网格稳定不受pitch/roll影响；
            // 使用flat投影的Y作为搜索窗口中心（与searchUp/searchDown配合），
            // 使用full投影的Y作为OBB底面的实际高度，使heightY正确反映车身倾角
            val top = sampleTerrainTop(level, worldFlat.x, worldFlat.y, worldFlat.z, searchUp, searchDown)
            if (top == null) {
                // 垂直窗口内无地形支撑（深坑/悬崖外）
                isPit[i] = true
                heightY[i] = searchDown
            } else {
                val rawPre = worldFull.y - top
                isPit[i] = rawPre > potholeDepth
                // 容差：极小的嵌入/悬空都吸附为贴合(0)，避免体素噪声造成的细碎抖动；
                // 超出容差后线性响应——上坡(负)允许少量横向嵌入，爬坡时车身抬头
                var h = rawPre
                h = if (h in -embedTolerance..embedTolerance) 0.0
                else if (h > 0) h - embedTolerance else h + embedTolerance
                heightY[i] = h.coerceIn(-searchUp, searchDown)
            }
        }

        // 坑洞忽略：坑占比不大时剔除坑采样点，避免少数坑洞把车身往下拽
        val pitCount = isPit.count { it }
        val ignorePits = pitCount in 1..(potholeIgnoreRatio * count).toInt()

        // 第二遍：对保留点做去中心化最小二乘平面拟合
        // （去中心化保证"剔除部分采样点后"残余的均匀高度偏移不会污染斜率）
        var n = 0
        var meanLx = 0.0
        var meanLz = 0.0
        var meanH = 0.0
        for (i in 0 until count) {
            if (ignorePits && isPit[i]) continue
            meanLx += sampleLx[i]
            meanLz += sampleLz[i]
            meanH += heightY[i]
            n++
        }
        if (n == 0) return
        meanLx /= n
        meanLz /= n
        meanH /= n

        var sumXH = 0.0
        var sumZH = 0.0
        var sumX2 = 0.0
        var sumZ2 = 0.0
        for (i in 0 until count) {
            if (ignorePits && isPit[i]) continue
            val dx = sampleLx[i] - meanLx
            val dz = sampleLz[i] - meanLz
            val dh = heightY[i] - meanH
            sumXH += dx * dh
            sumX2 += dx * dx
            sumZH += dz * dh
            sumZ2 += dz * dz
        }

        if (sumX2 > 1e-6 || sumZ2 > 1e-6) {
            updateTerrainCompact(vehicle, sumXH, sumZH, sumX2, sumZ2)
        }

        // 粒子特效：使用预设轮位
        if (level.isClientSide && vehicle.deltaMovement.horizontalDistanceSqr() > 0.01) {
            for (vec3 in positions) {
                val v = transformPosition(flatTransform, vec3.x, vec3.y - 0.02, vec3.z)
                val p = Vec3(v.x, v.y, v.z)
                val blockPos = BlockPos.containing(p.add(0.0, -0.3, 0.0))
                val state = level.getBlockState(blockPos)
                if (state.isAir) continue
                if (state.`is`(BlockTags.SAND) || state.`is`(BlockTags.SNOW)) {
                    val model = Minecraft.getInstance().modelManager.blockModelShaper.getBlockModel(state)
                    val sprite = model.particleIcon
                    val color = SpritePixelHelper.getRandomPixelRGB(sprite, 0)
                    val speed = Math.min(vehicle.deltaMovement.length(), 0.5).toFloat()
                    vehicle.addRandomParticle(
                        CustomCloudOption(
                            color,
                            70,
                            1f + 7f * speed + Math.random().toFloat() * 2,
                            Math.random().toFloat() * -0.12f,
                            false,
                            light = false
                        ),
                        p.add(0.0, 0.2, 0.0).subtract(vehicle.deltaMovement.scale(1.5)),
                        speed,
                        level,
                        1,
                        vehicle.deltaMovement.scale(1.0)
                    )
                } else {
                    vehicle.addRandomParticle(
                        BlockParticleOption(ParticleTypes.BLOCK, state),
                        p.add(0.0, 0.1, 0.0),
                        0.2f,
                        level,
                        0f,
                        1
                    )
                    if (vehicle.engineInfo is EngineInfo.Track && vehicle.drift() && vehicle.deltaMovement.horizontalDistanceSqr() > 0.0004
                        && state.`is`(BlockTags.MINEABLE_WITH_PICKAXE)
                    )
                        vehicle.addRandomParticle(
                            ModParticleTypes.FIRE_STAR.get(),
                            p.add(0.0, 0.1, 0.0),
                            0.25f,
                            level,
                            0.08f,
                            1
                        )
                }
            }
        }
    }

    fun terrainCompactAABB(vehicle: VehicleEntity, positions: MutableList<Vec3>) {
        if (vehicle.onGround()) {
            val transform = vehicle.getWheelsTransform(1f)
            val supportedPos = computeSupportedPosition(vehicle)

            for (vec3 in positions) {
                val vector4d = transformPosition(transform, vec3.x, vec3.y - 0.02, vec3.z)
                val p = Vec3(vector4d.x, vector4d.y, vector4d.z)
                val level = vehicle.level()
                val res = level.clip(
                    ClipContext(
                        p, p.add(0.0, -128.0, 0.0),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle
                    )
                )

                val heightY: Double

                var blockPos = BlockPos.containing(p)
                val blockPosUp = BlockPos.containing(p.add(0.0, 1.0, 0.0))
                if (level.getBlockState(blockPosUp).canOcclude()) {
                    blockPos = blockPosUp
                }
                val state = level.getBlockState(blockPos)
                val shape = state.getCollisionShape(level, blockPos)

                if (vehicle.level().isClientSide && vehicle.deltaMovement.horizontalDistanceSqr() > 0.01) {
                    if (state.`is`(BlockTags.SAND) || state.`is`(BlockTags.SNOW)) {
                        val model = Minecraft.getInstance().modelManager.blockModelShaper.getBlockModel(state)
                        val sprite = model.particleIcon
                        val color = SpritePixelHelper.getRandomPixelRGB(sprite, 0)
                        val speed = Math.min(vehicle.deltaMovement.length(), 0.5).toFloat()

                        val particleOption = CustomCloudOption(
                            color, 70, 1f + 7f * speed + Math.random().toFloat() * 2, Math.random().toFloat() * -0.12f,
                            cooldown = false,
                            light = false
                        )
                        vehicle.addRandomParticle(
                            particleOption,
                            p.add(0.0, 0.2, 0.0).subtract(vehicle.deltaMovement.scale(1.5)),
                            speed,
                            vehicle.level(),
                            1,
                            vehicle.deltaMovement.scale(60.0)
                        )
                    } else {
                        val particleData = BlockParticleOption(ParticleTypes.BLOCK, state)
                        vehicle.addRandomParticle(particleData, p.add(0.0, 0.1, 0.0), 0.2f, vehicle.level(), 0f, 1)

                        if (vehicle.engineInfo is EngineInfo.Track && vehicle.drift() && vehicle.deltaMovement.horizontalDistanceSqr() > 0.0004 && state.`is`(
                                BlockTags.MINEABLE_WITH_PICKAXE
                            )
                        ) {
                            vehicle.addRandomParticle(
                                ModParticleTypes.FIRE_STAR.get(),
                                p.add(0.0, 0.1, 0.0),
                                0.25f,
                                vehicle.level(),
                                0.08f,
                                1
                            )
                        }
                    }
                }

                heightY = if (!shape.isEmpty) {
                    p.y - (shape.max(Direction.Axis.Y) + blockPos.y)
                } else if (res.type == HitResult.Type.BLOCK && level.noCollision(AABB(p, p))) {
                    Mth.clamp(p.y - res.location.y, 0.0, 20.0)
                } else {
                    0.0
                }

                updateTerrainCompact(vehicle, p, heightY, supportedPos)
            }
        } else if (vehicle.isInFluidType) {
            vehicle.xRot *= 0.9f
            vehicle.setZRot(vehicle.roll * 0.9f)
        }
    }

    /**
    * Computes the one-time-per-tick "supporting block" position correction used by
    * [updateTerrainCompact].
    * <p>
    * This query only depends on the vehicle's own [net.minecraft.world.entity.Entity#getBoundingBox],
    * never on the per-wheel/leg sample point, so it must be computed exactly once per tick
    * instead of once per sample — previously it was re-evaluated for every entry of
    * {@code positions}, multiplying the cost of [net.minecraft.world.level.CollisionGetter.findSupportingBlock]
    * (one of the top offenders in profiling) by the wheel/leg count for no behavioural benefit.
    *
    * @param entity the vehicle entity.
    * @return the corrected reference position to use as `currentPos` inside [updateTerrainCompact].
    */
    private fun computeSupportedPosition(entity: VehicleEntity): Vec3 {
        val currentPos = entity.position()
        val aabb = entity.boundingBox
        val probe = AABB(aabb.minX, aabb.minY - 1.0E-6, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ)
        val supporting = entity.level().findSupportingBlock(entity, probe)
        return if (supporting.isPresent) {
            currentPos.add(currentPos.vectorTo(supporting.get().center).scale(0.6))
        } else {
            currentPos
        }
    }

    /**
    * Applies terrain-following tilt correction toward [landingTarget].
    *
    * @param entity        the vehicle entity.
    * @param landingTarget  world-space point the sample wheel/leg would land on.
    * @param heightY        signed ground-clearance value at the sample point.
    * @param supportedPos   pre-computed reference position from [computeSupportedPosition];
    *                       shared across all sample points within the same tick.
    */
    @JvmStatic
    fun updateTerrainCompact(entity: VehicleEntity, landingTarget: Vec3, heightY: Double, supportedPos: Vec3) {
        val horizontalOffset = Vec3(landingTarget.x - supportedPos.x, 0.0, landingTarget.z - supportedPos.z)
        val horizontalDistance = horizontalOffset.length()
        val horizontalDirection = if (horizontalDistance > 0) horizontalOffset.normalize() else Vec3.ZERO

        val tiltSmoothingFactor = 0.01f
        val targetTilt =
            Math.min(heightY * 9 * entity.data().compute().terrainCompatRotateRate * horizontalDistance, 45.0).toFloat()

        val yawRad = Math.toRadians(-entity.yRot)
        val localDirection = Vec3(
            horizontalDirection.x * Math.cos(yawRad) - horizontalDirection.z * Math.sin(yawRad),
            0.0,
            horizontalDirection.x * Math.sin(yawRad) + horizontalDirection.z * Math.cos(yawRad)
        )

        val targetXRot = (-localDirection.z * targetTilt).toFloat()
        val targetZRot = (localDirection.x * targetTilt).toFloat()

        entity.xRot = lerpAngle(entity.xRot, -targetXRot, tiltSmoothingFactor)
        entity.setZRot(lerpAngle(entity.roll, -targetZRot, tiltSmoothingFactor))
    }

    /**
    * @deprecated Retained for binary/source compatibility with external call sites that
    * still call the single-point overload. Prefer [computeSupportedPosition] +
    * the 4-argument [updateTerrainCompact] when calling in a loop, to avoid repeating
    * the expensive [net.minecraft.world.level.CollisionGetter.findSupportingBlock] query.
    */
    @Deprecated(
        message = "Recomputes findSupportingBlock on every call; use the 4-arg overload with a cached supportedPos when calling in a loop.",
        replaceWith = ReplaceWith("updateTerrainCompact(entity, landingTarget, heightY, computeSupportedPosition(entity))")
    )
    @JvmStatic
    fun updateTerrainCompact(entity: VehicleEntity, landingTarget: Vec3, heightY: Double) {
        updateTerrainCompact(entity, landingTarget, heightY, computeSupportedPosition(entity))
    }

    /**
     * 在指定列(wx,wz)上、以OBB底面高度wy为基准，探测最贴合的地形碰撞面顶部Y。
     *
     * 仅统计落在垂直窗口 [wy - searchDown, wy + searchUp] 内、且该列水平位置确实位于
     * 方块碰撞盒内的碰撞面，取其中最高的顶部（即车身会贴合到的地面）。
     * 直接遍历真实方块碰撞盒(forAllBoxes)而非高度图，因此对台阶/半砖/楼梯等也精确。
     *
     * @return 命中的地形顶部世界Y；窗口内无任何碰撞面时返回null（深坑/悬崖外）
     */
    private fun sampleTerrainTop(
        level: Level,
        wx: Double, wy: Double, wz: Double,
        searchUp: Double, searchDown: Double
    ): Double? {
        val bx = Mth.floor(wx)
        val bz = Mth.floor(wz)
        val topBlock = Mth.floor(wy + searchUp)
        val botBlock = Mth.floor(wy - searchDown)
        val ceil = wy + searchUp + 1e-6
        var best = Double.NaN
        val pos = BlockPos.MutableBlockPos()
        var by = topBlock
        while (by >= botBlock) {
            pos.set(bx, by, bz)
            val state = level.getBlockState(pos)
            if (!state.isAir) {
                val shape = state.getCollisionShape(level, pos)
                if (!shape.isEmpty) {
                    // Zero-allocation iteration: forAllBoxes avoids the List<AABB>
                    // and individual AABB allocations that toAabbs() would create.
                    // For 15 sample columns × 91 vehicles this eliminates ~thousands
                    // of short-lived objects per tick.
                    val curBy = by  // capture for lambda
                    shape.forAllBoxes { minX, _, minZ, maxX, maxY, maxZ ->
                        if (wx >= bx + minX - 1e-6 && wx <= bx + maxX + 1e-6 &&
                            wz >= bz + minZ - 1e-6 && wz <= bz + maxZ + 1e-6
                        ) {
                            val boxTop = curBy + maxY
                            if (boxTop <= ceil && (best.isNaN() || boxTop > best)) best = boxTop
                        }
                    }
                }
            }
            by--
        }
        return if (best.isNaN()) null else best
    }

    /**
     * 地形贴合角度调整
     * 通过对载具底部采样的地形高度进行最小二乘平面拟合，
     * 计算目标俯仰角和横滚角，使载具贴合地形斜面。
     *
     * 斜率定义（载具局部坐标系，X=右，Z=前）：
     *   slopeX = Σ(lx·heightY) / Σ(lx²) → 横滚角
     *   slopeZ = Σ(lz·heightY) / Σ(lz²) → 俯仰角
     *
     * 角度约定：正xRot = 低头，正roll = 右侧下沉
     */
    @JvmStatic
    fun updateTerrainCompact(entity: VehicleEntity, sumXH: Double, sumZH: Double, sumX2: Double, sumZ2: Double) {
        val rate = entity.data().compute().terrainCompatRotateRate

        val slopeX = if (sumX2 > 0.0) (sumXH / sumX2).coerceIn(-3.0, 3.0) * rate * 2.5 else 0.0
        val slopeZ = if (sumZ2 > 0.0) (sumZH / sumZ2).coerceIn(-3.0, 3.0) * rate * 2.5 else 0.0

        val targetXRot = Mth.clamp((Mth.atan2(slopeZ, 1.0) * Mth.RAD_TO_DEG).toFloat(), -45f, 45f)
        val targetRoll = -Mth.clamp((Mth.atan2(slopeX, 1.0) * Mth.RAD_TO_DEG).toFloat(), -45f, 45f)

        val smoothingFactor = 0.1f
        entity.xRot = lerpAngle(entity.xRot, targetXRot, smoothingFactor)
        entity.setZRot(lerpAngle(entity.roll, targetRoll, smoothingFactor))
    }

    /**
     * 检查载具的任意OBB是否接触地面
     * 将每个OBB向下偏移微小距离后检测与方块的碰撞
     *
     * @param vehicle 载具
     * @return 是否有OBB接触地面
     */
    @JvmStatic
    fun checkObbOnGround(vehicle: VehicleEntity): Boolean {
        val obb = vehicle.getCollisionOBB() ?: return vehicle.onGround()

        val testObb = obb.move(Vec3(0.0, -0.02, 0.0))
        val axes = testObb.getAxes()
        val ext = testObb.extents
        val halfX = Math.abs(axes[0].x) * ext.x + Math.abs(axes[1].x) * ext.y + Math.abs(axes[2].x) * ext.z
        val halfY = Math.abs(axes[0].y) * ext.x + Math.abs(axes[1].y) * ext.y + Math.abs(axes[2].y) * ext.z
        val halfZ = Math.abs(axes[0].z) * ext.x + Math.abs(axes[1].z) * ext.y + Math.abs(axes[2].z) * ext.z
        val searchAABB = AABB(
            testObb.center.x - halfX - 0.15, testObb.center.y - halfY - 0.15, testObb.center.z - halfZ - 0.15,
            testObb.center.x + halfX + 0.15, testObb.center.y + halfY + 0.15, testObb.center.z + halfZ + 0.15
        )
        for (pos in BlockPos.betweenClosedStream(searchAABB)) {
            val state = vehicle.level().getBlockState(pos)
            if (state.isAir) continue
            val shape = state.getCollisionShape(vehicle.level(), pos)
            if (shape.isEmpty) continue
            for (aabb in shape.toAabbs()) {
                if (OBB.isColliding(testObb, aabb.move(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()))) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 检查OBB底面支撑比例，用于防止载具步进下落时卡入小坑洞
     * 采样底面5个点（四角+中心），检查各点正下方是否有方块支撑
     *
     * @param vehicle 载具
     * @param obb 位于目标位置的OBB
     * @return Pair<支撑比例(0.0~1.0), 需要的向上修正量>
     *         修正量 > 0 表示OBB有部分陷入地表以下，需要向上推
     */
    @JvmStatic
    fun checkBottomSupportRatio(vehicle: VehicleEntity, obb: OBB): Pair<Double, Double> {
        val level = vehicle.level()
        val axes = obb.getAxes()
        val center = obb.center
        val ex = obb.extents.x
        val ey = obb.extents.y
        val ez = obb.extents.z

        // 底面5个采样点：四角 + 中心
        val sampleOffsets = listOf(
            Pair(-1.0, -1.0), Pair(-1.0, 1.0), Pair(1.0, -1.0), Pair(1.0, 1.0), Pair(0.0, 0.0)
        )

        val closeThreshold = 1.5  // 方块表面0.5格内视为"接触地表"
        var onSurfaceCount = 0
        var maxPenetration = 0.0  // 采样点低于方块表面的最大深度

        for ((fx, fz) in sampleOffsets) {
            val lx = fx * ex
            val lz = fz * ez

            // 计算底面采样点的世界坐标: center + lx*axis0 + (-ey)*axis1 + lz*axis2
            val wx = center.x + axes[0].x * lx + axes[1].x * (-ey) + axes[2].x * lz
            val wy = center.y + axes[0].y * lx + axes[1].y * (-ey) + axes[2].y * lz
            val wz = center.z + axes[0].z * lx + axes[1].z * (-ey) + axes[2].z * lz

            val p = Vec3(wx, wy, wz)
            val blockPos = BlockPos.containing(p)
            val blockPosBelow = BlockPos.containing(p.add(0.0, -0.02, 0.0))
            var state = level.getBlockState(blockPosBelow)
            var shape = state.getCollisionShape(level, blockPosBelow)

            // 如果正下方没有碰撞，检查当前方块位置
            if (shape.isEmpty) {
                state = level.getBlockState(blockPos)
                shape = state.getCollisionShape(level, blockPos)
            }

            if (!shape.isEmpty) {
                // 使用正确的blockPos（与shape对应的）
                val shapeBlockPos = if (!level.getBlockState(blockPosBelow)
                        .getCollisionShape(level, blockPosBelow).isEmpty
                ) blockPosBelow else blockPos
                val blockTopY = shapeBlockPos.y + shape.max(Direction.Axis.Y)
                val dist = wy - blockTopY  // >0=在地表上方, <0=陷入地表

                if (Math.abs(dist) <= closeThreshold) {
                    onSurfaceCount++
                    if (dist < 0) {
                        maxPenetration = Math.max(maxPenetration, -dist)
                    }
                }
            }
        }

        val ratio = onSurfaceCount.toDouble() / sampleOffsets.size
        return Pair(ratio, maxPenetration)
    }

    @JvmStatic
    fun getWheelsTransform(vehicle: VehicleEntity, partialTicks: Float): Matrix4d {
        val transform = Matrix4d()
        transform.translate(
            Mth.lerp(partialTicks.toDouble(), vehicle.xo, vehicle.x).toFloat().toDouble(),
            Mth.lerp(partialTicks.toDouble(), vehicle.yo, vehicle.y).toFloat().toDouble(),
            Mth.lerp(partialTicks.toDouble(), vehicle.zo, vehicle.z).toFloat().toDouble()
        )
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, vehicle.yRotO, vehicle.yRot)))
        return transform
    }

    /**
     * Resolves OBB-vs-world collisions for [vehicle] using Separating Axis Theorem (SAT).
     *
     * Convenience overload that uses the vehicle's single {@link OBB.Part#COLLISION} OBB.
     * Falls back to vanilla AABB collision when no collision OBB is defined.
     *
     * @param vehicle  the vehicle entity
     * @param movement intended movement vector (gravity already applied by caller)
     * @return corrected movement vector after collision clipping
     */
    @JvmStatic
    fun resolveObbWorldCollision(vehicle: VehicleEntity, movement: Vec3): Vec3 {
        vehicle.updateOBB()

        val collisionObb = vehicle.getCollisionOBB()
        if (collisionObb == null) {
            val aabb = vehicle.boundingBox
            val list = vehicle.level().getEntityCollisions(vehicle, aabb.expandTowards(movement))
            return Entity.collideBoundingBox(vehicle, movement, aabb, vehicle.level(), list)
        }

        return resolveObbWorldCollision(vehicle, movement, listOf(collisionObb))
    }

    /**
     * Resolves OBB-vs-world collisions along Y → X → Z axes using SAT clipping.
     *
     * @param vehicle  the vehicle entity performing collision resolution
     * @param movement the intended movement vector (including gravity) prior to clipping
     * @param obbs     the OBBs to test; must not be empty (caller should handle that)
     * @return the clipped movement vector; components are reduced but never reversed
     *         beyond zero (no bouncing)
     */
    @JvmStatic
    fun resolveObbWorldCollision(vehicle: VehicleEntity, movement: Vec3, obbs: List<OBB>): Vec3 {
        if (movement.lengthSqr() < 1e-7) return movement
        if (obbs.isEmpty()) return Entity.collideBoundingBox(
            vehicle, movement, vehicle.boundingBox, vehicle.level(),
            vehicle.level().getEntityCollisions(vehicle, vehicle.boundingBox.expandTowards(movement))
        )

        // Derive world-space axes once per OBB per tick; orientation is invariant
        // under translation, so this array is reused across all three axis passes.
        val obbsWithAxes = obbs.map { it to it.getAxes() }

        // Build search box: union of all OBB world-AABBs expanded toward movement
        var sMinX =  Double.MAX_VALUE; var sMinY =  Double.MAX_VALUE; var sMinZ =  Double.MAX_VALUE
        var sMaxX = -Double.MAX_VALUE; var sMaxY = -Double.MAX_VALUE; var sMaxZ = -Double.MAX_VALUE
        for (obb in obbs) {
            val a = OBB.getWorldAABB(obb).expandTowards(movement)
            if (a.minX < sMinX) sMinX = a.minX; if (a.minY < sMinY) sMinY = a.minY
            if (a.minZ < sMinZ) sMinZ = a.minZ; if (a.maxX > sMaxX) sMaxX = a.maxX
            if (a.maxY > sMaxY) sMaxY = a.maxY; if (a.maxZ > sMaxZ) sMaxZ = a.maxZ
        }
        val searchBox = AABB(sMinX, sMinY, sMinZ, sMaxX, sMaxY, sMaxZ)
            .inflate(0.5)
            .expandTowards(0.0, vehicle.stepHeight.toDouble() + 0.5, 0.0)

        // Collect candidate AABBs
        //
        // Block AABBs: tick-level cache stored as a flat DoubleArray
        // (6 doubles per AABB: x0,y0,z0,x1,y1,z1).  The cache is valid for the
        // entire vCollide call sequence (base + step-up + step-down) because the
        // world does not change between these sub-calls within a single tick.
        //
        // Entity AABBs: always fresh — cheap (few candidates) and position-sensitive.
        val cachedCoords: DoubleArray
        val cachedCount: Int
        if (vehicle.blockCollisionCacheTick == vehicle.tickCount) {
            cachedCoords = vehicle.blockCollisionCoords
            cachedCount  = vehicle.blockCollisionCount
        } else {
            var buf = if (vehicle.blockCollisionCoords.size >= 1200) vehicle.blockCollisionCoords
                      else DoubleArray(1200)
            var n = 0
            if (!isSearchBoxProvablyAirborne(vehicle.level(), searchBox)) {
                for (shape in vehicle.level().getBlockCollisions(vehicle, searchBox)) {
                    shape.forAllBoxes { x0, y0, z0, x1, y1, z1 ->
                        if (n + 6 > buf.size) buf = buf.copyOf(buf.size * 2)
                        buf[n] = x0; buf[n+1] = y0; buf[n+2] = z0
                        buf[n+3] = x1; buf[n+4] = y1; buf[n+5] = z1
                        n += 6
                    }
                }
            }
            vehicle.blockCollisionCoords     = buf
            vehicle.blockCollisionCount      = n
            vehicle.blockCollisionCacheTick  = vehicle.tickCount
            cachedCoords = buf
            cachedCount  = n
        }

        // Combine block cache + fresh entity collisions into a single AABB list.
        val allAabbs = ArrayList<AABB>(cachedCount / 6 + 4)
        var ci = 0
        while (ci < cachedCount) {
            allAabbs.add(AABB(cachedCoords[ci], cachedCoords[ci+1], cachedCoords[ci+2],
                              cachedCoords[ci+3], cachedCoords[ci+4], cachedCoords[ci+5]))
            ci += 6
        }
        for (shape in vehicle.level().getEntityCollisions(vehicle, searchBox)) {
            shape.forAllBoxes { x0, y0, z0, x1, y1, z1 ->
                allAabbs.add(AABB(x0, y0, z0, x1, y1, z1))
            }
        }
        if (allAabbs.isEmpty()) return movement

        var rx = movement.x
        var ry = movement.y
        var rz = movement.z

        // Ignore penetrations smaller than this; suppresses floating-point noise.
        val minPenetration = 0.005
        // Base allowed penetration along the tangential direction (parallel to surface).
        // The normal direction is always strict to prevent sinking or flying.
        val maxPenetration = 0.1

        // Y-axis pass
        // Translate each OBB by (0, ry, 0) and SAT-test against all candidates.
        for ((obb, axes) in obbsWithAxes) {
            val obbAabb    = OBB.getTranslatedWorldAABB(obb, axes, Vec3(0.0, ry, 0.0))
            val testCenter = Vector3d(obb.center.x, obb.center.y + ry, obb.center.z)
            for (aabb in allAabbs) {
                // Coarse rejection: skip AABBs with no XZ overlap (non-resolving axes).
                if (obbAabb.maxX <= aabb.minX || obbAabb.minX >= aabb.maxX) continue
                if (obbAabb.maxZ <= aabb.minZ || obbAabb.minZ >= aabb.maxZ) continue

                val mtv = OBB.computeObbAabbMtv(testCenter, axes, obb.extents, aabb) ?: continue
                if (Math.abs(mtv.y) < minPenetration) continue

                val mtvLen = Math.sqrt(mtv.x * mtv.x + mtv.y * mtv.y + mtv.z * mtv.z)
                // nY: how much the MTV points in the Y direction.
                // • nY → 1: flat ground contact → strict tolerance, full Y response.
                // • nY → 0: wall contact       → loose tolerance, minimal Y response.
                val nY = Math.abs(mtv.y) / mtvLen
                val effectiveMaxPen = maxPenetration * (1.0 - nY * 0.85)

                if (ry > 0 && mtv.y < 0) {
                    val excess = -mtv.y - effectiveMaxPen
                    if (excess > 0) ry = Math.max(0.0, ry - excess * nY)
                } else if (ry < 0 && mtv.y > 0) {
                    val excess = mtv.y - effectiveMaxPen
                    if (excess > 0) ry = Math.min(0.0, ry + excess * nY)
                } else if (ry <= 0 && mtv.y > 0) {
                    if (mtv.y > effectiveMaxPen) {
                        ry = Math.min(0.0, ry + (mtv.y - effectiveMaxPen) * 0.5 * nY)
                    }
                }
            }
        }

        // X-axis pass
        // Translate each OBB by (rx, ry, 0) and SAT-test against all candidates.
        for ((obb, axes) in obbsWithAxes) {
            val obbAabb    = OBB.getTranslatedWorldAABB(obb, axes, Vec3(rx, ry, 0.0))
            val testCenter = Vector3d(obb.center.x + rx, obb.center.y + ry, obb.center.z)
            for (aabb in allAabbs) {
                // Coarse rejection: skip AABBs with no YZ overlap.
                if (obbAabb.maxY <= aabb.minY || obbAabb.minY >= aabb.maxY) continue
                if (obbAabb.maxZ <= aabb.minZ || obbAabb.minZ >= aabb.maxZ) continue

                val mtv = OBB.computeObbAabbMtv(testCenter, axes, obb.extents, aabb) ?: continue
                if (Math.abs(mtv.x) < minPenetration) continue

                val mtvLen = Math.sqrt(mtv.x * mtv.x + mtv.y * mtv.y + mtv.z * mtv.z)
                val nX = Math.abs(mtv.x) / mtvLen
                val effectiveMaxPen = maxPenetration * (1.0 - nX)

                if (rx > 0 && mtv.x < 0) {
                    val excess = -mtv.x - effectiveMaxPen
                    if (excess > 0) rx = Math.max(0.0, rx - excess * nX)
                } else if (rx < 0 && mtv.x > 0) {
                    val excess = mtv.x - effectiveMaxPen
                    if (excess > 0) rx = Math.min(0.0, rx + excess * nX)
                }
            }
        }

        // Z-axis pass
        // Translate each OBB by (rx, ry, rz) and SAT-test against all candidates.
        for ((obb, axes) in obbsWithAxes) {
            val obbAabb    = OBB.getTranslatedWorldAABB(obb, axes, Vec3(rx, ry, rz))
            val testCenter = Vector3d(obb.center.x + rx, obb.center.y + ry, obb.center.z + rz)
            for (aabb in allAabbs) {
                // Coarse rejection: skip AABBs with no XY overlap.
                if (obbAabb.maxX <= aabb.minX || obbAabb.minX >= aabb.maxX) continue
                if (obbAabb.maxY <= aabb.minY || obbAabb.minY >= aabb.maxY) continue

                val mtv = OBB.computeObbAabbMtv(testCenter, axes, obb.extents, aabb) ?: continue
                if (Math.abs(mtv.z) < minPenetration) continue

                val mtvLen = Math.sqrt(mtv.x * mtv.x + mtv.y * mtv.y + mtv.z * mtv.z)
                val nZ = Math.abs(mtv.z) / mtvLen
                val effectiveMaxPen = maxPenetration * (1.0 - nZ)

                if (rz > 0 && mtv.z < 0) {
                    val excess = -mtv.z - effectiveMaxPen
                    if (excess > 0) rz = Math.max(0.0, rz - excess * nZ)
                } else if (rz < 0 && mtv.z > 0) {
                    val excess = mtv.z - effectiveMaxPen
                    if (excess > 0) rz = Math.min(0.0, rz + excess * nZ)
                }
            }
        }

        return Vec3(rx, ry, rz)
    }

    // Code based on Dragon Rise
    @JvmStatic
    fun towedTick(vehicle: VehicleEntity) {
        val tower = vehicle.towedByEntity

        if (tower == null) {
            vehicle.clearTowingInfo()
            return
        }

        val dist = vehicle.distanceTo(tower)
        val longestSide = calculateLongestSide(vehicle)
        val towerLongestSide = calculateLongestSide(tower)

        val minDist = max(
            VehicleConfig.TOW_PULL_DISTANCE.get().toDouble(),
            longestSide + towerLongestSide + 4.0
        )
        val maxDist = VehicleConfig.TOW_BREAK_DISTANCE.get().toDouble()

        if (dist > maxDist && maxDist > 0) {
            vehicle.clearTowingInfo()
            return
        }

        if (dist <= minDist) return

        val overshoot = dist - minDist
        val dir = vehicle.position().subtract(tower.position()).normalize()
        // 使用双方的相对速度，使阻尼更准确
        val relVelAlong = vehicle.deltaMovement.subtract(tower.deltaMovement).dot(dir)

        val k = 0.2  // 钢索刚性
        val d = 0.01 // 阻尼
        val ropeForce = -k * overshoot - d * relVelAlong

        val towerFactor = tower.computed().towForceFactor.toDouble().coerceAtLeast(0.0)
        val towedMass = vehicle.mass.toDouble().coerceAtLeast(0.01)
        val towerMass = tower.mass.toDouble().coerceAtLeast(0.01)

        val towForce = towerMass * towerFactor * ropeForce / 6.0

        val maxDeltaV = max(2.0, tower.deltaMovement.length())
        val towedScalar = (towForce / towedMass).coerceIn(-maxDeltaV, maxDeltaV)
        val towerScalar = (-ropeForce / towerMass).coerceIn(-maxDeltaV, maxDeltaV)

        var towerDir = dir.scale(towedScalar)

        vehicle.deltaMovement = vehicle.deltaMovement.add(towerDir)
        tower.deltaMovement = tower.deltaMovement.add(dir.scale(towerScalar))

        if (!vehicle.computed().forwardTowed) towerDir = towerDir.scale(-1.0)

        val diffY = Mth.wrapDegrees(
            -VehicleVecUtils.getYRotFromVector(towerDir) + VehicleVecUtils.getYRotFromVector(
                vehicle.getViewVector(1f)
            )
        ).toFloat()
        vehicle.yRot += 0.05f * diffY
    }

    @JvmStatic
    fun towingTick(vehicle: VehicleEntity) {
        val towed = vehicle.towingEntity
        if (towed == null) {
           vehicle.clearTowingInfo()
            return
        }
        if (towed is VehicleEntity) return

        val dist = vehicle.distanceTo(towed)
        val bb = towed.boundingBox
        val longestSide = maxOf(bb.xsize, bb.ysize, bb.zsize)
        val thisLongestSide = calculateLongestSide(vehicle)

        val minDist = max(
            VehicleConfig.TOW_PULL_DISTANCE.get().toDouble(),
            longestSide + thisLongestSide + 1.0
        )
        val maxDist = VehicleConfig.TOW_BREAK_DISTANCE.get().toDouble()

        if (dist > maxDist && maxDist > 0) {
            vehicle.clearTowingInfo()
            return
        }

        if (dist <= minDist) return

        val overshoot = dist - minDist
        val dir = vehicle.position().subtract(towed.position()).reverse().normalize()
        val relVelAlong = towed.deltaMovement.subtract(vehicle.deltaMovement).dot(dir)

        val k = 0.2  // 钢索刚性
        val d = 0.01 // 阻尼
        val ropeForce = -k * overshoot - d * relVelAlong

        val maxDeltaV = max(2.0, vehicle.deltaMovement.length())
        val pullForce = dir.scale((ropeForce / 6.0).coerceIn(-maxDeltaV, maxDeltaV))

        towed.fallDistance = 0f
        val diffY = Mth.wrapDegrees(
            -VehicleVecUtils.getYRotFromVector(pullForce) + VehicleVecUtils.getYRotFromVector(
                towed.getViewVector(1f)
            )
        ).toFloat()

        if (towed is Player && towed.level().isClientSide) {
            towed.deltaMovement = towed.deltaMovement.add(pullForce)
            towed.yRot += 0.05f * diffY
        } else {
            towed.deltaMovement = towed.deltaMovement.add(pullForce)
            towed.yRot += 0.05f * diffY
        }
    }

    @JvmStatic
    fun calculateLongestSide(vehicle: VehicleEntity): Double {
        val obb = vehicle.getCollisionOBB()
        if (obb == null || vehicle.enableAABB()) {
            val bb = vehicle.boundingBox
            return maxOf(bb.xsize, bb.ysize, bb.zsize)
        }
        return maxOf(obb.extents.x, obb.extents.y, obb.extents.z)
    }

    /**
    * Performs a cheap, exact broad-phase rejection test to determine whether a
    * vehicle's collision search volume can possibly contain any world block
    * collision geometry.
    * <p>
    * Instead of decomposing every block's [net.minecraft.world.phys.shapes.VoxelShape]
    * inside the search box (which is what [net.minecraft.world.level.Level.getBlockCollisions]
    * does internally, and is the single most expensive operation observed in profiling —
    * see {@code BlockCollisions#computeNext} / {@code BlockStateBase#getCollisionShape}),
    * this method only performs O(1) [net.minecraft.world.level.levelgen.Heightmap] look-ups
    * per horizontal column covered by the box.
    * <p>
    * The check is <b>exact, not heuristic</b>: {@link Heightmap.Types#MOTION_BLOCKING}
    * stores the Y of the topmost motion-blocking block per column, so if the search box's
    * minimum Y is strictly above that value for every column it covers, there is
    * provably no colliding block inside the box for those columns.
    * <p>
    * If any covered chunk is not loaded, the method conservatively returns {@code false}
    * (i.e. "cannot guarantee airborne") so that the caller falls back to the normal,
    * fully-correct collision query.
    *
    * @param level the level the vehicle resides in.
    * @param box   the world-space search box that would otherwise be passed to
    *              {@link net.minecraft.world.level.Level#getBlockCollisions}.
    * @return {@code true} if it is guaranteed that no block collision shape can be
    *         present inside [box]; {@code false} otherwise (caller must run the full query).
    */
    private fun isSearchBoxProvablyAirborne(level: Level, box: AABB): Boolean {
        val minBlockX = Mth.floor(box.minX)
        val maxBlockX = Mth.floor(box.maxX)
        val minBlockZ = Mth.floor(box.minZ)
        val maxBlockZ = Mth.floor(box.maxZ)
        val thresholdY = box.minY

        // Cap the column scan so pathologically large vehicles never turn this
        // "fast path" into a slow path; falling back to the normal query is always safe.
        val columnCount = (maxBlockX - minBlockX + 1).toLong() * (maxBlockZ - minBlockZ + 1).toLong()
        if (columnCount > MAX_HEIGHTMAP_PROBE_COLUMNS) return false

        var x = minBlockX
        while (x <= maxBlockX) {
            var z = minBlockZ
            while (z <= maxBlockZ) {
                if (!level.hasChunk(x shr 4, z shr 4)) return false
                // MOTION_BLOCKING height is the Y of the first air block above the
                // topmost blocking block, so any solid collision geometry in this
                // column is strictly below this value.
                val topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)
                if (topY >= thresholdY) return false
                z++
            }
            x++
        }
        return true
    }
}
