package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.entity.projectile.C4Entity
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

        val searchBox = vehicle.getCombinedAABB().inflate(0.5)
        val entities = vehicle.level().getEntities(
            EntityTypeTest.forClass(Entity::class.java), searchBox
        ) { entity ->
            entity !== vehicle && entity !== vehicle.getFirstPassenger() && entity.vehicle == null && entity !is C4Entity
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
     * 撞击实体并造成伤害
     *
     * @param vehicle 载具
     */
    @JvmStatic
    fun crushEntities(vehicle: VehicleEntity) {
        if (!vehicle.canCrushEntities()) return
        if (vehicle.isRemoved) return

        val vec3 = vehicle.deltaMovement

        val entities: MutableList<Entity>?
        if (!vehicle.enableAABB()) {
            val frontBox = vehicle.getCombinedAABB()
            entities = vehicle.level().getEntities(
                EntityTypeTest.forClass(Entity::class.java), frontBox
            ) { entity -> entity !== vehicle && entity !== vehicle.getFirstPassenger() && entity!!.vehicle == null }
                .stream().filter { entity ->
                    if (entity.isAlive && vehicle.isInObb(entity, vec3)) {
                        val type = ForgeRegistries.ENTITY_TYPES.getKey(entity.type)
                        return@filter (entity is VehicleEntity || entity is Boat || entity is Minecart || (entity is TurretWreckEntity && entity.tickCount > 5) || (entity is LivingEntity && !(entity is Player && entity.isSpectator))) || VehicleConfig.COLLISION_ENTITY_WHITELIST.get()
                            .contains(type.toString())
                    }
                    false
                }
                .toList()
        } else {
            val frontBox = vehicle.boundingBox.move(vec3)
            entities = vehicle.level().getEntities(
                EntityTypeTest.forClass(Entity::class.java), frontBox
            ) { entity -> entity !== vehicle && entity !== vehicle.getFirstPassenger() && entity!!.vehicle == null }
                .stream().filter { entity ->
                    if (entity.isAlive) {
                        val type = ForgeRegistries.ENTITY_TYPES.getKey(entity.type)
                        return@filter (entity is VehicleEntity || entity is Boat || entity is Minecart || (entity is TurretWreckEntity && entity.tickCount > 5)
                                || (entity is LivingEntity && !(entity is Player && entity.isSpectator)))
                                || VehicleConfig.COLLISION_ENTITY_WHITELIST.get().contains(type.toString())
                    }
                    false
                }
                .toList()
        }

        // TODO 继续优化这个逆天碰撞
        for (entity in entities) {
            val entitySize = entity.boundingBox.size
            val thisSize = vehicle.boundingBox.size
            val f: Double
            val f1: Double

            val v0 = vec3.subtract(entity.deltaMovement)
            if (v0.angleTo(vehicle.position().vectorTo(entity.position())) > 90) return

            if (vehicle.deltaMovement.lengthSqr() < 0.09) return

            // TODO 给非载具实体也设置质量
            if (entity is LivingEntity && entity.hasEffect(ModMobEffects.STRIKE_PROTECTION.get())) {
                continue
            }

            if (entity is VehicleEntity) {
                f = Mth.clamp((entity.mass / vehicle.mass).toDouble(), 0.25, 4.0)
                f1 = Mth.clamp((vehicle.mass / entity.mass).toDouble(), 0.25, 4.0)
            } else {
                f = Mth.clamp(entitySize / thisSize, 0.25, 4.0)
                f1 = Mth.clamp(thisSize / entitySize, 0.25, 4.0)
            }

            val length = v0.length().toFloat()
            var velAdd = v0.normalize().scale(0.8 * length)

            if (length <= 0.3) {
                continue
            }

            vehicle.level().playSound(null, vehicle, ModSounds.VEHICLE_STRIKE.get(), vehicle.soundSource, 1f, 1f)

            if (entity is LivingEntity) {
                entity.forceHurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        vehicle.level().registryAccess(),
                        vehicle,
                        if (vehicle.getFirstPassenger() == null) vehicle else vehicle.getFirstPassenger()
                    ),
                    (f1 * 80 * (Mth.abs(length) - 0.3) * (Mth.abs(length) - 0.3)).toFloat()
                )
            } else {
                entity.hurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        vehicle.level().registryAccess(),
                        vehicle,
                        if (vehicle.getFirstPassenger() == null) vehicle else vehicle.getFirstPassenger()
                    ), (f1 * 60 * (Mth.abs(length) - 0.3) * (Mth.abs(length) - 0.3)).toFloat()
                )
            }

            if (entity !is TargetEntity) {
                vehicle.pushNew(-0.3f * f * velAdd.x, -0.3f * f * velAdd.y, -0.3f * f * velAdd.z)
            }

            if (entity is VehicleEntity) {
                vehicle.hurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        vehicle.level().registryAccess(),
                        entity,
                        if (entity.getFirstPassenger() == null) entity else entity.getFirstPassenger()
                    ), (f * 40 * (Mth.abs(length) - 0.3) * (Mth.abs(length) - 0.3)).toFloat()
                )

                if (!vehicle.enableAABB()) {
                    if (vehicle.isInObb(entity, Vec3.ZERO)) {
                        var thisPos = vehicle.position()
                        var otherPos = entity.position()

                        for (obb in vehicle.getOBBs()) {
                            if (!entity.enableAABB()) {
                                val obbList2 = entity.getOBBs()
                                for (obb2 in obbList2) {
                                    if (OBB.isColliding(obb, obb2)) {
                                        thisPos = OBB.vector3dToVec3(obb.center)
                                        otherPos = OBB.vector3dToVec3(obb2.center)
                                    }
                                }
                            } else {
                                if (OBB.isColliding(obb, entity.boundingBox)) {
                                    thisPos = OBB.vector3dToVec3(obb.center)
                                }
                            }
                        }

                        val toVec = thisPos.add(
                            Vec3(1.0, 1.0, 1.0).scale(
                                (vehicle.getRandom().nextFloat() * 0.01f + 1f).toDouble()
                            )
                        ).vectorTo(otherPos)
                        velAdd = toVec.normalize().scale(Math.max(thisPos.distanceTo(otherPos), 0.0) * 0.01)
                        vehicle.pushNew(-f * velAdd.x, -f * velAdd.y, -f * velAdd.z)
                    }
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
        if (vehicle.enableAABB()) {
            return vehicle.boundingBox
        }

        val obbList = vehicle.getOBBs()

        if (obbList.isEmpty()) {
            return vehicle.boundingBox
        }

        val min = Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)
        val max = Vector3d(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE)

        for (obb in obbList) {
            val axes = obb.getAxes()
            val c = obb.center
            val e = obb.extents

            // OBB在三个世界坐标轴上的半长投影
            // halfX = |axis0.x|*extent.x + |axis1.x|*extent.y + |axis2.x|*extent.z
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
     * 载具在龙牙上行驶时，减速
     *
     * @param vehicle 载具
     */
    @JvmStatic
    fun handleVehicleMoveOnDragonTeeth(vehicle: VehicleEntity) {
        val aabb = vehicle.boundingBox
        val aabb1 = AABB(aabb.minX, aabb.minY - 1.0E-6, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ)
        val pos = vehicle.level().findSupportingBlock(vehicle, aabb1).orElse(null) ?: return

        val state = vehicle.level().getBlockState(pos)
        if (state.`is`(ModBlocks.DRAGON_TEETH.get())) {
            vehicle.power *= 0.8f
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(-0.1, 0.0, -0.1))
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
                        vehicle.deltaMovement.scale(60.0)
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

                        val particleOption = CustomCloudOption(color, 70, 1f + 7f * speed + Math.random().toFloat() * 2, Math.random().toFloat() * -0.12f, false, false)
                        vehicle.addRandomParticle(particleOption, p.add(0.0, 0.2, 0.0).subtract(vehicle.deltaMovement.scale(1.5)), speed, vehicle.level(), 1, vehicle.deltaMovement.scale(60.0))
                    } else {
                        val particleData = BlockParticleOption(ParticleTypes.BLOCK, state)
                        vehicle.addRandomParticle(particleData, p.add(0.0, 0.1, 0.0), 0.2f, vehicle.level(), 0f, 1)

                        if (vehicle.engineInfo is EngineInfo.Track && vehicle.drift() && vehicle.deltaMovement.horizontalDistanceSqr() > 0.0004 && state.`is`(BlockTags.MINEABLE_WITH_PICKAXE)) {
                            vehicle.addRandomParticle(ModParticleTypes.FIRE_STAR.get(), p.add(0.0, 0.1, 0.0), 0.25f, vehicle.level(), 0.08f, 1)
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

                updateTerrainCompact(vehicle, p, heightY)
            }
        } else if (vehicle.isInFluidType) {
            vehicle.xRot *= 0.9f
            vehicle.setZRot(vehicle.roll * 0.9f)
        }
    }

    fun updateTerrainCompact(entity: VehicleEntity, landingTarget: Vec3, heightY: Double) {
        var currentPos = entity.position()
        val aabb = entity.boundingBox
        val aabb1 = AABB(aabb.minX, aabb.minY - 1.0E-6, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ)
        val optional = entity.level().findSupportingBlock(entity, aabb1)
        if (optional.isPresent) {
            currentPos = currentPos.add(currentPos.vectorTo(optional.get().center).scale(0.6))
        }
        val horizontalOffset = Vec3(
            landingTarget.x - currentPos.x,
            0.0,
            landingTarget.z - currentPos.z
        )

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
     * 在指定列(wx,wz)上、以OBB底面高度wy为基准，探测最贴合的地形碰撞面顶部Y。
     *
     * 仅统计落在垂直窗口 [wy - searchDown, wy + searchUp] 内、且该列水平位置确实位于
     * 方块碰撞盒内的碰撞面，取其中最高的顶部（即车身会贴合到的地面）。
     * 直接遍历真实方块碰撞盒(toAabbs)而非高度图，因此对台阶/半砖/楼梯等也精确。
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
                    for (box in shape.toAabbs()) {
                        if (wx >= bx + box.minX - 1e-6 && wx <= bx + box.maxX + 1e-6 &&
                            wz >= bz + box.minZ - 1e-6 && wz <= bz + box.maxZ + 1e-6
                        ) {
                            val boxTop = by + box.maxY
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
     * 使用OBB的世界包围AABB + 原版碰撞逻辑进行载具与世界碰撞检测与解决
     *
     * @param vehicle  载具
     * @param movement 预期移动向量
     * @return 经过碰撞修正后的实际移动向量
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
     * 使用显式OBB列表进行碰撞解决
     * 基于SAT(分离轴定理)直接对OBB与世界AABB做碰撞检测，
     * 计算MTV(最小平移向量)并按Y→X→Z顺序逐轴裁剪运动向量。
     *
     * 相比旧的1x1网格分解法：
     * - 更精确：SAT找到最小穿透方向，阶梯地形边界处MTV由水平轴主导而非Y轴，
     *   因此vCollide的跨步探测能正确工作
     * - 更快速：配合粗过滤(broad phase)，OBB数量级(1-2)取代子AABB数量级(~30)
     *
     * @param vehicle  载具
     * @param movement 预期移动向量
     * @param obbs     参与碰撞的OBB列表
     * @return 经过碰撞修正后的实际移动向量
     */
    @JvmStatic
    fun resolveObbWorldCollision(vehicle: VehicleEntity, movement: Vec3, obbs: List<OBB>): Vec3 {
        if (movement.lengthSqr() < 1e-7) return movement
        if (obbs.isEmpty()) return Entity.collideBoundingBox(
            vehicle, movement, vehicle.boundingBox, vehicle.level(),
            vehicle.level().getEntityCollisions(vehicle, vehicle.boundingBox.expandTowards(movement))
        )

        // 计算搜索范围：所有OBB的世界AABB展开movement后取并集
        var sMinX = Double.MAX_VALUE
        var sMinY = Double.MAX_VALUE
        var sMinZ = Double.MAX_VALUE
        var sMaxX = -Double.MAX_VALUE
        var sMaxY = -Double.MAX_VALUE
        var sMaxZ = -Double.MAX_VALUE
        for (obb in obbs) {
            val obbAabb = OBB.getWorldAABB(obb)
            val expanded = obbAabb.expandTowards(movement)
            if (expanded.minX < sMinX) sMinX = expanded.minX
            if (expanded.minY < sMinY) sMinY = expanded.minY
            if (expanded.minZ < sMinZ) sMinZ = expanded.minZ
            if (expanded.maxX > sMaxX) sMaxX = expanded.maxX
            if (expanded.maxY > sMaxY) sMaxY = expanded.maxY
            if (expanded.maxZ > sMaxZ) sMaxZ = expanded.maxZ
        }
        val searchBox = AABB(sMinX, sMinY, sMinZ, sMaxX, sMaxY, sMaxZ)
            .inflate(0.5)
            .expandTowards(0.0, vehicle.stepHeight.toDouble() + 0.5, 0.0)

        // 收集世界碰撞AABB（方块 + 实体）
        val allAabbs = mutableListOf<AABB>()
        for (shape in vehicle.level().getBlockCollisions(vehicle, searchBox))
            for (aabb in shape.toAabbs()) allAabbs.add(aabb)
        for (shape in vehicle.level().getEntityCollisions(vehicle, searchBox))
            for (aabb in shape.toAabbs()) allAabbs.add(aabb)
        if (allAabbs.isEmpty()) return movement

        var rx = movement.x
        var ry = movement.y
        var rz = movement.z
        val minPenetration = 0.005  // 忽略极小的浮点穿透
        val maxPenetration = 0.1    // 基础允许陷入量：仅在切向（平行于接触面）方向生效
        // 法向（MTV方向）始终严格，避免陷入地面/飞天

        // Y轴：将每个OBB按(0, ry, 0)移动后，SAT检测与世界AABB的碰撞，通过MTV裁剪ry
        for (obb in obbs) {
            val testObb = obb.move(Vec3(0.0, ry, 0.0))
            val obbAabb = OBB.getWorldAABB(testObb)
            for (aabb in allAabbs) {
                // 粗过滤：在非解析轴(XZ)上快速排除无重叠的AABB
                if (obbAabb.maxX <= aabb.minX || obbAabb.minX >= aabb.maxX) continue
                if (obbAabb.maxZ <= aabb.minZ || obbAabb.minZ >= aabb.maxZ) continue
                val mtv = OBB.computeObbAabbMtv(testObb, aabb) ?: continue
                if (Math.abs(mtv.y) < minPenetration) continue
                val mtvLen = Math.sqrt(mtv.x * mtv.x + mtv.y * mtv.y + mtv.z * mtv.z)
                // nY = MTV中Y轴的分量占比，同时用于两个目的：
                // 1) effectiveMaxPen：法向严格(nY→1则容差→0)，切向软(nY→0则容差→满)
                // 2) nY作为响应缩放：nY高=地面接触→Y全响应；nY低=墙接触→Y响应趋零，避免突变弹出
                val nY = Math.abs(mtv.y) / mtvLen
                // nY→1(平坦地面接触)时允许1.5cm微小穿透作为稳定缓冲区，防止OBB在接地时微反弹
                // nY→0(墙面接触)时保持10cm完整容差，让水平碰撞平滑
                val effectiveMaxPen = maxPenetration * (1.0 - nY * 0.85)

                if (ry > 0 && mtv.y < 0) {
                    val excess = -mtv.y - effectiveMaxPen
                    if (excess > 0) ry = Math.max(0.0, ry - excess * nY)
                } else if (ry < 0 && mtv.y > 0) {
                    val excess = mtv.y - effectiveMaxPen
                    if (excess > 0) ry = Math.min(0.0, ry + excess * nY)
                } else if (ry <= 0 && mtv.y > 0) {
                    if (mtv.y > effectiveMaxPen) {
                        val excess = mtv.y - effectiveMaxPen
                        ry = Math.min(0.0, ry + excess * 0.5 * nY)
                    }
                }
            }
        }

        // X轴：将每个OBB按(rx, ry, 0)移动后检测
        for (obb in obbs) {
            val testObb = obb.move(Vec3(rx, ry, 0.0))
            val obbAabb = OBB.getWorldAABB(testObb)
            for (aabb in allAabbs) {
                // 粗过滤：在非解析轴(YZ)上快速排除
                if (obbAabb.maxY <= aabb.minY || obbAabb.minY >= aabb.maxY) continue
                if (obbAabb.maxZ <= aabb.minZ || obbAabb.minZ >= aabb.maxZ) continue
                val mtv = OBB.computeObbAabbMtv(testObb, aabb) ?: continue
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

        // Z轴：将每个OBB按(rx, ry, rz)移动后检测
        for (obb in obbs) {
            val testObb = obb.move(Vec3(rx, ry, rz))
            val obbAabb = OBB.getWorldAABB(testObb)
            for (aabb in allAabbs) {
                // 粗过滤：在非解析轴(XY)上快速排除
                if (obbAabb.maxX <= aabb.minX || obbAabb.minX >= aabb.maxX) continue
                if (obbAabb.maxY <= aabb.minY || obbAabb.minY >= aabb.maxY) continue
                val mtv = OBB.computeObbAabbMtv(testObb, aabb) ?: continue
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
}
